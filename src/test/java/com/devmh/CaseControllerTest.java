package com.devmh;

import com.devmh.model.Case;
import com.devmh.model.Docket;
import com.devmh.persistence.CaseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
class CaseControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper mapper;
    @MockBean private CaseRepository caseRepository;

    @Test
    void testCreateCase() throws Exception {
        Docket docket = new Docket("Test Docket");
        Case newCase = new Case("Test Case", docket);
        when(caseRepository.save(any(Case.class))).thenReturn(newCase);
        String requestBody = mapper.writeValueAsString(newCase);
        mockMvc.perform(post("/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Case"));
    }

    @Test
    void testGetAllCases() throws Exception {
        Docket folder = new Docket("Test Docket");
        Case case1 = new Case("Case 1", folder);
        Case case2 = new Case("Case 2", folder);
        when(caseRepository.findAll()).thenReturn(List.of(case1, case2));

        mockMvc.perform(get("/cases")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Case 1"))
                .andExpect(jsonPath("$[1].name").value("Case 2"));
    }
}
