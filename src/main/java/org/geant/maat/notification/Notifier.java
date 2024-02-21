package org.geant.maat.notification;

import io.vavr.control.Either;

abstract class Notifier {
    public void notifyListener(Listener listener, Event event) {
        if (listener.wantsEvent(event)) {
            sendNotification(listener, event);
        }
    }

    abstract Either<String, String> sendNotification(Listener listener, Event event);
}
