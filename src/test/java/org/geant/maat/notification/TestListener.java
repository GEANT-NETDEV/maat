package org.geant.maat.notification;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.vavr.control.Option;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;

class TestListener extends org.geant.maat.integration.testcontainers.BaseTestContainers{
    public final URL address;
    private final TestHandler handler;
    private final HttpServer httpServer;

    public TestListener(int port) throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        address = new URL("http://localhost:" + port);
        handler = new TestHandler();
        httpServer.createContext("/", handler);
        httpServer.start();
    }

    public Option<String> listenForOneMessage(int timeout) throws InterruptedException {
        int counter = 0;

        while (handler.lastRequestBody.isEmpty() && timeout * 1000 >= counter) {
            Thread.sleep(50);
            counter += 50;
        }

        var body = handler.lastRequestBody;
        handler.forgetLastRequestBody();
        return body;
    }

    public void stop() {
        httpServer.stop(0);
    }

    private static class TestHandler implements HttpHandler {
        Option<String> lastRequestBody = Option.none();

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            lastRequestBody = Option.of(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String response = "OK";
            exchange.getResponseHeaders().add("Content-Type", "text/html");
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length());
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
        }

        void forgetLastRequestBody() {
            lastRequestBody = Option.none();
        }
    }
}
