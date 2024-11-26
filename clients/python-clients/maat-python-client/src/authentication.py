import requests
import base64
import urllib3

urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

def get_token(token_url, client_id, client_secret):
    auth = f"{client_id}:{client_secret}"
    encoded_auth = base64.b64encode(auth.encode('utf-8')).decode('utf-8')
    print(encoded_auth)
    headers = {
        "Content-Type": "application/x-www-form-urlencoded",
        "Authorization": f"Basic {encoded_auth}"
    }
    data = {
        "grant_type": "client_credentials"
    }

    response = requests.post(token_url, headers=headers, data=data, verify=False)
    response_data = response.json()
    print("Response:", response_data)

    if 'access_token' in response_data:
        token = response_data['access_token']
        print("Token ->", token)
        return token
    else:
        print("Token not found in response")

def get_resources_with_token(resource_url, token):
    token = "Bearer " + token

    header = {"Authorization": token}
    r = requests.get(resource_url, data=None, headers=header, verify=False)
    print("Response from Maat" + str(r.json()))
    #print("Response from Maat" + json.dumps(r.json(), indent=4))