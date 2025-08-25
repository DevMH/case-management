package com.devmh.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.*;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.time.*;
import java.util.*;

public class FieldDescriptorCoverageChecker {

    private final ObjectMapper mapper;
    private final FieldDescriptorProvider provider;

    public <T extends FieldDescriptorProvider> FieldDescriptorCoverageChecker(
            ObjectMapper mapper, T fieldDescriptorProvider) {
        this.mapper = mapper;
        this.provider = fieldDescriptorProvider;
    }

    public record CoverageReport(Set<String> expectedKeys,
                                 Set<String> missingKeys,
                                 Set<String> extraKeys) { }

    public <T extends FieldDescriptorProvider> CoverageReport checkCoverage() {
        Set<String> expected = computeExpectedKeys(provider.getClass(), provider.getRootFieldName());
        Set<String> missing = diff(expected, provider.getFieldDescriptors().keySet());
        Set<String> extra = diff(provider.getFieldDescriptors().keySet(), expected);
        return new CoverageReport(expected, missing, extra);
    }

    private static Set<String> diff(Set<String> a, Set<String> b) {
        Set<String> out = new TreeSet<>(a);
        out.removeAll(b);
        return out;
    }

    private Set<String> computeExpectedKeys(Class<?> rootClass, String rootKey) {
        Set<String> keys = new TreeSet<>();
        keys.add(rootKey);

        Deque<PathNode> stack = new ArrayDeque<>();
        stack.push(new PathNode(rootKey, rootClass, false));

        while (!stack.isEmpty()) {
            PathNode current = stack.pop();

            BeanDescription bd = mapper.getSerializationConfig()
                    .introspect(TypeFactory.defaultInstance().constructType(current.type()));

            for (BeanPropertyDefinition prop : bd.findProperties()) {
                if (prop.getPrimaryMember() != null &&
                        prop.getPrimaryMember().hasAnnotation(JsonIgnore.class)) {
                    continue;
                }

                String propName = prop.getName();
                JavaType propType = prop.getPrimaryType();
                Class<?> raw = propType.getRawClass();

                String ownerKey = current.pathKey();
                boolean isCollection = isCollectionType(raw);

                if (isScalar(raw)) {
                    keys.add(ownerKey + "/" + propName);
                } else if (isCollection) {
                    keys.add(ownerKey + "/" + propName);
                    String itemKey = ownerKey + "/" + propName + "[]";
                    keys.add(itemKey);

                    Class<?> elem = elementType(propType);
                    if (isScalar(elem)) {
                        keys.add(itemKey);
                    } else {
                        stack.push(new PathNode(itemKey, elem, true));
                    }
                } else if (raw.isEnum()) {
                    keys.add(ownerKey + "/" + propName);
                } else if (isJavaTime(raw) || UUID.class.equals(raw)) {
                    keys.add(ownerKey + "/" + propName);
                } else {
                    String nestedOwner = ownerKey + "/" + propName;
                    keys.add(nestedOwner);
                    stack.push(new PathNode(nestedOwner, raw, false));
                }
            }
        }
        return keys;
    }

    private static final class PathNode {
        private final String pathKey;   // e.g., "case/findings[]"
        private final Class<?> type;
        private final boolean collectionItem;

        private PathNode(String pathKey, Class<?> type, boolean collectionItem) {
            this.pathKey = pathKey;
            this.type = type;
            this.collectionItem = collectionItem;
        }
        String pathKey() {
            return pathKey;
        }

        Class<?> type() {
            return type;
        }

        boolean collectionItem() {
            return collectionItem;
        }
    }

    // ---- Type helpers ----

    private static boolean isCollectionType(Class<?> c) {
        return Collection.class.isAssignableFrom(c) || c.isArray();
    }

    private static Class<?> elementType(JavaType jt) {
        if (jt.isArrayType()) {
            return jt.getContentType().getRawClass();
        }
        if (jt.isCollectionLikeType()) {
            return jt.getContentType().getRawClass();
        }
        return Object.class;
    }

    private static boolean isScalar(Class<?> c) {
        return c.isPrimitive()
                || Number.class.isAssignableFrom(c)
                || CharSequence.class.isAssignableFrom(c)
                || Boolean.class.equals(c)
                || UUID.class.equals(c)
                || c.isEnum()
                || isJavaTime(c);
    }

    private static boolean isJavaTime(Class<?> c) {
        return c.equals(LocalDate.class) || c.equals(LocalDateTime.class) ||
                c.equals(Instant.class) || c.equals(OffsetDateTime.class) ||
                c.equals(ZonedDateTime.class);
    }
}
