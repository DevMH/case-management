package com.devmh.log;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FieldDescriptor(@JsonProperty String label, @JsonProperty boolean collection,
                              @JsonProperty String possessive) {

    public static FieldDescriptor scalar(String label) {
        return new FieldDescriptor(label, false, null);
    }

    public static FieldDescriptor collection(String label) {
        return new FieldDescriptor(label, true, null);
    }

    public static FieldDescriptor owner(String label, String possessive) {
        return new FieldDescriptor(label, false, possessive);
    }
}
