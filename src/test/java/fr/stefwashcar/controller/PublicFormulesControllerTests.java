package fr.stefwashcar.controller;

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
}
