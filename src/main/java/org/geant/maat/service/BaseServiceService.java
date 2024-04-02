package org.geant.maat.service;

import com.fasterxml.jackson.databind.JsonNode;
import io.vavr.control.Either;
import org.geant.maat.infrastructure.DomainError;
import org.geant.maat.notification.EventType;
import org.geant.maat.notification.NotificationService;
import org.geant.maat.notification.dto.EventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class BaseServiceService implements ServiceService {

    private final ServiceRepository serviceRepository;
    private final ServiceCreator creator;
    private final NotificationService notifications;
    private final ServiceUpdater updater;
    private final ServiceFinder finder;

    BaseServiceService(
            ServiceRepository serviceRepository,
            NotificationService notifications,
            ServiceCreator serviceCreator) {
        this.serviceRepository = serviceRepository;
        this.notifications = notifications;
        this.creator = serviceCreator;
        this.updater = new ServiceUpdater(serviceRepository);
        this.finder = new ServiceFinder(serviceRepository);
    }

    @Override
    public Optional<Service> getService(String id) {
        BaseServiceService.BaseServiceLogger.info("Getting service with id " + id);

        var service = serviceRepository.find(id);

        service.ifPresentOrElse(
                r -> BaseServiceService.BaseServiceLogger.infoJson("Service found:", r.toJson()),
                () -> BaseServiceService.BaseServiceLogger.info("Could not find service " + id));
        return service;
    }

    @Override
    public Either<DomainError, JsonNode> createService(JsonNode json, Boolean registerNewEventFlag) {
        BaseServiceService.BaseServiceLogger.infoJson("Creating service from json:", json);

        var service = creator.create(json).flatMap(serviceRepository::save);

        if (registerNewEventFlag) service.peek(r -> notifications.registerNewEvent(new EventDto(EventType.ServiceCreateEvent, r.toJson())));

        service.peek(r -> BaseServiceService.BaseServiceLogger.infoJson("Service created: ", r.toJson()))
                .peekLeft(error -> BaseServiceService.BaseServiceLogger.info("Could not create service, because: " + error.message()));
        return service.map(Service::toJson);
    }

    @Override
    public Either<DomainError, JsonNode> getService(String id, Collection<String> propsToFilter) {
        BaseServiceService.BaseServiceLogger.info(String.format("Getting service %s, properties: %s", id, propsToFilter));

        return serviceRepository.find(id, propsToFilter)
                .peek(json -> BaseServiceService.BaseServiceLogger.infoJson(String.format("Service %s found", id), json))
                .peekLeft(error -> BaseServiceService.BaseServiceLogger.info(String.format("Getting %s failed: %s", id, error.message())));
    }

    @Override
    public Either<DomainError, String> deleteService(String id, Boolean registerNewEventFlag) {
        BaseServiceService.BaseServiceLogger.info(String.format("Deleting service %s", id));

        var deletedService = getService(id, new ArrayList<>());

        var result = serviceRepository.delete(id)
                .peek(s -> BaseServiceService.BaseServiceLogger.infoJson(String.format("Service %s deleted", id), deletedService.get()))
                .peekLeft(error -> BaseServiceService.BaseServiceLogger.info("Deleting failed: " + error.message()));

        if (registerNewEventFlag) result.peek(r -> notifications.registerNewEvent(new EventDto(EventType.ServiceDeleteEvent, deletedService.get())));

        return result;
    }

    @Override
    public Either<DomainError, JsonNode> updateService(String id, JsonNode updateJson, Boolean registerNewEventFlag) {
        BaseServiceService.BaseServiceLogger.infoJson(String.format("Updating service %s, update json:", id), updateJson);

        var result = updater.update(id, updateJson)
                .peek(json -> BaseServiceService.BaseServiceLogger.infoJson(String.format("Service %s updated successfully", id), json))
                .peekLeft(error -> BaseServiceService.BaseServiceLogger.info(String.format("Update of %s failed: %s", id, error.message())));

        if (registerNewEventFlag) result.peek(json -> notifications.registerNewEvent(new EventDto(EventType.ServiceAttributeValueChangeEvent, json)));

        return result;
    }

    @Override
    public Collection<JsonNode> getServices(List<String> fields, Map<String, String> filtering) {
        BaseServiceService.BaseServiceLogger.info(String.format("Getting services' fields %s matching filters %s", fields, filtering));

        var services = finder.find(fields, filtering);

        BaseServiceService.BaseServiceLogger.info(String.format("Found %d services", services.size()));
        return services;
    }

    @Override
    public Collection<JsonNode> getServices(List<String> fields, Map<String, String> filtering, int offset, int limit) {
        BaseServiceService.BaseServiceLogger.info(String.format("Getting services' fields %s matching filters %s offset %s limit %s",
                fields, filtering, offset, limit));

        var services = finder.find(fields, filtering, offset, limit);

        BaseServiceService.BaseServiceLogger.info(String.format("Found %d services", services.size()));
        return services;
    }

    @Override
    public Either<DomainError, JsonNode> updateServiceNC(String id, JsonNode updateJson) {
        BaseServiceService.BaseServiceLogger.infoJson(String.format("Updating service %s, update json:", id), updateJson);

        var result = updater.update(id, updateJson)
                .peek(json -> BaseServiceService.BaseServiceLogger.infoJson(String.format("Service %s updated successfully", id), json))
                .peekLeft(error -> BaseServiceService.BaseServiceLogger.info(String.format("Update of %s failed: %s", id, error.message())));


        return result;
    }

    private static class BaseServiceLogger {
        private static final Logger logger = LoggerFactory.getLogger(ServiceService.class);

        public static void infoJson(String prefix, JsonNode json) {
            logger.info(String.format("%s%s%s", prefix, System.lineSeparator(), json.toPrettyString()));
        }

        public static void info(String message) {
            logger.info(message);
        }
    }

}
