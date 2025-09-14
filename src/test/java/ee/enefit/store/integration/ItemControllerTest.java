package ee.enefit.store.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.enefit.store.controller.ItemController;
import ee.enefit.store.dto.ItemResponse;
import ee.enefit.store.messaging.ItemSoldEvent;
import ee.enefit.store.service.ItemService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

@WebMvcTest(controllers = ItemController.class)
class ItemControllerTest {
    @MockitoBean
    private ItemService itemService;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getItem_shouldReturn200_whenItemExists() throws Exception {
        UUID itemId = UUID.randomUUID();
        ItemResponse mockItem = new ItemResponse(itemId, "Laptop", BigDecimal.valueOf(150), 1250);
        when(itemService.getItemById(itemId)).thenReturn(Optional.of(mockItem));

        var mvcResult = mockMvc.perform(get("/api/items/{id}", itemId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        var body = objectMapper.readValue(
                mvcResult.getResponse().getContentAsString(),
                ee.enefit.store.dto.ItemResponse.class);

        assertEquals(mockItem, body);

    }

    @Test
    void getItem_shouldReturn404_whenItemNotFound() throws Exception {
        UUID itemId = UUID.randomUUID();
        when(itemService.getItemById(itemId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/items/{id}", itemId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void itemSell_ok() throws Exception {
        UUID itemId = UUID.randomUUID();
        ItemSoldEvent evt = new ItemSoldEvent(
                UUID.randomUUID(), itemId, 2, new BigDecimal("9.99"),
                new BigDecimal("19.98"), Instant.now());

        when(itemService.sell(ArgumentMatchers.eq(itemId), ArgumentMatchers.eq(2)))
                .thenReturn(evt);

        mockMvc.perform(post("/api/items/{id}/sell", itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":2}"))
                .andExpect(status().isOk());
    }
}
