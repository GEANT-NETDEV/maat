package org.geant.maat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Projections;
import io.vavr.control.Either;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.geant.maat.infrastructure.DomainError;
import org.geant.maat.service.dto.BaseService;

import java.util.*;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Filters.regex;
import static com.mongodb.client.model.Projections.excludeId;
class MongoRepository implements ServiceRepository {

    private final MongoCollection<BaseService> collection;

    public MongoRepository(MongoClient client, String database_name, String collection_name) {
        CodecRegistry codecRegistry = CodecRegistries.fromProviders(
                CodecRegistries.fromProviders(new BaseServiceCodecProvider()),
                MongoClientSettings.getDefaultCodecRegistry());
        collection = client.getDatabase(database_name)
                .getCollection(collection_name, BaseService.class)
                .withCodecRegistry(codecRegistry);
    }

    @Override
    public Optional<Service> find(String id) {
        var result = collection.find(eq("_id", id)).first();
        if (result == null) {
            return Optional.empty();
        }
        return Optional.of(new Service(result.toJson()));
    }

    @Override
    public Either<DomainError, JsonNode> find(String id, Collection<String> propsToFilter) {
        BaseService ser;
        if (propsToFilter.isEmpty()) {
            ser = collection.find(eq("_id", id)).first();
        } else {
            var projection = propsToFilter.contains("id") ?
                    Projections.include(new ArrayList<>(propsToFilter)) :
                    Projections.fields(Projections.include(new ArrayList<>(propsToFilter)), excludeId());
            ser = collection.find(eq("_id", id)).projection(projection).first();
        }

        if (ser != null) {
            return Either.right(ser.toJson());
        }
        return Either.left(new DomainError("Service not found", Error.SERVICE_MISSING));
    }

    @Override
    public Collection<JsonNode> findAll(List<String> fields, Map<String, String> filtering) {
        if(filtering.containsKey("id")){
            filtering.put("_id", filtering.get("id"));
            filtering.remove("id");
        }
        return findAllQuery(fields, filtering).into(new ArrayList<>())
                .stream()
                .map(BaseService::toJson)
                .toList();
    }

    @Override
    public Either<DomainError, Service> save(Service service) {
        var result = collection.insertOne(new BaseService(service.toJson()));

        // TODO errors handling?
        return Either.right(service);
    }

    @Override
    public Either<DomainError, String> delete(String id) {
        var result = collection.deleteOne(eq("_id", id));
        if (result.getDeletedCount() == 0) {
            return Either.left(new DomainError("No service with given id: " + id, Error.SERVICE_MISSING));
        }
        return Either.right(id);
    }

    @Override
    public Either<DomainError, JsonNode> update(String id, JsonNode updateJson) {
        // TODO Error handling
        var result = collection.replaceOne(eq("_id", id), new BaseService(updateJson));
        return Either.right(updateJson);
    }

    @Override
    public void clean() {
        collection.drop();
    }

    @Override
    public Collection<JsonNode> findAll(List<String> fields, Map<String, String> filtering, int offset, int limit) {
        if(filtering.containsKey("id")){
            filtering.put("_id", filtering.get("id"));
            filtering.remove("id");
        }
        return findAllQuery(fields, filtering)
                .skip(offset)
                .limit(limit)
                .into(new ArrayList<>())
                .stream()
                .map(BaseService::toJson)
                .toList();
    }

    public FindIterable<BaseService> findAllQuery(List<String> fields, Map<String, String> filtering) {
        if(filtering.containsKey("id")){
            filtering.put("_id", filtering.get("id"));
            filtering.remove("id");
        }
        var filters = new ArrayList<Bson>();
        filtering.forEach((key, value) -> {
            if (value.contains("*")) {
                filters.add(regex(key, value, "i"));
            } else {
                filters.add(regex(key, "^" + value + "$", "i"));
            }
        });

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
