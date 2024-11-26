import requests
import re
from requests.adapters import HTTPAdapter

# Custom adapter to bypass SSL verification
class SSLAdapter(HTTPAdapter):
    def __init__(self, ssl_context=None):
        self.ssl_context = ssl_context
        super().__init__()

    def init_poolmanager(self, *args, **kwargs):
        kwargs['ssl_context'] = self.ssl_context
        return super().init_poolmanager(*args, **kwargs)


def get_token(auth_url, token_url, client_id, client_secret, redirect_uri, username, password):
    session = requests.Session()
    session.headers.update({"Content-Type": "application/x-www-form-urlencoded"})

    print("Username: " + username)
    print("Password: " + password)

    auth_params = {
        "response_type": "code",
        "client_id": client_id,
        "redirect_uri": redirect_uri
    }
    auth_response = session.get(auth_url, params=auth_params)

    if auth_response.status_code == 200 and "kc-page-title" in auth_response.text:
        form_html = auth_response.text
        match = re.search(r'action="([^"]+)"', form_html)
        if not match:
            raise ValueError("URL not found in the form's action attribute.")

        login_url = match.group(1)
        login_data = {
            "username": username,
            "password": password
        }

        login_response = session.post(login_url, data=login_data, allow_redirects=False)

        if "Location" in login_response.headers:
            location = login_response.headers["Location"]
            authorization_code = location.split("code=")[1]

            print("Code: " + authorization_code)

            token_data = {
                "grant_type": "authorization_code",
                "code": authorization_code,
                "redirect_uri": redirect_uri,
                "client_id": client_id,
                "client_secret": client_secret
            }
            token_response = session.post(token_url, data=token_data)

            if token_response.status_code == 200:
                json_response = token_response.json()
                access_token = json_response["access_token"]
                print(f"Token -> {access_token}")
                return access_token
            else:
                print("Failed to get token")
        else:
            print("Failed to get authorization code")
    else:
        print("Failed to get login page")


def get_resources_with_token(resource_url, token):
    token = "Bearer " + token

    header = {"Authorization": token}
    r = requests.get(resource_url, data=None, headers=header)
    print("Response from Maat" + str(r.json()))
    #print("Response from Maat" + json.dumps(r.json(), indent=4))