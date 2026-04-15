package org.geant.maat.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KeycloakServiceTokenUserContextServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldMergeFiltersFromMultipleUserAttributes() {
        KeycloakAdminClient adminClient = mock(KeycloakAdminClient.class);
        KeycloakServiceTokenUserContextProperties properties = new KeycloakServiceTokenUserContextProperties();
        properties.setUserAccessFilterAttributes(List.of("filters-a", "filters-b"));
        KeycloakServiceTokenUserContextService service =
                new KeycloakServiceTokenUserContextService(adminClient, properties, objectMapper);

        when(adminClient.findUserByUsername("test")).thenReturn(Optional.of(userSummary("user-1")));
        when(adminClient.getUserDetails("user-1")).thenReturn(userDetails());
        when(adminClient.getUserClientRoles("user-1")).thenReturn(clientRoles("get", "post"));

        KeycloakUserContext context = service.load("test");

        assertEquals("test", context.username());
        assertEquals(2, context.maatClientRoles().size());
        assertEquals(1, ((List<?>) context.userAccessFilters().get("get_filter")).size());
        assertEquals(1, ((List<?>) context.userAccessFilters().get("post_filter")).size());
    }

    @Test
    void shouldSupportDedicatedFilterAttributes() {
        KeycloakAdminClient adminClient = mock(KeycloakAdminClient.class);
        KeycloakServiceTokenUserContextProperties properties = new KeycloakServiceTokenUserContextProperties();
        properties.setUserAccessFilterAttributes(List.of("get_filter", "post_filter"));
        KeycloakServiceTokenUserContextService service =
                new KeycloakServiceTokenUserContextService(adminClient, properties, objectMapper);

        when(adminClient.findUserByUsername("test")).thenReturn(Optional.of(userSummary("user-1")));
        when(adminClient.getUserDetails("user-1")).thenReturn(userDetailsWithDedicatedFilterAttributes());
        when(adminClient.getUserClientRoles("user-1")).thenReturn(clientRoles("get"));

        KeycloakUserContext context = service.load("test");

        assertEquals(1, ((List<?>) context.userAccessFilters().get("get_filter")).size());
        assertEquals(1, ((List<?>) context.userAccessFilters().get("post_filter")).size());
    }

    @Test
    void shouldReturn404WhenUserMissing() {
        KeycloakAdminClient adminClient = mock(KeycloakAdminClient.class);
        when(adminClient.findUserByUsername("missing")).thenReturn(Optional.empty());

        KeycloakServiceTokenUserContextService service =
                new KeycloakServiceTokenUserContextService(adminClient, new KeycloakServiceTokenUserContextProperties(), objectMapper);

        KeycloakServiceTokenUserContextException exception = assertThrows(
                KeycloakServiceTokenUserContextException.class,
                () -> service.load("missing")
        );

        assertEquals(404, exception.getHttpStatus());
    }

    private ObjectNode userSummary(String id) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("id", id);
        return node;
    }

    private ObjectNode userDetails() {
        ObjectNode node = objectMapper.createObjectNode();
        ObjectNode attributes = objectMapper.createObjectNode();
        ArrayNode filtersA = objectMapper.createArrayNode();
        filtersA.add("{\"get_filter\":[{\"category\":\"device.router\"}]}");
        ArrayNode filtersB = objectMapper.createArrayNode();
        filtersB.add("{\"post_filter\":[{\"category\":\"device.router\"}]}");
        attributes.set("filters-a", filtersA);
        attributes.set("filters-b", filtersB);
        node.set("attributes", attributes);
        return node;
    }

    private ObjectNode userDetailsWithDedicatedFilterAttributes() {
        ObjectNode node = objectMapper.createObjectNode();
        ObjectNode attributes = objectMapper.createObjectNode();
        ArrayNode getFilter = objectMapper.createArrayNode();
        getFilter.add("[{\"category\":\"device.router\"}]");
        ArrayNode postFilter = objectMapper.createArrayNode();
        postFilter.add("[{\"category\":\"device.switch\"}]");
        attributes.set("get_filter", getFilter);
        attributes.set("post_filter", postFilter);
        node.set("attributes", attributes);
        return node;
    }

    private ArrayNode clientRoles(String... roles) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        for (String role : roles) {
            ObjectNode roleNode = objectMapper.createObjectNode();
            roleNode.put("name", role);
            arrayNode.add(roleNode);
        }
        return arrayNode;
    }
}
