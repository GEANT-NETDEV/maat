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
import org.geant.maat.service.Service;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
    @Value("${keycloak.enabled}")
    private String keycloakStatus;

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

    static Optional<JwtAuthenticationToken> getCurrentRequestAuthentication() {
        if(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken jwtAuth) {
            return Optional.of(jwtAuth);
        }
        return Optional.empty();
    }

    static Optional<String> getBearerToken() {
        return getCurrentRequestAuthentication().map(JwtAuthenticationToken::getToken).map(Jwt::getTokenValue);
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
        Optional<List<JsonNode>> listRelationshipsType = getRelationshipTypeIfEquals(json, backward_regexA, "resourceRelationship");
        Optional<List<JsonNode>> listRelationshipsNoType = getRelationshipTypeIfNoEquals(json, backward_regexA, "resourceRelationship");
        Optional<List<JsonNode>> listRelationshipsTypeService = getRelationshipTypeIfEquals(json, backward_regexA, "serviceRelationship");
        Optional<List<JsonNode>> listRelationshipsNoTypeService = getRelationshipTypeIfNoEquals(json, backward_regexA, "serviceRelationship");

        List<String> resourcesNoExists = new ArrayList<>();
        List<String> servicesNoExists = new ArrayList<>();

        Map<String, JsonNode> mapRelationshipsType=new HashMap<>();
        Map<String, String> mapResourcesExists=new HashMap<>();
        Map<String, String> mapResourcesExistsName=new HashMap<>();

        Map<String, JsonNode> mapRelationshipsTypeService=new HashMap<>();
        Map<String, String> mapServicesExists=new HashMap<>();
        Map<String, String> mapServicesExistsName=new HashMap<>();

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode arrayWithBref = mapper.createArrayNode();
        ((ObjectNode) json).remove("resourceRelationship");

        ArrayNode arrayWithBrefService = mapper.createArrayNode();
        ((ObjectNode) json).remove("serviceRelationship");

        if(listRelationshipsNoType.isPresent()) {
            if(!listRelationshipsNoType.get().isEmpty())
                return Either.left(new DomainError("Cannot add a relation that does not contain the bref prefix", Error.RELATIONSHIP_ERROR));
        }

        if(listRelationshipsNoTypeService.isPresent()) {
            if(!listRelationshipsNoTypeService.get().isEmpty())
                return Either.left(new DomainError("Cannot add a relation that does not contain the bref prefix", Error.RELATIONSHIP_ERROR));
        }

        if(listRelationshipsType.isPresent()){
            for(JsonNode resource : listRelationshipsType.get()){
                String hrefPom=resource.get("resource").get("href").textValue();
                if(!mapRelationshipsType.containsKey(hrefPom)){
                    mapRelationshipsType.put(hrefPom,resource);
                    arrayWithBref.add(resource);
                }
                else return Either.left(new DomainError("Cannot add multiple relationships to the same resource", Error.RELATIONSHIP_ERROR));
         }
            mapRelationshipsType.forEach((key, value) -> checkResourceExisting(key)
                    .peek(res -> mapResourcesExists.put(key, res.getCategory()))
                    .peek(res -> mapResourcesExistsName.put(key, res.getName()))
                    .peekLeft(er -> resourcesNoExists.add(key))
                    .peekLeft(error -> ExtendedResourceLogger.info("Could not create resource, because: " + error.message())));
        }

        if(listRelationshipsTypeService.isPresent()){
            for(JsonNode service : listRelationshipsTypeService.get()){
                String hrefPom=service.get("service").get("href").textValue();
                if(!mapRelationshipsTypeService.containsKey(hrefPom)){
                    mapRelationshipsTypeService.put(hrefPom,service);
                    arrayWithBrefService.add(service);
                }
                else return Either.left(new DomainError("Cannot add multiple relationships to the same service", Error.RELATIONSHIP_ERROR));
            }
            mapRelationshipsTypeService.forEach((key, value) -> checkServiceExisting(key)
                    .peek(res -> mapServicesExists.put(key, res.getCategory()))
                    .peek(res -> mapServicesExistsName.put(key, res.getName()))
                    .peekLeft(er -> servicesNoExists.add(key))
                    .peekLeft(error -> ExtendedResourceLogger.info("Could not create resource, because: " + error.message())));
        }


        Map<String, String> mapResourcesExistsSetName=new HashMap<>();
        Map<String, String> mapServicesExistsSetName=new HashMap<>();

        for(JsonNode js : arrayWithBref){
            String href_js =js.get("resource").get("href").textValue();
            if(!mapResourcesExists.containsKey(href_js)){
                return Either.left(new DomainError("Some resources referenced by relationships do not exist: " + href_js, Error.RESOURCE_MISSING));
            }
            String category=mapResourcesExists.get(href_js);
            String name=mapResourcesExistsName.get(href_js);
            String category_js=js.get("relationshipType").textValue();
            category_js=category_js.replaceFirst("bref:", "");
            if (!category_js.equals(category))
                return Either.left(new DomainError("The resource category specified in the query differs from " +
                        " category in the database: href=" + js.get("resource").get("href").textValue() +", category in " +
                        "query="+ category_js +", category in database="+ category , Error.BAD_CATEGORY));
            ((ObjectNode) js).put("relationshipType", "bref:"+category);
            if(js.get("resource").has("name")){
                String name_pom=js.get("resource").get("name").asText();
                if(name_pom.equals("set-name")){
                  ((ObjectNode) js.get("resource")).put("name", name);
                  String hrefPom=js.get("resource").get("href").asText();
                  mapResourcesExistsSetName.put(hrefPom, mapResourcesExistsName.get(hrefPom));
              }
            }
        }

        for(JsonNode js : arrayWithBrefService){
            String href_js =js.get("service").get("href").textValue();
            if(!mapServicesExists.containsKey(href_js)){
                return Either.left(new DomainError("Some services referenced by relationships do not exist: " + href_js, Error.SERVICE_MISSING));
            }
            String category=mapServicesExists.get(href_js);
            String name=mapServicesExistsName.get(href_js);
            String category_js=js.get("relationshipType").textValue();
            category_js=category_js.replaceFirst("bref:", "");
            if (!category_js.equals(category))
                return Either.left(new DomainError("The service category specified in the query differs from " +
                        " category in the database: href=" + js.get("service").get("href").textValue() +", category in " +
                        "query="+ category_js +", category in database="+ category , Error.BAD_CATEGORY));
            ((ObjectNode) js).put("relationshipType", "bref:"+category);
            if(js.get("service").has("name")){
                String name_pom=js.get("service").get("name").asText();
                if(name_pom.equals("set-name")){
                    ((ObjectNode) js.get("service")).put("name", name);
                    String hrefPom=js.get("service").get("href").asText();
                    mapServicesExistsSetName.put(hrefPom, mapServicesExistsName.get(hrefPom));
                }
            }
        }

        ((ObjectNode) json).putArray("resourceRelationship").addAll(arrayWithBref);
        if(Objects.requireNonNull(environment.getProperty("resourceService.checkExistingResource")).equalsIgnoreCase("true") && !resourcesNoExists.isEmpty()){
            ExtendedResourceLogger.info("Some resources referenced by relationships do not exist:" +resourcesNoExists);
            return Either.left(new DomainError("Some resources referenced by relationships do not exist: " + resourcesNoExists, Error.RESOURCE_MISSING));
        }

        ((ObjectNode) json).putArray("serviceRelationship").addAll(arrayWithBrefService);
        if(Objects.requireNonNull(environment.getProperty("resourceService.checkExistingResource")).equalsIgnoreCase("true") && !servicesNoExists.isEmpty()){
            ExtendedResourceLogger.info("Some services referenced by relationships do not exist:" +servicesNoExists);
            return Either.left(new DomainError("Some services referenced by relationships do not exist: " + servicesNoExists, Error.SERVICE_MISSING));
        }

        String date = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
        ((ObjectNode) json).put("startOperatingDate", date);
        ((ObjectNode) json).put("lastUpdateDate", date);

        var resource = creator.create(json).flatMap(resourceRepository::save);

        if (registerNewEventFlag) resource.peek(r -> notifications.registerNewEvent(new EventDto(EventType.ResourceCreateEvent, r.toJson())));

        resource.peek(r -> ExtendedResourceLogger.infoJson("Resource created: ", r.toJson()))
                .peek(r->updateAfterCreate(mapResourcesExists, resourceCategory, r, mapResourcesExistsSetName.keySet(),"resource"))
                .peek(r->updateAfterCreate(mapServicesExists, resourceCategory, r, mapServicesExistsSetName.keySet(), "service"))
                .peekLeft(error -> ExtendedResourceLogger.info("Could not create resource, because: " + error.message()));

        return resource.map(Resource::toJson);
    }
    public void updateAfterCreate(Map<String, String> map, String resourceCategory, Resource newResource, Set<String> set, String resourceOrService) {
        String finalResourceCategory = "bref:"+resourceCategory;
        Map<String, String> mapWithSetName=new HashMap<>();
        Map<String, String> mapNoSetName=new HashMap<>();
        for(String s : map.keySet()){
            if(set.contains(s)){
                mapWithSetName.put(s, newResource.getName());
            }
            else{
                mapNoSetName.put(s, newResource.getName());
            }
        }
        if(resourceOrService.equals("resource")) mapNoSetName.forEach((key, val) -> addRelationsToResource(key, finalResourceCategory, newResource.getHref()));
        else if(resourceOrService.equals("service")) mapNoSetName.forEach((key, val) -> addRelationsToService(key, finalResourceCategory, newResource.getHref()));
        for(String key : mapWithSetName.keySet()){
            Map<String, String> mapPom = new HashMap<>();
            mapPom.put(key, mapWithSetName.get(key));
            if(resourceOrService.equals("resource"))addRelationsToResource(key, finalResourceCategory, newResource.getHref(), mapPom);
            else if(resourceOrService.equals("service"))addRelationsToService(key, finalResourceCategory, newResource.getHref(), mapPom);
        }
    }

    private JsonNode deletePropertiesForbiddenToUpdate(JsonNode toJson) {
        ((ObjectNode) toJson).remove("@type");
        ((ObjectNode) toJson).remove("@schemaLocation");
        ((ObjectNode) toJson).remove("href");
        ((ObjectNode) toJson).remove("id");
        ((ObjectNode) toJson).remove("category");
        ((ObjectNode) toJson).remove("name");
        ((ObjectNode) toJson).remove("startOperatingDate");
        ((ObjectNode) toJson).remove("serviceDate");
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

    private JsonNode createRelationshipTypeJson(String relationName, String href, String name){
        String[] hrefTab=href.split("/resource/");
        String id=hrefTab[1];
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode relationshipType=mapper.createObjectNode();
        ObjectNode resource=mapper.createObjectNode();
        relationshipType.put("relationshipType", relationName);
        resource.put("id", id);
        resource.put("href", href);
        resource.put("name", name);
        relationshipType.put("resource", resource);
        relationshipType.put("@type", "ResourceRelationship");
        return relationshipType;
    }

    private Either<DomainError, Resource> checkResourceExisting(String href) {
        X509ExtendedTrustManager trustManager = new X509ExtendedTrustManager() {
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

        HttpRequest request;
        if (Objects.equals(keycloakStatus, "true")) {
            request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Authorization", "Bearer " + getBearerToken().get())
                    .method("GET", HttpRequest.BodyPublishers.noBody())
                    .build();
        } else {
            request = HttpRequest.newBuilder()
                    .uri(uri)
                    .method("GET", HttpRequest.BodyPublishers.noBody())
                    .build();
        }
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

    private Either<DomainError, Service> checkServiceExisting(String href) {
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

        URI uri;
        try {
            uri = new URI(href);
        } catch (URISyntaxException uriSyntaxException) {
            return Either.left(new DomainError("Error, cannot connect to service: " + uriSyntaxException.getMessage(), Error.SERVICE_MISSING));
        }

        HttpRequest request;
        if (Objects.equals(keycloakStatus, "true")) {
            request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Authorization", "Bearer " + getBearerToken().get())
                    .method("GET", HttpRequest.BodyPublishers.noBody())
                    .build();
        } else {
            request = HttpRequest.newBuilder()
                    .uri(uri)
                    .method("GET", HttpRequest.BodyPublishers.noBody())
                    .build();
        }
        HttpResponse<String> response;
        try {
            response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException err) {
            Thread.currentThread().interrupt();
            return Either.left(new DomainError("Error, cannot connect to service: " + err.getMessage(), Error.SERVICE_MISSING));
        }

        int statusCode = response.statusCode();

        if(statusCode == 200)return Either.right(Service.from(response.body()));
        else return Either.left(new DomainError("Error, cannot connect to service " + href, Error.SERVICE_MISSING));
    }

    private Either<DomainError, Service> updateService(String href, JsonNode service) {
        href=href.replace("/service/", "/serviceNC/");
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

        URI uri;
        try {
            uri = new URI(href);
        } catch (URISyntaxException uriSyntaxException) {
            return Either.left(new DomainError("Error, cannot connect to service: " + uriSyntaxException.getMessage(), Error.SERVICE_MISSING));
        }

        HttpRequest request;
        if (Objects.equals(keycloakStatus, "true")) {
            request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + getBearerToken().get())
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(service.toString()))
                    // .POST(HttpRequest.BodyPublishers.ofString(service.toString()))
                    .build();
        } else {
            request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(service.toString()))
                    // .POST(HttpRequest.BodyPublishers.ofString(service.toString()))
                    .build();
        }
        HttpResponse<String> response;
        try {
            response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException err) {
            Thread.currentThread().interrupt();
            return Either.left(new DomainError("Error, cannot connect to service: " + err.getMessage(), Error.SERVICE_MISSING));
        }

        int statusCode = response.statusCode();

        if(statusCode == 200)return Either.right(Service.from(response.body()));
        else return Either.left(new DomainError("Error, cannot connect to service " + href, Error.SERVICE_MISSING));
    }

    private Optional<List<JsonNode>> getRelationshipTypeIfEquals(JsonNode json, String prefix, String whatRelationship){
        JsonNode getRelationship=json.get(whatRelationship);
        if(getRelationship!=null) {
            return Optional.of(StreamSupport.stream(getRelationship.spliterator(), false)
                    .filter(r -> checkPrefix(prefix, r.get("relationshipType").textValue()))
                    .toList());
        }
        return Optional.empty();
    }



    private Optional<List<JsonNode>> getRelationshipTypeIfNoEquals(JsonNode json, String prefix, String whatRelationship){
        JsonNode getRelationship=json.get(whatRelationship);
        if(getRelationship!=null) {
            return Optional.of(StreamSupport.stream(getRelationship.spliterator(), false)
                    .filter(r -> !checkPrefix(prefix, r.get("relationshipType").textValue()))
                    .toList());
        }
        return Optional.empty();
    }

    private Boolean checkPrefix(String prefix, String relationshipType){
        Pattern pattern = Pattern.compile(prefix);
        Matcher matcher = pattern.matcher(relationshipType);
        return matcher.find();}

    private String returnPrefix(String relationshipType){
        Pattern bref = Pattern.compile("bref:");
        Pattern ref = Pattern.compile("ref:");
        Matcher matcherBref = bref.matcher(relationshipType);
        Matcher matcherRef = ref.matcher(relationshipType);
        if (matcherBref.find()) return "bref:";
        else if (matcherRef.find()) return "ref:";
        else return "";
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
        Map<String, ArrayList<String>> mapServices;
        map=getResourcesWithRelations(resourceJson);
        mapServices=getServicesWithRelations(resourceJson);
        map.forEach((k,v)->deleteRelationsFromResource(k, resource.getHref()));
        mapServices.forEach((k,v)->deleteRelationsFromService(k, resource.getHref()));

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

    private Map<String, ArrayList<String>> getServicesWithRelations(JsonNode resourceJson) {
        JsonNode resourceRelationship=resourceJson.get("serviceRelationship");
        Map<String, ArrayList<String>> map = new HashMap<String, ArrayList<String>>();
        if(resourceRelationship!=null) {

            for (JsonNode j:resourceRelationship) {
                String href=j.get("service").get("href").textValue();
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

    private Map<String, String> getMapOfSetNameResources(JsonNode resourceJson, String baseName){
        JsonNode resourceRelationship=resourceJson.get("resourceRelationship");
        Map<String, String> map = new HashMap<>();
        if(resourceRelationship!=null) {
            for (JsonNode j:resourceRelationship) {
                String href=j.get("resource").get("href").textValue();
                if(j.get("resource").has("name")){
                    if(j.get("resource").get("name").asText().equals("set-name")){
                        map.put(href, baseName);
                    }
                }
            }
        }
        return map;
    }

    private Map<String, String> getMapOfSetNameServices(JsonNode resourceJson, String baseName){
        JsonNode serviceRelationship=resourceJson.get("serviceRelationship");
        Map<String, String> map = new HashMap<>();
        if(serviceRelationship!=null) {
            for (JsonNode j:serviceRelationship) {
                String href=j.get("service").get("href").textValue();
                if(j.get("service").has("name")){
                    if(j.get("service").get("name").asText().equals("set-name")){
                        map.put(href, baseName);
                    }
                }
            }
        }
        return map;
    }
    @Override
    public Either<DomainError, JsonNode> updateResource(String id, JsonNode updateJson, Boolean registerNewEventFlag) {
        ExtendedResourceLogger.infoJson(String.format("Updating resource %s, update json:", id), updateJson);

        String updateDate = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
        ((ObjectNode) updateJson).put("lastUpdateDate", updateDate);

        Resource baseResource=getResource(id).stream().findFirst().orElse(null);
        if(baseResource==null){
            return Either.left(new DomainError("Update failed. Resources not found: " + id, Error.RESOURCE_MISSING));
        }
        JsonNode baseResourceJson= baseResource.toJson();

        if(updateJson.get("resourceRelationship")==null && updateJson.get("serviceRelationship")==null){
            var result =  updater.update(id, deletePropertiesForbiddenToUpdate(updateJson))
                    .peek(json -> ExtendedResourceLogger.infoJson(String.format("Resource %s updated successfully", id), json))
                    .peekLeft(error -> ExtendedResourceLogger.info(String.format("Update of %s failed: %s", id, error.message())));
            if (registerNewEventFlag) result.peek(json -> notifications.registerNewEvent(new EventDto(EventType.ResourceAttributeValueChangeEvent, json)));
            return result;
        }
        ExtendedResourceLogger.info(updateJson.toPrettyString());

        if(updateJson.get("resourceRelationship")==null && baseResourceJson.get("resourceRelationship")==null){((ObjectNode) updateJson).putArray("resourceRelationship");}
        else if(updateJson.get("serviceRelationship")==null && baseResourceJson.get("serviceRelationship")==null){((ObjectNode) updateJson).putArray("serviceRelationship");}

        ExtendedResourceLogger.info(baseResourceJson.toPrettyString());
        if (!updateJson.get("resourceRelationship").toString().equals(baseResourceJson.get("resourceRelationship").toString()) || !updateJson.get("serviceRelationship").toString().equals(baseResourceJson.get("serviceRelationship").toString())) {
            var difUpdate = updateDifferences(baseResourceJson, updateJson)
                    .peekLeft(error -> ExtendedResourceLogger.info(String.format("Update failed: %s", error.message())));
            if(difUpdate.isLeft()){
                return Either.left(difUpdate.getLeft());
            }
            else{
                ExtendedResourceLogger.info("Differences Update Completed");
                Map<String, String> mapReturnWithNamesToAdd=difUpdate.get();
                for( JsonNode j : updateJson.get("resourceRelationship")){
                    if(mapReturnWithNamesToAdd.containsKey(j.get("resource").get("href").asText())){
                        ((ObjectNode) j.get("resource")).put("name", mapReturnWithNamesToAdd.get(j.get("resource").get("href").asText()));}
                }
                for( JsonNode j : updateJson.get("serviceRelationship")){
                    if(mapReturnWithNamesToAdd.containsKey(j.get("service").get("href").asText())){
                        ((ObjectNode) j.get("service")).put("name", mapReturnWithNamesToAdd.get(j.get("service").get("href").asText()));}
                }
            }

        }

        var result = updater.update(id, deletePropertiesForbiddenToUpdate(updateJson))
                .peek(json -> ExtendedResourceLogger.infoJson(String.format("Resource %s updated successfully", id), json))
                .peekLeft(error -> ExtendedResourceLogger.info(String.format("Update of %s failed: %s", id, error.message())));

        if (registerNewEventFlag) result.peek(json -> notifications.registerNewEvent(new EventDto(EventType.ResourceAttributeValueChangeEvent, json)));

        return result;
    }


    private Either<DomainError, Map<String, String>> updateDifferences(JsonNode baseResource, JsonNode updateResource) {
        if(checkMultiRelationsAndnoBrefRef(updateResource).isLeft()){
            return Either.left(checkMultiRelationsAndnoBrefRef(updateResource).getLeft());}
        Map<String, ArrayList<String>>mapBaseResource=getResourcesWithRelations(baseResource);
        Map<String, ArrayList<String>>mapUpdateResource=getResourcesWithRelations(updateResource);
        Map<String, ArrayList<String>>mapServiceBaseResource=getServicesWithRelations(baseResource);
        Map<String, ArrayList<String>>mapServiceUpdateResource=getServicesWithRelations(updateResource);
        Map<String, String> mapReturnWithNamesToAdd=new HashMap<>();


        HashSet<String> differentKeysToDelete = new HashSet<>(mapBaseResource.keySet());
        differentKeysToDelete.removeAll(mapUpdateResource.keySet());

        HashSet<String> differentKeysToAdd = new HashSet<>(mapUpdateResource.keySet());
        differentKeysToAdd.removeAll(mapBaseResource.keySet());

        HashSet<String> differentKeysServiceToDelete = new HashSet<>(mapServiceBaseResource.keySet());
        differentKeysServiceToDelete.removeAll(mapServiceUpdateResource.keySet());

        HashSet<String> differentServiceKeysToAdd = new HashSet<>(mapServiceUpdateResource.keySet());
        differentServiceKeysToAdd.removeAll(mapServiceBaseResource.keySet());

        Resource resource = new Resource(baseResource);

        String href=baseResource.get("href").textValue();
        String categoryBase=resource.getCategory();

        ((ObjectNode) updateResource).put("id", baseResource.get("id"));
        ((ObjectNode) updateResource).put("href", baseResource.get("href"));

        Map<String, String> mapRelationsHrefWithSetName=getMapOfSetNameResources(updateResource, resource.getName());
        Map<String, String> relationNames=new HashMap<>();

        Map<String, String> mapServiceRelationsHrefWithSetName=getMapOfSetNameServices(updateResource, resource.getName());
        Map<String, String> relationServiceNames=new HashMap<>();
        for( String add : differentKeysToAdd ){
            String relation=mapUpdateResource.get(add).get(0);
            Resource ifResourceExists=checkResourceExisting(add).getOrNull();

            if(ifResourceExists==null){
                return Either.left(new DomainError("Some resources referenced by relationships do not exist: " + add, Error.RESOURCE_MISSING));
            }

            String category=ifResourceExists.getCategory();
            String relationWithoutPrefix = relation.replace(returnPrefix(relation), "");

            if(!category.equals(relationWithoutPrefix)){
                return Either.left( new DomainError("The resource category specified in the query differs from "+
                        " category in the database: href=" + href + ", category in query=" + relation +
                        ", category in database=" + category, Error.BAD_CATEGORY));
            }
            else {
                relationNames.put(add,returnPrefix(relation)+categoryBase);
                if(mapRelationsHrefWithSetName.containsKey(add)){
                    mapReturnWithNamesToAdd.put(add, ifResourceExists.getName());
                }
            }
        }
        for( String add : differentServiceKeysToAdd ){
            String relation=mapServiceUpdateResource.get(add).get(0);
            Service ifServiceExists=checkServiceExisting(add).getOrNull();

            if(ifServiceExists==null){
                return Either.left(new DomainError("Some services referenced by relationships do not exist: " + add, Error.SERVICE_MISSING));
            }

            String category=ifServiceExists.getCategory();
            String relationWithoutPrefix = relation.replace(returnPrefix(relation), "");

            if(!category.equals(relationWithoutPrefix)){
                return Either.left( new DomainError("The service category specified in the query differs from "+
                        " category in the database: href=" + href + ", category in query=" + relation +
                        ", category in database=" + category, Error.BAD_CATEGORY));
            }
            else {
                relationServiceNames.put(add,returnPrefix(relation)+categoryBase);
                if(mapServiceRelationsHrefWithSetName.containsKey(add)){
                    mapReturnWithNamesToAdd.put(add, ifServiceExists.getName());
                }
            }
        }
        for( String add : differentKeysToAdd ){
            addRelationsToResource(add, relationNames.get(add), href, mapRelationsHrefWithSetName);
        }
        for( String add : differentServiceKeysToAdd ){
            addRelationsToService(add, relationServiceNames.get(add), href, mapServiceRelationsHrefWithSetName);
        }
        for( String del : differentKeysToDelete ){
            deleteRelationsFromResource(del, href);
        }
        for( String del : differentKeysServiceToDelete){
            deleteRelationsFromService(del, href);
        }
        return Either.right(mapReturnWithNamesToAdd);
    }

    private Either<DomainError, Boolean> checkMultiRelationsAndnoBrefRef(JsonNode resourceJson) {
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
                        return Either.left(new DomainError("Cannot add multiple relationships to the same resource", Error.RELATIONSHIP_ERROR));}
                    else {array=new ArrayList<String>();}
                    array.add(array_size, relationName);
                    map.put(href, array);
                }
                else return Either.left(new DomainError("Cannot add a relation that does not contain the bref or ref prefix", Error.RELATIONSHIP_ERROR));
            }
        }
        return Either.right(true);
    }

    private void addRelationsToResource(String resourceHref, String relationName, String baseHref) {
        Resource ifResourceExists = checkResourceExisting(resourceHref).getOrNull();

        if (ifResourceExists == null) {
            return;
        }
        JsonNode resource = ifResourceExists.toJson();

        String updateDate = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
        ((ObjectNode) resource).put("lastUpdateDate", updateDate);

        JsonNode resourceRelationship = resource.get("resourceRelationship");

        boolean exist = false;
        if (resourceRelationship != null) {
            Iterator<JsonNode> nodes = resourceRelationship.elements();
            while (nodes.hasNext()) {
                JsonNode next = nodes.next();
                if (next.get("resource").get("href").textValue().equals(baseHref)) {
                    if (relationName.equals(next.get("relationshipType").textValue())) exist = true;

                }
            }

            if (!exist) {
                ((ArrayNode) resource.withArray("resourceRelationship")).add(createRelationshipTypeJson(changePrefix(relationName), baseHref));
            }
            updater.update(ifResourceExists.getId(), deletePropertiesForbiddenToUpdate(resource));
        }
    }

    private void addRelationsToResource(String resourceHref, String relationName, String baseHref, Map<String, String> mapRelationsHrefWithSetName) {
        Resource ifResourceExists = checkResourceExisting(resourceHref).getOrNull();

        if (ifResourceExists == null) {
            return;
        }
        JsonNode resource = ifResourceExists.toJson();

        String updateDate = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
        ((ObjectNode) resource).put("lastUpdateDate", updateDate);

        JsonNode resourceRelationship = resource.get("resourceRelationship");

        boolean exist = false;
        if (resourceRelationship != null) {
            Iterator<JsonNode> nodes = resourceRelationship.elements();
            while (nodes.hasNext()) {
                JsonNode next = nodes.next();
                if (next.get("resource").get("href").textValue().equals(baseHref)) {
                    if (relationName.equals(next.get("relationshipType").textValue())) exist = true;

                }
            }

            if (!exist) {
                if(mapRelationsHrefWithSetName.containsKey(resourceHref)){
                    ((ArrayNode) resource.withArray("resourceRelationship")).add(createRelationshipTypeJson(changePrefix(relationName), baseHref, mapRelationsHrefWithSetName.get(resourceHref)));
                }
                else ((ArrayNode) resource.withArray("resourceRelationship")).add(createRelationshipTypeJson(changePrefix(relationName), baseHref));
            }
            updater.update(ifResourceExists.getId(), deletePropertiesForbiddenToUpdate(resource));
        }
    }

    private void addRelationsToService(String serviceHref, String relationName, String baseHref) {
        Service ifServiceExists = checkServiceExisting(serviceHref).getOrNull();

        if (ifServiceExists == null) {
            return;
        }
        JsonNode service = ifServiceExists.toJson();

        String updateDate = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
        ((ObjectNode) service).put("lastUpdateDate", updateDate);

        JsonNode resourceRelationship = service.get("resourceRelationship");

        boolean exist = false;
        if (resourceRelationship != null) {
            Iterator<JsonNode> nodes = resourceRelationship.elements();
            while (nodes.hasNext()) {
                JsonNode next = nodes.next();
                if (next.get("resource").get("href").textValue().equals(baseHref)) {
                    if (relationName.equals(next.get("relationshipType").textValue())) exist = true;

                }
            }

            if (!exist) {
                ((ArrayNode) service.withArray("resourceRelationship")).add(createRelationshipTypeJson(changePrefix(relationName), baseHref));
            }
            //updater.update(ifServiceExists.getId(), deletePropertiesForbiddenToUpdate(service));

        }
        else ((ArrayNode) service.withArray("resourceRelationship")).add(createRelationshipTypeJson(changePrefix(relationName), baseHref));
        updateService(serviceHref, deletePropertiesForbiddenToUpdate(service));
    }

    private void addRelationsToService(String serviceHref, String relationName, String baseHref, Map<String, String> mapRelationsHrefWithSetName) {
        Service ifServiceExists = checkServiceExisting(serviceHref).getOrNull();

        if (ifServiceExists == null) {
            return;
        }
        JsonNode service = ifServiceExists.toJson();

        String updateDate = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
        ((ObjectNode) service).put("lastUpdateDate", updateDate);

        JsonNode resourceRelationship = service.get("resourceRelationship");

        boolean exist = false;
        if (resourceRelationship != null) {
            Iterator<JsonNode> nodes = resourceRelationship.elements();
            while (nodes.hasNext()) {
                JsonNode next = nodes.next();
                if (next.get("resource").get("href").textValue().equals(baseHref)) {
                    if (relationName.equals(next.get("relationshipType").textValue())) exist = true;

                }
            }

            if (!exist) {
                if(mapRelationsHrefWithSetName.containsKey(serviceHref)){
                    ((ArrayNode) service.withArray("resourceRelationship")).add(createRelationshipTypeJson(changePrefix(relationName), baseHref, mapRelationsHrefWithSetName.get(serviceHref)));
                }
                else ((ArrayNode) service.withArray("resourceRelationship")).add(createRelationshipTypeJson(changePrefix(relationName), baseHref));
            }

        }
        else{
            if(mapRelationsHrefWithSetName.containsKey(serviceHref)){
                ((ArrayNode) service.withArray("resourceRelationship")).add(createRelationshipTypeJson(changePrefix(relationName), baseHref, mapRelationsHrefWithSetName.get(serviceHref)));
            }
            else ((ArrayNode) service.withArray("resourceRelationship")).add(createRelationshipTypeJson(changePrefix(relationName), baseHref));
        }
        updateService(serviceHref, deletePropertiesForbiddenToUpdate(service));
    }

    private void deleteRelationsFromResource(String resourceHref, String baseHref) {
        Resource ifResourceExists=checkResourceExisting(resourceHref).getOrNull();

        if(ifResourceExists==null){
            return;
        }

        JsonNode resource = ifResourceExists.toJson();

        String updateDate = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
        ((ObjectNode) resource).put("lastUpdateDate", updateDate);

        JsonNode resourceRelationship = resource.get("resourceRelationship");

        if(resourceRelationship!=null){
            Iterator<JsonNode> nodes = resourceRelationship.elements();
            while(nodes.hasNext()) {
                JsonNode next= nodes.next();
                if(next.get("resource").get("href").textValue().equals(baseHref)){
                    nodes.remove();
                }
            }
        }
        updater.update(ifResourceExists.getId(), deletePropertiesForbiddenToUpdate(resource));
    }

    private void deleteRelationsFromService(String serviceHref, String baseHref) {
        Service ifServiceExists=checkServiceExisting(serviceHref).getOrNull();

        if(ifServiceExists==null){
            return;
        }

        JsonNode service = ifServiceExists.toJson();

        String updateDate = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
        ((ObjectNode) service).put("lastUpdateDate", updateDate);

        JsonNode resourceRelationship = service.get("resourceRelationship");

        if(resourceRelationship!=null){
            Iterator<JsonNode> nodes = resourceRelationship.elements();
            while(nodes.hasNext()) {
                JsonNode next= nodes.next();
                if(next.get("resource").get("href").textValue().equals(baseHref)){
                    nodes.remove();
                }
            }
        }
        updateService(serviceHref, deletePropertiesForbiddenToUpdate(service));
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
