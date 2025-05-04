package com.devmh.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class DeepDiffUtil {

    private static final Logger log = LoggerFactory.getLogger(DeepDiffUtil.class);

    private final Set<String> includedFields;
    private final Set<String> excludedFields;
    private final Set<ComparedPair> visited = new HashSet<>();
    private final List<String> skippedPaths = new ArrayList<>();

    public DeepDiffUtil(Set<String> includedFields, Set<String> excludedFields) {
        this.includedFields = includedFields != null ? includedFields : new HashSet<>();
        this.excludedFields = excludedFields != null ? excludedFields : new HashSet<>();
    }

    public List<String> getSkippedPaths() {
        return skippedPaths;
    }

    public static class Difference {
        public final String path;
        public final Object value1;
        public final Object value2;

        public Difference(String path, Object value1, Object value2) {
            this.path = path;
            this.value1 = value1;
            this.value2 = value2;
        }

        public boolean existsInLeftOnly() {
            return value1 != null && value2 == null;
        }

        public boolean existsInRightOnly() {
            return value1 == null && value2 != null;
        }

        public boolean existsInBoth() {
            return value1 != null && value2 != null;
        }

        public boolean missingInBoth() {
            return value1 == null && value2 == null;
        }

        @Override
        public String toString() {
            return path + ": " + value1 + " != " + value2;
        }
    }

    public static class DifferenceSummary {
        public final List<Difference> leftOnly;
        public final List<Difference> rightOnly;
        public final List<Difference> both;
        public final List<Difference> neither;

        public DifferenceSummary(List<Difference> diffs) {
            this.leftOnly = diffs.stream().filter(Difference::existsInLeftOnly).collect(Collectors.toList());
            this.rightOnly = diffs.stream().filter(Difference::existsInRightOnly).collect(Collectors.toList());
            this.both = diffs.stream().filter(Difference::existsInBoth).collect(Collectors.toList());
            this.neither = diffs.stream().filter(Difference::missingInBoth).collect(Collectors.toList());
        }

        public void printSummary() {
            printGroup("Fields only in left:", leftOnly);
            printGroup("Fields only in right:", rightOnly);
            printGroup("Fields differing in both:", both);
            printGroup("Fields missing in both:", neither);
        }

        private void printGroup(String title, List<Difference> group) {
            System.out.println("\n" + title);
            if (group.isEmpty()) {
                System.out.println("  (none)");
            } else {
                group.forEach(d -> System.out.println("  " + d));
            }
        }
    }

    private static class ComparedPair {
        private final Object obj1;
        private final Object obj2;

        ComparedPair(Object o1, Object o2) {
            this.obj1 = o1;
            this.obj2 = o2;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ComparedPair)) return false;
            ComparedPair that = (ComparedPair) o;
            return (this.obj1 == that.obj1 && this.obj2 == that.obj2);
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(obj1) ^ System.identityHashCode(obj2);
        }
    }

    public List<Difference> diff(Object a, Object b) {
        List<Difference> diffs = new ArrayList<>();
        diffInternal(a, b, "", diffs);
        if (!skippedPaths.isEmpty()) {
            log.warn("DeepDiff skipped {} paths:", skippedPaths.size());
            skippedPaths.forEach(p -> log.warn(" - {}", p));
        }
        return diffs;
    }

    public DifferenceSummary summarizeDifferences(Object a, Object b) {
        return new DifferenceSummary(diff(a, b));
    }

    private void diffInternal(Object a, Object b, String path, List<Difference> diffs) {
        if (a == b) return;
        if (a == null || b == null || !a.getClass().equals(b.getClass())) {
            diffs.add(new Difference(path, a, b));
            return;
        }

        if (visited.contains(new ComparedPair(a, b))) return;
        visited.add(new ComparedPair(a, b));

        Class<?> clazz = a.getClass();

        if (clazz.isPrimitive() || a instanceof Number || a instanceof String || a instanceof Boolean) {
            if (!a.equals(b)) {
                diffs.add(new Difference(path, a, b));
            }
            return;
        }

        if (a instanceof Collection<?> colA && b instanceof Collection<?> colB) {
            if (colA.size() != colB.size()) {
                diffs.add(new Difference(path + ".size", colA.size(), colB.size()));
            } else if (a instanceof List && b instanceof List) {
                int index = 0;
                Iterator<?> itA = colA.iterator();
                Iterator<?> itB = colB.iterator();
                while (itA.hasNext()) {
                    diffInternal(itA.next(), itB.next(), path + "[" + index++ + "]", diffs);
                }
            } else if (a instanceof Set && b instanceof Set) {
                Set<?> setA = new HashSet<>(colA);
                Set<?> setB = new HashSet<>(colB);
                if (!setA.equals(setB)) {
                    diffs.add(new Difference(path, setA, setB));
                }
            }
            return;
        }

        if (a instanceof Map<?, ?> mapA && b instanceof Map<?, ?> mapB) {
            Set<Object> allKeys = new HashSet<>();
            allKeys.addAll(mapA.keySet());
            allKeys.addAll(mapB.keySet());
            for (Object key : allKeys) {
                diffInternal(mapA.get(key), mapB.get(key), path + "[" + key + "]", diffs);
            }
            return;
        }

        for (Field field : getAllFields(clazz)) {
            field.setAccessible(true);
            String fieldName = path.isEmpty() ? field.getName() : path + "." + field.getName();

            if (!includedFields.isEmpty() && !includedFields.contains(fieldName)) continue;
            if (excludedFields.contains(fieldName)) continue;

            try {
                Object valA = field.get(a);
                Object valB = field.get(b);
                diffInternal(valA, valB, fieldName, diffs);
            } catch (IllegalAccessException e) {
                skippedPaths.add("Access error at: " + fieldName);
                log.warn("Access error at {}: {}", fieldName, e.toString());
            } catch (Exception e) {
                skippedPaths.add("Failed to compare field: " + fieldName + " (" + e.getClass().getSimpleName() + ")");
                log.warn("Failed to compare field {}: {}", fieldName, e.toString());
            }
        }
    }

    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }
}
