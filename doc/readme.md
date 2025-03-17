# Table of Contents

- [Maat](#maat)
- [Installation from sources](#installation-from-sources)
  - [Requirements](#requirements)
  - [Before building](#before-building)
  - [Building](#building)
  - [Running](#running)
- [Installation using Docker](#installation-using-docker)
  - [Configuration of the .env file](#configuration-of-the-env-file)
    - [Maat Parameters](#maat-parameters)
    - [MongoDB Parameters for Maat](#mongodb-parameters-for-maat)
    - [Keycloak Parameters](#keycloak-parameters)
    - [EventListener Parameters for Maat](#eventlistener-parameters-for-maat)
    - [MongoDB Parameters for EventListener](#mongodb-parameters-for-eventlistener)
    - [Graylog Parameters](#graylog-parameters)
    - [Logging Configuration](#logging-configuration)
    - [Grafana Loki Parameters](#grafana-loki-parameters)
  - [Installation of Maat](#installation-of-maat)
  - [Installation of Maat with EventListener](#installation-of-maat-with-eventlistener)
  - [Installation of Maat (with EventListener) with Maat UI, Keycloak and SSL](#installation-of-maat-with-eventlistener-with-maat-ui-keycloak-and-ssl)
  - [Installation of Maat (with EventListener) with Maat UI and HTTPS access (for Maat) by NGINX](#installation-of-maat-with-eventlistener-with-maat-ui-and-https-access-for-maat-by-nginx)
  - [Installation of Maat (with EventListener, Maat UI, NGINX, Keycloak and Graylog)](#installation-of-maat-with-eventlistener-maat-ui-nginx-keycloak-and-graylog)
  - [Installation of Maat (with EventListener, Maat UI, NGINX, Keycloak and Grafana Loki)](#installation-of-maat-with-eventlistener-maat-ui-nginx-keycloak-and-grafana-loki)
- [Example API requests](#example-api-requests)
- [Backward Relationships](#backward-relationships)
- [POSTMAN](#postman)
- [SWAGGER UI](#swagger-ui)
- [Configuration](#configuration)
  - [Basic Configuration](#basic-configuration)
  - [SSL Configuration](#ssl-configuration)
  - [Authentication and authorization configuration - Keycloak](#authentication-and-authorization-configuration---keycloak)
  - [Graylog Configuration](#graylog-configuration)
  - [Grafana Loki Configuration](#grafana-loki-configuration)
- [REST API](#rest-api)
  - [Request validation](#request-validation)
    - [Non-TMF schema for validation](#non-tmf-schema-for-validation)
  - [Authentication](#authentication)
    - [Enabling authentication in Maat with Keycloak](#enabling-authentication-in-maat-with-keycloak)
  - [Authorization](#authorization)
    - [Rest method filtering](#rest-method-filtering)
    - [JSON content filtering](#json-content-filtering)
- [MongoDB](#mongodb)
  - [MongoDB backup data](#mongodb-backup-data)
  - [MongoDB delete data](#mongodb-delete-data)
- [Graylog](#graylog) 
    - [Graylog in Docker](#graylog-in-docker)
    - [Graylog in Maat or EventListener](#graylog-in-maat-or-eventlistener)
    - [Graylog Input Configuration Guide](#graylog-input-configuration-guide)
- [Grafana Loki](#grafana-loki)
  - [Grafana Loki in Docker](#grafana-loki-in-docker)
  - [Grafana Loki in Maat or Eventlistener](#grafana-loki-in-maat-or-eventlistener)
  - [Grafana Loki Input Configuration Guide](#grafana-loki-input-configuration-guide)

<a name="maat"></a>
## Maat

Maat is an application that stores information about resources and services and exposes the TMF 638 Service Inventory
and TMF 639 Resource Inventory APIs.
It is powered by Spring Boot 3.0 and NoSQL databases MongoDB.
API access can be encrypted (ssl) and authenticated (OAuth 2.0; use of Keycloak).

<a name="installation-from-sources"></a>
## Installation from sources

<a name="requirements"></a>
### Requirements

- Maven 3.8.1 (or higher versions)
- Java 21 (or higher versions)
- NoSQL Database - MongoDB

<a name="before-building"></a>
### Before building

Install MongoDB with admin account.

You can create admin account using [mongosh](https://www.mongodb.com/docs/mongodb-shell/) and the following script:

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

<a name="building"></a>
### Building

```mvn clean install```

In case you want to skip running the tests during the installation then you can use the following command:

```mvn clean install -DskipTests```

In both cases, the *jar* application will be created in the *target* folder.

<a name="running"></a>
### Running

Go to **target/** folder and run .jar file with created name for Maat.

``` java -jar maat-1.0.10.jar ```

<a name="installation-using-docker"></a>
## Installation using Docker

Maat can be run in a Docker container. The Docker image is available on the Artifactory repository at `artifactory.software.geant.org/spa-docker/maat:<actuall_version e.g. 1.0.10>`.

<a name="configuration-of-the-env-file"></a>
### Configuration of the .env file

Environment variables are key-value pairs that are used to configure application settings and other parameters that may
vary between environments.
<br><br>The ```.env``` file is located in the ```docker/``` folder.

**Warning!** <br> It is not recommended to use localhost in the .env file. Instead, use the address of the machine where Docker is running (e.g., the IP address).

The .env file contains environment variables used to configure the Maat application and other related services. Below is
a description of the most important configuration options.

<a name="maat-parameters"></a>
#### Maat Parameters

|        Property        |        Values        |                                         Description                                         |
|:----------------------:|:--------------------:|:-------------------------------------------------------------------------------------------:|
|     MAAT_PROTOCOL      |         http         |    Protocol used in the Maat container to communicate with the EventListener application    |
|       MAAT_HOST        |       maathost       |    Hostname used in the Maat container to communicate with the EventListener application    |
|       MAAT_PORT        |         8080         |      Port used in the Maat container to communicate with the EventListener application      |
| MAAT_RESOURCE_PROTOCOL |      http/https      | Protocol (for resources) used to communicate with Maat (also used to create href addresses) |
| MAAT_RESOURCE_ADDRESS  | localhost/ip_address | Address (for resources) used to communicate with Maat (also used to create href addresses)  |
|   MAAT_RESOURCE_PORT   |         8082         |   Port (for resources) used to communicate with Maat (also used to create href addresses)   |
| MAAT_SERVICE_PROTOCOL  |      http/https      | Protocol (for services) used to communicate with Maat (also used to create href addresses)  |
|  MAAT_SERVICE_ADDRESS  | localhost/ip_address |  Address (for services) used to communicate with Maat (also used to create href addresses)  |
|   MAAT_SERVICE_PORT    |         8082         |   Port (for services) used to communicate with Maat (also used to create href addresses)    |
|    MAAT_SSL_ENABLED    |      true/false      |                           Enable/disable https protocol for Maat                            |

<a name="mongodb-parameters-for-maat"></a>
#### MongoDB Parameters for Maat

|    Property    |  Values   |                            Description                            |
|:--------------:|:---------:|:-----------------------------------------------------------------:|
|  MONGODB_HOST  | mongohost |          Hostname for the MongoDB database used by Maat           |
|   MONGO_PORT   |   27017   |            Port for the MongoDB database used by Maat             |
|   MONGO_USER   |   admin   |                 Username for the MongoDB database                 |
| MONGO_PASSWORD |  abc123   |                 Password for the MongoDB database                 |
| MONGO_TIMEOUT  |   3000    | Timeout (in milliseconds) for connections to the MongoDB database |

<a name="keycloak-parameters"></a>
#### Keycloak Parameters

|             Property              |       Values       |                       Description                       |
|:---------------------------------:|:------------------:|:-------------------------------------------------------:|
|         KEYCLOAK_PROTOCOL         |        http        |       Protocol used to communicate with Keycloak        |
|           KEYCLOAK_HOST           |    keycloakhost    |            Hostname for the Keycloak server             |
|           KEYCLOAK_PORT           |        8090        |              Port for the Keycloak server               |
|           KEYCLOAK_USER           |       admin        |         Username for the Keycloak administrator         |
|           KEYCLOAK_PASS           |       admin        |         Password for the Keycloak administrator         |
|  KEYCLOAK_CLIENT_ID_FOR_CLIENTS   |        maat        |           Client ID for the Maat application            |
|   KEYCLOAK_CLIENT_ID_FOR_USERS    |    maat-account    |     Client ID for the users of the Maat application     |
|  KEYCLOAK_GRANT_TYPE_FOR_CLIENTS  | client_credentials |             Grant type for Keycloak clients             |
|   KEYCLOAK_GRANT_TYPE_FOR_USERS   | authorization_code |              Grant type for Keycloak users              |
|      KEYCLOAK_CLIENT_SECRET       |  <client_secret>   |               Client secret for Keycloak                |
|         KEYCLOAK_ENABLED          |     true/false     |      Enable/disable Keycloak application for Maat       |
|  KEYCLOAK_AUTHORIZATION_L1_ROLES  |     true/false     |  Enable/disable level 1 role authorization in Keycloak  |
| KEYCLOAK_AUTHORIZATION_L2_FILTERS |     true/false     | Enable/disable level 2 filter authorization in Keycloak |

<a name="eventlistener-parameters-for-maat"></a>
#### EventListener Parameters for Maat

|        Property         | Values |                   Description                   |
|:-----------------------:|:------:|:-----------------------------------------------:|
|      AUTO_REGISTER      | on/off |     Automatic registration of EventListener     |
| EVENT_LISTENER_PROTOCOL |  http  | Protocol used to communicate with EventListener |
|   EVENT_LISTENER_HOST   | elhost |           Hostname for EventListener            |
|   EVENT_LISTENER_PORT   |  8081  |             Port for EventListener              |

<a name="mongodb-parameters-for-eventlistener"></a>
#### MongoDB Parameters for EventListener

|     Property      |    Values    |                                       Description                                       |
|:-----------------:|:------------:|:---------------------------------------------------------------------------------------:|
|  MONGODB_EL_HOST  | mongo_elhost |                 Hostname for the MongoDB database used by EventListener                 |
|   MONGO_EL_USER   |    admin     |                 Username for the MongoDB database used by EventListener                 |
| MONGO_EL_PASSWORD |    abc123    |                 Password for the MongoDB database used by EventListener                 |
|   MONGO_EL_PORT   |    27017     |                   Port for the MongoDB database used by EventListener                   |
| MONGO_EL_TIMEOUT  |     3000     | Timeout (in milliseconds) for connections to the MongoDB database used by EventListener |

<a name="graylog-parameters"></a>
#### Graylog Parameters

|   Property   |   Values    |           Description           |
|:------------:|:-----------:|:-------------------------------:|
| GRAYLOG_HOST | grayloghost | Hostname for the Graylog server |
| GRAYLOG_PORT |    12201    |   Port for the Graylog server   |

<a name="logging-configuration"></a>
#### Logging Configuration

|    Property    |                           Values                            |                                                       Description                                                       |
|:--------------:|:-----------------------------------------------------------:|:-----------------------------------------------------------------------------------------------------------------------:|
| LOGGING_CONFIG |                classpath:logback-spring.xml                 |                          Logging configuration for the Maat application. Logs only in Graylog                           |
| LOGGING_CONFIG |               classpath:logback-with-file.xml               |                        Logging configuration for the Maat application. Logs in Graylog and file                         |
| LOGGING_CONFIG |         classpath:logback-with-file-and-console.xml         |                   Logging configuration for the Maat application. Logs in Graylog, file, and console                    |
| LOGGING_CONFIG | classpath:logback-with-file-and-console-without-graylog.xml |                      Logging configuration for the Maat application. Logs only in file and console                      |
| LOGGING_CONFIG |              classpath:logback-loki-docker.xml              | Logging configuration for the Maat application. Logs in Grafana Loki stored from all containers. Use in docker-compose. |
| LOGGING_CONFIG |             classpath:logback-loki-appender.xml             |                     Logging configuration for the Maat application. Use outside of docker-compose .                     | 

<a name="grafana-loki-parameters"></a>
#### Grafana Loki Parameters

|   Property   |  Values  |             Description              |
|:------------:|:--------:|:------------------------------------:|
|  LOKI_HOST   | lokihost | Hostname for the Grafana Loki server |

<a name="installation-of-maat"></a>
### Installation of Maat

An alternative installation procedure using docker containers.

Go to **docker/** folder and run:

```docker-compose up -d```

<a name="installation-of-maat-with-eventlistener"></a>
### Installation of Maat with EventListener

[EventListener](https://bitbucket.software.geant.org/projects/OSSBSS/repos/maat-eventlistener) is a suporting
application for storing notifications from Maat. Notifications inform about any events (add/update/delete
resources/services) in Maat.
EventListener automatically registers to Maat when starting (address and port of Maat are located in the properties
maat-host and maat-port).

Go to **docker/** folder and run:

```docker-compose -f docker-compose-2.yml up```

<a name="installation-of-maat-with-eventlistener-with-maat-ui-keycloak-and-ssl"></a>
### Installation of Maat (with EventListener) with Maat UI, Keycloak and SSL

Maat UI is a user interface for the Maat application, which manages information about resources and services. It provides a graphical interface for interacting with the Maat backend, allowing users to view, add, update, and delete resources and services.

The default port used by Maat UI is 9100

Go to **docker/** folder and run:

```docker-compose -f docker-compose-3.yml up```


**Warning!** <br> When Maat works with Keycloak and SSL you must manually register EventListener using the steps below:
- Get access token</br>
For example for encoded `client_id=maat` and `client_secret=d0b8122f-8dfb-46b7-b68a-f5cc4e25d123` use the following command:</br>
Replace `<keycloak_protocol>`, `<keycloak_host>`, `<keycloak_port>` with the appropriate values</br>
  ```curl -k -X POST "<keycloak_protocol>://<keycloak_host>:<keycloak_port>/realms/MaatRealm/protocol/openid-connect/token" -H "Content-Type: application/x-www-form-urlencoded" -H "Authorization: Basic bWFhdDpkMGI4MTIyZi04ZGZiLTQ2YjctYjY4YS1mNWNjNGUyNWQxMjM=" -d "grant_type=client_credentials"```


- Register EventListener to Maat</br>
Replace `<maat_host>`, `<maat_port>` with the appropriate values</br>
Replace `<TOKEN>` with the access token (access_token attribute in the response) received in the previous step and execute the following command </br>
  ```curl -X POST -k https://<maat_host>:<maat_port>/hub -H "Authorization: Bearer <TOKEN>" -H "Content-Type:Application/json" -d "{\"callback\":\"http://elhost:8081/eventlistener\",\"query\":null}"```
  </br></br>or</br></br>
  ```curl -X POST -k https://<maat_host>:<maat_port>/hub -H "Authorization: Bearer <TOKEN>" -H "Content-Type:Application/json" -d @add_listener.json```
  </br>with the content of the file add_listener.json
```
{
    "callback" : "http://elhost:8081/eventlistener",
    "query" : null
}
```

<a name="installation-of-maat-with-eventlistener-with-maat-ui-and-https-access-for-maat-by-nginx"></a>
### Installation of Maat (with EventListener) with Maat UI and HTTPS access (for Maat) by NGINX

An alternative way to configure SSL (https) for the Maat application is to run nginx, which takes over handling secure
communication.
In this case, SSL configuration in Maat is no longer needed. An example of installation of Maat with nginx can be seen
using the docker-compose-4.yml file.

The default port used by nginx is 8082

Go to **docker/** folder and run:

```docker-compose -f docker-compose-4.yml up```

<a name="installation-of-maat-with-eventlistener-maat-ui-nginx-keycloak-and-graylog"></a>
### Installation of Maat (with EventListener, Maat UI, NGINX, Keycloak and Graylog

Complete installation of Maat with EventListener, Keycloak, Graylog, and NGINX.

Go to **docker/** folder and run:

```docker-compose -f docker-compose-5.yml up```

<a name="installation-of-maat-with-eventlistener-maat-ui-nginx-keycloak-and-grafana-loki"></a>
### Installation of Maat (with EventListener, Maat UI, NGINX, Keycloak and Grafana Loki)

Complete installation of Maat with EventListener, Keycloak, Grafana Loki, and NGINX.

Go to **docker/** folder and run:

```docker-compose -f docker-compose-6.yml up```

<br>**Warning!**<br>All of the above options for running Maat application with Docker use Volumes. Each MongoDB database has its own volume assigned in the docker-compose file. 
When you delete a database container, the volume still exists and when you restart the service, the old data will be included.
To remove all data, when you delete the containers, you must also delete the volumes.

<a name="example-api-requests"></a>
## Example API requests

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

Attribute "@schemaLocation" must have the correct local path/url of schema file (see "Request Validation" section of
this documentation).

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

Attribute "@schemaLocation" must have the correct local path/url of schema file (see "Request Validation" section of
this documentation).

<br><b>DELETE Requests</b>

- Delete resource

```curl -X DELETE http://127.0.0.1:8080/resourceInventoryManagement/v4.0.0/resource/<ID>```

- Delete service

```curl -X DELETE http://127.0.0.1:8080/serviceInventoryManagement/v4.0.0/service/<ID>```

```<ID>``` is identifier of a resource

<a name="backward-relationships"></a>
## Backward Relationships

Maat has an automatic reference completion, the so-called backward reference (relationship) generation. This is based on
the fact that when a resource/service A that has a reference to resource/service B (relationship A->B) is created or
updated, a backward reference to resource/service A (relationship B->A) is automatically created in resource/service B
as well.

To activate the creation of backward references, the prefix "bref" in the relationshipType attribute of the
resourceRelationship element must be used. The second condition is also to add (after the prefix) the name of the
resource/service category in the relationshipType to which the reference is created.

Example:

Create (REST API POST method) a new resource with the relationship to the existing resource id="Res-123" and force
generation of the backward reference to the resource to be created:

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

When such a POST request is received and accepted (validation is correct) by Maat, the following backward reference to
the newly created resource is added to the above resource id="Res-123":

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

Relationships can occur in the following options: resource<->resource, service<->service, resource<->service, service<->
resource. A reference to the resource is added using resourceRelationship, while serviceRelationship is used for a
reference to the service.

<br>
In addition, the name of the referenced resource/service can be automatically added. This can be helpful in some situations to limit the number of calls to Maat to retrieve the name of the resource/service referenced. For this purpose, the attribute name with the value "set-name"  must be placed in the relationship.

- Example:
  <br>Create (REST API POST method) a new resource with a backward reference to the service id="Service-123" and with
  the name attribute having the value "set-name".

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

When such a POST request is received and accepted (validation is correct) by Maat, the following backward reference to
the newly created resource, with its name, is added to the service id="Service-123" .

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

<br>The above functionalities cause that updating the "name" and "category" attributes is not allowed. It is also forbidden for the user to update the parmeters: “@type”, "@schemaLocation", "href", "id", "startOperatingDate", "serviceDate".

<a name="postman"></a>
## POSTMAN

Postman
collection ([Maat-Test.postman_collection](https://bitbucket.software.geant.org/projects/OSSBSS/repos/maat/browse/src/main/resources/Maat_Test.postman_collection.json))
to test REST API is available in the folder:

- [src/main/resources](https://bitbucket.software.geant.org/projects/OSSBSS/repos/maat/browse/src/main/resources)

<a name="swagger-ui"></a>
## SWAGGER UI

Swagger UI for Maat is available at http://localhost:8080. Sample service and resource for the
POST method are provided in the section above.

<a name="configuration"></a>
## Configuration

<a name="basic-configuration"></a>
### Basic Configuration:

|                 Property                 |                       Values                        |                                        Description                                        |
|:----------------------------------------:|:---------------------------------------------------:|:-----------------------------------------------------------------------------------------:|
|                mongo-host                |               localhost or 127.0.0.1                |                 Hostname for Maat database in the form of name or address                 |
|                mongo-user                |                    &lt;user&gt;                     |                                   Username for MongoDB                                    |
|              mongo-password              |                  &lt;password&gt;                   |                                   Password for MongoDB                                    |
|               server.port                |                 &lt;port number&gt;                 |                                Port for Maat application                                  |
|            resource.protocol             |                  &lt;protocol&gt;                   |                   Remote application server protocol for resource part                    |
|             resource.address             |       &lt;domain name> or &lt;ip address&gt;        |                    Remote application server address for resource part                    |
|              resource.port               |                 &lt;port number&gt;                 |                     Remote application server port for resource part                      |
|             service.protocol             |                  &lt;protocol&gt;                   |                    Remote application server protocol for service part                    |
|             service.address              |      &lt;domain name&gt; or &lt;ip address&gt;      |                    Remote application server address for service part                     |
|               service.port               |                 &lt;port number&gt;                 |                      Remote application server port for service part                      |
|            server.ssl.enabled            |                     true/false                      |                          Enable/Disable https protocol for Maat                           |
|             keycloak.enabled             |                     true/false                      |                       Enable/Disable Keycloak application for Maat                        |
|         keycloak.auth-server-url         | http://&lt;domain name&gt;:&lt;port number&gt;/auth |                                  Keycloak server address                                  |
|           resourceService.type           |                    base/extended                    | Select 'base' version for resource part or 'extended'<br/> Extended version used for PSNC |
|  resourceService.checkExistingResource   |                     true/false                      |         Enable/Disable check existing resource functionality for extended version         |
| notification.sendNotificationToListeners |                     true/false                      |                     Enable/Disable sending notifications to listeners                     |

<a name="ssl-configuration"></a>
### SSL Configuration

|           Property            |      Values      |                            Description                            |
|:-----------------------------:|:----------------:|:-----------------------------------------------------------------:|
|      server.ssl.enabled       |    true/false    |              Enable/Disable https protocol for Maat               |
|   server.ssl.key-store-type   |      PKCS12      |                           Keystore type                           |
|     server.ssl.key-store      | src/main.key.p12 |                     The path of keystore file                     |
| server.ssl.key-store-password |     test123      |                       Password for keystore                       |
|     server.ssl.key-alias      |   exampleAlias   | The alias (or name) under which the key is stored in the keystore |

<a name="authentication-and-authorization-configuration---keycloak"></a>
### Authentication and authorization configuration - Keycloak

|                       Property                        |                                        Values                                         |                                              Description                                               |
|:-----------------------------------------------------:|:-------------------------------------------------------------------------------------:|:------------------------------------------------------------------------------------------------------:|
|                   keycloak.enabled                    |                                      true/false                                       |                              Enable/Disable Keycloak application for Maat                              |
|            keycloak.authorization.l1.roles            |                                      true/false                                       |                             Enable/Disable role authorization in Keycloak                              |
|           keycloak.authorization.l2.filters           |                                      true/false                                       |                            Enable/Disable filter authorization in Keycloak                             |
| spring.security.oauth2.resourceserver.jwt.issuer-uri  |               http://<domain name&gt;:<port number&gt;/realms/MaatRealm               |                                       The URL to Keycloak realm                                        |
| spring.security.oauth2.resourceserver.jwt.jwk-set-uri | ${spring.security.oauth2.resourceserver.jwt.issuer-uri}/protocol/openid-connect/certs |                            JSON Web Key URI to use to verify the JWT token                             |
|          token.converter.principal-attribute          |                                  preferred_username                                   | Parameter that allows to extract the Keycloak user name from a token available on the Spring Boot side |
|              token.converter.resource-id              |                                     maat-account                                      |                        The name of the client that Spring Boot application uses                        |

<a name="graylog-configuration"></a>
### Graylog Configuration
|     Property      |            Values             |         Description         |
|:-----------------:|:-----------------------------:|:---------------------------:|
| logging.gelf.host |           127.0.0.1           | Host for the Graylog server |
| logging.gelf.port |             12201             | Port for the Graylog server |
|  logging.config   | 	classpath:logback-spring.xml |   Log configuration file    |

<a name="grafana-loki-configuration"></a>
### Grafana Loki Configuration
|     Property      |               Values               |           Description            |
|:-----------------:|:----------------------------------:|:--------------------------------:|
|   logging.loki    |             127.0.0.1              | Host for the Grafana Loki server |
|  logging.config   | 	classpath:logback-loki-docker.xml |      Log configuration file      |

<a name="rest-api"></a>
## REST API

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

There is also the possibility to search elements by key-value option. For example parameter **name=resource1**
will search all resources or services attributes **name** with value **resource1**.
Similarly, the parameter **category=device.router** will search all resources where the attribute **category** has the value **device.router**.

Example:
- to search for resources where the category parameter has the value device.router: <br>
  ```/resourceInventoryManagement/v4.0.0/resource?category=device.router```
- to combine this with displaying specific fields, for example, category and name: <br>
  ```/resourceInventoryManagement/v4.0.0/resource?fields=category,name&category=device.router```

Note the use of the **?** operator to start the query parameters and the **&** operator to combine multiple parameters.

<a name="request-validation"></a>
### Request validation

Every resource or service added to the Maat via REST API is validated.
Validation is performed using a schema that defines the appropriate attributes and relationships according to the TMF
standards (TMF 638 Service Inventory and TMF 639 Resource Inventory APIs).

Schema location for validation in the POST request is located in the @schemaLocation attribute. This attribute can
contain public <b>[GitHub](https://github.com/GEANT-NETDEV/Inv3-schema)</b> address:

- for resource
  validation: https://raw.githubusercontent.com/GEANT-NETDEV/Inv3-schema/main/TMF639-ResourceInventory-v4-pionier.json
- for service
  validation: https://raw.githubusercontent.com/GEANT-NETDEV/Inv3-schema/main/TMF638-ServiceInventory-v4-pionier.json

or a file path

- for Linux:
    - file:///home/maat/schema/TMF639-ResourceInventory-v4.json
    - file:///home/maat/schema/TMF638-ServiceInventory-v4.json

- for Windows:
    - file:///C:/Users/schema/TMF639-ResourceInventory-v4.json
    - file:///C:/Users/schema/TMF638-ServiceInventory-v4.json

<a name="non-tmf-schema-for-validation"></a>
### Non-TMF schema for validation

The schema file does not have to follow the TMF standard. It can be simplified to address user requirements regarding
data models. An example of simple schema files for resources and services can be found here:

- for resource
  validation: https://raw.githubusercontent.com/GEANT-NETDEV/Inv3-schema/main/TMF639-ResourceInventory-v4-pionier.json
- for service
  validation: https://raw.githubusercontent.com/GEANT-NETDEV/Inv3-schema/main/TMF638-ServiceInventory-v4-pionier.json

A Postman collection for testing requests with above schema files is available
here: [Example_with_simple_schema.postman_collection.json](https://bitbucket.software.geant.org/projects/OSSBSS/repos/maat/browse/src/main/resources/Example_with_simple_schema.postman_collection.json)

<a name="authentication"></a>
### Authentication
For authentication, the Maat application uses OAuth 2.0 with Keycloak (25.0.4). The application is secured with Keycloak, which is an open-source identity and access management solution.

<a name="enabling-authentication-in-maat-with-keycloak"></a>
#### Enabling authentication in Maat with Keycloak
To enable authentication in Maat using Keycloak, the following properties must be set:
- in the application.properties file for the standalone Maat application:
  - keycloak.enabled
  - spring.security.oauth2.resourceserver.jwt.issuer-uri
  - spring.security.oauth2.resourceserver.jwt.jwk-set-uri
  - token.converter.resource-id

Example values are in the [Authentication and authorization configuration - Keycloak](#authentication-and-authorization-configuration---keycloak) section.

- or in .env file for Docker:
  - KEYCLOAK_ENABLED (true)
  - KEYCLOAK_PROTOCOL
  - KEYCLOAK_HOST
  - KEYCLOAK_PORT
  - KEYCLOAK_USER
  - KEYCLOAK_PASS
  - KEYCLOAK_CLIENT_ID_FOR_CLIENTS
  - KEYCLOAK_CLIENT_ID_FOR_USERS

Example values are in the [Keycloak Parameters](#keycloak-parameters) section.

It is recommended to use Docker for the Maat application with Keycloak. In docker-compose-5.yml or docker-compose-3.yml files, the environment variables are set for the Maat application and Keycloak.
There is also separate container (keycloak-dev-setup) with script for Keycloak configuration. The script is used to create roles, clients, and users in Keycloak and run it automatically when the container starts.

More information about fetching the access token from Keycloak and accessing the Maat application by sample clients is available in the [/clients](../clients/readme.md) folder.


<a name="authorization"></a>
### Authorization

The Maat application implements two levels of authorization (filtering) using Keycloak (25.0.4) to ensure secure and role-based access to resources and services.  

<a name="rest-method-filtering"></a>
#### REST API methods filtering

The first level of filtering is based on the roles assigned to the user in Keycloak. Depending on the roles, users are granted different levels of access to the application's endpoints.
The roles and their corresponding HTTP methods are as follows:  

- **GET** requests require the **"get"** role.
- **POST** requests require the **"post"** role.
- **DELETE** requests require the **"delete"** role.
- **PATCH** requests require the **"patch"** role.

If the keycloak.authorization.l1.roles property is set to true, the application will enforce these REST method access controls.
Otherwise, all authenticated users will have access to the endpoints.

<a name="json-content-filtering"></a>
#### JSON content filtering

The second level of filtering is based on filters included in the user's JWT token.
The token contains claims that specify additional filters for accessing resources and services.
These filters are applied on top of the role-based filtering to further restrict access based on specific criteria.

The token-based filters are defined in the user_access_filters claim within the JWT token.
This claim can include different types of filters such as get_filter, post_filter, patch_filter, and delete_filter.
These filters are applied to the corresponding HTTP methods to ensure that users can only access data that matches the specified criteria.

**Adding Attribute Mapping with filters to Token (Instructions for Keycloak)**

Configuration of "Client Scopes"

For OIDC clients, you can use Client Scopes to add user attributes to tokens.

Adding Client Scope to Client
1. Navigate to "Client Scopes":
    - In the left menu, select "Client Scopes".
2. Create a new "Client Scope":
    - Click "Create".
    - Name it **user_access_filters**.
    - Add a description: **Custom filters for REST methods assigned to the user**.
    - Select type **Default Client Scopes**.

![Screenshot of client scope](images/maat-keycloak-client-scope.png "Client scope")

3. Add "Mapper" in Client Scope:
    - After creating the Client Scope, select it from the list.
    - Go to the "Mappers" tab.
    - Click "Configure a new mapper" or "Add mapper" and choose the mapper type **User Attribute**.
    - In the "Name" field, enter **get_filter**.
    - In the "User Attribute" field, enter **get_filter**.
    - In the "Token Claim Name" field, enter **user_access_filters.get_filter**.
    - In the "Claim JSON Type" field, select **JSON**.
    - Check the options "Add to ID token" and "Add to access token".
    - Click "Save".

![Screenshot of mappers](images/maat-keycloak-mappers.png "Mappers")

Assign Client Scope to Client **maat-account**
1. Navigate to the client **maat-account**:
    - In the "Client Scopes" tab, add the newly created Client Scope (**user_access_filters**) to Assigned Client Scopes as Default.

Create Roles for Client **maat-account**
1. Create roles:
    - For the client **maat-account**, create roles named **get**, **post**, **delete**, **patch**.

![Screenshot of client roles](images/maat-keycloak-roles.png "Client roles")

2. Assign roles to the user:
    - Assign the appropriate roles to the user.

![Screenshot of user roles](images/maat-keycloak-user-roles.png "User roles")

**Example**

For example, if a user has the **get_only** role and their token includes a **get_filter** that restricts access to resources with a specific attribute,
the user will only be able to perform GET requests on resources that match both the role and the filter criteria.

By combining these two levels of filtering, the Maat application ensures that access to resources and services is tightly controlled based on both user roles and specific filter criteria included in the JWT token.

<b>Filter Logic</b>

In Keycloak, for a given user, you must define an attribute where the key is one of the four filter types (**GET_FILTER**, **POST_FILTER**, **DELETE_FILTER**, **PATCH_FILTER**) and the value is an array of objects. Everything inside an object (within {}) is represented as a logical AND function, while each separate object ({}) represents a logical OR operation.  

**Example Filters**

- get_filter:  
```
[
{"resourceCharacteristic.name": "location", "resourceCharacteristic.value": "Poznan", "category": "device.router"},
{"resourceCharacteristic.name": "location", "resourceCharacteristic.value": "Warszawa", "category": "device.router"}
]
```
This filter allows access to resources where the resourceCharacteristic.name is "location" AND resourceCharacteristic.value is "Poznan" AND category is "device.router" OR where the resourceCharacteristic.name is "location" AND resourceCharacteristic.value is "Warszawa" AND category is "device.router".  

- post_filter:  
```
[
{"category": "device.router"},
{"category": "device.switch"}
]
```
This filter allows creating resources where the category is "device.router" OR category is "device.switch".  

- delete_filter:  
```
[
{"resourceCharacteristic.name": "location", "resourceCharacteristic.value": "Poznan"}
]
```
This filter allows deleting resources where the resourceCharacteristic.name is "location" AND resourceCharacteristic.value is "Poznan".  

- patch_filter:  
```
[
{"resourceCharacteristic.name": "location", "resourceCharacteristic.value": "Poznan", "category": "device.router"},
{"resourceCharacteristic.name": "location", "resourceCharacteristic.value": "Warszawa", "category": "device.switch"}
]
```
This filter allows updating resources where the resourceCharacteristic.name is "location" AND resourceCharacteristic.value is "Poznan" AND category is "device.router" OR where the resourceCharacteristic.name is "location" AND resourceCharacteristic.value is "Warszawa" AND category is "device.switch".

![Screenshot of user attributes](images/maat-keycloak-attributes.png "User attributes")

<a name="mongodb"></a>
## MongoDB

<a name="mongodb-backup-data"></a>
### MongoDB backup data

To create a copy of the MongoDB database from the container or restore the data to database follow the steps below:

- create backup:<br>
  1\) use "mongodump" tool in MongoDB container <br>
  ```docker exec -i <container_id> /usr/bin/mongodump --username <username> --password <password> --out /dump```<br>
  2\) copy created data from container to host machine <br>
  ```docker cp <container_id>:/dump /home/service/MongodbBackup/Maat```<br><br>
- restore data:<br>
  1\) copy data from host machine to MongoDB container<br>
  ```docker cp ./dump/. <container_id>:/dump```<br>
  2\) before restoring data, delete the entire database to avoid conflicts with existing IDs.
  Below is a section for deleting data from the database. If you want to delete everything, use the **"for all of these databases"** point. <br>
  3\) use "mongorestore" tool in MongoDB container<br>
  ```docker exec -i <container_id> /usr/bin/mongorestore --username <username> --password <password> /dump```

<a name="mongodb-delete-data"></a>
### MongoDB delete data

To delete data from MongoDB for resources_db, services_db and listeners_db follow the steps below:

- for resources:
  ```docker exec -it <container_id> /usr/bin/mongosh --username <username> --password <password> --authenticationDatabase admin --eval "use resources_db;" --eval  "db.dropDatabase()"```
- for services:
  ```docker exec -it <container_id> /usr/bin/mongosh --username <username> --password <password> --authenticationDatabase admin --eval "use services_db;" --eval  "db.dropDatabase()"```
- for listeners:
  ```docker exec -it <container_id> /usr/bin/mongosh --username <username> --password <password> --authenticationDatabase admin --eval "use listeners_db;" --eval  "db.dropDatabase()"```
- for all of these databases:
  ```docker exec -it <container_id> /usr/bin/mongosh --username <username> --password <password> --authenticationDatabase admin --eval "use resources_db;" --eval  "db.dropDatabase()" --eval "use services_db;" --eval  "db.dropDatabase()" --eval "use listeners_db;" --eval  "db.dropDatabase()"```


<a name="graylog"></a>
## Graylog
Graylog is a log management system that allows you to collect, index, and analyze any machine data. It provides a web interface for searching and analyzing logs.

<a name="graylog-in-docker"></a>
### Graylog in Docker
In order to launch a docker instance in which Graylog will run, use docker-compose-5.yml. This is described in: [Installation of Maat (with EventListener, NGINX, Keycloak and Graylog)](#installation-of-maat-with-eventlistener-nginx-keycloak-and-graylog). The user should additionally make sure that the variables for Graylog are set correctly in the .env file. These are contained in: [Graylog Parameters](#graylog-parameters) and [Logging Configuration](#logging-configuration).

<a name="graylog-in-maat-or-eventlistener"></a>
### Graylog in Maat or EventListener
When building and running the Maat or EventListener application itself outside of the docker, the user, in order to enable communication with the Graylog server, must correctly set the parameters in application.properties file, described in: [Graylog Configuration](#graylog-configuration).

<a name="graylog-input-configuration-guide"></a>
### Graylog Input Configuration Guide
This readme part provides step-by-step instructions on how to add inputs in the Graylog WebGUI and locate received logs.

**Log in to Graylog Web Interface**
1. Open a web browser.
2. Navigate to the Graylog Web Interface URL:  
   `http://<SERVER_IP>:9000`
3. Log in using your credentials (by default: `admin:admin`).
![Screenshot of graylog login](images/maat-graylog-login.png "Graylog login")

**Adding an Input**
1. Go to the Inputs Section:
    - From the sidebar menu, click **System / Inputs**.
2. Choose the Input Type:
    - At the top of the page, find the dropdown menu labeled **Select Input**.
    - Select the input type you want to configure (needed: `Syslog UDP`, `Syslog TCP`, `GELF UDP`, `GELF TCP`).
3. Launch a New Input:
    - Click the **Launch new input** button.
      ![Screenshot of graylog new input](images/maat-graylog-new-input.png "New input")
4. Configure the Input:
    - Fill in the configuration form:
        - **Check the global box**: To apply the input to all nodes. 
        - **Title**: A descriptive name for the input.
        - **Bind Address**: The IP address where Graylog will listen for logs.
        - **Port**: The port Graylog will use to receive logs (e.g., `514` for Syslog, `12201` for GELF).
        - **Additional Settings**: Configure options like decoding or protocols based on the input type.
          ![Screenshot of graylog new inpiut configuration](images/maat-graylog-input-configuration.png "Input configuration")
    - Click **Launch Input** to enable the input.
5. Check Input Status:
    - Ensure the input is running; it should be marked as **Running**.
      ![Screenshot of graylog input status](images/maat-graylog-input-status.png "Input status")

**Locating and Analyzing Received Logs**
1. Navigate to the "Search" Section:
    - In the main menu, click **Search**.
    - All logs received by Graylog will be displayed in real-time.
2. Use the Sources Filter:
    - Click **Sources** to view a list of devices/servers sending logs.
3. Leverage Graylog Search Query Language (GLS):
    - Use queries to filter logs by fields:
        - Example: `source: "app_name"`
        - Example: `message: "log_keyword"`
4. Adjust the Time Range:
    - In the top-right corner, select a time range (e.g., **Last hour**, **Last day**).

For more information on the graylog configuration, visit: https://go2docs.graylog.org

<a name="grafana-loki"></a>
## Grafana Loki
Grafana Loki is a set of open source components that can be composed into a fully featured logging stack. A small index and highly compressed chunks simplifies the operation and significantly lowers the cost of Loki.

<a name="grafana-loki-in-docker"></a>
### Grafana Loki in Docker
In order to launch a docker instance in which Grafana Loki will run, use docker-compose-6.yml. This is described in: [Installation of Maat (with EventListener, NGINX, Keycloak and Grafana Loki)](#installation-of-maat-with-eventlistener-nginx-keycloak-and-grafana-loki). The user should additionally make sure that the variables for Grafana Loki are set correctly in the .env file. These are contained in: [Grafana Loki Parameters](#grafana-loki-parameters) and [Logging Configuration](#logging-configuration). Use `classpath:logback-loki-docker.xml`. 

<a name="grafana-loki-in-maat-or-eventlistener"></a>
### Grafana Loki in Maat or EventListener
When building and running the Maat or EventListener application itself outside of the docker, the user, in order to enable communication with the Grafana Loki server, must correctly set the parameters in application.properties file, described in: [Grafana Loki Configuration](#grafana-loki-configuration). Use `classpath:logback-loki-appender.xml`.


<a name="grafana-loki-input-configuration-guide"></a>
### Grafana Loki Input Configuration Guide
This readme part provides step-by-step instructions on how to locate received logs in Grafana Loki.

**Go to Grafana Web Interface**
1. Open a web browser.
2. Navigate to the Grafana Web Interface URL:  
   `http://<SERVER_IP>:3000`
   ![Screenshot of Grafana Web Interface](images/maat-grafana-loki-main.png "Grafana Web Interface")
3. Logging in is not necessary (by default: `admin:admin`).

**Locating and Analyzing Received Logs**
1. Go to the Explore:
   - From the left menu, click **Explore**.
     ![Screenshot of Grafana Explore](images/maat-grafana-loki-explore.png "Grafana Explore")
2. Choose Loki as data source.
![Screenshot of Grafana Data Source](images/maat-grafana-loki-data-source.png "Grafana Data Source")
3. Decide from which container logs you want to view:
   - In "select label" select "container"
   - In “select value” select the name of the container you are interested in.
     - Example: `select label: "container"`
     - Example: `select value: "/docker-grafana-1"`
   ![Screenshot of Grafana Select](images/maat-grafana-loki-select.png "Grafana Select")
4. Run query or launch live listening:
   - For run query choose a time range.
     ![Screenshot of Grafana Run](images/maat-grafana-loki-run.png "Grafana Run")
5. The log panel will show all received logs of a given container from the selected time interval.
   - In addition, you can expand the query with appropriate filters.
     - Example: `Line contains: "info"`
     ![Screenshot of Grafana Logs](images/maat-grafana-loki-logs.png "Grafana Logs")

For more information on the Grafana Loki configuration, visit [the official Loki documentation](https://grafana.com/docs/loki/latest/?pg=oss-loki&plcmt=quick-links)