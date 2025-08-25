package com.devmh.util;

public final class FieldDescriptor {
    public final String label;
    public final boolean isCollection;
    public final String possessive;

    public FieldDescriptor(String label, boolean isCollection, String possessive) {
        this.label = label;
        this.isCollection = isCollection;
        this.possessive = possessive;
    }

    public static FieldDescriptor scalar(String label) {
        return new FieldDescriptor(label, false, null);
    }

    public static FieldDescriptor collection(String label) {
        return new FieldDescriptor(label, true, null);
    }
}
