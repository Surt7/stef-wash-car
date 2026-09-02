package fr.stefwashcar.controller;

import fr.stefwashcar.repository.FormuleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PublicFormulesControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FormuleRepository formuleRepository;

    @Test
    void shouldReturn200WhenGettingAllFormules() throws Exception {
        mockMvc.perform(get("/api/public/formules"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn404WhenFormuleDoesNotExist() throws Exception {
        mockMvc.perform(get(
                        "/api/public/formules/{publicId}",
                        "01AAAAAAAAAAAAAAAAAAAAAAAA"
                ))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnASeededFormuleWithItsLazyRelations() throws Exception {
        String publicId = formuleRepository.findAll().getFirst().getPublicId();

        mockMvc.perform(get("/api/public/formules/{publicId}", publicId))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.color.value").value("#0078D4"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.servicePublicId").isString());
    }
}
