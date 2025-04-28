package com.devmh;

import com.devmh.controller.CaseController;
import com.devmh.model.Case;
import com.devmh.model.Docket;
import com.devmh.persistence.CaseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
@AutoConfigureMockMvc
//@WebMvcTest(CaseController.class)
class CaseControllerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private WebApplicationContext context;

    @Autowired private ObjectMapper mapper;

    @MockBean private CaseRepository caseRepository;

    @BeforeEach
    void setup() {
        this.mockMvc = webAppContextSetup(context)
                .apply(springSecurity())
                .defaultRequest(post("/secure").with(csrf())) // Automatically add csrf for POST
                .build();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void whenAdmin_thenSaveCaseTeamSucceeds() throws Exception {
        mockMvc.perform(post("/cases/save"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void whenUser_thenSaveCaseTeamForbidden() throws Exception {
        mockMvc.perform(post("/cases/save"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Disabled
    void testCreateCase() throws Exception {
        Case testCase = generateCase(CASE_NAME);
        when(caseRepository.save(any(Case.class))).thenReturn(testCase);
        String response = mockMvc.perform(post("/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(testCase)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Case returnedCase = mapper.readValue(response, Case.class);
        then(returnedCase.getName()).isEqualTo(CASE_NAME);
    }

    @Test
    @Disabled
    void testGetAllCases() throws Exception {
        List<Case> testCases = List.of(generateCase(CASE_NAME), generateCase(CASE_2_NAME));
        when(caseRepository.findAll()).thenReturn(testCases);

        String response = mockMvc.perform(get("/cases")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Case> returnedCases = Arrays.asList(mapper.readValue(response, Case[].class));
        assert returnedCases.size() == 2;
        assert returnedCases.get(0).getName().equals(CASE_NAME);
        assert returnedCases.get(1).getName().equals(CASE_2_NAME);
    }

    private static Docket generateDocket() {
        return Docket.builder()
                .id(UUID.randomUUID())
                .name(CaseControllerTest.DOCKET_NAME)
                .build();
    }

    private static Case generateCase(String name) {
        return Case.builder()
                .id(UUID.randomUUID())
                .name(name)
                .docket(generateDocket())
                .build();
    }

    private static final String DOCKET_NAME = "Test Docket";
    private static final String CASE_NAME = "Case 1";
    private static final String CASE_2_NAME = "Case 2";
}
