package fr.stefwashcar.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.security.admin.username=admin",
        "app.security.admin.password=test-password"
})
@AutoConfigureMockMvc
class BackendConfigurationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void openApiExposesMainBusinessSchemasInDev() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.PublicServiceResponse").exists())
                .andExpect(jsonPath("$.components.schemas.DailyAvailabilityResponse").exists())
                .andExpect(jsonPath("$.components.schemas.ReservationResponse").exists())
                .andExpect(jsonPath("$.components.schemas.CreateShopRequest").exists())
                .andExpect(jsonPath("$.components.schemas.CreateEventRequest").exists())
                .andExpect(jsonPath("$.components.schemas.UpdateEventRequest").exists())
                .andExpect(jsonPath("$.components.schemas.UpdateShopNotesRequest").exists())
                .andExpect(jsonPath("$.components.schemas.UpdateShopRequest").exists())
                .andExpect(jsonPath("$.components.securitySchemes.basicAuth").exists());
    }

    @Test
    void adminApiRequiresValidBasicAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/providers"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/providers")
                        .with(httpBasic("admin", "test-password")))
                .andExpect(status().isOk());
    }

    @Test
    void publicReservationPostIsNotRejectedByCsrf() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mailSendingIsDisabledByDefault() throws Exception {
        mockMvc.perform(get("/test-mail")
                        .with(httpBasic("admin", "test-password")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("disabled"));
    }
}
