package org.geant.maat.service;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vavr.control.Either;
import org.geant.maat.common.UserDataFilters;
import org.geant.maat.infrastructure.DomainError;
import org.geant.maat.infrastructure.ResultMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/serviceInventoryManagement")
@Tag(name = "API Service Inventory Management")
public class ServiceController implements ResultMapper{

    private final ServiceService serviceService;

    @Value("${keycloak.enabled}")
    private String keycloakStatus;
    @Value("${keycloak.authorization.l2.filters}")
    private String keycloakAuthorizationL2Status;

    @Autowired
    public Environment environment;

    @Autowired
    ServiceController(ServiceService serviceService) {
        this.serviceService = serviceService;
    }

    @Operation(
            operationId = "listService",
            summary = "List or find Service objects",
            description = "This operation list or find Service entities")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = { @Content( mediaType = "application/json") }),
            @ApiResponse(responseCode = "400", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "401", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "403", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "404", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "405", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "409", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "500", content = { @Content(schema = @Schema()) }) })
    @GetMapping("/${api.service.version}/service")
    ResponseEntity<Collection<JsonNode>> getServices(
            @RequestParam(required = false, defaultValue = "") List<String> fields,
            @RequestParam Map<String, String> allRequestParams,
            @RequestParam(required = false, defaultValue = "0") int offset,
            @RequestParam(required = false, defaultValue = "0") int limit
    ) {
        allRequestParams.remove("fields");
        allRequestParams.remove("offset");
        allRequestParams.remove("limit");

        if (Objects.equals(keycloakStatus, "true") && Objects.equals(keycloakAuthorizationL2Status, "true")) {
            UserDataFilters userFilters = new UserDataFilters(serviceService, false);
            if (offset == 0 && limit == 0) {
                var allServicesWithFilters = userFilters.getFilter(fields, allRequestParams, "service");
                return toResponseEntity(allServicesWithFilters, allServicesWithFilters.size());
            } else {
                var allServicesWithFilters = userFilters.getFilter(fields, allRequestParams, offset, limit, "service");
                return toResponseEntity(allServicesWithFilters, allServicesWithFilters.size());
            }
        } else {
            var allServices = serviceService.getServices(fields, allRequestParams);
            if (offset == 0 && limit == 0) {
                return toResponseEntity(allServices, allServices.size());
            }
            return toResponseEntity(serviceService.getServices(fields, allRequestParams, offset, limit),
                    allServices.size());
        }
    }

    private ResponseEntity<Collection<JsonNode>> toResponseEntity(Collection<JsonNode> services, long totalCount) {
        return ResponseEntity.status(200)
                .header("X-Total-Count", Long.toString(totalCount))
                .body(services);
    }

    @Operation(
            operationId = "retrieveService",
            summary = "Retrieves a Service by ID",
            description = "This operation retrieves a Service entity. Attribute selection is enabled for all first level attributes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = { @Content( mediaType = "application/json") }),
            @ApiResponse(responseCode = "400", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "401", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "403", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "404", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "405", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "409", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "500", content = { @Content(schema = @Schema()) }) })
    @GetMapping("/${api.service.version}/service/{id}")
    ResponseEntity<?> getService(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "") List<String> fields,
            @RequestParam Map<String, String> allRequestParams
    ) {
        allRequestParams.remove("fields");

        if (Objects.equals(keycloakStatus, "true") && Objects.equals(keycloakAuthorizationL2Status, "true")) {
            UserDataFilters userFilters = new UserDataFilters(serviceService, false);
            var serviceByIdWithFilters = userFilters.getFilterById(id, fields, "service");
            return foldResultWithStatus(serviceByIdWithFilters, HttpStatus.OK);
        } else {
            var service = serviceService.getService(id, fields);
            return foldResultWithStatus(service, HttpStatus.OK);
        }
    }

    @Operation(
            operationId = "createService",
            summary = "Creates a Service",
            description = "This operation creates a Service entity.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "400", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "401", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "403", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "405", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "409", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "500", content = { @Content(schema = @Schema()) }) })
    @PostMapping(value = "/${api.service.version}/service")
    ResponseEntity<?> addService(@RequestBody JsonNode requestBody) {

        if(Objects.requireNonNull(environment.getProperty("notification.sendNotificationToListeners")).equalsIgnoreCase("true")) {
            if (Objects.equals(keycloakStatus, "true") && Objects.equals(keycloakAuthorizationL2Status, "true")) {
                UserDataFilters userFilters = new UserDataFilters(serviceService, true);
                Either<DomainError, JsonNode> service = userFilters.postFilter(requestBody, "service");
                return foldResultWithStatus(service, HttpStatus.CREATED);
            } else {
                var service = serviceService.createService(requestBody, true);
                return foldResultWithStatus(service, HttpStatus.CREATED);
            }
        } else {
            if (Objects.equals(keycloakStatus, "true") && Objects.equals(keycloakAuthorizationL2Status, "true")) {
                UserDataFilters userFilters = new UserDataFilters(serviceService, false);
                Either<DomainError, JsonNode> service = userFilters.postFilter(requestBody, "service");
                return foldResultWithStatus(service, HttpStatus.CREATED);
            } else {
                var service = serviceService.createService(requestBody, false);
                return foldResultWithStatus(service, HttpStatus.CREATED);
            }
        }

    }
    @Operation(
            operationId = "deleteService",
            summary = "Deletes a Service",
            description = "This operation deletes a Service entity.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "400", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "401", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "403", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "404", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "405", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "409", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "500", content = { @Content(schema = @Schema()) }) })
    @DeleteMapping(value = "/${api.service.version}/service/{id}")
    ResponseEntity<?> deleteService(@PathVariable String id) {

        if(Objects.requireNonNull(environment.getProperty("notification.sendNotificationToListeners")).equalsIgnoreCase("true")) {
            if (Objects.equals(keycloakStatus, "true") && Objects.equals(keycloakAuthorizationL2Status, "true")) {
                UserDataFilters userFilters = new UserDataFilters(serviceService, true);
                Either<DomainError, String> service = userFilters.deleteFilter(id, "service");
                return foldResultWithStatus(service, HttpStatus.NO_CONTENT);
            } else {
                var service = serviceService.deleteService(id, true);
                return foldResultWithStatus(service, HttpStatus.NO_CONTENT);
            }
        } else {
            if (Objects.equals(keycloakStatus, "true") && Objects.equals(keycloakAuthorizationL2Status, "true")) {
                UserDataFilters userFilters = new UserDataFilters(serviceService, false);
                Either<DomainError, String> service = userFilters.deleteFilter(id, "service");
                return foldResultWithStatus(service, HttpStatus.NO_CONTENT);
            } else {
                var service = serviceService.deleteService(id, false);
                return foldResultWithStatus(service, HttpStatus.NO_CONTENT);
            }
        }
    }

    @Operation(
            operationId = "patchService",
            summary = "Updates partially a Service",
            description = "This operation updates partially a Service entity.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = { @Content( mediaType = "application/json") }),
            @ApiResponse(responseCode = "400", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "401", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "403", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "404", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "405", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "409", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "500", content = { @Content(schema = @Schema()) }) })
    @PatchMapping(value = "/${api.service.version}/service/{id}")
    ResponseEntity<?> updateService(@PathVariable String id,
                                    @RequestBody JsonNode requestBody) {

        if(Objects.requireNonNull(environment.getProperty("notification.sendNotificationToListeners")).equalsIgnoreCase("true")) {
            if (Objects.equals(keycloakStatus, "true") && Objects.equals(keycloakAuthorizationL2Status, "true")) {
                UserDataFilters userFilters = new UserDataFilters(serviceService, true);
                Either<DomainError, JsonNode> service = userFilters.patchFilter(id, requestBody, "service");
                return foldResultWithStatus(service, HttpStatus.OK);
            } else {
                var service = serviceService.updateService(id, requestBody, true);
                return foldResultWithStatus(service, HttpStatus.OK);
            }
        } else {
            if (Objects.equals(keycloakStatus, "true") && Objects.equals(keycloakAuthorizationL2Status, "true")) {
                UserDataFilters userFilters = new UserDataFilters(serviceService, false);
                Either<DomainError, JsonNode> service = userFilters.patchFilter(id, requestBody, "service");
                return foldResultWithStatus(service, HttpStatus.OK);
            } else {
                var service = serviceService.updateService(id, requestBody, false);
                return foldResultWithStatus(service, HttpStatus.OK);
            }
        }
    }

}
