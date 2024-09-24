package org.geant.maat.resource;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.vavr.control.Either;
import jakarta.servlet.http.HttpServletRequest;
import org.geant.maat.common.UserDataFilters;
import org.geant.maat.infrastructure.DomainError;
import org.geant.maat.infrastructure.ResultMapper;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.keycloak.KeycloakPrincipal;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/resourceInventoryManagement")
@Tag(name = "API Resource Inventory Management")
class ResourceController implements ResultMapper {
    private final ResourceService resourceService;

    @Value("${keycloak.enabled}")
    private String keycloakStatus;

    @Autowired
    public Environment environment;

    @Autowired
    ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @Operation(
            operationId = "listResource",
            summary = "List or find Resource objects",
            description = "This operation list or find Resource entities.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = { @Content( mediaType = "application/json") }),
            @ApiResponse(responseCode = "400", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "401", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "403", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "404", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "405", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "409", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "500", content = { @Content(schema = @Schema()) }) })

    @GetMapping("/${api.resource.version}/resource")
    ResponseEntity<Collection<JsonNode>> getResources(
            @RequestParam(required = false, defaultValue = "") List<String> fields,
            @RequestParam Map<String, String> allRequestParams,
            @RequestParam(required = false, defaultValue = "0") int offset,
            @RequestParam(required = false, defaultValue = "0") int limit,
            @RequestHeader("Authorization") String token
    ) {
        allRequestParams.remove("fields");
        allRequestParams.remove("offset");
        allRequestParams.remove("limit");


        if (Objects.equals(keycloakStatus, "true")) {
            UserDataFilters userFilters = new UserDataFilters(resourceService);
            if (offset == 0 && limit == 0) {
                var allResourcesWithFilters = userFilters.getFilter(token, fields, allRequestParams);
                return toResponseEntity(allResourcesWithFilters, allResourcesWithFilters.size());
            } else {
                var allResourcesWithFilters = userFilters.getFilter(token, fields, allRequestParams, offset, limit);
                return toResponseEntity(allResourcesWithFilters, allResourcesWithFilters.size());
            }
        } else {
            var allResources = resourceService.getResources(fields, allRequestParams);
            if (offset == 0 && limit == 0) {
                return toResponseEntity(allResources, allResources.size());
            }
            return toResponseEntity(resourceService.getResources(fields, allRequestParams, offset, limit),
                    allResources.size());
        }

    }

    private ResponseEntity<Collection<JsonNode>> toResponseEntity(Collection<JsonNode> services, long totalCount) {
        return ResponseEntity.status(200)
                .header("X-Total-Count", Long.toString(totalCount))
                .body(services);
    }

    @Operation(
            operationId = "retrieveResource",
            summary = "Retrieves a Resource by ID",
            description = "This operation retrieves a Resource entity. Attribute selection is enabled for all first level attributes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = { @Content( mediaType = "application/json") }),
            @ApiResponse(responseCode = "400", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "401", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "403", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "404", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "405", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "409", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "500", content = { @Content(schema = @Schema()) }) })
    @GetMapping("/${api.resource.version}/resource/{id}")
    ResponseEntity<?> getResource(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "") List<String> fields,
            @RequestParam Map<String, String> allRequestParams,
            @RequestHeader("Authorization") String token
    ) {
        allRequestParams.remove("fields");

        if (Objects.equals(keycloakStatus, "true")) {
            UserDataFilters userFilters = new UserDataFilters(resourceService);
            var resourceByIdWithFilters = userFilters.getFilterById(token, id, fields);
            return foldResultWithStatus(resourceByIdWithFilters, HttpStatus.OK);
        } else {
            var resource = resourceService.getResource(id, fields);
            return foldResultWithStatus(resource, HttpStatus.OK);
        }
    }

    @Operation(
            operationId = "createResource",
            summary = "Creates a Resource",
            description = "This operation creates a Resource entity.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "400", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "401", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "403", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "405", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "409", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "500", content = { @Content(schema = @Schema()) }) })
    @PostMapping(value = "/${api.resource.version}/resource")
    ResponseEntity<?> addResource(@RequestBody JsonNode requestBody,
                                  @RequestHeader("Authorization") String token) {

        if(Objects.requireNonNull(environment.getProperty("notification.sendNotificationToListeners")).equalsIgnoreCase("true")) {
            if (Objects.equals(keycloakStatus, "true")) {
                UserDataFilters userFilters = new UserDataFilters(resourceService, true);
                Either<DomainError, JsonNode> resource = userFilters.postFilter(token, requestBody);
                return foldResultWithStatus(resource, HttpStatus.CREATED);
            } else {
                var resource = resourceService.createResource(requestBody, true);
                return foldResultWithStatus(resource, HttpStatus.CREATED);
            }
        } else {
            if (Objects.equals(keycloakStatus, "true")) {
                UserDataFilters userFilters = new UserDataFilters(resourceService, false);
                Either<DomainError, JsonNode> resource = userFilters.postFilter(token, requestBody);
                return foldResultWithStatus(resource, HttpStatus.CREATED);
            } else {
                var resource = resourceService.createResource(requestBody, false);
                return foldResultWithStatus(resource, HttpStatus.CREATED);
            }
        }
    }

    @Operation(
            operationId = "deleteResource",
            summary = "Deletes a Resource",
            description = "This operation deletes a Resource entity.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "400", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "401", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "403", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "404", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "405", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "409", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "500", content = { @Content(schema = @Schema()) }) })
    @DeleteMapping(value = "/${api.resource.version}/resource/{id}")
    ResponseEntity<?> deleteResource(@PathVariable String id,
                                     @RequestHeader("Authorization") String token) {
        if(Objects.requireNonNull(environment.getProperty("notification.sendNotificationToListeners")).equalsIgnoreCase("true")) {
            if (Objects.equals(keycloakStatus, "true")) {
                UserDataFilters userFilters = new UserDataFilters(resourceService, true);
                Either<DomainError, String> resource = userFilters.deleteFilter(token, id);
                return foldResultWithStatus(resource, HttpStatus.NO_CONTENT);
            } else {
                var resource = resourceService.deleteResource(id, true);
                return foldResultWithStatus(resource, HttpStatus.NO_CONTENT);
            }
        } else {
            if (Objects.equals(keycloakStatus, "true")) {
                UserDataFilters userFilters = new UserDataFilters(resourceService, false);
                Either<DomainError, String> resource = userFilters.deleteFilter(token, id);
                return foldResultWithStatus(resource, HttpStatus.NO_CONTENT);
            } else {
                var resource = resourceService.deleteResource(id, false);
                return foldResultWithStatus(resource, HttpStatus.NO_CONTENT);
            }
        }
    }

    @Operation(
            operationId = "patchResource",
            summary = "Updates partially a Resource",
            description = "This operation updates partially a Resource entity.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = { @Content( mediaType = "application/json") }),
            @ApiResponse(responseCode = "400", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "401", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "403", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "404", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "405", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "409", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "500", content = { @Content(schema = @Schema()) }) })
    @PatchMapping(value = "/${api.resource.version}/resource/{id}")
    ResponseEntity<?> updateResource(@PathVariable String id, @RequestBody JsonNode requestBody) {
        if(Objects.requireNonNull(environment.getProperty("notification.sendNotificationToListeners")).equalsIgnoreCase("true")) {
            var resource = resourceService.updateResource(id, requestBody, true);
            return foldResultWithStatus(resource, HttpStatus.OK);
        } else {
            var resource = resourceService.updateResource(id, requestBody, false);
            return foldResultWithStatus(resource, HttpStatus.OK);
        }
    }
}

