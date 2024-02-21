package org.geant.maat.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class RelationshipHelper extends org.geant.maat.integration.testcontainers.BaseTestContainers{
    private final ObjectMapper mapper = new ObjectMapper();
    public JsonNode createRelationshipTypeJson(String prefix, String relationName, String href){
        String[] hrefTab=href.split("/resource/");
        String id=hrefTab[1];
        ObjectNode relationshipType=mapper.createObjectNode();
        ObjectNode resource=mapper.createObjectNode();
        relationshipType.put("relationshipType", prefix+relationName);
        resource.put("id", id);
        resource.put("href", href);
        relationshipType.put("resource", resource);
        relationshipType.put("@type", "ResourceRelationship");
        return relationshipType;
    }

    public JsonNode addRelationshipToResource(String prefix, String relationName, String href, String resourceString){
        JsonNode resource;
        try {
            resource = (ObjectNode) mapper.readTree(resourceString);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        ((ArrayNode) resource.withArray("resourceRelationship")).add(createRelationshipTypeJson(prefix, relationName, href));
        return resource;
    }

    public String updateResourceWithRelation(String prefix, String relationName, String href, String resourceString) {
        JsonNode resource;
        try {
            resource = (ObjectNode) mapper.readTree(resourceString);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        ((ArrayNode) resource.withArray("resourceRelationship")).removeAll().add(createRelationshipTypeJson(prefix, relationName, href));
        return deletePropertiesForbiddenToUpdate(resource).toString();
    }

    private JsonNode deletePropertiesForbiddenToUpdate(JsonNode toJson) {
        ((ObjectNode) toJson).remove("@type");
        ((ObjectNode) toJson).remove("@schemaLocation");
        ((ObjectNode) toJson).remove("href");
        ((ObjectNode) toJson).remove("id");
        return toJson;
    }

    public JsonNode clearRelationship(JsonNode resource){
        return resource=((ArrayNode) resource.withArray("resourceRelationship")).removeAll();
    }


    public JsonNode addOnlyOneRelationshipToResource(String prefix, String relationName, String href, String resourceString) {
        JsonNode resource;
        try {
            resource = (ObjectNode) mapper.readTree(resourceString);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        ((ArrayNode) resource.withArray("resourceRelationship")).removeAll().add(createRelationshipTypeJson(prefix, relationName, href));
        return resource;
    }
}
