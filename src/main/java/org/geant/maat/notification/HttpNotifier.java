package org.geant.maat.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

class HttpNotifier extends Notifier {
    private static final Logger logger = LoggerFactory.getLogger(HttpNotifier.class);
    private static final int RESPONSE_BODY_LOG_LIMIT = 300;
    private static final int HTTP_SUCCESS_MIN = 200;
    private static final int HTTP_SUCCESS_MAX = 299;
    private static final Duration CONNECT_TIMEOUT = Duration.ofMillis(200);
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(5);
    private final HttpClient client;

    public HttpNotifier() {
        client = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
    }

    @Override
    public Either<String, String> sendNotification(Listener listener, Event event) {
        String body = null;
        try {
            body = new ObjectMapper().registerModule(new JSR310Module()).writeValueAsString(event);
        } catch (JsonProcessingException e) {
            return Either.left(String.format("Jackson parsing exception - '%s", e));
        }
        URI uri = null;
        try {
            uri = new URI(listener.callback().toString());
        } catch (URISyntaxException e) {
            return Either.left(String.format("Uri not valid - '%s'", listener.callback()));
        }
        var request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .uri(uri)
                .timeout(RESPONSE_TIMEOUT)
                .build();
        long startTimeNanos = System.nanoTime();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .whenComplete((response, throwable) -> {
                    long durationMs = Duration.ofNanos(System.nanoTime() - startTimeNanos).toMillis();
                    if (throwable != null) {
                        logger.warn("EVENT_DELIVERY_ERROR eventId={} listenerId={} callback={} durationMs={} error={}",
                                event.eventId(),
                                listener.id(),
                                listener.callback(),
                                durationMs,
                                throwable.getMessage());
                        return;
                    }

                    int statusCode = response.statusCode();
                    String responseBody = abbreviate(response.body());
                    if (isSuccessStatus(statusCode)) {
                        logger.info("EVENT_DELIVERED eventId={} listenerId={} callback={} statusCode={} durationMs={} body={}",
                                event.eventId(),
                                listener.id(),
                                listener.callback(),
                                statusCode,
                                durationMs,
                                responseBody);
                    } else {
                        logger.warn("EVENT_DELIVERY_FAILED eventId={} listenerId={} callback={} statusCode={} durationMs={} body={}",
                                event.eventId(),
                                listener.id(),
                                listener.callback(),
                                statusCode,
                                durationMs,
                                responseBody);
                    }
                });

        return Either.right(String.format("HTTP request accepted for callback=%s", listener.callback()));
    }

    private static String abbreviate(String body) {
        if (body == null || body.isBlank()) {
            return "<empty>";
        }

        String normalizedBody = body.replaceAll("\\s+", " ").trim();
        if (normalizedBody.length() <= RESPONSE_BODY_LOG_LIMIT) {
            return normalizedBody;
        }
        return normalizedBody.substring(0, RESPONSE_BODY_LOG_LIMIT) + "...";
    }

    private static boolean isSuccessStatus(int statusCode) {
        return statusCode >= HTTP_SUCCESS_MIN && statusCode <= HTTP_SUCCESS_MAX;
    }
}
