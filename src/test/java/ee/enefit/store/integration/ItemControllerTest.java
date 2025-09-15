package ee.enefit.store.integration;


import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.enefit.store.controller.ItemController;
import ee.enefit.store.dto.ItemResponse;
import ee.enefit.store.dto.ItemUpdateRequest;
import ee.enefit.store.messaging.ItemSoldEvent;
import ee.enefit.store.service.ItemService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
    void getItem_returns404_whenItemNotFound() throws Exception {
        UUID itemId = UUID.randomUUID();
        when(itemService.getItemById(itemId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/items/{id}", itemId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void getItemsByName_returns200_withResults() throws Exception {
        String query = "lap";
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        List<ItemResponse> results = List.of(
                new ItemResponse(id1, "Laptop",  new BigDecimal("150.00"), 5),
                new ItemResponse(id2, "Lapdesk", new BigDecimal("20.00"),  8)
        );
        when(itemService.searchByName(query)).thenReturn(results);

        mockMvc.perform(get("/api/items").param("name", query).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(id1.toString())))
                .andExpect(jsonPath("$[0].name", is("Laptop")))
                .andExpect(jsonPath("$[0].quantity", is(5)))
                .andExpect(jsonPath("$[1].id", is(id2.toString())))
                .andExpect(jsonPath("$[1].name", is("Lapdesk")))
                .andExpect(jsonPath("$[1].quantity", is(8)));

        verify(itemService).searchByName(query);
    }

    @Test
    void getItemsByName_returns200_emptyArray_whenNoMatches() throws Exception {
        String query = "zzz";
        when(itemService.searchByName(query)).thenReturn(List.of());

        mockMvc.perform(get("/api/items").param("name", query).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        verify(itemService).searchByName(query);
    }

    @Test
    void updateItem_returns200_andBody_whenUpdated() throws Exception {
        UUID id = UUID.randomUUID();
        ItemResponse updated =
                new ItemResponse(id, "NewName", new BigDecimal("12.50"), 7);
        when(itemService.updateItem(eq(id), any(ItemUpdateRequest.class)))
                .thenReturn(Optional.of(updated));
        String payload = """
        {
          "name": "NewName",
          "price": 12.50,
          "quantity": 7
        }
        """;

        mockMvc.perform(put("/api/items/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(id.toString())))
                .andExpect(jsonPath("$.name", is("NewName")))
                .andExpect(jsonPath("$.price", is(12.50)))
                .andExpect(jsonPath("$.quantity", is(7)));

        ArgumentCaptor<ItemUpdateRequest> captor = ArgumentCaptor.forClass(ItemUpdateRequest.class);
        verify(itemService).updateItem(eq(id), captor.capture());
        ItemUpdateRequest arg = captor.getValue();
        assertEquals("NewName", arg.getName());
        assertEquals(7, arg.getQuantity());
    }

    @Test
    void updateItem_returns404_whenNotFound() throws Exception {
        UUID id = UUID.randomUUID();

        when(itemService.updateItem(eq(id), any(ItemUpdateRequest.class)))
                .thenReturn(Optional.empty());

        String payload = """
        { "price": 9.99, "quantity": 3 }
        """;

        mockMvc.perform(put("/api/items/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteItem_returns204_whenDeleted() throws Exception {
        when(itemService.deleteItem(any(UUID.class))).thenReturn(true);

        mockMvc.perform(delete("/api/items/{id}", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteItem_returns404_whenNotFound() throws Exception {
        when(itemService.deleteItem(any(UUID.class))).thenReturn(false);

        mockMvc.perform(delete("/api/items/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }


    @Test
    void itemSell_returnsOk_whenItemExists() throws Exception {
        UUID itemId = UUID.randomUUID();
        ItemSoldEvent evt = new ItemSoldEvent(
                UUID.randomUUID(), itemId, 2, new BigDecimal("9.99"),
                new BigDecimal("19.98"), Instant.now());

        when(itemService.sell(eq(itemId), eq(2)))
                .thenReturn(evt);

        mockMvc.perform(post("/api/items/{id}/sell", itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":2}"))
                .andExpect(status().isOk());
    }
}
