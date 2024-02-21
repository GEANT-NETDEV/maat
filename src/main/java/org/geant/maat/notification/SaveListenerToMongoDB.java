package org.geant.maat.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Projections;
import io.vavr.control.Either;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.geant.maat.infrastructure.DomainError;
import org.geant.maat.notification.dto.ListenerCreatedDto;
import java.util.*;

import static com.mongodb.client.model.Filters.*;


public class SaveListenerToMongoDB {
    private final MongoCollection<BaseListener> collection;

    public SaveListenerToMongoDB(String mongoConnectionData){

        MongoClient client;
        if (mongoConnectionData != null) {
            client = MongoClients.create(mongoConnectionData);
        } else {
            client = MongoClients.create("mongodb://admin:abc123@localhost");
        }

        String databaseName = "listeners_db";
        String collectionName = "listeners";

        CodecRegistry codecRegistry = CodecRegistries.fromProviders(
                CodecRegistries.fromProviders(new BaseListenerCodecProvider()),
                MongoClientSettings.getDefaultCodecRegistry());

        collection = client.getDatabase(databaseName).getCollection(collectionName,BaseListener.class).withCodecRegistry(codecRegistry);
    }

    public SaveListenerToMongoDB(String mongoConnectionData, String testCollection){
        MongoClient client = MongoClients.create(mongoConnectionData);

        String databaseName = "listeners_db";
        String collectionName = testCollection;

        CodecRegistry codecRegistry = CodecRegistries.fromProviders(
                CodecRegistries.fromProviders(new BaseListenerCodecProvider()),
                MongoClientSettings.getDefaultCodecRegistry());

        collection = client.getDatabase(databaseName).getCollection(collectionName,BaseListener.class).withCodecRegistry(codecRegistry);

        try {
            collection.drop();
        } catch (MongoException e){
            System.out.println("Can't drop test collection!");
        }
    }

    public Either<DomainError,ListenerCreatedDto> save(Either<DomainError,ListenerCreatedDto> dto) throws JsonProcessingException {
        String id = dto.get().id();
        String callback = dto.get().callback();
        String query = dto.get().query();
        String jsonString = String.format("{\"id\":\"%s\",\"callback\":\"%s\",\"query\":\"%s\"}",id, callback, query);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(jsonString);

        try{
            collection.insertOne(new BaseListener(json));
            return Either.right(dto.get());
        } catch(MongoException e){
            return Either.left(new DomainError("Connection with database lost.",Error.DB_CONNECTION_ERROR));
        }
    }

    public Either<DomainError, ?> deleteListener(String id) {
        try {
            var result = collection.deleteOne(eq("_id", id));
            if (result.getDeletedCount() == 0) {
                return Either.left(new DomainError("Can not find listener with id: " + id, Error.ID_MISSING));
            } else {
                return Either.right("Deleted listener with id: " + id);
            }
        } catch (MongoException e) {
            return Either.left(new DomainError("Connection with database lost.", Error.DB_CONNECTION_ERROR));
        }
    }

    public Either<DomainError,Collection<JsonNode>> findAll(List<String> fields, Map<String, String> filtering) {
        try {
            return Either.right(findAllListeners(fields, filtering).into(new ArrayList<>())
                    .stream()
                    .map(BaseListener::toJson)
                    .toList());
        } catch (MongoException e) {
            return Either.left(new DomainError("Connection with database lost", Error.DB_CONNECTION_ERROR));
        }
    }

    public FindIterable<BaseListener> findAllListeners(List<String> fields, Map<String, String> filtering) {
        var filters = new ArrayList<Bson>();
        filtering.forEach((key, value) -> filters.add(eq(key, value)));
        if (filters.isEmpty()) {
            if (fields.isEmpty()) {
                return collection.find();
            } else {
                return collection.find().projection(Projections.include(fields));
            }
        }
        return collection.find(and(filters)).projection(Projections.include(fields));
    }

}
