package fr.stefwashcar.controller;

import fr.stefwashcar.controller.admin.AdminShopController;
import fr.stefwashcar.dto.admin.UpdateShopRequest;
import fr.stefwashcar.service.admin.AdminShopService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class AdminShopControllerUnitTest {

    @Mock
    private AdminShopService adminShopService;

    @Test
    void patchShopReturnsTheHttpResponseProducedByTheService() {
        AdminShopController controller = new AdminShopController(adminShopService);
        UpdateShopRequest request = new UpdateShopRequest(
                "Studio modifié", null, null, "75011", "Paris",
                null, "FR", null, "studio@example.com"
        );
        ResponseEntity<?> expected = ResponseEntity.ok(Map.of("ok", true));
        doReturn(expected).when(adminShopService).patchShop(12L, request.toMap());

        ResponseEntity<?> actual = controller.patchShop(12L, request);

        assertEquals(200, actual.getStatusCode().value());
        assertEquals(Map.of("ok", true), actual.getBody());
        verify(adminShopService).patchShop(12L, request.toMap());
    }
}
