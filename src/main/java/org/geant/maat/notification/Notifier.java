package org.geant.maat.notification;

import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class Notifier {
    private static final Logger logger = LoggerFactory.getLogger(Notifier.class);

    public void notifyListener(Listener listener, Event event) {
        if (!listener.wantsEvent(event)) {
            logger.debug("Skipping event id={} type={} for listener id={} callback={} because query does not match",
                    event.eventId(), event.eventType(), listener.id(), listener.callback());
            return;
        }

        logger.info("Sending event id={} type={} to listener id={} callback={} changedByUser={}",
                event.eventId(), event.eventType(), listener.id(), listener.callback(), event.changedByUser());

        Either<String, String> result = sendNotification(listener, event);
        result.peek(response -> logger.info("Event id={} dispatch started for listener id={} callback={} details={}",
                        event.eventId(), listener.id(), listener.callback(), response))
                .peekLeft(error -> logger.warn("Event id={} dispatch failed for listener id={} callback={}: {}",
                        event.eventId(), listener.id(), listener.callback(), error));
    }

    abstract Either<String, String> sendNotification(Listener listener, Event event);
}
