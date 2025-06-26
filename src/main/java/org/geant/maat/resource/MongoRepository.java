package org.geant.maat.resource;

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
import org.geant.maat.resource.dto.BaseResource;


import java.util.*;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.excludeId;
import static com.mongodb.client.model.Sorts.*;

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
    public Collection<JsonNode> findAll(List<String> fields, Map<String, String> filtering, String sort) {
        if(filtering.containsKey("id")){
            filtering.put("_id", filtering.get("id"));
            filtering.remove("id");
        }
        return findAllQuery(fields, filtering, sort).into(new ArrayList<>())
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
    public Collection<JsonNode> findAll(List<String> fields, Map<String, String> filtering, int offset, int limit, String sort) {
        if(filtering.containsKey("id")){
            filtering.put("_id", filtering.get("id"));
            filtering.remove("id");
        }
        return findAllQuery(fields, filtering, sort)
                .skip(offset)
                .limit(limit)
                .into(new ArrayList<>())
                .stream()
                .map(BaseResource::toJson)
                .toList();
    }

    public FindIterable<BaseResource> findAllQuery(List<String> fields, Map<String, String> filtering, String sort) {
        if(filtering.containsKey("id")){
            filtering.put("_id", filtering.get("id"));
            filtering.remove("id");
        }

        var filters = new ArrayList<Bson>();
        filtering.forEach((key, value) -> {
            if (isBoolean(value)) {
                filters.add(or(regex(key, "^" + value + "$", "i"), eq(key, Boolean.parseBoolean(value))));
            }
            else if (isNumeric(value)) {
                filters.add(or(regex(key, "^" + value + "$", "i"), eq(key, Integer.parseInt(value))));
            }
            else if (value.contains("*")) {
                filters.add(regex(key, value.replace("*", ".*"), "i"));
            } else {
                filters.add(regex(key, "^" + value + "$", "i"));
            }
        });

        Bson projection = fields.isEmpty() ? null : Projections.include(fields);

        // Parse TMF-style sort string
        Bson sortBson = null;
        if (sort != null && !sort.isBlank()) {
            List<Bson> sortFields = new ArrayList<>();
            for (String field : sort.split(",")) {
                field = field.trim();
                if (field.startsWith("-")) {
                    sortFields.add(descending(field.substring(1)));
                } else {
                    sortFields.add(ascending(field));
                }
            }
            sortBson = orderBy(sortFields);
        }

        var query = filters.isEmpty() ? collection.find() : collection.find(and(filters));
        if (projection != null) {
            query = query.projection(projection);
        }
        if (sortBson != null) {
            query = query.sort(sortBson);
        }

        return query;

    }

    private boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isBoolean(String str) {
        return "true".equalsIgnoreCase(str) || "false".equalsIgnoreCase(str);
    }
}
