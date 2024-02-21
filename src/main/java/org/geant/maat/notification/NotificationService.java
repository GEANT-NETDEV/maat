package org.geant.maat.notification;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Either;
import org.geant.maat.infrastructure.DomainError;
import org.geant.maat.notification.dto.CreateListenerDto;
import org.geant.maat.notification.dto.EventDto;
import org.geant.maat.notification.dto.ListenerCreatedDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;


public class NotificationService {
    private final Collection<Listener> listeners;
    private final Notifier notifier;
    private final ListenerCreator creator;
    private static SaveListenerToMongoDB finder;

    @Autowired
    public Environment environment;


    public NotificationService(String mongoConnectionData, Notifier notifier) {
        this.notifier = notifier;
        this.creator = new ListenerCreator();
        this.listeners = new ArrayList<>();
        this.finder = new SaveListenerToMongoDB(mongoConnectionData);
    }

    public NotificationService(String mongoConnectionData, Notifier notifier, String testCollection) {
        this.notifier = notifier;
        this.creator = new ListenerCreator();
        this.listeners = new ArrayList<>();
        this.finder = new SaveListenerToMongoDB(mongoConnectionData, testCollection);
    }

    public Either<DomainError, ListenerCreatedDto> addListener(CreateListenerDto dto) throws JsonProcessingException {
        NotificationLogger.infoJson("Creating notifier from json:", dto);

        var result =  creator.createListener(dto)
                .peekLeft(error -> NotificationLogger.warning("Could not create listener: " + error.message()))
                .map(Listener::toListenerCreatedDto);

        if (result.isRight()) {
            return finder.save(result).peek(listener -> NotificationLogger.infoJson("Created listener:", listener))
                    .peekLeft(error -> NotificationLogger.warning("Could not create listener: " + error.message()));
        } else {
            return result;
        }
    }

    public Either<DomainError, ListenerCreatedDto> addListenerFromDb(CreateListenerDto dto, String id) {

        return creator.createListenerFromDb(dto,id)
                .peek(listeners::add)
                .map(Listener::toListenerCreatedDto);

    }

    public void registerNewEvent(EventDto eventDto) {
        // TODO log when notifier fails to send event
        if(Objects.requireNonNull(environment.getProperty("notification.sendNotificationToListeners")).equalsIgnoreCase("true")) {
            this.getListenersFromDb();
            listeners.forEach(listener -> notifier.notifyListener(listener, Event.from(eventDto)));
        }
    }

    public void registerNewEventForTests(EventDto eventDto) {
        // TODO log when notifier fails to send event
            this.getListenersFromDb();
            listeners.forEach(listener -> notifier.notifyListener(listener, Event.from(eventDto)));
    }

    public Either<DomainError, ?> deleteListener(String id) {
        NotificationLogger.info(String.format("Deleting listener with id %s", id));
        return finder.deleteListener(id)
                .peek(listener->NotificationLogger.info(String.format("Deleted listener with id %s successful", id)))
                .peekLeft(error -> NotificationLogger.warning(String.format("Deleting listener with id %s failed. ", id) + error.message()));
    }

    public Either<DomainError,Collection<ListenerCreatedDto>> getListeners() {
        var result = this.getListenersFromDb();
        if (result.isRight()) {
            return Either.right(listeners.stream().map(Listener::toListenerCreatedDto).toList());
        } else {
            return Either.left(result.getLeft());
        }
    }

    public Either<DomainError, ListenerCreatedDto> getListener(String id) {
        var result = this.getListenersFromDb();
        if (result.isRight()) {
            return listeners.stream()
                    .filter(listener -> listener.id().equals(id))
                    .findFirst()
                    .map(listener -> Either.<DomainError, ListenerCreatedDto>right(listener.toListenerCreatedDto()))
                    .orElse(Either.left(new DomainError(String.format("Notifier with id %s missing", id), () -> 404)));
        } else {
            return Either.left(result.getLeft());
        }
    }

    private static class NotificationLogger {
        private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
        private final static ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(
                JsonInclude.Include.NON_NULL);

        private static String format(Object object) {
            try {
                return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
            } catch (JsonProcessingException e) {
                logger.debug(String.format("Could not parse object: %s. Do not use infoJson for this.", object));
                return object.toString();
            }
        }


        public static void infoJson(String prefix, Object object) {
            logger.info(String.format("%s%s%s", prefix, System.lineSeparator(), format(object)));
        }

        public static void info(String message) {
            logger.info(message);
        }
        public static void warning(String message) { logger.warn(message); }

    }

    public Either<DomainError,?> getListenersFromDb() {
        listeners.clear();

        List<String> fields = new ArrayList<String>();
        Map<String, String> allRequestParams = new HashMap<>();

        var listeners = finder.findAll(fields, allRequestParams);

        if (listeners.isRight()) {
            NotificationLogger.info(String.format("Found %d listeners", listeners.get().size()));

            for (JsonNode json : listeners.get()) {
                String id = json.get("id").toString().replaceAll("\"", "");
                String callback = json.get("callback").toString().replaceAll("\"", "");
                String query = json.get("query").toString();

                URL urlCallback;

                try {
                    urlCallback = new URL(callback);
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }

                CreateListenerDto dto;

                if (query.equals("\"null\"")) {
                    dto = new CreateListenerDto(urlCallback, null);
                } else {
                    query = query.replaceAll("\"", "");
                    dto = new CreateListenerDto(urlCallback, query);
                }

                this.addListenerFromDb(dto, id);
            }
            return Either.right("");
        } else {
            NotificationLogger.warning("Connection with database lost. Can not get listeners from database.");
            return Either.left(listeners.getLeft());
        }
    }
}
