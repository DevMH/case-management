package com.devmh;

import com.devmh.model.Case;
import com.devmh.model.Docket;
import com.devmh.model.User;
import com.devmh.persistence.CaseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Comparator;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.instancio.Select.field;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(ElasticsearchTestExtension.class)
class CaseControllerIntegrationTest {

    @RegisterExtension
    static ElasticsearchTestExtension extension =
            new ElasticsearchTestExtension(Docket.class, Case.class, User.class);

    @Autowired private MockMvc mockMvc;
    @Autowired private CaseRepository caseRepository;
    @Autowired private ObjectMapper objectMapper;
    // @Autowired private UserMapper userMapper;

    private static final Comparator<UUID> ALWAYS_EQUALS_UUID = (u1, u2) -> 0;

    @Test
    void testUserPersistenceAndRetrieval() throws Exception {
        UUID id = UUID.randomUUID();

        Case testCase = Instancio.of(Case.class)
                //.ignore("id") // skip non-matching fields
                .supply(field("fullName"), () -> "Alice Smith")
                .create();

        /*
        Case testCase = Case.builder()
                .id(id)
                .build();
         */

        caseRepository.save(testCase);

        Thread.sleep(500); // wait for ES to index

        String json = mockMvc.perform(get("/users/{id}", id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Case actual = objectMapper.readValue(json, Case.class);

        assertThat(actual)
                .usingRecursiveComparison()
                .isEqualTo(testCase);
    }

    /*
    assertThat(actual)
            .usingRecursiveComparison()
            .withComparatorForType(ALWAYS_EQUALS_UUID, UUID.class)
            .withMappedField("name", "fullName")
            .withMappedField("address.street", "address.street")
            .withMappedField("address.city", "address.city")
            .isEqualTo(entity);
     */
}

