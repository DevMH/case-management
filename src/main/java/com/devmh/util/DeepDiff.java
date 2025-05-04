package com.devmh.util;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class DeepDiff {

    private static final Logger log = LoggerFactory.getLogger(DeepDiff.class);

    private final Set<String> includedFields;
    private final Set<String> excludedFields;

    public DeepDiff(Set<String> includedFields, Set<String> excludedFields) {
        this.includedFields = includedFields != null ? includedFields : new HashSet<>();
        this.excludedFields = excludedFields != null ? excludedFields : new HashSet<>();
    }

    public record Difference(String path, Object value1, Object value2) {

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

    public List<Difference> diff(Object a, Object b) {
        Map<String, Object> flatA = new LinkedHashMap<>();
        Map<String, Object> flatB = new LinkedHashMap<>();
        flatten(a, "", flatA);
        flatten(b, "", flatB);

        if (!includedFields.isEmpty()) {
            flatA.keySet().retainAll(includedFields);
            flatB.keySet().retainAll(includedFields);
        }
        flatA.keySet().removeAll(excludedFields);
        flatB.keySet().removeAll(excludedFields);

        MapDifference<String, Object> diff = Maps.difference(flatA, flatB);
        List<Difference> result = new ArrayList<>();

        diff.entriesOnlyOnLeft().forEach((k, v) -> result.add(new Difference(k, v, null)));
        diff.entriesOnlyOnRight().forEach((k, v) -> result.add(new Difference(k, null, v)));
        diff.entriesDiffering().forEach((k, v) -> result.add(new Difference(k, v.leftValue(), v.rightValue())));

        return result;
    }

    public DifferenceSummary summarizeDifferences(Object a, Object b) {
        return new DifferenceSummary(diff(a, b));
    }

    private void flatten(Object obj, String path, Map<String, Object> map) {
        if (obj == null || path.contains("[recursive]")) return;

        if (obj instanceof String || obj instanceof Number || obj instanceof Boolean || obj.getClass().isPrimitive()) {
            map.put(path, obj);
            return;
        }

        if (obj instanceof Collection<?> col) {
            int i = 0;
            for (Object item : col) {
                flatten(item, path + "[" + i++ + "]", map);
            }
            return;
        }

        if (obj instanceof Map<?, ?> rawMap) {
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                flatten(entry.getValue(), path + "[" + entry.getKey() + "]", map);
            }
            return;
        }

        for (Field field : getAllFields(obj.getClass())) {
            field.setAccessible(true);
            try {
                Object val = field.get(obj);
                flatten(val, path.isEmpty() ? field.getName() : path + "." + field.getName(), map);
            } catch (Exception e) {
                log.warn("Failed to access field {}: {}", field.getName(), e.toString());
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
