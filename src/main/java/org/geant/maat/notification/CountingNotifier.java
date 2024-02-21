package org.geant.maat.notification;

import io.vavr.control.Either;

class CountingNotifier extends Notifier {
    private int count = 0;
    private int countNullEvent = 0;


    @Override
    public Either<String, String> sendNotification(Listener listener, Event event) {
        count++;
        if(event.eventType() == null) {
            countNullEvent++;
        }
        return Either.right("OK");
    }

    public int getSentNotificationsCount() {
        return count;
    }
}
