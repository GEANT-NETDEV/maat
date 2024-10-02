package org.geant.maat.common;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import io.vavr.control.Either;
import org.geant.maat.infrastructure.DomainError;
import org.geant.maat.infrastructure.ErrorEntity;
import org.geant.maat.resource.Resource;
import org.geant.maat.resource.ResourceService;
import org.geant.maat.service.ServiceService;

import java.util.*;

public class UserDataFilters {
    private ResourceService resourceService;
    private ServiceService serviceService;
    private Boolean notificationStatus;

    public UserDataFilters(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    public UserDataFilters(ResourceService resourceService, Boolean notificationStatus) {
        this.resourceService = resourceService;
        this.notificationStatus = notificationStatus;
    }

    public UserDataFilters(ServiceService serviceService, Boolean notificationStatus) {
        this.serviceService = serviceService;
        this.notificationStatus = notificationStatus;
    }


    public Collection<JsonNode> getFilter(String token, List<String> fields, Map<String, String> oldRequestsParams,
                                          String type) {
        String cleanedToken = token.replace("Bearer ", "");
        DecodedJWT jwt = JWT.decode(cleanedToken);
        Map<String, Object> userAccessFilters = jwt.getClaim("user_access_filters").asMap();

        List<Map<String, String>> orFilters = new ArrayList<>();
        if (userAccessFilters != null) {
            List<Map<String, String>> getFilterList = (List<Map<String, String>>) userAccessFilters.get("get_filter");

            if (getFilterList != null) {
                System.out.println("GET_FILTER jest !null");
                orFilters.addAll(getFilterList);
            } else {
                if (type.equals("resource")){
                    return resourceService.getResources(fields, oldRequestsParams);
                }
                else {
                    return serviceService.getServices(fields, oldRequestsParams);
                }
            }
        } else {
            if (type.equals("resource")){
                return resourceService.getResources(fields, oldRequestsParams);
            }
            else {
                return serviceService.getServices(fields, oldRequestsParams);
            }
        }

        Collection<JsonNode> finalResult = new ArrayList<>();

        if (orFilters.isEmpty()) {
            return resourceService.getResources(fields, oldRequestsParams);
        }

        for (Map<String, String> filter : orFilters) {
            Map<String, String> combinedParams = new HashMap<>(oldRequestsParams);
            System.out.println(combinedParams);
            combinedParams.putAll(filter);
            System.out.println(combinedParams);
            Collection<JsonNode> currentResult;
            if (type.equals("resource")){
                currentResult = resourceService.getResources(fields, combinedParams);
            }
            else {
                currentResult = serviceService.getServices(fields, combinedParams);
            }
            finalResult.addAll(currentResult);
        }

        return finalResult;

    }

    public Collection<JsonNode> getFilter(String token, List<String> fields, Map<String, String> oldRequestsParams,
                                          int offset, int limit, String type) {
        String cleanedToken = token.replace("Bearer ", "");
        DecodedJWT jwt = JWT.decode(cleanedToken);
        Map<String, Object> userAccessFilters = jwt.getClaim("user_access_filters").asMap();

        List<Map<String, String>> orFilters = new ArrayList<>();
        if (userAccessFilters != null) {
            List<Map<String, String>> getFilterList = (List<Map<String, String>>) userAccessFilters.get("get_filter");

            if (getFilterList != null) {
                orFilters.addAll(getFilterList);
            } else {
                if (type.equals("resource")){
                    return resourceService.getResources(fields, oldRequestsParams, offset, limit);
                }
                else {
                    return serviceService.getServices(fields, oldRequestsParams, offset, limit);
                }
            }
        } else {
            if (type.equals("resource")){
                return resourceService.getResources(fields, oldRequestsParams, offset, limit);
            }
            else {
                return serviceService.getServices(fields, oldRequestsParams, offset, limit);
            }
        }

        Collection<JsonNode> finalResult = new ArrayList<>();

        for (Map<String, String> filter : orFilters) {
            Map<String, String> combinedParams = new HashMap<>(oldRequestsParams);
            combinedParams.putAll(filter);
            Collection<JsonNode> currentResult;

            if (type.equals("resource")){
                currentResult = resourceService.getResources(fields, combinedParams);
            }
            else {
                currentResult = serviceService.getServices(fields, combinedParams);
            }

            finalResult.addAll(currentResult);

            if (limit > 0 && finalResult.size() >= limit + offset) {
                break;
            }
        }

        return applyLimitAndOffset(finalResult, limit, offset);

    }

    public Either<DomainError, JsonNode> getFilterById(String token, String id, List<String> fields, String type) {
        String cleanedToken = token.replace("Bearer ", "");
        DecodedJWT jwt = JWT.decode(cleanedToken);
        Map<String, Object> userAccessFilters = jwt.getClaim("user_access_filters").asMap();

        List<Map<String, String>> orFilters = new ArrayList<>();

        if (userAccessFilters != null) {
            List<Map<String, String>> getFilterList = (List<Map<String, String>>) userAccessFilters.get("get_filter");

            if (getFilterList != null) {
                orFilters.addAll(getFilterList);
            } else {
                if (type.equals("resource")){
                    return resourceService.getResource(id, fields);
                }
                else {
                    return serviceService.getService(id, fields);
                }
            }
        } else {
            if (type.equals("resource")){
                return resourceService.getResource(id, fields);
            }
            else {
                return serviceService.getService(id, fields);
            }
        }

        Collection<String> collection = List.of();
        Either<DomainError, JsonNode> resourceById = null;
        Either<DomainError, JsonNode> serviceById = null;

        if (type.equals("resource")){
            resourceById = resourceService.getResource(id, collection);
        }
        else {
            serviceById = serviceService.getService(id, collection);
        }

        System.out.println("orFilters:" + orFilters);

        if (type.equals("resource")){
            for (Map<String, String> filter : orFilters) {
                if (matchesFilter(filter, resourceById.get())) {
                    System.out.println("Match to: " + filter);
                    return resourceService.getResource(id, fields);
                }
            }
        }
        else {
            for (Map<String, String> filter : orFilters) {
                if (matchesFilter(filter, serviceById.get())) {
                    System.out.println("Match to: " + filter);
                    return serviceService.getService(id, fields);
                }
            }
        }

        System.out.println("Doesn't match");
        return Either.left(new DomainError("User filter does not match", Error.FILTER_ERROR));

    }

    public Either<DomainError, JsonNode> postFilter(String token, JsonNode requestBody, String type) {
        String cleanedToken = token.replace("Bearer ", "");
        DecodedJWT jwt = JWT.decode(cleanedToken);
        Map<String, Object> userAccessFilters = jwt.getClaim("user_access_filters").asMap();

        List<Map<String, String>> orFilters = new ArrayList<>();

        if (userAccessFilters != null) {
            List<Map<String, String>> postFilterList = (List<Map<String, String>>) userAccessFilters.get("post_filter");

            if (postFilterList != null) {
                orFilters.addAll(postFilterList);
            } else {
                if (type.equals("resource")){
                    return resourceService.createResource(requestBody, notificationStatus);
                }
                else {
                    return serviceService.createService(requestBody, notificationStatus);
                }
            }
        } else {
            if (type.equals("resource")){
                return resourceService.createResource(requestBody, notificationStatus);
            }
            else {
                return serviceService.createService(requestBody, notificationStatus);
            }
        }

        System.out.println("orFilters:" + orFilters);

        if (type.equals("resource")){
            for (Map<String, String> filter : orFilters) {
                if (matchesFilter(filter, requestBody)) {
                    System.out.println("Match to: " + filter);
                    return resourceService.createResource(requestBody, notificationStatus);
                }
            }
        }
        else {
            for (Map<String, String> filter : orFilters) {
                if (matchesFilter(filter, requestBody)) {
                    System.out.println("Match to: " + filter);
                    return serviceService.createService(requestBody, notificationStatus);
                }
            }
        }

        System.out.println("Doesn't match");
        return Either.left(new DomainError("User filter does not match", Error.FILTER_ERROR));
    }

    public Either<DomainError, String> deleteFilter(String token, String id, String type) {
        String cleanedToken = token.replace("Bearer ", "");
        DecodedJWT jwt = JWT.decode(cleanedToken);
        Map<String, Object> userAccessFilters = jwt.getClaim("user_access_filters").asMap();

        List<Map<String, String>> orFilters = new ArrayList<>();

        if (userAccessFilters != null) {
            List<Map<String, String>> deleteFilterList = (List<Map<String, String>>) userAccessFilters.get("delete_filter");

            if (deleteFilterList != null) {
                orFilters.addAll(deleteFilterList);
            } else {
                if (type.equals("resource")){
                    return resourceService.deleteResource(id, notificationStatus);
                }
                else {
                    return serviceService.deleteService(id, notificationStatus);
                }
            }
        } else {
            if (type.equals("resource")){
                return resourceService.deleteResource(id, notificationStatus);
            }
            else {
                return serviceService.deleteService(id, notificationStatus);
            }
        }

        Collection<String> collection = List.of();
        Either<DomainError, JsonNode> resourceForRemoval = null;
        Either<DomainError, JsonNode> serviceForRemoval = null;

        if (type.equals("resource")){
            resourceForRemoval = resourceService.getResource(id, collection);
        }
        else {
            serviceForRemoval = serviceService.getService(id, collection);
        }

        System.out.println("orFilters:" + orFilters);

        if (type.equals("resource")){
            for (Map<String, String> filter : orFilters) {
                if (matchesFilter(filter, resourceForRemoval.get())) {
                    System.out.println("Match to: " + filter);
                    return resourceService.deleteResource(id, notificationStatus);
                }
            }
        }
        else {
            for (Map<String, String> filter : orFilters) {
                if (matchesFilter(filter, serviceForRemoval.get())) {
                    System.out.println("Match to: " + filter);
                    return serviceService.deleteService(id, notificationStatus);
                }
            }
        }

        System.out.println("Doesn't match");
        return Either.left(new DomainError("User filter does not match", Error.FILTER_ERROR));
    }

    public Either<DomainError, JsonNode> patchFilter(String token, String id, JsonNode requestBody, String type) {
        String cleanedToken = token.replace("Bearer ", "");
        DecodedJWT jwt = JWT.decode(cleanedToken);
        Map<String, Object> userAccessFilters = jwt.getClaim("user_access_filters").asMap();

        List<Map<String, String>> orFilters = new ArrayList<>();

        if (userAccessFilters != null) {
            List<Map<String, String>> patchFilterList = (List<Map<String, String>>) userAccessFilters.get("patch_filter");

            if (patchFilterList != null) {
                orFilters.addAll(patchFilterList);
            } else {
                if (type.equals("resource")){
                    return resourceService.updateResource(id, requestBody, notificationStatus);
                }
                else {
                    return serviceService.updateService(id, requestBody, notificationStatus);
                }
            }
        } else {
            if (type.equals("resource")){
                return resourceService.updateResource(id, requestBody, notificationStatus);
            }
            else {
                return serviceService.updateService(id, requestBody, notificationStatus);
            }
        }

        Collection<String> collection = List.of();
        Either<DomainError, JsonNode> resourceById = null;
        Either<DomainError, JsonNode> serviceById = null;

        if (type.equals("resource")){
            resourceById = resourceService.getResource(id, collection);
        }
        else {
            serviceById = serviceService.getService(id, collection);
        }

        System.out.println("orFilters:" + orFilters);

        if (type.equals("resource")){
            for (Map<String, String> filter : orFilters) {
                if (matchesFilter(filter, resourceById.get())) {
                    System.out.println("Match to: " + filter);
                    return resourceService.updateResource(id, requestBody, notificationStatus);
                }
            }
        }
        else {
            for (Map<String, String> filter : orFilters) {
                if (matchesFilter(filter, serviceById.get())) {
                    System.out.println("Match to: " + filter);
                    return serviceService.updateService(id, requestBody, notificationStatus);
                }
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
