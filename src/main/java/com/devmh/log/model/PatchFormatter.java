package com.devmh.log.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Formats JSON Patch (RFC6902) operations into human-readable sentences
 * using a FieldDescriptorProvider.
 * Expected op object fields: op, path, (value|from)
 */
@RequiredArgsConstructor
public class PatchFormatter {

    private final FieldDescriptorProvider provider;

    public List<String> format(JsonNode patchArray) {
        if (patchArray == null || !patchArray.isArray()) return List.of();
        Map<String, FieldDescriptor> dict = provider.getFieldDescriptors();
        List<String> out = new ArrayList<>();
        for (JsonNode opNode : patchArray) {
            String op = text(opNode, "op");
            String path = text(opNode, "path");
            String from = text(opNode, "from");
            JsonNode value = opNode.get("value");
            String sentence = formatOne(op, path, from, value, dict);
            out.add(sentence);
        }
        return out;
    }

    public String formatOne(String opRaw, String jsonPointer, String from, JsonNode value, Map<String, FieldDescriptor> dict) {
        PatchOperation op = PatchOperation.valueOf(opRaw);
        List<PathSegment> segments = parsePointer(jsonPointer);
        String key = joinKey(segments);
        FieldDescriptor field = dict.get(key);
        String ownerPhrase = ownerPhrase(segments, dict);
        String fieldLabel = field != null ? field.label() : lastSegmentName(segments);

        return switch (op) {
            case add -> ownerPhrase + fieldLabel + " was set to " + pretty(value) + ".";
            case replace -> ownerPhrase + fieldLabel + " was changed to " + pretty(value) + ".";
            case remove -> ownerPhrase + fieldLabel + " was removed.";
            case move -> {
                String fromOwner = ownerPhrase(parsePointer(from), dict);
                yield ownerPhrase + fieldLabel + " was moved from " + fromOwner + " to this location.";
            }
            case copy -> {
                String fromOwner = ownerPhrase(parsePointer(from), dict);
                yield ownerPhrase + fieldLabel + " was copied from " + fromOwner + ".";
            }
            case test -> ownerPhrase + fieldLabel + " should equal " + pretty(value) + ".";
        };
    }

    // Owner phrase: e.g., "The case's " or "The case team's first team member's "
    private String ownerPhrase(List<PathSegment> segments, Map<String, FieldDescriptor> dict) {
        if (segments.isEmpty()) return "";
        List<PathSegment> owner = segments.subList(0, segments.size()-1);
        if (owner.isEmpty()) return "";
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < owner.size(); i++) {
            List<PathSegment> prefix = owner.subList(0, i+1);
            String keyWithIdx = joinKey(prefix);
            String keyNoIdx = joinKeyNoIndex(prefix);
            FieldDescriptor d = dict.get(keyWithIdx);
            if (d == null) {
                d = dict.get(keyNoIdx);
            }
            PathSegment seg = owner.get(i);
            String phrase;
            if (d != null && d.possessive() != null) {
                // Explicit possessive overrides any earlier chain (avoid "the case's the case team's ...")
                parts.clear();
                phrase = "the " + d.possessive() + " ";
            } else if (d != null && d.collection() && seg.collectionIndex != null) {
                // Indexed collection: ordinal + singular label
                phrase = ordinal(seg.collectionIndex) + " " + d.label() + "'s ";
            } else if (d != null) {
                phrase = "the " + d.label() + "'s ";
            } else {
                // Fallback on raw segment name
                phrase = (seg.collectionIndex != null ? ordinal(seg.collectionIndex) + " " + seg.name : "the " + seg.name) + "'s ";
            }
            parts.add(phrase);
        }
        String chain = String.join("", parts).replace("'s 's", "'s ");
        return chain.isEmpty() ? "" : chain.substring(0,1).toUpperCase() + chain.substring(1);
    }

    private List<PathSegment> parsePointer(String pointer) {
        if (pointer == null || pointer.isEmpty() || "/".equals(pointer)) return List.of();
        if (pointer.charAt(0) != '/') throw new IllegalArgumentException("Pointer must start with '/': " + pointer);
        String[] raw = pointer.substring(1).split("/");
        List<PathSegment> segs = new ArrayList<>();
        for (String r : raw) {
            String token = unescape(r);
            if (isNonNegativeInt(token) && !segs.isEmpty()) {
                PathSegment prev = segs.get(segs.size()-1);
                if (prev.collectionIndex == null) {
                    prev.collectionIndex = Integer.parseInt(token);
                    continue;
                }
            }
            segs.add(new PathSegment(token));
        }
        return segs;
    }

    private static String lastSegmentName(List<PathSegment> segments) {
        return segments.isEmpty() ? "" : segments.get(segments.size()-1).name;
    }

    private static boolean isNonNegativeInt(String s) {
        for (int i=0; i<s.length(); i++) {
            char c = s.charAt(i);
            if (c<'0'||c>'9') {
                return false;
            }
        }
        return !s.isEmpty();
    }

    private static String unescape(String token) {
        return token.replace("~1", "/").replace("~0", "~");
    }

    private static String pretty(JsonNode node) {
        if (node == null || node.isNull()) return "null";
        if (node.isTextual()) return '"' + node.asText() + '"';
        if (node.isNumber() || node.isBoolean()) return node.asText();
        return node.toString();
    }

    private static String joinKey(List<PathSegment> segs) {
        return segs.stream()
                .map(s -> s.collectionIndex == null ? s.name : s.name + "[]")
                .collect(Collectors.joining("/"));
    }

    private static String joinKeyNoIndex(List<PathSegment> segs) {
        return segs.stream()
                .map(s -> s.name)
                .collect(Collectors.joining("/"));
    }

    private static String ordinal(int n) {
        int a = n % 100;
        int b = n % 10;
        String suf = (a - b == 10) ? "th" :
                (b == 1 ? "st" : b == 2 ? "nd" : b == 3 ? "rd" : "th");
        return switch (n) {
            case 0 -> "first";
            case 1 -> "second";
            case 2 -> "third";
            default -> (n+1) + suf;
        };
    }

    private static String text(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return n == null || n.isNull() ? null : n.asText();
    }

    private static final class PathSegment {
        final String name;
        Integer collectionIndex;
        PathSegment(String name) {
            this.name = name;
        }
    }
}
