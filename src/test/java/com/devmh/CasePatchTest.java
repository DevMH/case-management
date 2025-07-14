package com.devmh;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.devmh.model.Case;
import com.devmh.model.Finding;
import com.devmh.persistence.CaseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
class CasePatchTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CaseRepository caseRepository;

    @Test
    @WithMockUser(roles = "ADMIN")
    void patchCase_addFinding_shouldSucceed() throws Exception {
        UUID id = UUID.randomUUID();
        Case c = Case.builder().id(id).name("Patchable").findings(List.of()).build();
        caseRepository.save(c);

        String patchJson = "[ { \"op\": \"add\", \"path\": \"/findings/0\", \"value\": { \"startDate\": \"2024-01-01\", \"endDate\": \"2024-12-31\", \"sensitive\": true } } ]";

        mockMvc.perform(patch("/case-team/" + id)
                        .contentType("application/json-patch+json")
                        .content(patchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.findings[0].sensitive").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void patchCase_removeFinding_shouldSucceed() throws Exception {
        UUID id = UUID.randomUUID();
        Finding f = Finding.builder().startDate(Instant.now()).endDate(Instant.now().plus(10, ChronoUnit.DAYS)).sensitive(false).build();
        Case c = Case.builder().id(id).name("ToRemove").findings(new ArrayList<>(List.of(f))).build();
        caseRepository.save(c);

        String patchJson = "[ { \"op\": \"remove\", \"path\": \"/findings/0\" } ]";

        mockMvc.perform(patch("/case-team/" + id)
                        .contentType("application/json-patch+json")
                        .content(patchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.findings").isEmpty());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void mergePatchCase_updateName_shouldSucceed() throws Exception {
        UUID id = UUID.randomUUID();
        Case c = Case.builder().id(id).name("Old Name").build();
        caseRepository.save(c);

        String patchJson = "{ \"name\": \"New Name\" }";

        mockMvc.perform(patch("/case-team/" + id)
                        .contentType("application/merge-patch+json")
                        .content(patchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"));
    }
}
