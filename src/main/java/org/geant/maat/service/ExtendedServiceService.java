package org.geant.maat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vavr.control.Either;
import org.geant.maat.infrastructure.DomainError;
import org.geant.maat.notification.EventType;
import org.geant.maat.notification.NotificationService;
import org.geant.maat.notification.dto.EventDto;
import org.geant.maat.resource.Resource;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

public class ExtendedServiceService implements ServiceService {
    private final ServiceRepository serviceRepository;
    private final ServiceCreator creator;
    private final NotificationService notifications;
    private final ServiceUpdater updater;
    private final ServiceFinder finder;
    @Autowired
    public Environment environment;

    final String backward_regexA = "^bref:";
    final String backward_regexB = "^ref:";
    final String backward_prefixA = "bref:";
    final String backward_prefixB = "ref:";

    ExtendedServiceService(
            ServiceRepository serviceRepository,
            NotificationService notifications,
            ServiceCreator serviceCreator) {
        this.serviceRepository = serviceRepository;
        this.notifications = notifications;
        this.creator = serviceCreator;
        this.updater = new ServiceUpdater(serviceRepository);
        this.finder = new ServiceFinder(serviceRepository);
    }

    @Override
    public Optional<Service> getService(String id) {
        ExtendedServiceLogger.info("Getting service with id " + id);

        var service = serviceRepository.find(id);

        service.ifPresentOrElse(
                r -> ExtendedServiceLogger.infoJson("Service found:", r.toJson()),
                () -> ExtendedServiceLogger.info("Could not find service " + id));
        return service;
    }

    @Override
    public Either<DomainError, JsonNode> createService(JsonNode json, Boolean registerNewEventFlag) {
        ExtendedServiceLogger.infoJson("Creating service from json:", json);
        String serviceCategory = json.get("category").textValue();
        Optional<List<JsonNode>> listRelationshipsType = getRelationshipTypeIfEquals(json, backward_regexA, "serviceRelationship");
        Optional<List<JsonNode>> listRelationshipsNoType = getRelationshipTypeIfNoEquals(json, backward_regexA, "serviceRelationship");
        Optional<List<JsonNode>> listRelationshipsTypeResource = getRelationshipTypeIfEquals(json, backward_regexA, "resourceRelationship");
        Optional<List<JsonNode>> listRelationshipsNoTypeResource = getRelationshipTypeIfNoEquals(json, backward_regexA, "resourceRelationship");

        List<String> servicesNoExists = new ArrayList<>();
        List<String> resourcesNoExists = new ArrayList<>();

        Map<String, JsonNode> mapRelationshipsType = new HashMap<>();
        Map<String, String> mapServicesExists = new HashMap<>();
        Map<String, String> mapServicesExistsName=new HashMap<>();

        Map<String, JsonNode> mapRelationshipsTypeResource = new HashMap<>();
        Map<String, String> mapResourcesExists = new HashMap<>();
        Map<String, String> mapResourcesExistsName=new HashMap<>();

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode arrayWithBref = mapper.createArrayNode();
        ((ObjectNode) json).remove("serviceRelationship");

        ArrayNode arrayWithBrefResource = mapper.createArrayNode();
        ((ObjectNode) json).remove("resourceRelationship");

        if (listRelationshipsNoType.isPresent()) {
            if (!listRelationshipsNoType.get().isEmpty())
                return Either.left(new DomainError("Cannot add a relation that does not contain the bref prefix", Error.RELATIONSHIP_ERROR));
        }

        if (listRelationshipsNoTypeResource.isPresent()) {
            if (!listRelationshipsNoTypeResource.get().isEmpty())
                return Either.left(new DomainError("Cannot add a relation that does not contain the bref prefix", Error.RELATIONSHIP_ERROR));
        }

        if (listRelationshipsType.isPresent()) {
            for (JsonNode service : listRelationshipsType.get()) {
                String hrefPom = service.get("service").get("href").textValue();
                if (!mapRelationshipsType.containsKey(hrefPom)) {
                    mapRelationshipsType.put(hrefPom, service);
                    arrayWithBref.add(service);
                } else
                    return Either.left(new DomainError("Cannot add multiple relationships to the same service", Error.RELATIONSHIP_ERROR));
            }
            mapRelationshipsType.forEach((key, value) -> checkServiceExisting(key)
                    .peek(res -> mapServicesExists.put(key, res.getCategory()))
                    .peek(res -> mapServicesExistsName.put(key, res.getName()))
                    .peekLeft(er -> servicesNoExists.add(key))
                    .peekLeft(error -> ExtendedServiceLogger.info("Could not create service, because: " + error.message())));
        }

        if (listRelationshipsTypeResource.isPresent()) {
            for (JsonNode resource : listRelationshipsTypeResource.get()) {
                String hrefPom = resource.get("resource").get("href").textValue();
                if (!mapRelationshipsTypeResource.containsKey(hrefPom)) {
                    mapRelationshipsTypeResource.put(hrefPom, resource);
                    arrayWithBrefResource.add(resource);
                } else
                    return Either.left(new DomainError("Cannot add multiple relationships to the same resource", Error.RELATIONSHIP_ERROR));
            }
            mapRelationshipsTypeResource.forEach((key, value) -> checkResourceExisting(key)
                    .peek(res -> mapResourcesExists.put(key, res.getCategory()))
                    .peek(res -> mapResourcesExistsName.put(key, res.getName()))
                    .peekLeft(er -> resourcesNoExists.add(key))
                    .peekLeft(error -> ExtendedServiceLogger.info("Could not create service, because: " + error.message())));
        }
        Map<String, String> mapServicesExistsSetName=new HashMap<>();
        Map<String, String> mapResourcesExistsSetName=new HashMap<>();

        for (JsonNode js : arrayWithBref) {
            String href_js = js.get("service").get("href").textValue();
            if (!mapServicesExists.containsKey(href_js)) {
                return Either.left(new DomainError("Some services referenced by relationships do not exist: " + href_js, Error.SERVICE_MISSING));
            }
            String category = mapServicesExists.get(href_js);
            String name=mapServicesExistsName.get(href_js);
            String category_js = js.get("relationshipType").textValue();
            category_js = category_js.replaceFirst("bref:", "");
            if (!category_js.equals(category))
                return Either.left(new DomainError("The service category specified in the query differs from " +
                        " category in the database: href=" + js.get("service").get("href").textValue() + ", category in " +
                        "query=" + category_js + ", category in database=" + category, Error.BAD_CATEGORY));
            ((ObjectNode) js).put("relationshipType", "bref:" + category);
            if(js.get("service").has("name")){
                String name_pom=js.get("service").get("name").asText();
                if(name_pom.equals("set-name")){
                    ((ObjectNode) js.get("service")).put("name", name);
                    String hrefPom=js.get("service").get("href").asText();
                    mapServicesExistsSetName.put(hrefPom, mapServicesExistsName.get(hrefPom));
                }
            }
        }

        for (JsonNode js : arrayWithBrefResource) {
            String href_js = js.get("resource").get("href").textValue();
            if (!mapResourcesExists.containsKey(href_js)) {
                return Either.left(new DomainError("Some resources referenced by relationships do not exist: " + href_js, Error.SERVICE_MISSING));
            }
            String category = mapResourcesExists.get(href_js);
            String name=mapResourcesExistsName.get(href_js);
            String category_js = js.get("relationshipType").textValue();
            category_js = category_js.replaceFirst("bref:", "");
            if (!category_js.equals(category))
                return Either.left(new DomainError("The resource category specified in the query differs from " +
                        " category in the database: href=" + js.get("resource").get("href").textValue() + ", category in " +
                        "query=" + category_js + ", category in database=" + category, Error.BAD_CATEGORY));
            ((ObjectNode) js).put("relationshipType", "bref:" + category);
            if(js.get("resource").has("name")){
                String name_pom=js.get("resource").get("name").asText();
                if(name_pom.equals("set-name")){
                    ((ObjectNode) js.get("resource")).put("name", name);
                    String hrefPom=js.get("resource").get("href").asText();
                    mapResourcesExistsSetName.put(hrefPom, mapResourcesExistsName.get(hrefPom));
                }
            }
        }

        ((ObjectNode) json).putArray("serviceRelationship").addAll(arrayWithBref);
        if (Objects.requireNonNull(environment.getProperty("serviceService.checkExistingService")).equalsIgnoreCase("true") && !servicesNoExists.isEmpty()) {
            ExtendedServiceLogger.info("Some services referenced by relationships do not exist:" + servicesNoExists);
            return Either.left(new DomainError("Some services referenced by relationships do not exist: " + servicesNoExists, Error.SERVICE_MISSING));
        }

        ((ObjectNode) json).putArray("resourceRelationship").addAll(arrayWithBrefResource);
        if (Objects.requireNonNull(environment.getProperty("serviceService.checkExistingService")).equalsIgnoreCase("true") && !resourcesNoExists.isEmpty()) {
            ExtendedServiceLogger.info("Some services referenced by relationships do not exist:" + resourcesNoExists);
            return Either.left(new DomainError("Some services referenced by relationships do not exist: " + resourcesNoExists, Error.RESOURCE_MISSING));
        }

        String date = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
        ((ObjectNode) json).put("lastUpdateDate", date);

        var service = creator.create(json).flatMap(serviceRepository::save);
        if (registerNewEventFlag)
            service.peek(r -> notifications.registerNewEvent(new EventDto(EventType.ServiceCreateEvent, r.toJson())));

        service.peek(r -> ExtendedServiceLogger.infoJson("Service created: ", r.toJson()))
                .peek(r -> updateAfterCreate(mapServicesExists, serviceCategory, r, mapServicesExistsSetName.keySet(), "service"))
                .peek(r -> updateAfterCreate(mapResourcesExists, serviceCategory, r, mapResourcesExistsSetName.keySet(), "resource"))
                .peekLeft(error -> ExtendedServiceLogger.info("Could not create service, because: " + error.message()));

        return service.map(Service::toJson);
    }

    public void updateAfterCreate(Map<String, String> map, String serviceCategory, Service newService, Set<String> set, String resourceOrService) {
        String finalServiceCategory = "bref:" + serviceCategory;
        Map<String, String> mapWithSetName=new HashMap<>();
        Map<String, String> mapNoSetName=new HashMap<>();
        for(String s : map.keySet()){
            if(set.contains(s)){
                mapWithSetName.put(s, newService.getName());
            }
            else{
                mapNoSetName.put(s, newService.getName());
            }
        }
        if(resourceOrService.equals("service")) mapNoSetName.forEach((key, val) -> addRelationsToService(key, finalServiceCategory, newService.getHref()));
        else if(resourceOrService.equals("resource")) mapNoSetName.forEach((key, val) -> addRelationsToResource(key, finalServiceCategory, newService.getHref()));
        for(String key : mapWithSetName.keySet()){
            Map<String, String> mapPom = new HashMap<>();
            mapPom.put(key, mapWithSetName.get(key));
            if(resourceOrService.equals("service"))addRelationsToService(key, finalServiceCategory, newService.getHref(), mapPom);
            else if(resourceOrService.equals("resource"))addRelationsToResource(key, finalServiceCategory, newService.getHref(), mapPom);
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

    private JsonNode createRelationshipTypeJson(String relationName, String href) {
        String[] hrefTab = href.split("/service/");
        String id = hrefTab[1];
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode relationshipType = mapper.createObjectNode();
        ObjectNode service = mapper.createObjectNode();
        relationshipType.put("relationshipType", relationName);
        service.put("id", id);
        service.put("href", href);
        relationshipType.put("service", service);
        relationshipType.put("@type", "ServiceRelationship");
        return relationshipType;
    }

    private JsonNode createRelationshipTypeJson(String relationName, String href, String name){
        String[] hrefTab=href.split("/service/");
        String id=hrefTab[1];
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode relationshipType=mapper.createObjectNode();
        ObjectNode service=mapper.createObjectNode();
        relationshipType.put("relationshipType", relationName);
        service.put("id", id);
        service.put("href", href);
        service.put("name", name);
        relationshipType.put("service", service);
        relationshipType.put("@type", "ServiceRelationship");
        return relationshipType;
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
        //HttpClient client = HttpClient.newBuilder().build();

        URI uri;
        try {
            uri = new URI(href);
        } catch (URISyntaxException uriSyntaxException) {
            return Either.left(new DomainError("Error, cannot connect to service: " + uriSyntaxException.getMessage(), Error.SERVICE_MISSING));
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
            return Either.left(new DomainError("Error, cannot connect to service: " + err.getMessage(), Error.SERVICE_MISSING));
        }

        int statusCode = response.statusCode();

        if (statusCode == 200) return Either.right(Service.from(response.body()));
        else return Either.left(new DomainError("Error, cannot connect to service: " + href, Error.SERVICE_MISSING));
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

        if (statusCode == 200) return Either.right(Resource.from(response.body()));
        else return Either.left(new DomainError("Error, cannot connect to resource: " + href, Error.RESOURCE_MISSING));
    }

    private Either<DomainError, Resource> updateResource(String href, JsonNode resource) {
        href=href.replace("/resource/", "/resourceNC/");
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
            return Either.left(new DomainError("Error, cannot connect to resource: " + uriSyntaxException.getMessage(), Error.RESOURCE_MISSING));
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(resource.toString()))
                // .POST(HttpRequest.BodyPublishers.ofString(service.toString()))
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
        else return Either.left(new DomainError("Error, cannot connect to resource " + href, Error.RESOURCE_MISSING));
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
        return matcher.find();
    }

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
    public Either<DomainError, JsonNode> getService(String id, Collection<String> propsToFilter) {
        ExtendedServiceLogger.info(String.format("Getting service %s, properties: %s", id, propsToFilter));

        return serviceRepository.find(id, propsToFilter)
                .peek(json -> ExtendedServiceLogger.infoJson(String.format("Service %s found", id), json))
                .peekLeft(error -> ExtendedServiceLogger.info(String.format("Getting %s failed: %s", id, error.message())));
    }

    @Override
    public Either<DomainError, String> deleteService(String id, Boolean registerNewEventFlag) {
        ExtendedServiceLogger.info(String.format("Deleting service %s", id));

        var deletedService = getService(id, new ArrayList<>());

        Optional<Service> optRes=serviceRepository.find(id);
        if(optRes.isEmpty()){
            ExtendedServiceLogger.info("Deleting failed. Services not found: " + id);
            return Either.left(new DomainError("Deleting failed. Services not found: " + id, Error.SERVICE_MISSING));
        }
        Service service=optRes.stream().findFirst().orElse(null);

        var result = serviceRepository.delete(id)
                .peek(s -> ExtendedServiceLogger.infoJson(String.format("Service %s deleted", id), deletedService.get()))
                .peek(s-> {
                    assert service != null;
                    UpdateDeletedRelationshipsServices(service);
                })
                .peekLeft(error -> ExtendedServiceLogger.info("Deleting failed: " + error.message()));

        if (registerNewEventFlag) result.peek(r -> notifications.registerNewEvent(new EventDto(EventType.ServiceDeleteEvent, deletedService.get())));

        return result;
    }

    private void UpdateDeletedRelationshipsServices(@NotNull Service service) {
        JsonNode serviceJson=service.toJson();

        Map<String, ArrayList<String>> map;
        Map<String, ArrayList<String>> mapResources;
        map=getServicesWithRelations(serviceJson);
        mapResources=getResourcesWithRelations(serviceJson);
        map.forEach((k,v)->deleteRelationsFromService(k, service.getHref()));
        mapResources.forEach((k,v)->deleteRelationsFromResource(k, service.getHref()));
    }

    private Map<String, ArrayList<String>> getServicesWithRelations(JsonNode serviceJson) {
        JsonNode serviceRelationship=serviceJson.get("serviceRelationship");
        Map<String, ArrayList<String>> map = new HashMap<String, ArrayList<String>>();
        if(serviceRelationship!=null) {

            for (JsonNode j:serviceRelationship) {
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

    private Map<String, ArrayList<String>> getResourcesWithRelations(JsonNode serviceJson) {
        JsonNode resourceRelationship=serviceJson.get("resourceRelationship");
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

    private Map<String, String> getMapOfSetNameServices(JsonNode serviceJson, String baseName){
        JsonNode serviceRelationship=serviceJson.get("serviceRelationship");
        Service service=new Service(serviceJson);
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

    @Override
    public Either<DomainError, JsonNode> updateService(String id, JsonNode updateJson, Boolean registerNewEventFlag) {
        ExtendedServiceLogger.infoJson(String.format("Updating service %s, update json:", id), updateJson);

        String updateDate = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
        ((ObjectNode) updateJson).put("lastUpdateDate", updateDate);

        Service baseService=getService(id).stream().findFirst().orElse(null);
        if(baseService==null){
            return Either.left(new DomainError("Update failed. Services not found: " + id, Error.SERVICE_MISSING));
        }
        JsonNode baseServiceJson= baseService.toJson();

        if(updateJson.get("serviceRelationship")==null && updateJson.get("resourceRelationship")==null){
            var result =  updater.update(id, deletePropertiesForbiddenToUpdate(updateJson))
                    .peek(json -> ExtendedServiceLogger.infoJson(String.format("Service %s updated successfully", id), json))
                    .peekLeft(error -> ExtendedServiceLogger.info(String.format("Update of %s failed: %s", id, error.message())));
            if (registerNewEventFlag) result.peek(json -> notifications.registerNewEvent(new EventDto(EventType.ServiceAttributeValueChangeEvent, json)));
            return result;
        }
        ExtendedServiceLogger.info(updateJson.toPrettyString());

        if(updateJson.get("serviceRelationship")==null){((ObjectNode) updateJson).putArray("serviceRelationship");}
        else if(updateJson.get("resourceRelationship")==null){((ObjectNode) updateJson).putArray("resourceRelationship");}

        ExtendedServiceLogger.info(baseServiceJson.toPrettyString());
        if (!updateJson.get("serviceRelationship").toString().equals(baseServiceJson.get("serviceRelationship").toString()) || !updateJson.get("resourceRelationship").toString().equals(baseServiceJson.get("resourceRelationship").toString())) {
            var difUpdate = updateDifferences(baseServiceJson, updateJson)
                    .peekLeft(error -> ExtendedServiceLogger.info(String.format("Update failed: %s", error.message())));
            if(difUpdate.isLeft()){
                return Either.left(difUpdate.getLeft());
            }
            else{
                ExtendedServiceLogger.info("Differences Update Completed");
                Map<String, String> mapReturnWithNamesToAdd=difUpdate.get();
                for( JsonNode j : updateJson.get("serviceRelationship")){
                    if(mapReturnWithNamesToAdd.containsKey(j.get("service").get("href").asText())){
                        ((ObjectNode) j.get("service")).put("name", mapReturnWithNamesToAdd.get(j.get("service").get("href").asText()));}
                }
                for( JsonNode j : updateJson.get("resourceRelationship")){
                    if(mapReturnWithNamesToAdd.containsKey(j.get("resource").get("href").asText())){
                        ((ObjectNode) j.get("resource")).put("name", mapReturnWithNamesToAdd.get(j.get("resource").get("href").asText()));}
                }
            }

        }

        var result = updater.update(id, deletePropertiesForbiddenToUpdate(updateJson))
                .peek(json -> ExtendedServiceLogger.infoJson(String.format("Service %s updated successfully", id), json))
                .peekLeft(error -> ExtendedServiceLogger.info(String.format("Update of %s failed: %s", id, error.message())));

        if (registerNewEventFlag) result.peek(json -> notifications.registerNewEvent(new EventDto(EventType.ServiceAttributeValueChangeEvent, json)));

        return result;
    }

    private Either<DomainError, Map<String, String>> updateDifferences(JsonNode baseService, JsonNode updateService) {
        if(checkMultiRelationsAndnoBrefRef(updateService).isLeft()){
            return Either.left(checkMultiRelationsAndnoBrefRef(updateService).getLeft());}
        Map<String, ArrayList<String>>mapBaseService=getServicesWithRelations(baseService);
        Map<String, ArrayList<String>>mapUpdateService=getServicesWithRelations(updateService);
        Map<String, ArrayList<String>>mapResourceBaseService=getResourcesWithRelations(baseService);
        Map<String, ArrayList<String>>mapResourceUpdateService=getResourcesWithRelations(updateService);
        Map<String, String> mapReturnWithNamesToAdd=new HashMap<>();

        HashSet<String> differentKeysToDelete = new HashSet<>(mapBaseService.keySet());
        differentKeysToDelete.removeAll(mapUpdateService.keySet());

        HashSet<String> differentKeysToAdd = new HashSet<>(mapUpdateService.keySet());
        differentKeysToAdd.removeAll(mapBaseService.keySet());

        HashSet<String> differentKeysResourceToDelete = new HashSet<>(mapResourceBaseService.keySet());
        differentKeysResourceToDelete.removeAll(mapResourceUpdateService.keySet());

        HashSet<String> differentKeysResourceToAdd = new HashSet<>(mapResourceUpdateService.keySet());
        differentKeysResourceToAdd.removeAll(mapResourceBaseService.keySet());

        Service service = new Service(baseService);

        String href=baseService.get("href").textValue();
        String categoryBase=service.getCategory();

        ((ObjectNode) updateService).put("id", baseService.get("id"));
        ((ObjectNode) updateService).put("href", baseService.get("href"));

        Map<String, String> mapRelationsHrefWithSetName=getMapOfSetNameServices(updateService, service.getName());
        Map<String, String> relationNames=new HashMap<>();

        Map<String, String> mapResourceRelationsHrefWithSetName=getMapOfSetNameResources(updateService, service.getName());
        Map<String, String> relationResourceNames=new HashMap<>();
        for( String add : differentKeysToAdd ){
            String relation=mapUpdateService.get(add).get(0);
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
                relationNames.put(add,returnPrefix(relation)+categoryBase);
                if(mapRelationsHrefWithSetName.containsKey(add)){
                    mapReturnWithNamesToAdd.put(add, ifServiceExists.getName());
                }
            }
        }

        for( String add : differentKeysResourceToAdd ){
            String relation=mapResourceUpdateService.get(add).get(0);
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
                relationResourceNames.put(add,returnPrefix(relation)+categoryBase);
                if(mapResourceRelationsHrefWithSetName.containsKey(add)){
                    mapReturnWithNamesToAdd.put(add, ifResourceExists.getName());
                }
            }
        }

        for( String add : differentKeysToAdd ){
            addRelationsToService(add, relationNames.get(add), href, mapRelationsHrefWithSetName);
        }
        for( String add : differentKeysResourceToAdd ){
            addRelationsToResource(add, relationResourceNames.get(add), href, mapResourceRelationsHrefWithSetName);
        }
        for( String del : differentKeysToDelete ){
            deleteRelationsFromService(del, href);
        }
        for( String del : differentKeysResourceToDelete ){
            deleteRelationsFromResource(del, href);
        }
        return Either.right(mapReturnWithNamesToAdd);
    }

    private Either<DomainError, Boolean> checkMultiRelationsAndnoBrefRef(JsonNode serviceJson) {
        JsonNode serviceRelationship=serviceJson.get("serviceRelationship");
        Map<String, ArrayList<String>> map = new HashMap<String, ArrayList<String>>();
        if(serviceRelationship!=null) {

            for (JsonNode j:serviceRelationship) {
                String href=j.get("service").get("href").textValue();
                String relationName=j.get("relationshipType").textValue();
                if(checkPrefix(backward_regexA,relationName) || checkPrefix(backward_regexB, relationName)){
                    int array_size=0;
                    ArrayList<String> array;
                    if((map.containsKey(href))){
                        return Either.left(new DomainError("Cannot add multiple relationships to the same service", Error.RELATIONSHIP_ERROR));}
                    else {array=new ArrayList<String>();}
                    array.add(array_size, relationName);
                    map.put(href, array);
                }
                else return Either.left(new DomainError("Cannot add a relation that does not contain the bref or ref prefix", Error.RELATIONSHIP_ERROR));
            }
        }
        return Either.right(true);
    }

    private void addRelationsToService(String serviceHref, String relationName, String baseHref) {
        Service ifServiceExists = checkServiceExisting(serviceHref).getOrNull();
        if (ifServiceExists == null) {
            return;
        }
        JsonNode service = ifServiceExists.toJson();

        String updateDate = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
        ((ObjectNode) service).put("lastUpdateDate", updateDate);

        JsonNode serviceRelationship = service.get("serviceRelationship");

        boolean exist = false;
        if (serviceRelationship != null) {
            Iterator<JsonNode> nodes = serviceRelationship.elements();
            while (nodes.hasNext()) {
                JsonNode next = nodes.next();
                if (next.get("service").get("href").textValue().equals(baseHref)) {
                    if (relationName.equals(next.get("relationshipType").textValue())) exist = true;

                }
            }

            if (!exist) {
                ((ArrayNode) service.withArray("serviceRelationship")).add(createRelationshipTypeJson(changePrefix(relationName), baseHref));
            }
            updater.update(ifServiceExists.getId(), deletePropertiesForbiddenToUpdate(service));
        }
    }

    private void addRelationsToService(String serviceHref, String relationName, String baseHref, Map<String, String> mapRelationsHrefWithSetName) {
        Service ifServiceExists = checkServiceExisting(serviceHref).getOrNull();

        if (ifServiceExists == null) {
            return;
        }
        JsonNode service = ifServiceExists.toJson();

        String updateDate = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
        ((ObjectNode) service).put("lastUpdateDate", updateDate);

        JsonNode serviceRelationship = service.get("serviceRelationship");

        boolean exist = false;
        if (serviceRelationship != null) {
            Iterator<JsonNode> nodes = serviceRelationship.elements();
            while (nodes.hasNext()) {
                JsonNode next = nodes.next();
                if (next.get("service").get("href").textValue().equals(baseHref)) {
                    if (relationName.equals(next.get("relationshipType").textValue())) exist = true;

                }
            }

            if (!exist) {
                if(mapRelationsHrefWithSetName.containsKey(serviceHref)){
                    ((ArrayNode) service.withArray("serviceRelationship")).add(createRelationshipTypeJson(changePrefix(relationName), baseHref, mapRelationsHrefWithSetName.get(serviceHref)));
                }
                else ((ArrayNode) service.withArray("serviceRelationship")).add(createRelationshipTypeJson(changePrefix(relationName), baseHref));
            }
            updater.update(ifServiceExists.getId(), deletePropertiesForbiddenToUpdate(service));
        }
    }

    private void addRelationsToResource(String resourceHref, String relationName, String baseHref) {
        Resource ifResourceExists = checkResourceExisting(resourceHref).getOrNull();

        if (ifResourceExists == null) {
            return;
        }
        JsonNode resource = ifResourceExists.toJson();

        String updateDate = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
        ((ObjectNode) resource).put("lastUpdateDate", updateDate);

        JsonNode resourceRelationship = resource.get("serviceRelationship");

        boolean exist = false;
        if (resourceRelationship != null) {
            Iterator<JsonNode> nodes = resourceRelationship.elements();
            while (nodes.hasNext()) {
                JsonNode next = nodes.next();
                if (next.get("service").get("href").textValue().equals(baseHref)) {
                    if (relationName.equals(next.get("relationshipType").textValue())) exist = true;

                }
            }

            if (!exist) {
                ((ArrayNode) resource.withArray("serviceRelationship")).add(createRelationshipTypeJson(changePrefix(relationName), baseHref));
            }

        }
        else ((ArrayNode) resource.withArray("serviceRelationship")).add(createRelationshipTypeJson(changePrefix(relationName), baseHref));
        updateResource(resourceHref, deletePropertiesForbiddenToUpdate(resource));
    }

    private void addRelationsToResource(String resourceHref, String relationName, String baseHref, Map<String, String> mapRelationsHrefWithSetName) {
        Resource ifResourceExists = checkResourceExisting(resourceHref).getOrNull();

        if (ifResourceExists == null) {
            return;
        }
        JsonNode resource = ifResourceExists.toJson();

        String updateDate = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
        ((ObjectNode) resource).put("lastUpdateDate", updateDate);

        JsonNode serviceRelationship = resource.get("serviceRelationship");

        boolean exist = false;
        if (serviceRelationship != null) {
            Iterator<JsonNode> nodes = serviceRelationship.elements();
            while (nodes.hasNext()) {
                JsonNode next = nodes.next();
                if (next.get("service").get("href").textValue().equals(baseHref)) {
                    if (relationName.equals(next.get("relationshipType").textValue())) exist = true;

                }
            }

            if (!exist) {
                if(mapRelationsHrefWithSetName.containsKey(resourceHref)){
                    ((ArrayNode) resource.withArray("serviceRelationship")).add(createRelationshipTypeJson(changePrefix(relationName), baseHref, mapRelationsHrefWithSetName.get(resourceHref)));
                }
                else ((ArrayNode) resource.withArray("serviceRelationship")).add(createRelationshipTypeJson(changePrefix(relationName), baseHref));
            }
        }
        else{
            if(mapRelationsHrefWithSetName.containsKey(resourceHref)){
                ((ArrayNode) resource.withArray("serviceRelationship")).add(createRelationshipTypeJson(changePrefix(relationName), baseHref, mapRelationsHrefWithSetName.get(resourceHref)));
            }
            else ((ArrayNode) resource.withArray("serviceRelationship")).add(createRelationshipTypeJson(changePrefix(relationName), baseHref));
        }
        updateResource(resourceHref, deletePropertiesForbiddenToUpdate(resource));
    }

    private void deleteRelationsFromService(String serviceHref, String baseHref) {
        Service ifServiceExists=checkServiceExisting(serviceHref).getOrNull();

        if(ifServiceExists==null){
            return;
        }

        JsonNode service = ifServiceExists.toJson();

        String updateDate = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
        ((ObjectNode) service).put("lastUpdateDate", updateDate);

        JsonNode serviceRelationship = service.get("serviceRelationship");

        if(serviceRelationship!=null){
            Iterator<JsonNode> nodes = serviceRelationship.elements();
            while(nodes.hasNext()) {
                JsonNode next= nodes.next();
                if(next.get("service").get("href").textValue().equals(baseHref)){
                    nodes.remove();
                }
            }
        }
        updater.update(ifServiceExists.getId(), deletePropertiesForbiddenToUpdate(service));
    }

    private void deleteRelationsFromResource(String resourceHref, String baseHref) {
        Resource ifResourceExists=checkResourceExisting(resourceHref).getOrNull();

        if(ifResourceExists==null){
            return;
        }

        JsonNode resource = ifResourceExists.toJson();

        String updateDate = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
        ((ObjectNode) resource).put("lastUpdateDate", updateDate);

        JsonNode serviceRelationship = resource.get("serviceRelationship");

        if(serviceRelationship!=null){
            Iterator<JsonNode> nodes = serviceRelationship.elements();
            while(nodes.hasNext()) {
                JsonNode next= nodes.next();
                if(next.get("service").get("href").textValue().equals(baseHref)){
                    nodes.remove();
                }
            }
        }
        updateResource(resourceHref, deletePropertiesForbiddenToUpdate(resource));
    }

    @Override
    public Collection<JsonNode> getServices(List<String> fields, Map<String, String> filtering) {
        ExtendedServiceLogger.info(String.format("Getting services' fields %s matching filters %s", fields, filtering));

        var services = finder.find(fields, filtering);

        ExtendedServiceLogger.info(String.format("Found %d services", services.size()));
        return services;
    }

    @Override
    public Collection<JsonNode> getServices(List<String> fields, Map<String, String> filtering, int offset, int limit) {
        ExtendedServiceLogger.info(String.format("Getting services' fields %s matching filters %s offset %s limit %s",
                fields, filtering, offset, limit));

        var services = finder.find(fields, filtering, offset, limit);

        ExtendedServiceLogger.info(String.format("Found %d services", services.size()));
        return services;
    }

    @Override
    public Either<DomainError, JsonNode> updateServiceNC(String id, JsonNode updateJson) {
        ExtendedServiceLogger.infoJson(String.format("Updating service %s, update json:", id), updateJson);

        var result = updater.update(id, updateJson)
                .peek(json -> ExtendedServiceLogger.infoJson(String.format("Service %s updated successfully", id), json))
                .peekLeft(error -> ExtendedServiceLogger.info(String.format("Update of %s failed: %s", id, error.message())));


        return result;
    }

    private static class ExtendedServiceLogger {
        private static final Logger logger = LoggerFactory.getLogger(ServiceService.class);

        public static void infoJson(String prefix, JsonNode json) {
            logger.info(String.format("%s%s%s", prefix, System.lineSeparator(), json.toPrettyString()));
        }

        public static void info(String message) {
            logger.info(message);
        }
    }

}





