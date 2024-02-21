package org.geant.maat.resource;

import org.geant.maat.notification.NotificationConfiguration;
import org.geant.maat.notification.NotificationService;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResourceConfiguration {
    public ResourceService inMemoryResourceService() {
        return baseResourceService(new InMemoryResourceRepository(),
                               new NotificationConfiguration().inMemoryNotificationService(),
                               ResourceHrefBuilder.builder());
    }

    public ResourceService resourceServiceWithTestMongo() {
        var mongo = mongoRepository(mongoClient("admin", "abc123", "localhost"));
        mongo.clean();
        return baseResourceService(mongo,
                               new NotificationConfiguration().inMemoryNotificationService(),
                               ResourceHrefBuilder.builder());
    }

    InMemoryResourceRepository repository() {
        return new InMemoryResourceRepository();
    }

    @Bean
    MongoClient mongoClient(
            @Value("${mongo-user}") String user,
            @Value("${mongo-password}") String password,
            @Value("${mongo-host}") String host) {
        return MongoClients.create(String.format("mongodb://%s:%s@%s", user, password, host));
    }

    @Bean
    ResourceRepository mongoRepository(MongoClient mongoClient) {
        return new MongoRepository(mongoClient);
    }


   // @ConditionalOnProperty(name = "resourceService.type", havingValue = "base", matchIfMissing = true)
    @Bean
    @ConditionalOnProperty(name = "resourceService.type", havingValue = "base", matchIfMissing = true)
    ResourceService baseResourceService(
            ResourceRepository resourceRepository,
            NotificationService notificationService,
            ResourceHrefBuilder hrefBuilder) {
        return new BaseResourceService(resourceRepository, notificationService, new ResourceCreator(hrefBuilder));
    }


    //@ConditionalOnProperty(name = "resourceService.type", havingValue = "extended")
    @Bean
    @ConditionalOnProperty(name = "resourceService.type", havingValue = "extended")
    ResourceService extendedResourceService(
            ResourceRepository resourceRepository,
            NotificationService notificationService,
            ResourceHrefBuilder hrefBuilder) {
        return new ExtendedResourceService(resourceRepository, notificationService, new ResourceCreator(hrefBuilder));
    }
}
