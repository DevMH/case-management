package com.devmh;

import com.devmh.util.FieldDescriptor;
import com.devmh.util.FieldDescriptorProvider;
import com.devmh.util.PatchFormatter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

import static org.assertj.core.api.Assertions.assertThat;

class PatchFormatterTest {
    @Test
    void formatsReplaceAndAdd() {
        var formatter = new PatchFormatter(new ObjectMapper(), TEST_DICTIONARY);
        String json = """
      [
        { "op":"replace", "path":"case/team/teamMember[0]/firstName", "value":"Tom" },
        { "op":"add", "path":"/case/name", "value":"testCase" }
      ]""";
        var out = formatter.format(json);
        assertThat(out.get(0)).contains("first team member").contains("changed to 'Tom'");
        assertThat(out.get(1)).contains("case's name").contains("set to 'testCase'");
    }

    @Test
    void supportsRemoveFromArray() {
        var formatter = new PatchFormatter(new ObjectMapper(), TEST_DICTIONARY);
        String json = """
      [ { "op":"remove", "path":"/case/findings/1" } ]
      """;
        var out = formatter.format(json);
        assertThat(out.get(0)).contains("Removed the second finding");
    }

    public static final Map<String, FieldDescriptor> TEST_DICTIONARY = new HashMap<>();
    static {
        TEST_DICTIONARY.put("case", new FieldDescriptor("case", false, "case's"));
        TEST_DICTIONARY.put("case/team", new FieldDescriptor("case team", false, "case team's"));
        TEST_DICTIONARY.put("case/findings", FieldDescriptor.collection("findings"));
        TEST_DICTIONARY.put("case/team/members", FieldDescriptor.collection("team members"));
        TEST_DICTIONARY.put("case/name", FieldDescriptor.scalar("name"));
        TEST_DICTIONARY.put("case/state", FieldDescriptor.scalar("approval state"));
        TEST_DICTIONARY.put("case/docket/name", FieldDescriptor.scalar("docket name"));
        TEST_DICTIONARY.put("case/team/members[]/firstName", FieldDescriptor.scalar("first name"));
        TEST_DICTIONARY.put("case/team/members[]/lastName", FieldDescriptor.scalar("last name"));
        TEST_DICTIONARY.put("case/findings[]/startDate", FieldDescriptor.scalar("finding start date"));
        TEST_DICTIONARY.put("case/findings[]/endDate", FieldDescriptor.scalar("finding end date"));
        TEST_DICTIONARY.put("case/findings[]/sensitive", FieldDescriptor.scalar("finding sensitivity"));
    }
}
