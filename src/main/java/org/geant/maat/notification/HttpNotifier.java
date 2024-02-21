package org.geant.maat.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import io.vavr.control.Either;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

class HttpNotifier extends Notifier {
    private final HttpClient client;

    public HttpNotifier() {
        client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(200)).build();
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
                .uri(uri)
                .build();

        return Either.right(String.valueOf(client.sendAsync(request, HttpResponse.BodyHandlers.ofString())));
    }
}
