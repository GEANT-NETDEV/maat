package org.geant.maat.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vavr.control.Either;
import org.geant.maat.infrastructure.DomainError;
import org.geant.maat.notification.EventType;
import org.geant.maat.notification.NotificationService;
import org.geant.maat.notification.dto.EventDto;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;


public class ExtendedResourceService implements ResourceService {
    private final ResourceRepository resourceRepository;
    private final ResourceCreator creator;
    private final NotificationService notifications;
    private final ResourceUpdater updater;
    private final ResourceFinder finder;
    @Autowired
    public Environment environment;

    final String backward_regexA="^bref:";
    final String backward_regexB="^ref:";

    final String backward_prefixA="bref:";
    final String backward_prefixB="ref:";

    ExtendedResourceService(
            ResourceRepository resourceRepository,
            NotificationService notifications,
            ResourceCreator resourceCreator) {
        this.resourceRepository = resourceRepository;
        this.notifications = notifications;
        this.creator = resourceCreator;
        this.updater = new ResourceUpdater(resourceRepository);
        this.finder = new ResourceFinder(resourceRepository);
    }

    @Override
    public Optional<Resource> getResource(String id) {
        ExtendedResourceLogger.info("Getting resource with id " + id);

        var resource = resourceRepository.find(id);

        resource.ifPresentOrElse(
                r -> ExtendedResourceLogger.infoJson("Resource found:", r.toJson()),
                () -> ExtendedResourceLogger.info("Could not find resource " + id));
        return resource;
    }

    @Override
    public Either<DomainError, JsonNode> createResource(JsonNode json, Boolean registerNewEventFlag) {
        ExtendedResourceLogger.infoJson("Creating resource from json:", json);
        String resourceCategory = json.get("category").textValue();
        Optional<List<JsonNode>> listRelationshipsType = getRelationshipTypeIfEquals(json, backward_regexA);
        Optional<List<JsonNode>> listRelationshipsNoType = getRelationshipTypeIfNoEquals(json, backward_regexA);
        List<String> resourcesNoExists = new ArrayList<>();
        Map<String, JsonNode> mapRelationshipsType=new HashMap<>();
        Map<String, String> mapResourcesExists=new HashMap<>();

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode arrayNoBref = mapper.createArrayNode();
        ArrayNode arrayWithBref = mapper.createArrayNode();
        ((ObjectNode) json).remove("resourceRelationship");

        if(listRelationshipsNoType.isPresent()) {
            for (JsonNode resource : listRelationshipsNoType.get()) {
                arrayNoBref.add(resource);
            }
        }

        if(listRelationshipsType.isPresent()){
            for(JsonNode resource : listRelationshipsType.get()){
                String hrefPom=resource.get("resource").get("href").textValue();
                if(!mapRelationshipsType.containsKey(hrefPom)){
                    mapRelationshipsType.put(hrefPom,resource);
                    arrayWithBref.add(resource);
                }
         }
            mapRelationshipsType.forEach((key, value) -> checkResourceExisting(key)
                    .peek(res -> mapResourcesExists.put(key, res.getCategory()))
                    .peekLeft(er -> resourcesNoExists.add(key))
                    .peekLeft(error -> ExtendedResourceLogger.info("Could not create resource, because: " + error.message())));
        }

        for(JsonNode js : arrayWithBref){
            String category=mapResourcesExists.get(js.get("resource").get("href").textValue());
            ((ObjectNode) js).put("relationshipType", "bref:"+category);
        }
        ((ObjectNode) json).putArray("test").addAll(arrayNoBref).addAll(arrayWithBref);
        if(Objects.requireNonNull(environment.getProperty("resourceService.checkExistingResource")).equalsIgnoreCase("true") && !resourcesNoExists.isEmpty()){
            ExtendedResourceLogger.info("Some relationships referenced by relationships do not exist:" +resourcesNoExists);
            return Either.left(new DomainError("Some relationships referenced by relationships do not exist: " + resourcesNoExists, Error.RESOURCE_MISSING));
        }
        var resource = creator.create(json).flatMap(resourceRepository::save);

        if (registerNewEventFlag) resource.peek(r -> notifications.registerNewEvent(new EventDto(EventType.ResourceCreateEvent, r.toJson())));

        resource.peek(r -> ExtendedResourceLogger.infoJson("Resource created: ", r.toJson()))
                .peek(r->innerUpdateResource(mapResourcesExists, resourceCategory, r))
                .peekLeft(error -> ExtendedResourceLogger.info("Could not create resource, because: " + error.message()));

        return resource.map(Resource::toJson);
    }
    public void innerUpdateResource(Map<String, String> map, String resourceCategory, Resource newResource) {
        ArrayList <String> categoryPomArray =new ArrayList<>();
        categoryPomArray.add("bref:"+resourceCategory);
        map.forEach((key, val) -> addRelationsToResource(key, categoryPomArray, newResource.getHref()));
    }

    private JsonNode deletePropertiesForbiddenToUpdate(JsonNode toJson) {
        ((ObjectNode) toJson).remove("@type");
        ((ObjectNode) toJson).remove("@schemaLocation");
        ((ObjectNode) toJson).remove("href");
        ((ObjectNode) toJson).remove("id");
        return toJson;
    }

    private JsonNode createRelationshipTypeJson(String relationName, String href){
        String[] hrefTab=href.split("/resource/");
        String id=hrefTab[1];
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode relationshipType=mapper.createObjectNode();
        ObjectNode resource=mapper.createObjectNode();
        relationshipType.put("relationshipType", relationName);
        resource.put("id", id);
        resource.put("href", href);
        relationshipType.put("resource", resource);
        relationshipType.put("@type", "ResourceRelationship");
        return relationshipType;
    }

    private Either<DomainError, Resource> checkResourceExisting(String href) {
        var trustManager = new X509ExtendedTrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[]{};
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
            }
        };
        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("TLS");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        try {
            sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());
        } catch (KeyManagementException e) {
            throw new RuntimeException(e);
        }

        var client = HttpClient.newBuilder().sslContext(sslContext).build();
        //HttpClient client = HttpClient.newBuilder().build();

        URI uri;
        try {
            uri = new URI(href);
        } catch (URISyntaxException uriSyntaxException) {
            return Either.left(new DomainError("Error, cannot connect to resource: " + uriSyntaxException.getMessage(), Error.RESOURCE_MISSING));
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response;
        try {
            response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException err) {
            Thread.currentThread().interrupt();
            return Either.left(new DomainError("Error, cannot connect to resource: " + err.getMessage(), Error.RESOURCE_MISSING));
        }

        int statusCode = response.statusCode();

        if(statusCode == 200)return Either.right(Resource.from(response.body()));
        else return Either.left(new DomainError("Error, cannot connect to resource: " + href, Error.RESOURCE_MISSING));
    }
    private Optional<List<JsonNode>> getRelationshipTypeIfEquals(JsonNode json, String prefix){
        JsonNode resourceRelationship=json.get("resourceRelationship");
        if(resourceRelationship!=null) {
            return Optional.of(StreamSupport.stream(resourceRelationship.spliterator(), false)
                    .filter(r -> checkPrefix(prefix, r.get("relationshipType").textValue()))
                    .toList());
        }
        return Optional.empty();
    }

    private Optional<List<JsonNode>> getRelationshipTypeIfNoEquals(JsonNode json, String prefix){
        JsonNode resourceRelationship=json.get("resourceRelationship");
        if(resourceRelationship!=null) {
            return Optional.of(StreamSupport.stream(resourceRelationship.spliterator(), false)
                    .filter(r -> !checkPrefix(prefix, r.get("relationshipType").textValue()))
                    .toList());
        }
        return Optional.empty();
    }

    private Boolean checkPrefix(String prefix, String relationshipType){
        Pattern pattern = Pattern.compile(prefix);
        Matcher matcher = pattern.matcher(relationshipType);
        return matcher.find();}

    @Override
    public Either<DomainError, JsonNode> getResource(String id, Collection<String> propsToFilter) {
        ExtendedResourceLogger.info(String.format("Getting resource %s, properties: %s", id, propsToFilter));

        return resourceRepository.find(id, propsToFilter)
                .peek(json -> ExtendedResourceLogger.infoJson(String.format("Resource %s found", id), json))
                .peekLeft(error -> ExtendedResourceLogger.info(String.format("Getting %s failed: %s", id, error.message())));
    }

    @Override
    public Either<DomainError, String> deleteResource(String id, Boolean registerNewEventFlag) {
        ExtendedResourceLogger.info(String.format("Deleting resource %s", id));

        var deletedResource = getResource(id, new ArrayList<>());

        Optional<Resource> optRes=resourceRepository.find(id);
        if(optRes.isEmpty()){
            ExtendedResourceLogger.info("Deleting failed. Resources not found: " + id);
            return Either.left(new DomainError("Deleting failed. Resources not found: " + id, Error.RESOURCE_MISSING));
        }
        Resource resource=optRes.stream().findFirst().orElse(null);

        var result = resourceRepository.delete(id)
                .peek(s -> ExtendedResourceLogger.infoJson(String.format("Resource %s deleted", id), deletedResource.get()))
                .peek(s-> {
                    assert resource != null;
                    UpdateDeletedRelationshipsResources(resource);
                })
                .peekLeft(error -> ExtendedResourceLogger.info("Deleting failed: " + error.message()));

        if (registerNewEventFlag) result.peek(r -> notifications.registerNewEvent(new EventDto(EventType.ResourceDeleteEvent, deletedResource.get())));

        return result;
    }

    private void UpdateDeletedRelationshipsResources(@NotNull Resource resource) {
        JsonNode resourceJson=resource.toJson();

        Map<String, ArrayList<String>> map;
        map=getResourcesWithRelations(resourceJson);
        map.forEach((k,v)->deleteRelationsFromResource(k, v, resource.getHref()));
    }

    private void deleteRelationsFromResource(String resourceHref, ArrayList<String> relationNames, String baseHref) {
        Resource ifResourceExists=checkResourceExisting(resourceHref).getOrNull();

        if(ifResourceExists==null){
            return;
        }

        JsonNode resourceRelationship=ifResourceExists.getProperty("resourceRelationship").orElse(null);
        ArrayList<String>  listPom=new ArrayList<>();
        relationNames.forEach(rel->listPom.add(listPom.size(), changePrefix(rel)));
        relationNames=listPom;

        if(resourceRelationship!=null){
            Iterator<JsonNode> nodes = resourceRelationship.elements();
            while(nodes.hasNext()) {
                JsonNode next= nodes.next();
                if(next.get("resource").get("href").textValue().equals(baseHref)){
                    if(relationNames.contains(next.get("relationshipType").textValue())){
                        nodes.remove();
                    }
                }
            }
        }
        updater.update(ifResourceExists.getId(), deletePropertiesForbiddenToUpdate(ifResourceExists.toJson()));
    }

    private void addRelationsToResource(String resourceHref, ArrayList<String> relationNames, String baseHref) {
        Resource ifResourceExists=checkResourceExisting(resourceHref).getOrNull();

        if(ifResourceExists==null){
            return;
        }
        JsonNode resource=ifResourceExists.toJson();
        JsonNode resourceRelationship=resource.get("resourceRelationship");

        ArrayList<String>  listPom=new ArrayList<>();
        relationNames.forEach(rel->listPom.add(listPom.size(), changePrefix(rel)));
        relationNames=listPom;

        if(resourceRelationship!=null){
            Iterator<JsonNode> nodes = resourceRelationship.elements();
            while(nodes.hasNext()) {
                JsonNode next= nodes.next();
                if(next.get("resource").get("href").textValue().equals(baseHref)){
                    if(relationNames.contains(next.get("relationshipType").textValue()))
                        relationNames.remove(next.get("relationshipType").textValue());
                }
            }
        }

        for(String relation : relationNames){
            ((ArrayNode) resource.withArray("resourceRelationship")).add(createRelationshipTypeJson(relation,baseHref));
        }
        updater.update(ifResourceExists.getId(),deletePropertiesForbiddenToUpdate(resource));
    }

    private String changePrefix(String relationName) {
        if(checkPrefix(backward_regexA,relationName)){
            relationName=relationName.replaceFirst(backward_regexA, backward_prefixB);
        }
        else if (checkPrefix(backward_regexB, relationName)) {
            relationName=relationName.replaceFirst(backward_regexB,backward_prefixA);
        }
        return relationName;
    }

    private Map<String, ArrayList<String>> getResourcesWithRelations(JsonNode resourceJson) {
        JsonNode resourceRelationship=resourceJson.get("resourceRelationship");
        Map<String, ArrayList<String>> map = new HashMap<String, ArrayList<String>>();
        if(resourceRelationship!=null) {

            for (JsonNode j:resourceRelationship) {
                String href=j.get("resource").get("href").textValue();
                String relationName=j.get("relationshipType").textValue();
                if(checkPrefix(backward_regexA,relationName) || checkPrefix(backward_regexB, relationName)){
                    int array_size=0;
                    ArrayList<String> array;
                    if((map.containsKey(href))){
                        array=map.get(href);
                        array_size=array.size();}
                    else {array=new ArrayList<String>();}
                    array.add(array_size, relationName);
                    map.put(href, array);
                }
            }
        }
        return map;
    }
    @Override
    public Either<DomainError, JsonNode> updateResource(String id, JsonNode updateJson, Boolean registerNewEventFlag) {
        ExtendedResourceLogger.infoJson(String.format("Updating resource %s, update json:", id), updateJson);
        Resource baseResource=getResource(id).stream().findFirst().orElse(null);
        if(baseResource==null){
            return Either.left(new DomainError("Update failed. Resources not found: " + id, Error.RESOURCE_MISSING));
        }
        JsonNode baseResourceJson= baseResource.toJson();

        if(updateJson.get("resourceRelationship")==null){
            var result =  updater.update(id, updateJson)
                    .peek(json -> ExtendedResourceLogger.infoJson(String.format("Resource %s updated successfully", id), json))
                    .peekLeft(error -> ExtendedResourceLogger.info(String.format("Update of %s failed: %s", id, error.message())));
            if (registerNewEventFlag) result.peek(json -> notifications.registerNewEvent(new EventDto(EventType.ResourceAttributeValueChangeEvent, json)));
            return result;
        }
        ExtendedResourceLogger.info(updateJson.toPrettyString());
        if (baseResourceJson.get("resourceRelationship")==null){
            ((ObjectNode)baseResourceJson).putArray("resourceRelationship");
        }

        ExtendedResourceLogger.info(baseResourceJson.toPrettyString());
        if (!updateJson.get("resourceRelationship").toString().equals(baseResourceJson.get("resourceRelationship").toString())) {
            updateDifferences(baseResourceJson, updateJson);
        }

        var result = updater.update(id, updateJson)
                .peek(json -> ExtendedResourceLogger.infoJson(String.format("Resource %s updated successfully", id), json))
                .peekLeft(error -> ExtendedResourceLogger.info(String.format("Update of %s failed: %s", id, error.message())));

        if (registerNewEventFlag) result.peek(json -> notifications.registerNewEvent(new EventDto(EventType.ResourceAttributeValueChangeEvent, json)));

        return result;
    }

    private void updateDifferences(JsonNode baseResource, JsonNode updateResource) {
        Map<String, ArrayList<String>>mapBaseResource=getResourcesWithRelations(baseResource);
        Map<String, ArrayList<String>>mapUpdateResource=getResourcesWithRelations(updateResource);

        Map<String, ArrayList<String>> toDelete = new HashMap<>();
        Map<String, ArrayList<String>> toAdd = new HashMap<>();

        HashSet<String> differentKeysToDelete = new HashSet<>(mapBaseResource.keySet());
        differentKeysToDelete.addAll(mapUpdateResource.keySet());

        HashSet<String> differentKeysToAdd = new HashSet<>(mapBaseResource.keySet());
        differentKeysToAdd.addAll(mapUpdateResource.keySet());

        HashSet<String> unionKeys = new HashSet<>(mapBaseResource.keySet());
        unionKeys.addAll(mapBaseResource.keySet());
        unionKeys.addAll(mapUpdateResource.keySet());
        if(!mapBaseResource.keySet().equals(mapUpdateResource.keySet())){
            differentKeysToDelete.removeAll(mapUpdateResource.keySet());
            differentKeysToDelete.forEach(k->toDelete.put(k,mapBaseResource.get(k)));

            differentKeysToAdd.removeAll(mapBaseResource.keySet());
            differentKeysToAdd.forEach(k->toAdd.put(k, mapUpdateResource.get(k)));

            unionKeys.removeAll(differentKeysToAdd);
            unionKeys.removeAll(differentKeysToDelete);
        }

        for(String key: unionKeys){
            ArrayList<String> arrayToCompare=mapUpdateResource.get(key);
            ArrayList<String> newArrayList=new ArrayList<>();
            for (String relationName: mapBaseResource.get(key)) {
                if(!arrayToCompare.contains(relationName)){
                    newArrayList.add(relationName);
                }
            }
            toDelete.put(key, newArrayList);

            arrayToCompare=mapBaseResource.get(key);
            ArrayList<String> newArrayList2=new ArrayList<>();
            for (String relationName: mapUpdateResource.get(key)) {
                if(!arrayToCompare.contains(relationName)){
                    newArrayList2.add(relationName);
                }
            }
            toAdd.put(key, newArrayList2);
        }
        toDelete.forEach((k,v)->deleteRelationsFromResource(k,v,baseResource.get("href").asText()));
        toAdd.forEach((k,v)->addRelationsToResource(k,v,baseResource.get("href").asText()));

    }



    @Override
    public Collection<JsonNode> getResources(List<String> fields, Map<String, String> filtering) {
        ExtendedResourceLogger.info(String.format("Getting resources' fields %s matching filters %s", fields, filtering));

        var resources = finder.find(fields, filtering);

        ExtendedResourceLogger.info(String.format("Found %d resources", resources.size()));
        return resources;
    }

    @Override
    public Collection<JsonNode> getResources(List<String> fields, Map<String, String> filtering, int offset, int limit) {
        ExtendedResourceLogger.info(String.format("Getting resources' fields %s matching filters %s offset %s limit %s",
                fields, filtering, offset, limit));

        var resources = finder.find(fields, filtering, offset, limit);

        ExtendedResourceLogger.info(String.format("Found %d resources", resources.size()));
        return resources;
    }

    private static class ExtendedResourceLogger {
        private static final Logger logger = LoggerFactory.getLogger(ResourceService.class);

        public static void infoJson(String prefix, JsonNode json) {
            logger.info(String.format("%s%s%s", prefix, System.lineSeparator(), json.toPrettyString()));
        }

        public static void info(String message) {
            logger.info(message);
        }
    }
}
