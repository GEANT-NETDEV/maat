# Documentation for Maat Client Application

## Overview
This Python application is a client that authenticates with Keycloak (Maat's identity provider) and retrieves resources from a specified Maat's URL.

## Requirements
- Python 3.13

## Configuration
The application requires a configuration file named `config.json` in the root directory. The file should have the following structure:

```
{
  "keycloak": {
    "auth_url": "http://1.2.3.4:8090/realms/MaatRealm/protocol/openid-connect/auth",
    "token_url": "http://1.2.3.4:8090/realms/MaatRealm/protocol/openid-connect/token",
    "client_id": "maat-account",
    "client_secret": "d0b8122f-8dfb-46b7-b68a-f5cc4e25d123",
    "redirect_uri": "https://1.2.3.4:8082",
    "username": "username",
    "password": "password"
  },
  "maat": {
    "resource_url": "http://1.2.3.4:8080/resourceInventoryManagement/v4.0.0/resource"
  }
}
```

Parameters
- keycloak.auth_url: The URL for the Keycloak authentication endpoint.
- keycloak.token_url: The URL for the Keycloak token endpoint.
- keycloak.client_id: The client ID for authentication.
- keycloak.client_secret: The client secret for authentication.
- keycloak.redirect_uri: The redirect URI for authentication.
- keycloak.username: The username for authentication.
- keycloak.password: The password for authentication.
- maat.resource_url: The URL for the Maat resource endpoint.

## Running the Application
Run the main.py module to authenticate and retrieve resources:

```python src/main.py```