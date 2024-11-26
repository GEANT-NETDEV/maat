# Client Applications

This folder contains sample client applications for communicating with the Maat application, which uses Keycloak for authentication.

There are two methods of communication: as a client using **client_credentials** and as a user with **authorization_code**.

Each version includes its own dedicated instructions. The following versions are available:
- for client applications in Java:
  - [Maat Java User](java-clients/maat-java-user/readme.md)
  - [Maat Java Client](java-clients/maat-java-client/readme.md)
- for client applications in Python:
  - [Maat Python User](python-clients/maat-python-user/readme.md)
  - [Maat Python Client](python-clients/maat-python-client/readme.md)

There is also a [Maat with Keycloak - Postman Collection](postman/maat-with-keycloak.postman_collection.json) available for fetching tokens and resources using Postman.