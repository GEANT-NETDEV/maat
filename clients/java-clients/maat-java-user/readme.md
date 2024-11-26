# Documentation for Maat User Application

## Overview
This Java application is a client that authenticates with Keycloak (Maat's identity provider) and retrieves resources from a specified Maat's URL.  

## Requirements
- Java 21
- Maven

## Configuration
The Authentication class requires several parameters for initialization, which are provided in the **Main** class:
- **authUrl**: URL for the authentication endpoint.
- **tokenUrl**: URL for the token endpoint.
- **maatResourceUrl**: URL for the resource to be accessed.
- **clientId**: Client ID for authentication.
- **clientSecret**: Client secret for authentication.
- **redirectUri**: Redirect URI for the authentication flow.
- **username**: Username for login.
- **password**: Password for login.

## Building and running the application
To run the application, execute the main method in Main.java:

### Building

```mvn clean install```

the *jar* application will be created in the *target* folder.

### Running:

Go to **target/** folder and run **.jar** file with created name for example:

```java -jar maat-java-user-1.0.jar```

## Notes
- Ensure the URLs and credentials are correctly configured.
- The application prints the token and resource response to the console.