import json
from authentication import get_token, get_resources_with_token

with open('../config.json', 'r') as config_file:
    config = json.load(config_file)

AUTH_URL = config['keycloak']['auth_url']
TOKEN_URL = config['keycloak']['token_url']
CLIENT_ID = config['keycloak']['client_id']
CLIENT_SECRET = config['keycloak']['client_secret']
REDIRECT_URI = config['keycloak']['redirect_uri']
USERNAME = config['keycloak']['username']
PASSWORD = config['keycloak']['password']
MAAT_RESOURCE_URL = config['maat']['resource_url']

token = get_token(AUTH_URL, TOKEN_URL, CLIENT_ID, CLIENT_SECRET, REDIRECT_URI, USERNAME, PASSWORD)
get_resources_with_token(MAAT_RESOURCE_URL, token)