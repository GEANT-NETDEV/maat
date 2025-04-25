package org.geant.maat.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Projections;
import static com.mongodb.client.model.Filters.regex;
import io.vavr.control.Either;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.geant.maat.infrastructure.DomainError;
import org.geant.maat.resource.dto.BaseResource;

import java.util.*;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.excludeId;

class MongoRepository implements ResourceRepository {
    private final MongoCollection<BaseResource> collection;

    public MongoRepository(MongoClient client, String database_name, String collection_name) {
        CodecRegistry codecRegistry = CodecRegistries.fromProviders(
                CodecRegistries.fromProviders(new BaseResourceCodecProvider()),
                MongoClientSettings.getDefaultCodecRegistry());
        collection = client.getDatabase(database_name)
                .getCollection(collection_name, BaseResource.class)
                .withCodecRegistry(codecRegistry);
    }

    // TODO return Vavr Option
    @Override
    public Optional<Resource> find(String id) {
        var result = collection.find(eq("_id", id)).first();
        if (result == null) {
            return Optional.empty();
        }
        return Optional.of(new Resource(result.toJson()));
    }

    @Override
    public Either<DomainError, JsonNode> find(String id, Collection<String> propsToFilter) {
        BaseResource res;
        if (propsToFilter.isEmpty()) {
            res = collection.find(eq("_id", id)).first();
        } else {
            var projection = propsToFilter.contains("id") ?
                    Projections.include(new ArrayList<>(propsToFilter)) :
                    Projections.fields(Projections.include(new ArrayList<>(propsToFilter)), excludeId());
            res = collection.find(eq("_id", id)).projection(projection).first();
        }

        if (res != null) {
            return Either.right(res.toJson());
        }
        return Either.left(new DomainError("Resource not found", Error.RESOURCE_MISSING));
    }

    @Override
    public Collection<JsonNode> findAll(List<String> fields, Map<String, String> filtering) {
        if(filtering.containsKey("id")){
            filtering.put("_id", filtering.get("id"));
            filtering.remove("id");
        }
        return findAllQuery(fields, filtering).into(new ArrayList<>())
                .stream()
                .map(BaseResource::toJson)
                .toList();
    }

    @Override
    public Either<DomainError, Resource> save(Resource resource) {
        var result = collection.insertOne(new BaseResource(resource.toJson()));

        // TODO errors handling?
        return Either.right(resource);
    }


    @Override
    public Either<DomainError, String> delete(String id) {
        var result = collection.deleteOne(eq("_id", id));
        if (result.getDeletedCount() == 0) {
            return Either.left(new DomainError("No resource with given id: " + id, Error.RESOURCE_MISSING));
        }
        return Either.right(id);
    }

    @Override
    public Either<DomainError, JsonNode> update(String id, JsonNode updateJson) {
        // TODO Error handling
        var result = collection.replaceOne(eq("_id", id), new BaseResource(updateJson));
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
                .map(BaseResource::toJson)
                .toList();
    }

    public FindIterable<BaseResource> findAllQuery(List<String> fields, Map<String, String> filtering) {
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
