package com.devmh.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.mergepatch.JsonMergePatch;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
}
