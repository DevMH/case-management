package com.devmh.log;

import com.devmh.log.model.FieldDescriptor;
import com.devmh.log.model.FieldDescriptorProvider;

import java.util.LinkedHashMap;
import java.util.Map;

public class TestDictionaryProvider implements FieldDescriptorProvider {
    private final Map<String, FieldDescriptor> TEST_DICTIONARY = new LinkedHashMap<>();

    public TestDictionaryProvider() {
        TEST_DICTIONARY.put("case", FieldDescriptor.owner("case", "case's"));
        TEST_DICTIONARY.put("case/team", FieldDescriptor.owner("case team", "case team's"));
        TEST_DICTIONARY.put("case/findings", FieldDescriptor.collection("findings"));
        TEST_DICTIONARY.put("case/team/members", FieldDescriptor.collection("team member"));
        TEST_DICTIONARY.put("case/name", FieldDescriptor.scalar("name"));
        TEST_DICTIONARY.put("case/state", FieldDescriptor.scalar("approval state"));
        TEST_DICTIONARY.put("case/docket/name", FieldDescriptor.scalar("docket name"));
        TEST_DICTIONARY.put("case/team/members[]/firstName", FieldDescriptor.scalar("first name"));
        TEST_DICTIONARY.put("case/team/members[]/lastName", FieldDescriptor.scalar("last name"));
        TEST_DICTIONARY.put("case/findings[]/startDate", FieldDescriptor.scalar("finding start date"));
        TEST_DICTIONARY.put("case/findings[]/endDate", FieldDescriptor.scalar("finding end date"));
        TEST_DICTIONARY.put("case/findings[]/sensitive", FieldDescriptor.scalar("finding sensitivity"));
    }


    @Override
    public Map<String, FieldDescriptor> getFieldDescriptors() {
        return TEST_DICTIONARY;
    }
}
