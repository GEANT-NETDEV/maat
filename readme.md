# Maat

Maat is an application that stores information about resources and services and exposes the TMF 638 Service Inventory and TMF 639 Resource Inventory APIs.
It is powered by Spring Boot 3.0 and NoSQL databases MongoDB.
API access can be encrypted (ssl) and authenticated (OAuth 2.0; use of Keycloak).

# Installation from sources

### Requirements:

- Maven 3.8.1 (or higher versions)
- Java 17 (or higher versions)
- NoSQL Database - MongoDB

### Before building

Install MongoDB with admin account.

You can create admin account using mongosh and the following script:

```
use admin  
db.createUser(  
  {  
    user: "admin",  
    pwd: "abc123",  
    roles: [ { role: "userAdminAnyDatabase", db: "admin" } ]  
  }  
)
```

### Building:

```mvn clean install```

In case you want to skip running the tests during the installation then you can use the following command:

```mvn clean install -DskipTests```

In both cases, the *jar* application will be created in the *target* folder.
### Running:

Go to **target/** folder and run jar file with created name for Maat.

``` java -jar maat-0.9.1.jar ```

# Installation using Docker

## Configuration (.env) file

Environment variables are key-value pairs that are used to configure application settings and other parameters that may vary between environments.
<br><br>The ```.env``` file is located in the ```docker/``` folder.

<br>The most important options for Maat service that may need to be changed when running the application by docker-compose:


|        Property         |      Example Values      |                                         Description                                          |
|:-----------------------:|:------------------------:|:--------------------------------------------------------------------------------------------:|
|        MAAT_HOST        |         maathost         |    Hostname used in the Maat container to communicate with the EventListener application     |
|        MAAT_PORT        |           8080           |      Port used in the Maat container to communicate with the EventListener application       |
| MAAT_RESOURCE_PROTOCOL  |        http/https        | Protocol (for resources) used to communicate with Maat (also used to create href addresses)  |
|  MAAT_RESOURCE_ADDRESS  | localhost or 192.168.5.1 |  Address (for resources) used to communicate with Maat (also used to create href addresses)  |
|   MAAT_RESOURCE_PORT    |           8080           |   Port (for resources) used to communicate with Maat (also used to create href addresses)    |
| MAAT_SERVICE_PROTOCOL   |        http/https        |  Protocol (for services) used to communicate with Maat (also used to create href addresses)  |
|  MAAT_SERVICE_ADDRESS   | localhost or 192.168.5.1 |  Address (for services) used to communicate with Maat (also used to create href addresses)   |
|    MAAT_SERVICE_PORT    |           8080           |    Port (for services) used to communicate with Maat (also used to create href addresses)    |

## Installation Maat

An alternative installation procedure using docker containers.



Go to **docker/** folder and run:

```docker-compose up -d```


## Installation Maat with EventListener

[EventListener](https://bitbucket.software.geant.org/projects/OSSBSS/repos/maat-eventlistener) is a suporting application for storing notifications from Maat. Notifications inform about any events (add/update/delete resources/services) in Maat.
EventListener automatically registers to Maat when starting (address and port of Maat are located in the properties maat-host and maat-port).


Go to **docker/** folder and run:

```docker-compose -f docker-compose-2.yml up```

## Installation Maat (with EventListener) with Keycloak and SSL

Go to **docker/** folder and run:

```docker-compose -f docker-compose-3.yml up```

After starting all services in containers, run container with setup for Keycloak:

```docker-compose --profile keycloak_setup -f docker-compose-3.yml up -d```

Warning! When Maat works with Keycloak and SSL you must manually register EventListener using the steps below:
- Get access token</br>
  ```curl -X POST --data "client_id=inv3&username=test&password=test&grant_type=password&client_secret=d0b8122f-8dfb-46b7-b68a-f5cc4e25d123" -H "Host: keycloakhost:8090" http://localhost:8090/realms/MaatRealm/protocol/openid-connect/token```


- Replace `<TOKEN>` with the access token (access_token attribute in the response) received in the previous step and execute the following command </br>
  ```curl -X POST -k https://localhost:8080/hub -H "Authorization: Bearer <TOKEN>" -H "Content-Type:Application/json" -d "{\"callback\":\"http://elhost:8081/eventlistener\",\"query\":null}"```
  </br></br>or</br></br>
  ```curl -X POST -k https://localhost:8080/hub -H "Authorization: Bearer <TOKEN>" -H "Content-Type:Application/json" -d @add_listener.json```
  </br>with the content of the file add_listener.json
```
{
    "callback" : "http://elhost:8081/eventlistener",
    "query" : null
}
```

## Installation Maat (with EventListener) with HTTPS access (for Maat) by NGINX

An alternative way to configure SSL (https) for the Maat application is to run nginx, which takes over handling secure communication.
In this case, SSL configuration in Maat is no longer needed. An example of installation of Maat with nginx can be seen using the docker-compose-4.yml file.

The default port used by nginx is 8082

Go to **docker/** folder and run:

```docker-compose -f docker-compose-4.yml up```

<br> <b>Warning!</b><br>All of the above options for running Maat applications using Docker use Volumes. Each MongoDB database has its own volume assigned in the docker-compose file. 
When you delete a database container, the volume still exists and when you restart the service, the old data will be included.
To remove all data, when you delete the containers, you must also delete the volumes.


# Example API requests

<b>GET Requests</b>

- Get all resources

```curl http://127.0.0.1:8080/resourceInventoryManagement/v4.0.0/resource```


- Get all services

```curl http://127.0.0.1:8080/serviceInventoryManagement/v4.0.0/service```

<br><b>POST Requests</b>

- Add resource

```curl -X POST -H "Content-Type: application/json" -d @request_resource.json http://127.0.0.1:8080/resourceInventoryManagement/v4.0.0/resource```

Content of the file request_resource.json

```
{
    "name": "resource1",
    "description": "Resource's description",
    "category": "link",
    "@type": "LogicalResource",
    "@schemaLocation": "https://raw.githubusercontent.com/GEANT-NETDEV/Inv3-schema/main/TMF639-ResourceInventory-v4-pionier.json"
}
```

Attribute "@schemaLocation" must have the correct local path/url of schema file (see "Request Validation" section of this documentation).

- Add service

```curl -X POST -H "Content-Type: application/json" -d @C:\Users\Desktop\Maat\request_service.json http://127.0.0.1:8080/serviceInventoryManagement/v4.0.0/service```

Content of the file request_service.json

```
{
    "serviceType": "Link",
    "name": "name2",
    "description": "Service description",
    "@type": "Service",
    "@schemaLocation": "https://raw.githubusercontent.com/GEANT-NETDEV/Inv3-schema/main/TMF638-ServiceInventory-v4-pionier.json"
}
```
Attribute "@schemaLocation" must have the correct local path/url of schema file (see "Request Validation" section of this documentation).

<br><b>DELETE Requests</b>

- Delete resource

```curl -X DELETE http://127.0.0.1:8080/resourceInventoryManagement/v4.0.0/resource/<ID>```


- Delete service

```curl -X DELETE http://127.0.0.1:8080/serviceInventoryManagement/v4.0.0/service/<ID>```

```<ID>``` is identifier of a resource

## Backward Relationships
Maat has an automatic reference completion, the so-called backward reference (relationship) generation. This is based on the fact that when a resource/service A that has a reference to resource/service B (relationship A->B) is created or updated, a backward reference to resource/service A (relationship B->A) is automatically created in resource/service B as well.

To activate the creation of backward references, the prefix "bref" in the relationshipType attribute of the resourceRelationship element must be used. The second condition is also to add (after the prefix) the name of the resource/service category in the relationshipType to which the reference is created.  

Example:

Create (REST API POST method) a new resource with the relationship to the existing resource id="Res-123" and force generation of the backward reference to the resource to be created:

```
{
    "name": "test",
    "category": "testCategoryB",
    "@type": "LogicalResource",
    "@schemaLocation": "https://raw.githubusercontent.com/GEANT-NETDEV/Inv3-schema/main/TMF639-ResourceInventory-v4-pionier.json",
    "resourceRelationship": [
      {
        "relationshipType": "bref:testCategoryA",
        "resource": {
          "id": "Res-123",
          "href": "http://localhost:8080/resourceInventoryManagement/v4.0.0/resource/Res-123"
        }
      }
    ]
}
```

When such a POST request is received and accepted (validation is correct) by Maat, the following backward reference to the newly created resource is added to the above resource id="Res-123":

```
"resourceRelationship": [
            {
                "relationshipType": "ref:testCategoryB",
                "resource": {
                    "id": "Res-new-456",
                    "href": "http://localhost:8080/resourceInventoryManagement/v4.0.0/resource/Res-new-456"
                }
            }
        ],
```

Relationships can occur in the following options: resource<->resource, service<->service, resource<->service, service<->resource. A reference to the resource is added using resourceRelationship, while serviceRelationship is used for a reference to the service. 

<br>
In addition, the name of the referenced resource/service can be automatically added. This can be helpful in some situations to limit the number of calls to Maat to retrieve the name of the resource/service referenced. For this purpose, the attribute name with the value "set-name"  must be placed in the relationship.

- Example:
  <br>Create (REST API POST method) a new resource with a backward reference to the service id="Service-123" and with the name attribute having the value "set-name".

```
{
    "name": "test4name",
    "category": "testCategory",
    "@type": "LogicalResource",
    "@schemaLocation": "https://raw.githubusercontent.com/GEANT-NETDEV/Inv3-schema/main/TMF639-ResourceInventory-v4-pionier.json",
    "serviceRelationship": [
      {
        "relationshipType": "bref:testServiceCategory",
        "service": {
          "id": "Service-123",
          "href": "http://localhost:8080/serviceInventoryManagement/v4.0.0/service/Service-123",
          "name":"set-name"
        }
      }
    ]
}
```

When such a POST request is received and accepted (validation is correct) by Maat, the following backward reference to the newly created resource, with its name, is added to the service id="Service-123" .

```
"resourceRelationship": [
            {
                "relationshipType": "ref:testCategory",
                "resource": {
                    "id": "Res-new-456",
                    "href": "http://localhost:8080/resourceInventoryManagement/v4.0.0/resource/Res-new-456",
                    "name": "test4name"
                }
            }
        ]
```

The relationship in the newly created resource in such a case looks as follows:

```
 "serviceRelationship": [
        {
            "relationshipType": "bref:testServiceCategory",
            "service": {
                "id": "Service-123",
                "href": "http://localhost:8080/serviceInventoryManagement/v4.0.0/service/Service-123",
                "name": "testServiceName"
            }
        }
    ]
```

<br>The above functionalities cause that updating the "name" and "category" attributes is not allowed.

## POSTMAN

Postman collection ([Maat-Test.postman_collection](https://bitbucket.software.geant.org/projects/OSSBSS/repos/maat/browse/src/main/resources/Maat_Test.postman_collection.json))
to test REST API is available in the folder:
- [src/main/resources](https://bitbucket.software.geant.org/projects/OSSBSS/repos/maat/browse/src/main/resources)

## SWAGGER UI
Swagger UI for Maat is available at http://localhost:8080/swagger-ui/index.html. Sample service and resource for the POST method are provided in the section above.

# Configuration

Basic Configuration:

|                 Property                 |       Example Values       |                                        Description                                        |
|:----------------------------------------:|:--------------------------:|:-----------------------------------------------------------------------------------------:|
|                mongo-host                |   localhost or 127.0.0.1   |                 Hostname for Maat database in the form of name or address                 |
|                mongo-user                |            user            |                                   Username for MongoDB                                    |
|              mongo-password              |          password          |                                   Password for MongoDB                                    |
|            resource.protocol             |            http            |                   Remote application server protocol for resource part                    |
|             resource.address             |         10.27.1.10         |                    Remote application server address for resource part                    |
|              resource.port               |            8080            |                     Remote application server port for resource part                      |
|             service.protocol             |            http            |                    Remote application server protocol for service part                    |
|             service.address              |         10.27.1.10         |                    Remote application server address for service part                     |
|               service.port               |            8080            |                      Remote application server port for service part                      |
|            server.ssl.enabled            |         true/false         |                          Enable/Disable https protocol for Maat                           |
|             keycloak.enabled             |         true/false         |                       Enable/Disable Keycloak application for Maat                        |
|         keycloak.auth-server-url         | http://127.0.0.1:8090/auth |                                  Keycloak server address                                  |
|           resourceService.type           |       base/extended        | Select 'base' version for resource part or 'extended'<br/> Extended version used for PSNC |
|  resourceService.checkExistingResource   |         true/false         |         Enable/Disable check existing resource functionality for extended version         |
| notification.sendNotificationToListeners |         true/false         |                     Enable/Disable sending notifications to listeners                     |
|             openapi.dev-url              |   http://localhost:8080    |                      Base URL development environment for Swagger UI                      |
|             openapi.prod-url             |     https://maat:8080      |                      Base URL production environment for Swagger UI                       |

### SSL Configuration

|                 Property                 |  Example Values  |                            Description                            |
|:----------------------------------------:|:----------------:|:-----------------------------------------------------------------:|
|            server.ssl.enabled            |    true/false    |              Enable/Disable https protocol for Maat               |
|        server.ssl.key-store-type         |      PKCS12      |                           Keystore type                           |
|           server.ssl.key-store           | src/main.key.p12 |                     The path of keystore file                     |
|      server.ssl.key-store-password       |     test123      |                       Password for keystore                       |
|           server.ssl.key-alias           |   exampleAlias   | The alias (or name) under which the key is stored in the keystore |


### API Authentication - Keycloak

|                       Property                        |                                     Example Values                                     |                                              Description                                               |
|:-----------------------------------------------------:|:--------------------------------------------------------------------------------------:|:------------------------------------------------------------------------------------------------------:|
|                   keycloak.enabled                    |                                       true/false                                       |                              Enable/Disable Keycloak application for Maat                              |
| spring.security.oauth2.resourceserver.jwt.issuer-uri  |                    http://keycloakhost:8090/realms/MaatRealm                     |                                       The URL to Keycloak realm                                        |
| spring.security.oauth2.resourceserver.jwt.jwk-set-uri | ${spring.security.oauth2.resourceserver.jwt.issuer-uri}/protocol/openid-connect/certs  |                            JSON Web Key URI to use to verify the JWT token                             |
|          token.converter.principal-attribute          |                                   preferred_username                                   | Parameter that allows to extract the Keycloak user name from a token available on the Spring Boot side |
|              token.converter.resource-id              |                                          inv3                                          |                        The name of the client that Spring Boot application uses                        |


# REST API

REST APIs are compliant with TMForum:<br>
https://github.com/tmforum-apis/TMF639_ResourceInventory <br>
https://github.com/tmforum-apis/TMF638_ServiceInventory


|                                  Link                                  | Method | Input |       Description        |
|:----------------------------------------------------------------------:|:------:|:-----:|:------------------------:|
|   http://127.0.0.1:8080/resourceInventoryManagement/v4.0.0/resource    |  GET   |   -   |    Get all resources     |
| http://127.0.0.1:8080/resourceInventoryManagement/v4.0.0/resource/[ID] |  GET   |  ID   |    Get resource by ID    |
|   http://127.0.0.1:8080/resourceInventoryManagement/v4.0.0/resource    |  POST  | JSON  |       Add resource       |
| http://127.0.0.1:8080/resourceInventoryManagement/v4.0.0/resource/[ID] | DELETE |  ID   |  Delete resource by ID   |
| http://127.0.0.1:8080/resourceInventoryManagement/v4.0.0/resource/[ID] | PATCH  |  ID   | Update existing resource |
|    http://127.0.0.1:8080/serviceInventoryManagement/v4.0.0/service     |  GET   |   -   |     Get all services     |
|  http://127.0.0.1:8080/serviceInventoryManagement/v4.0.0/service/[ID]  |  GET   |  ID   |    Get service by ID     |
|    http://127.0.0.1:8080/serviceInventoryManagement/v4.0.0/service     |  POST  | JSON  |       Add service        |
|  http://127.0.0.1:8080/serviceInventoryManagement/v4.0.0/service/[ID]  | DELETE |  ID   |   Delete service by ID   |
|  http://127.0.0.1:8080/serviceInventoryManagement/v4.0.0/service/[ID]  | PATCH  |  ID   | Update existing service  |


For GET method we can use a few params to filter resources or services more accurately:

- limit *--to limit the number of displayed objects*
- offset *--to make offset on received objects*
- fields *--to display only selected fields (for example: fields=name,description)*

There is also the possibility to search elements by key-value option. For example parameter "name=resource1"
will search all resources or services attributes "name" with value "resource1".

## Request validation

Every resource or service added to the Maat via REST API is validated.
Validation is performed using a schema that defines the appropriate attributes and relationships according to the TMF standards (TMF 638 Service Inventory and TMF 639 Resource Inventory APIs).

Schema location for validation in the POST request is located in the @schemaLocation attribute. This attribute can contain public <b>[GitHub](https://github.com/GEANT-NETDEV/Inv3-schema)</b> address:

- for resource validation: https://raw.githubusercontent.com/GEANT-NETDEV/Inv3-schema/main/TMF639-ResourceInventory-v4-pionier.json
- for service validation: https://raw.githubusercontent.com/GEANT-NETDEV/Inv3-schema/main/TMF638-ServiceInventory-v4-pionier.json

or a file path

- for Linux:
  - file:///home/maat/schema/TMF639-ResourceInventory-v4.json
  - file:///home/maat/schema/TMF638-ServiceInventory-v4.json

- for Windows:
  - file:///C:/Users/schema/TMF639-ResourceInventory-v4.json
  - file:///C:/Users/schema/TMF638-ServiceInventory-v4.json

### Non-TMF schema for validation
The schema file does not have to follow the TMF standard. It can be simplified to address user requirements regarding data models. An example of simple schema files for resources and services can be found here:
- for resource validation: https://raw.githubusercontent.com/GEANT-NETDEV/Inv3-schema/main/TMF639-ResourceInventory-v4-pionier.json
- for service validation: https://raw.githubusercontent.com/GEANT-NETDEV/Inv3-schema/main/TMF638-ServiceInventory-v4-pionier.json

A Postman collection for testing requests with above schema files is available here: [Example_with_simple_schema.postman_collection.json](https://bitbucket.software.geant.org/projects/OSSBSS/repos/maat/browse/src/main/resources/Example_with_simple_schema.postman_collection.json)

# MongoDB
## MongoDB backup data
To create a copy of the MongoDB database from the container or restore the data to database follow the steps below:
- create backup:<br>
  1\) use "mongodump" tool in MongoDB container <br>
```docker exec -i <container_id> /usr/bin/mongodump --username <username> --password <password> --out /dump```<br>
  2\) copy created data from container to host machine <br>
  ```docker cp <container_id>:/dump /home/service/MongodbBackup/Maat```<br><br>
- restore data:<br>
  1\) copy data from host machine to MongoDB container<br>
```docker cp ./dump <container_id>:/dump```<br>
  2\) use "mongorestore" tool in MongoDB container<br>
```docker exec -i <container_id> /usr/bin/mongorestore --username <username> --password <password> /dump```

## MongoDB delete data
To delete data from MongoDB for resources_db, services_db and listeners_db follow the steps below:
- for resources:
  ```docker exec -it <container_id> /usr/bin/mongosh --username <username> --password <password> --authenticationDatabase admin --eval "use resources_db;" --eval  "db.dropDatabase()"```
- for services:
```docker exec -it <container_id> /usr/bin/mongosh --username <username> --password <password> --authenticationDatabase admin --eval "use services_db;" --eval  "db.dropDatabase()"```
- for listeners:
```docker exec -it <container_id> /usr/bin/mongosh --username <username> --password <password> --authenticationDatabase admin --eval "use listeners_db;" --eval  "db.dropDatabase()"```
- for all of these databases:
```docker exec -it <container_id> /usr/bin/mongosh --username <username> --password <password> --authenticationDatabase admin --eval "use resources_db;" --eval  "db.dropDatabase()" --eval "use services_db;" --eval  "db.dropDatabase()" --eval "use listeners_db;" --eval  "db.dropDatabase()"```