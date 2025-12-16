#!/bin/bash
/opt/keycloak/bin/kcadm.sh config credentials --server http://keycloak-dev:8090 --realm master --user $KEYCLOAK_USER --password $KEYCLOAK_PASS
/opt/keycloak/bin/kcadm.sh create realms -s realm=$KEYCLOAK_REALM -s enabled=true
/opt/keycloak/bin/kcadm.sh create users -r $KEYCLOAK_REALM -s username=$KEYCLOAK_USER -s enabled=true -s email=account@mail.com -s firstName=firstName -s lastName=lastName -s emailVerified=true
/opt/keycloak/bin/kcadm.sh set-password -r $KEYCLOAK_REALM --username $KEYCLOAK_USER --new-password $KEYCLOAK_PASS
/opt/keycloak/bin/kcadm.sh create clients -r $KEYCLOAK_REALM -s clientId=$KEYCLOAK_CLIENT_ID_FOR_USERS -s enabled=true -s publicClient=true -s standardFlowEnabled=true -s redirectUris="[\"http://${KEYCLOAK_HOST}*\", \"https://${KEYCLOAK_HOST}*\"]" -s webOrigins="[\"${MAAT_UI_PROTOCOL}://${MAAT_UI_HOST}:${MAAT_UI_PORT}\"]"
/opt/keycloak/bin/kcadm.sh create clients -r $KEYCLOAK_REALM -s clientId=$KEYCLOAK_CLIENT_ID_FOR_CLIENTS -s enabled=true -s clientAuthenticatorType=client-secret -s secret=$KEYCLOAK_CLIENT_SECRET -s serviceAccountsEnabled=true -s standardFlowEnabled=false
/opt/keycloak/bin/kcadm.sh create roles -r $KEYCLOAK_REALM -s name=maatuser
/opt/keycloak/bin/kcadm.sh add-roles --uusername $KEYCLOAK_USER --rolename maatuser -r $KEYCLOAK_REALM
/opt/keycloak/bin/kcadm.sh add-roles --uusername service-account-maat --rolename maatuser -r $KEYCLOAK_REALM