package org.geant.maat.notification;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class NotificationConfiguration {

    private String mongoConnectionData;

    public NotificationService inMemoryNotificationService() {
        return new NotificationService(mongoConnectionData, new CountingNotifier());
    }


    @Bean
    public NotificationService notificationService(Notifier notifier, @Value("${mongo-user}") String user,
                                                   @Value("${mongo-password}") String password,
                                                   @Value("${mongo-host}") String host) {
        mongoConnectionData = String.format("mongodb://%s:%s@%s", user, password, host);
        NotificationService service = new NotificationService(mongoConnectionData, notifier);
        return service;
    }

    @Bean
    public Notifier notifier() {
        return new HttpNotifier();
    }



}
