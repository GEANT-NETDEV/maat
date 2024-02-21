package org.geant.maat.service;
import org.geant.maat.notification.NotificationConfiguration;
import org.geant.maat.notification.NotificationService;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class ServiceConfiguration {

    public ServiceService inMemoryServiceService() {
        return baseServiceService(new InMemoryServiceRepository(),
                new NotificationConfiguration().inMemoryNotificationService(),
                ServiceHrefBuilder.builder());
    }

    public ServiceService serviceServiceWithTestMongo() {
        var mongo = mongoRepositoryService(mongoClientService("admin", "abc123", "localhost"));
        mongo.clean();
        return baseServiceService(mongo,
                new NotificationConfiguration().inMemoryNotificationService(),
                ServiceHrefBuilder.builder());
    }

    InMemoryServiceRepository repository() {
        return new InMemoryServiceRepository();
    }

    @Bean
    MongoClient mongoClientService(
            @Value("${mongo-user}") String user,
            @Value("${mongo-password}") String password,
            @Value("${mongo-host}") String host) {
        return MongoClients.create(String.format("mongodb://%s:%s@%s", user, password, host));
    }

    @Bean
    ServiceRepository mongoRepositoryService(MongoClient mongoClientService) {
        return new org.geant.maat.service.MongoRepository(mongoClientService);
    }




    // @ConditionalOnProperty(name = "serviceService.type", havingValue = "base", matchIfMissing = true)
    @Bean
    @ConditionalOnProperty(name = "serviceService.type", havingValue = "base", matchIfMissing = true)
    ServiceService baseServiceService(
            ServiceRepository serviceRepository,
            NotificationService notificationService,
            ServiceHrefBuilder hrefBuilder) {
        return new BaseServiceService(serviceRepository, notificationService, new ServiceCreator(hrefBuilder));
    }

}
