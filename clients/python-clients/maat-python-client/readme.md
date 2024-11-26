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
    "token_url": "http://1.2.3.4:8090/realms/MaatRealm/protocol/openid-connect/token",
    "client_id": "maat",
    "client_secret": "d0b8122f-8dfb-46b7-b68a-f5cc4e25d123"
  },
  "maat": {
    "resource_url": "https://1.2.3.4:8082/resourceInventoryManagement/v4.0.0/resource"
  }
}
```

Parameters
- keycloak.token_url: The URL for the Keycloak token endpoint.
- keycloak.client_id: The client ID for authentication.
- keycloak.client_secret: The client secret for authentication.
- maat.resource_url: The URL for the Maat resource endpoint.

## Running the Application
Run the main.py module to authenticate and retrieve resources:

```python src/main.py```