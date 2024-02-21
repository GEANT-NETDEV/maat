package org.geant.maat.notification;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.geant.maat.infrastructure.ResultMapper;
import org.geant.maat.notification.dto.CreateListenerDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.io.IOException;


@RestController
@ConditionalOnProperty(name = "notification.sendNotificationToListeners", havingValue = "true", matchIfMissing = true)
@Tag(name = "API Listener Inventory Management")
class NotificationController implements ResultMapper {
    private final NotificationService notificationService;

    NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(
            operationId = "registerListener",
            summary = "Register a listener",
            description = "Sets the communication endpoint address the service instance must use to deliver information about its health state, execution state, failures and metrics.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "400", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "401", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "403", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "405", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "409", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "500", content = { @Content(schema = @Schema()) }) })
    @PostMapping("/hub")
    ResponseEntity<?> createListener(@Valid @RequestBody CreateListenerDto dto) throws IOException {
        var result = notificationService.addListener(dto);
        return foldResultWithStatus(result, HttpStatus.CREATED);
    }

    @Operation(
            operationId = "unregisterListener",
            summary = "Unregister a listener",
            description = "Resets the communication endpoint address the service instance must use to deliver information about its health state, execution state, failures and metrics.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "400", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "401", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "403", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "404", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "405", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "500", content = { @Content(schema = @Schema()) }) })
    @DeleteMapping("/hub/{id}")
    ResponseEntity<?> deleteListener(@PathVariable String id) {
        var result = notificationService.deleteListener(id);
        return foldResultWithStatus(result, HttpStatus.NO_CONTENT);
    }

    @Operation(
            operationId = "listListener",
            summary = "List or find Listener objects",
            description = "This operation list or find Listener entities")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = { @Content( mediaType = "application/json") }),
            @ApiResponse(responseCode = "400", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "401", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "403", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "404", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "405", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "409", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "500", content = { @Content(schema = @Schema()) }) })
    @GetMapping("/hub")
    ResponseEntity<?> getListeners() {
        var result = notificationService.getListeners();
        return foldResultWithStatus(result, HttpStatus.OK);
    }
    @Operation(
            operationId = "retrieveListener",
            summary = "Retrieves a Listener by ID",
            description = "This operation retrieves a Listener entity.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = { @Content( mediaType = "application/json") }),
            @ApiResponse(responseCode = "400", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "401", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "403", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "404", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "405", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "409", content = { @Content(schema = @Schema()) }),
            @ApiResponse(responseCode = "500", content = { @Content(schema = @Schema()) }) })
    @GetMapping("/hub/{id}")
    ResponseEntity<?> getListener(@PathVariable String id) {
        var result = notificationService.getListener(id);
        return foldResultWithStatus(result, HttpStatus.OK);
    }




}
