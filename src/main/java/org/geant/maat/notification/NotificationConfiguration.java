package org.geant.maat.notification;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.geant.maat.common.CurrentUserResolver;


@Configuration
public class NotificationConfiguration {

    private String mongoConnectionData;

    public NotificationService inMemoryNotificationService() {
        return inMemoryNotificationService(() -> CurrentUserResolver.UNKNOWN_USER);
    }

    public NotificationService inMemoryNotificationService(CurrentUserResolver currentUserResolver) {
        return new NotificationService(mongoConnectionData, new CountingNotifier(), currentUserResolver);
    }


    @Bean
    public NotificationService notificationService(Notifier notifier, @Value("${mongo-user}") String user,
                                                   @Value("${mongo-password}") String password,
                                                   @Value("${mongo-host}") String host,
                                                   CurrentUserResolver currentUserResolver) {
        mongoConnectionData = String.format("mongodb://%s:%s@%s", user, password, host);
        NotificationService service = new NotificationService(mongoConnectionData, notifier, currentUserResolver);
        return service;
    }

    @Bean
    public Notifier notifier() {
        return new HttpNotifier();
    }



}
