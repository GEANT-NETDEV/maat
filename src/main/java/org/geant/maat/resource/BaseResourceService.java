package org.geant.maat.resource;

import com.fasterxml.jackson.databind.JsonNode;
import io.vavr.control.Either;
import org.geant.maat.infrastructure.DomainError;
import org.geant.maat.notification.EventType;
import org.geant.maat.notification.NotificationService;
import org.geant.maat.notification.dto.EventDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class BaseResourceService implements ResourceService {

    private final ResourceRepository resourceRepository;
    private final ResourceCreator creator;
    private final NotificationService notifications;
    private final ResourceUpdater updater;
    private final ResourceFinder finder;
    BaseResourceService(
            ResourceRepository resourceRepository,
            NotificationService notifications,
            ResourceCreator resourceCreator) {
        this.resourceRepository = resourceRepository;
        this.notifications = notifications;
        this.creator = resourceCreator;
        this.updater = new ResourceUpdater(resourceRepository);
        this.finder = new ResourceFinder(resourceRepository);
    }

    @Override
    public Optional<Resource> getResource(String id) {
        BaseResourceLogger.info("Getting resource with id " + id);

        var resource = resourceRepository.find(id);

        resource.ifPresentOrElse(
                r -> BaseResourceLogger.infoJson("Resource found:", r.toJson()),
                () -> BaseResourceLogger.info("Could not find resource " + id));
        return resource;
    }

    @Override
    public Either<DomainError, JsonNode> createResource(JsonNode json, Boolean registerNewEventFlag) {
        BaseResourceLogger.infoJson("Creating resource from json:", json);

        var resource = creator.create(json).flatMap(resourceRepository::save);

        if (registerNewEventFlag) resource.peek(r -> notifications.registerNewEvent(new EventDto(EventType.ResourceCreateEvent, r.toJson())));

        resource.peek(r -> BaseResourceLogger.infoJson("Resource created: ", r.toJson()))
                .peekLeft(error -> BaseResourceLogger.info("Could not create resource, because: " + error.message()));
        return resource.map(Resource::toJson);
    }

    @Override
    public Either<DomainError, JsonNode> getResource(String id, Collection<String> propsToFilter) {
        BaseResourceLogger.info(String.format("Getting resource %s, properties: %s", id, propsToFilter));

        return resourceRepository.find(id, propsToFilter)
                .peek(json -> BaseResourceLogger.infoJson(String.format("Resource %s found", id), json))
                .peekLeft(error -> BaseResourceLogger.info(String.format("Getting %s failed: %s", id, error.message())));
    }

    @Override
    public Either<DomainError, String> deleteResource(String id, Boolean registerNewEventFlag) {
        BaseResourceLogger.info(String.format("Deleting resource %s", id));

        var deletedResource = getResource(id, new ArrayList<>());

        var result = resourceRepository.delete(id)
                .peek(s -> BaseResourceLogger.infoJson(String.format("Resource %s deleted", id), deletedResource.get()))
                .peekLeft(error -> BaseResourceLogger.info("Deleting failed: " + error.message()));

        if (registerNewEventFlag) result.peek(r -> notifications.registerNewEvent(new EventDto(EventType.ResourceDeleteEvent, deletedResource.get())));

        return result;
    }

    @Override
    public Either<DomainError, JsonNode> updateResource(String id, JsonNode updateJson, Boolean registerNewEventFlag) {
        BaseResourceLogger.infoJson(String.format("Updating resource %s, update json:", id), updateJson);

        var result = updater.update(id, updateJson)
                .peek(json -> BaseResourceLogger.infoJson(String.format("Resource %s updated successfully", id), json))
                .peekLeft(error -> BaseResourceLogger.info(String.format("Update of %s failed: %s", id, error.message())));

        if (registerNewEventFlag) result.peek(json -> notifications.registerNewEvent(new EventDto(EventType.ResourceAttributeValueChangeEvent, json)));

        return result;
    }

    @Override
    public Collection<JsonNode> getResources(List<String> fields, Map<String, String> filtering) {
        BaseResourceLogger.info(String.format("Getting resources' fields %s matching filters %s", fields, filtering));

        var resources = finder.find(fields, filtering);

        BaseResourceLogger.info(String.format("Found %d resources", resources.size()));
        return resources;
    }

    @Override
    public Collection<JsonNode> getResources(List<String> fields, Map<String, String> filtering, int offset, int limit) {
        BaseResourceLogger.info(String.format("Getting resources' fields %s matching filters %s offset %s limit %s",
                fields, filtering, offset, limit));

        var resources = finder.find(fields, filtering, offset, limit);

        BaseResourceLogger.info(String.format("Found %d resources", resources.size()));
        return resources;
    }


    private static class BaseResourceLogger {
        private static final Logger logger = LoggerFactory.getLogger(ResourceService.class);

        public static void infoJson(String prefix, JsonNode json) {
            logger.info(String.format("%s%s%s", prefix, System.lineSeparator(), json.toPrettyString()));
        }

        public static void info(String message) {
            logger.info(message);
        }
    }
}
