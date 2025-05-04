package com.devmh.util;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.*;

@Slf4j
public class DeepDiff {

    private final Set<String> includedFields;
    private final Set<String> excludedFields;

    public DeepDiff(Set<String> includedFields, Set<String> excludedFields) {
        this.includedFields = includedFields != null ? includedFields : new HashSet<>();
        this.excludedFields = excludedFields != null ? excludedFields : new HashSet<>();
    }

    public MapDifference<String, Object> diff(Object a, Object b) {
        Map<String, Object> flatA = new LinkedHashMap<>();
        Map<String, Object> flatB = new LinkedHashMap<>();
        Set<Object> visitedA = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Object> visitedB = Collections.newSetFromMap(new IdentityHashMap<>());
        flatten(a, "", flatA, visitedA);
        flatten(b, "", flatB, visitedB);

        if (!includedFields.isEmpty()) {
            flatA.keySet().retainAll(includedFields);
            flatB.keySet().retainAll(includedFields);
        }
        flatA.keySet().removeAll(excludedFields);
        flatB.keySet().removeAll(excludedFields);

        return Maps.difference(flatA, flatB);
    }

    private void flatten(Object obj, String path, Map<String, Object> map, Set<Object> visited) {
        if (obj == null || visited.contains(obj)) return;
        visited.add(obj);

        if (obj instanceof String || obj instanceof Number || obj instanceof Boolean || obj.getClass().isPrimitive()) {
            map.put(path, obj);
            return;
        }

        if (obj instanceof Collection<?> col) {
            int i = 0;
            for (Object item : col) {
                flatten(item, path + "[" + i++ + "]", map, visited);
            }
            return;
        }

        if (obj instanceof Map<?, ?> rawMap) {
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                flatten(entry.getValue(), path + "[" + entry.getKey() + "]", map, visited);
            }
            return;
        }

        for (Field field : getAllFields(obj.getClass())) {
            field.setAccessible(true);
            try {
                Object val = field.get(obj);
                flatten(val, path.isEmpty() ? field.getName() : path + "." + field.getName(), map, visited);
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
