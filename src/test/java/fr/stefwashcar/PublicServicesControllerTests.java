package fr.stefwashcar;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PublicServicesControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturn200WhenGettingAllServices() throws Exception {
        mockMvc.perform(get("/api/public/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isNotEmpty())
                .andExpect(jsonPath("$[0].publicId").isString());

    }

    @Test
    void shouldReturn404WhenServiceDoesNotExist() throws Exception {
        mockMvc.perform(get(
                        "/api/public/services/{publicId}",
                        "01AAAAAAAAAAAAAAAAAAAAAAAA"
                ))
                .andExpect(status().isNotFound());
    }
}
