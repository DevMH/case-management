package com.devmh.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PatchFormatterTest {

    @Test
    void formatsHumanReadable() throws Exception {
        ObjectMapper om = new ObjectMapper();
        String patchJson = """
            [
            { "op":"replace", "path":"/case/name", "value":"Apollo" },
            { "op":"add", "path":"/case/team/members/0/firstName", "value":"Tom" },
            { "op":"remove", "path":"/case/findings/1/endDate" }
            ]
        """;
        var patch = om.readTree(patchJson);
        PatchFormatter fmt = new PatchFormatter(new TestDictionaryProvider());
        List<String> out = fmt.format(patch);

        assertThat(out).containsExactly(
            "The case's name was changed to \"Apollo\".",
            "The case team's first team member's first name was set to \"Tom\".",
            "The case's second findings's finding end date was removed."
        );
    }
}