package com.devmh;

import com.devmh.util.FieldDescriptorCoverageChecker;
import com.devmh.util.FieldDescriptor;
import com.devmh.util.FieldDescriptorProvider;
import com.devmh.util.PatchFormatter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class FieldDescriptorCoverageCheckerTest {

    @Test
    void provider_coversAllFields() {
        ObjectMapper mapper = new ObjectMapper();
        FieldDescriptorCoverageChecker checker = new FieldDescriptorCoverageChecker(mapper, TEST_PROVIDER);
        var report = checker.checkCoverage();

        log.info("EXPECTED KEYS:\n" + String.join("\n", report.expectedKeys()));
        log.warn("MISSING KEYS:\n" + String.join("\n", report.missingKeys()));
        log.warn("EXTRA KEYS:\n" + String.join("\n", report.extraKeys()));

        assertThat(report.missingKeys()).as("Dictionary missing entries").isEmpty();
    }

    @Data
    private static final class TestFieldDescriptorProvider implements FieldDescriptorProvider {

        private String stringField;
        private SortedSet<String> collectionField;

        @Override
        @JsonIgnore
        public String getRootFieldName() {
            return "test";
        }

        @Override
        @JsonIgnore
        public Map<String, FieldDescriptor> getFieldDescriptors() {
            return Map.of();
        }
    };

    private static final FieldDescriptorProvider TEST_PROVIDER = new TestFieldDescriptorProvider();
}

