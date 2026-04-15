package org.geant.maat.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class KeycloakServiceTokenUserContextService {
    private final KeycloakAdminClient adminClient;
    private final KeycloakServiceTokenUserContextProperties properties;
    private final ObjectMapper objectMapper;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    KeycloakServiceTokenUserContextService(KeycloakAdminClient adminClient,
                                          KeycloakServiceTokenUserContextProperties properties,
                                          ObjectMapper objectMapper) {
        this.adminClient = adminClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    KeycloakUserContext load(String username) {
        CacheEntry cachedEntry = cache.get(username);
        if (cachedEntry != null && Instant.now().isBefore(cachedEntry.expiresAt())) {
            return cachedEntry.value();
        }

        JsonNode userSummary = adminClient.findUserByUsername(username)
                .orElseThrow(() -> new KeycloakServiceTokenUserContextException(404,
                        "Keycloak user " + username + " not found"));

        JsonNode userIdNode = userSummary.get("id");
        if (userIdNode == null || userIdNode.asText().isBlank()) {
            throw new KeycloakServiceTokenUserContextException(503,
                    "Keycloak user lookup did not return user id for " + username);
        }

        JsonNode userDetails = adminClient.getUserDetails(userIdNode.asText());
        JsonNode clientRoles = adminClient.getUserClientRoles(userIdNode.asText());

        KeycloakUserContext context = new KeycloakUserContext(
                username,
                extractUserAccessFilters(userDetails),
                extractRoleNames(clientRoles)
        );
        cache.put(username, new CacheEntry(context, Instant.now().plusSeconds(properties.getCacheTtlSeconds())));
        return context;
    }

    private Map<String, Object> extractUserAccessFilters(JsonNode userDetails) {
        JsonNode attributesNode = userDetails.get("attributes");
        if (attributesNode == null || !attributesNode.isObject()) {
            return Collections.emptyMap();
        }

        Map<String, Object> mergedFilters = new HashMap<>();
        for (String attributeName : properties.getUserAccessFilterAttributes()) {
            JsonNode attributeNode = attributesNode.get(attributeName);
            if (attributeNode == null) {
                continue;
            }

            if (attributeNode.isArray()) {
                for (JsonNode valueNode : attributeNode) {
                    mergeFilters(mergedFilters, normalizeFilterValue(attributeName, parseFilterValue(attributeName, valueNode.asText())));
                }
            } else if (attributeNode.isTextual()) {
                mergeFilters(mergedFilters, normalizeFilterValue(attributeName, parseFilterValue(attributeName, attributeNode.asText())));
            }
        }

        return mergedFilters;
    }

    private Object parseFilterValue(String attributeName, String rawValue) {
        try {
            return objectMapper.readValue(rawValue, Object.class);
        } catch (JsonProcessingException exception) {
            throw new KeycloakServiceTokenUserContextException(503,
                    "Could not parse Keycloak user attribute " + attributeName + ": " + exception.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> normalizeFilterValue(String attributeName, Object parsedValue) {
        if (parsedValue instanceof Map<?, ?> mapValue) {
            return (Map<String, Object>) mapValue;
        }

        if (parsedValue instanceof List<?> listValue) {
            Map<String, Object> wrappedValue = new HashMap<>();
            wrappedValue.put(attributeName, listValue);
            return wrappedValue;
        }

        throw new KeycloakServiceTokenUserContextException(503,
                "Unsupported Keycloak user attribute format for " + attributeName);
    }

    @SuppressWarnings("unchecked")
    private void mergeFilters(Map<String, Object> target, Map<String, Object> source) {
        source.forEach((key, value) -> {
            Object currentValue = target.get(key);
            if (currentValue instanceof List<?> currentList && value instanceof List<?> sourceList) {
                List<Object> merged = new ArrayList<>(currentList);
                merged.addAll(sourceList);
                target.put(key, merged);
                return;
            }
            if (currentValue instanceof Map<?, ?> currentMap && value instanceof Map<?, ?> sourceMap) {
                Map<String, Object> merged = new HashMap<>((Map<String, Object>) currentMap);
                mergeFilters(merged, (Map<String, Object>) sourceMap);
                target.put(key, merged);
                return;
            }
            target.put(key, value);
        });
    }

    private Collection<String> extractRoleNames(JsonNode clientRoles) {
        if (clientRoles == null || !clientRoles.isArray()) {
            return List.of();
        }

        List<String> roles = new ArrayList<>();
        Iterator<JsonNode> iterator = clientRoles.elements();
        while (iterator.hasNext()) {
            JsonNode roleNode = iterator.next();
            JsonNode nameNode = roleNode.get("name");
            if (nameNode != null && !nameNode.asText().isBlank()) {
                roles.add(nameNode.asText());
            }
        }
        return roles;
    }

    private record CacheEntry(KeycloakUserContext value, Instant expiresAt) {
    }
}
