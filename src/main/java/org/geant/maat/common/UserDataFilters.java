package org.geant.maat.common;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import io.vavr.control.Either;
import org.geant.maat.infrastructure.DomainError;
import org.geant.maat.infrastructure.ErrorEntity;
import org.geant.maat.resource.ResourceService;

import java.util.*;

public class UserDataFilters {
    private final ResourceService resourceService;

    public UserDataFilters(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    public Collection<JsonNode> getFilter(String token, List<String> fields, Map<String, String> oldRequestsParams) {
        String cleanedToken = token.replace("Bearer ", "");
        DecodedJWT jwt = JWT.decode(cleanedToken);
        Map<String, Object> userAccessFilters = jwt.getClaim("user_access_filters").asMap();

        List<Map<String, String>> orFilters = new ArrayList<>();
        if (userAccessFilters != null) {
            List<Map<String, String>> getFilterList = (List<Map<String, String>>) userAccessFilters.get("get_filter");

            if (getFilterList != null) {
                orFilters.addAll(getFilterList);
            }
        }

        Collection<JsonNode> finalResult = new ArrayList<>();

        if (orFilters.isEmpty()) {
            return resourceService.getResources(fields, oldRequestsParams);
        }

        for (Map<String, String> filter : orFilters) {
            Map<String, String> combinedParams = new HashMap<>(oldRequestsParams);
            combinedParams.putAll(filter);
            Collection<JsonNode> currentResult = resourceService.getResources(fields, combinedParams);
            finalResult.addAll(currentResult);
        }

        return finalResult;

    }

    public Collection<JsonNode> getFilter(String token, List<String> fields, Map<String, String> oldRequestsParams,
                                          int offset, int limit) {
        String cleanedToken = token.replace("Bearer ", "");
        DecodedJWT jwt = JWT.decode(cleanedToken);
        Map<String, Object> userAccessFilters = jwt.getClaim("user_access_filters").asMap();

        List<Map<String, String>> orFilters = new ArrayList<>();
        if (userAccessFilters != null) {
            List<Map<String, String>> getFilterList = (List<Map<String, String>>) userAccessFilters.get("get_filter");

            if (getFilterList != null) {
                orFilters.addAll(getFilterList);
            }
        }

        Collection<JsonNode> finalResult = new ArrayList<>();

        if (orFilters.isEmpty()) {
            return resourceService.getResources(fields, oldRequestsParams);
        }

        for (Map<String, String> filter : orFilters) {
            Map<String, String> combinedParams = new HashMap<>(oldRequestsParams);
            combinedParams.putAll(filter);
            Collection<JsonNode> currentResult = resourceService.getResources(fields, combinedParams);
            finalResult.addAll(currentResult);

            if (limit > 0 && finalResult.size() >= limit + offset) {
                break;
            }
        }

        return applyLimitAndOffset(finalResult, limit, offset);

    }

    public Either<DomainError, JsonNode> postFilter(String token, JsonNode requestBody) {
        String cleanedToken = token.replace("Bearer ", "");
        DecodedJWT jwt = JWT.decode(cleanedToken);
        Map<String, Object> userAccessFilters = jwt.getClaim("user_access_filters").asMap();

        List<Map<String, String>> orFilters = new ArrayList<>();

        if (userAccessFilters != null) {
            List<Map<String, String>> getFilterList = (List<Map<String, String>>) userAccessFilters.get("post_filter");

            if (getFilterList != null) {
                orFilters.addAll(getFilterList);
            }
        }

        System.out.println("orFilters:" + orFilters);
        for (Map<String, String> filter : orFilters) {
            if (matchesFilter(filter, requestBody)) {
                System.out.println("Match to: " + filter);
                return resourceService.createResource(requestBody, true);
            }
        }

        System.out.println("Doesn't match");
        return Either.left(new DomainError("User filter does not match", Error.FILTER_ERROR));
    }

    private boolean matchesFilter(Map<String, String> filter, JsonNode requestBody) {
        for (Map.Entry<String, String> entry : filter.entrySet()) {
            String filterKey = entry.getKey();
            String expectedValue = entry.getValue();
            String[] keys = filterKey.split("\\.");

            JsonNode currentNode = requestBody;

            for (int i = 0; i < keys.length; i++) {
                String key = keys[i];

                if (currentNode.isArray()) {
                    boolean foundInArray = false;

                    for (JsonNode arrayElement : currentNode) {
                        JsonNode valueNode = arrayElement.get(key);
                        if (valueNode != null && (i == keys.length - 1) && valueNode.asText().equals(expectedValue)) {
                            foundInArray = true;
                            break;
                        } else if (valueNode != null && i < keys.length - 1) {
                            currentNode = valueNode;
                            foundInArray = true;
                            break;
                        }
                    }

                    if (!foundInArray) {
                        return false;
                    }
                } else {
                    currentNode = currentNode.get(key);

                    if (currentNode == null) {
                        return false;
                    }

                    if (i == keys.length - 1 && !currentNode.asText().equals(expectedValue)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private Collection<JsonNode> applyLimitAndOffset(Collection<JsonNode> data, int limit, int offset) {
        List<JsonNode> resultList = new ArrayList<>(data);

        if (offset > 0) {
            if (offset >= resultList.size()) {
                return Collections.emptyList();
            }
            resultList = resultList.subList(offset, resultList.size());
        }

        if (limit > 0 && limit < resultList.size()) {
            resultList = resultList.subList(0, limit);
        }

        return resultList;
    }

}
