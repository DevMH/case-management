package com.devmh.log;

import java.util.Map;

public interface FieldDescriptorProvider {

    Map<String, FieldDescriptor> getFieldDescriptors();
}
