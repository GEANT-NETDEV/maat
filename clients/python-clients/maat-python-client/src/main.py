import json

from authentication import get_token, get_resources_with_token

with open('../config.json', 'r') as config_file:
    config = json.load(config_file)

TOKEN_URL = config['keycloak']['token_url']
CLIENT_ID = config['keycloak']['client_id']
CLIENT_SECRET = config['keycloak']['client_secret']
MAAT_RESOURCE_URL = config['maat']['resource_url']

token = get_token(TOKEN_URL, CLIENT_ID, CLIENT_SECRET)
get_resources_with_token(MAAT_RESOURCE_URL, token)
