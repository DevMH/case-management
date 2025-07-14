package com.devmh.controller;

import com.devmh.model.Case;
import com.devmh.model.Finding;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.mergepatch.JsonMergePatch;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

class PatchUtils {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static <T> T applyPatch(JsonPatch patch, T targetBean, Class<T> beanClass) {
        try {
            JsonNode patched = patch.apply(mapper.convertValue(targetBean, JsonNode.class));
            return mapper.treeToValue(patched, beanClass);
        } catch (JsonPatchException | com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JSON Patch", e);
        }
    }

    public static <T> T applyMergePatch(JsonMergePatch mergePatch, T targetBean, Class<T> beanClass) {
        try {
            JsonNode target = mapper.convertValue(targetBean, JsonNode.class);
            JsonNode patched = mergePatch.apply(target);
            return mapper.treeToValue(patched, beanClass);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Merge Patch", e);
        }
    }

    public static List<String> diffCases(Case original, Case updated) {
        try {
            List<String> diffs = new ArrayList<>();

            if (!Objects.equals(original.getName(), updated.getName()))
                diffs.add("name changed from '" + original.getName() + "' to '" + updated.getName() + "'");

            if (!Objects.equals(original.getState(), updated.getState()))
                diffs.add("state changed from '" + original.getState() + "' to '" + updated.getState() + "'");

            if (!Objects.equals(original.getDocket(), updated.getDocket()))
                diffs.add("docket changed");

            if (!Objects.equals(original.getFindings(), updated.getFindings())) {
                diffs.add("findings changed");
                List<Finding> originalList = Optional.ofNullable(original.getFindings()).orElse(List.of());
                List<Finding> updatedList = Optional.ofNullable(updated.getFindings()).orElse(List.of());
                Set<Finding> removed = new HashSet<>(originalList);
                removed.removeAll(updatedList);
                Set<Finding> added = new HashSet<>(updatedList);
                added.removeAll(originalList);
                if (!removed.isEmpty()) diffs.add("removed findings: " + removed);
                if (!added.isEmpty()) diffs.add("added findings: " + added);
            }

            return diffs;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Merge Patch", e);
        }
    }

}
