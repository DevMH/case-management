package com.devmh;

import com.devmh.util.PatchFormatter;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;

class PatchHumanizerTest {
    @Test
    void formatsReplaceAndAdd() {
        var formatter = new PatchFormatter(new ObjectMapper(), PatchFormatter.defaultDictionary());
        String json = """
      [
        { "op":"replace", "path":"case/team/teamMember/0/firstName", "value":"Tom" },
        { "op":"add", "path":"/case/name", "value":"testCase" }
      ]""";
        var out = formatter.format(json);
        assertThat(out.get(0)).contains("first team member").contains("changed to 'Tom'");
        assertThat(out.get(1)).contains("case's name").contains("set to 'testCase'");
    }

    @Test
    void supportsRemoveFromArray() {
        var humanizer = new PatchFormatter(new ObjectMapper(), PatchFormatter.defaultDictionary());
        String json = """
      [ { "op":"remove", "path":"/case/findings/1" } ]
      """;
        var out = humanizer.format(json);
        assertThat(out.get(0)).contains("Removed the second finding");
    }
}
