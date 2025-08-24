package com.devmh.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PatchFormatter {

    public enum Op { add, remove, replace, move, copy }

    public static final class FieldDescriptor {
        public final String label;
        public final boolean isCollection;
        public final String possessive;

        public FieldDescriptor(String label, boolean isCollection, String possessive) {
            this.label = label;
            this.isCollection = isCollection;
            this.possessive = possessive;
        }
        public static FieldDescriptor scalar(String label) { return new FieldDescriptor(label, false, null); }
        public static FieldDescriptor collection(String label) { return new FieldDescriptor(label, true, null); }
    }

    private final ObjectMapper mapper;
    private final Map<String, FieldDescriptor> dictionary;

    public PatchFormatter(ObjectMapper mapper, Map<String, FieldDescriptor> dictionary) {
        this.mapper = mapper;
        this.dictionary = dictionary;
    }

    public List<String> format(String jsonPatchArray) {
        try {
            JsonNode arr = mapper.readTree(jsonPatchArray);
            if (!arr.isArray()) return List.of("Invalid patch payload (not an array).");
            List<String> out = new ArrayList<>();
            for (JsonNode op : arr) out.add(formatOp(op));
            return out;
        } catch (Exception e) {
            return List.of("Failed to parse patch: " + e.getMessage());
        }
    }

    private String formatOp(JsonNode opNode) {
        String op = text(opNode, "op");
        String pathRaw = text(opNode, "path");
        String fromRaw = text(opNode, "from");
        JsonNode value = opNode.get("value");

        List<PathSegment> path = parsePath(pathRaw);
        List<PathSegment> from = fromRaw != null ? parsePath(fromRaw) : null;

        return switch (Op.valueOf(op)) {
            case add     -> describeAdd(path, value);
            case replace -> describeReplace(path, value);
            case remove  -> describeRemove(path);
            case move    -> describeMove(from, path);
            case copy    -> describeCopy(from, path);
        };
    }

    // --- Generators per op ---

    private String describeAdd(List<PathSegment> path, JsonNode value) {
        var ctx = context(path);
        String valStr = valueToPhrase(value);
        if (ctx.fieldIsCollectionElement) {
            String nth = ordinal(ctx.collectionIndex);
            return String.format("Added the %s %s to the %s.", nth, ctx.leafLabel, ctx.ownerPossessive);
        }
        return String.format("The %s %s was set to %s.", ctx.ownerPossessive, ctx.leafLabel, valStr);
    }

    private String describeReplace(List<PathSegment> path, JsonNode value) {
        var ctx = context(path);
        String valStr = valueToPhrase(value);
        if (ctx.fieldIsCollectionElement) {
            String nth = ordinal(ctx.collectionIndex);
            return String.format("The %s %s was changed to %s.", nth, ctx.leafLabel, valStr);
        }
        return String.format("The %s %s was set to %s.", ctx.ownerPossessive, ctx.leafLabel, valStr);
    }

    private String describeRemove(List<PathSegment> path) {
        var ctx = context(path);
        if (ctx.fieldIsCollectionElement) {
            String nth = ordinal(ctx.collectionIndex);
            return String.format("Removed the %s %s.", nth, ctx.leafLabel);
        }
        return String.format("Removed the %s %s.", ctx.ownerPossessive, ctx.leafLabel);
    }

    private String describeMove(List<PathSegment> from, List<PathSegment> to) {
        var fromCtx = context(from);
        var toCtx = context(to);
        return String.format(
                "Moved the %s %s to the %s %s.",
                fromCtx.ownerPossessive, fromCtx.leafLabel, toCtx.ownerPossessive, toCtx.leafLabel
        );
    }

    private String describeCopy(List<PathSegment> from, List<PathSegment> to) {
        var fromCtx = context(from);
        var toCtx = context(to);
        return String.format(
                "Copied the %s %s to the %s %s.",
                fromCtx.ownerPossessive, fromCtx.leafLabel, toCtx.ownerPossessive, toCtx.leafLabel
        );
    }

    // --- Context derivation ---

    private static final class Context {
        String ownerPossessive;
        String leafLabel;
        boolean fieldIsCollectionElement;
        int collectionIndex;
    }

    private Context context(List<PathSegment> path) {
        Context c = new Context();
        if (path.isEmpty()) {
            c.ownerPossessive = "resource's";
            c.leafLabel = "value";
            return c;
        }
        PathSegment leaf = path.get(path.size() - 1);

        List<PathSegment> ownerPath = path.subList(0, path.size() - 1);
        String ownerKey = joinKey(ownerPath);
        FieldDescriptor ownerDesc = dictionary.getOrDefault(ownerKey, FieldDescriptor.scalar(ownerKey));
        c.ownerPossessive = ownerDesc.possessive != null ? ownerDesc.possessive : ownerDesc.label + "'s";

        String leafKey = joinKey(path);
        FieldDescriptor leafDesc = dictionary.getOrDefault(leafKey, FieldDescriptor.scalar(leaf.name));
        c.leafLabel = leafDesc.label;

        if (leaf.index != null) {
            c.fieldIsCollectionElement = true;
            c.collectionIndex = leaf.index;
        } else if (!ownerPath.isEmpty()) {
            PathSegment ownerLeaf = ownerPath.get(ownerPath.size() - 1);
            FieldDescriptor ownerLeafDesc = dictionary.get(joinKey(ownerPath));
            if (ownerLeaf.index != null || (ownerLeafDesc != null && ownerLeafDesc.isCollection)) {
                c.fieldIsCollectionElement = true;
                c.collectionIndex = ownerLeaf.index != null ? ownerLeaf.index : 0;
            }
        }
        return c;
    }

    private static String ordinal(int i) {
        int n = i + 1;
        if (n % 100 >= 11 && n % 100 <= 13) return n + "th";
        return switch (n % 10) {
            case 1 -> "first";
            case 2 -> "second";
            case 3 -> "third";
            default -> n + "th";
        };
    }

    private static String valueToPhrase(JsonNode v) {
        if (v == null || v.isNull()) return "null";
        if (v.isTextual()) return "'" + v.asText() + "'";
        if (v.isNumber() || v.isBoolean()) return v.asText();
        return "`" + v + "`";
    }

    private record PathSegment(String name, Integer index) {}

    private static final Pattern BRACKET = Pattern.compile("^(.*)\\[(\\d+)]$");

    private List<PathSegment> parsePath(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        String p = raw.startsWith("/") ? raw.substring(1) : raw;
        List<String> parts = Arrays.stream(p.split("/"))
                .filter(s -> !s.isEmpty())
                .toList();
        List<PathSegment> out = new ArrayList<>();
        for (String part : parts) {
            Matcher m = BRACKET.matcher(part);
            if (m.matches()) {
                out.add(new PathSegment(m.group(1), Integer.parseInt(m.group(2))));
            } else if (part.matches("\\d+")) {
                out.add(new PathSegment("_", Integer.parseInt(part)));
            } else {
                out.add(new PathSegment(part, null));
            }
        }
        return out;
    }

    private static String joinKey(List<PathSegment> segs) {
        return segs.stream().map(s ->
                s.index == null ? s.name : s.name + "[]"
        ).collect(Collectors.joining("/"));
    }

    private static String text(JsonNode n, String field) {
        return n.has(field) ? n.get(field).asText() : null;
    }

    public static Map<String, FieldDescriptor> defaultDictionary() {
        Map<String, FieldDescriptor> m = new HashMap<>();
        m.put("case", new FieldDescriptor("case", false, "case's"));
        m.put("case/team", new FieldDescriptor("case team", false, "case team's"));
        m.put("case/findings", FieldDescriptor.collection("findings"));
        m.put("case/team/teamMember[]", FieldDescriptor.collection("team members"));
        m.put("case/name", FieldDescriptor.scalar("name"));
        m.put("case/state", FieldDescriptor.scalar("approval state"));
        m.put("case/docket/name", FieldDescriptor.scalar("docket name"));
        m.put("case/team/teamMember[]/firstName", FieldDescriptor.scalar("first name"));
        m.put("case/team/teamMember[]/lastName", FieldDescriptor.scalar("last name"));
        m.put("case/findings[]/startDate", FieldDescriptor.scalar("finding start date"));
        m.put("case/findings[]/endDate", FieldDescriptor.scalar("finding end date"));
        m.put("case/findings[]/sensitive", FieldDescriptor.scalar("finding sensitivity"));
        return m;
    }
}
