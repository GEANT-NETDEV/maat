package org.geant.maat.service;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.geant.maat.infrastructure.ResultMapper;
import org.springframework.beans.factory.annotation.Autowired;
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

        var allResources = serviceService.getServices(fields, allRequestParams);
        if (offset == 0 && limit == 0) {
            return toResponseEntity(allResources, allResources.size());
        }
        return toResponseEntity(serviceService.getServices(fields, allRequestParams, offset, limit),
                allResources.size());
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
        var service = serviceService.getService(id, fields);
        return foldResultWithStatus(service, HttpStatus.OK);
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
            var service = serviceService.createService(requestBody, true);
            return foldResultWithStatus(service, HttpStatus.CREATED);
        } else {
            var service = serviceService.createService(requestBody, false);
            return foldResultWithStatus(service, HttpStatus.CREATED);
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
            var service = serviceService.deleteService(id, true);
            return foldResultWithStatus(service, HttpStatus.NO_CONTENT);
        } else {
            var service = serviceService.deleteService(id, false);
            return foldResultWithStatus(service, HttpStatus.NO_CONTENT);
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
    ResponseEntity<?> updateService(@PathVariable String id, @RequestBody JsonNode requestBody) {
        if(Objects.requireNonNull(environment.getProperty("notification.sendNotificationToListeners")).equalsIgnoreCase("true")) {
            var service = serviceService.updateService(id, requestBody, true);
            return foldResultWithStatus(service, HttpStatus.OK);
        } else {
            var service = serviceService.updateService(id, requestBody, false);
            return foldResultWithStatus(service, HttpStatus.OK);
        }
    }

    @PatchMapping(value = "/${api.service.version}/serviceNC/{id}")
    ResponseEntity<?> updateServiceNC(@PathVariable String id, @RequestBody JsonNode requestBody) {
        var service = serviceService.updateServiceNC(id, requestBody);
        return foldResultWithStatus(service, HttpStatus.OK);
    }



}
