"""SEC-BASELINE-001 — cliente mínimo del laboratorio Security Baseline (solo stdlib).

Ejecutar con `python -I`. Lee y escribe %USERPROFILE%\\.asistencias-security-lab\\lab.env y nunca
imprime contraseñas, secretos de cliente, credenciales de aplicación ni tokens.
"""
import json
import os
import re
import secrets
import socket
import string
import urllib.error
import urllib.parse
import urllib.request
from html.parser import HTMLParser
from http.cookiejar import CookieJar, DefaultCookiePolicy

ENV_PATH = os.path.join(os.environ["USERPROFILE"], ".asistencias-security-lab", "lab.env")
KEYCLOAK = "http://keycloak.localhost:19090"
PDP = "http://localhost:18080"
PEP = "http://127.0.0.1:18081"
REALM = "security-baseline"

# Windows no resuelve *.localhost por DNS; el issuer del laboratorio es keycloak.localhost.
_original_getaddrinfo = socket.getaddrinfo


def _lab_getaddrinfo(host, *args, **kwargs):
    if host in ("keycloak.localhost", "localhost"):
        host = "127.0.0.1"
    return _original_getaddrinfo(host, *args, **kwargs)


socket.getaddrinfo = _lab_getaddrinfo


def read_env():
    values = {}
    with open(ENV_PATH, encoding="ascii") as handle:
        for line in handle:
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                key, value = line.split("=", 1)
                values[key.strip()] = value.strip()
    return values


def write_env_value(key, value):
    with open(ENV_PATH, encoding="ascii") as handle:
        lines = handle.read().splitlines()
    replaced = False
    for index, line in enumerate(lines):
        if line.split("=", 1)[0].strip() == key:
            lines[index] = f"{key}={value}"
            replaced = True
    if not replaced:
        lines.append(f"{key}={value}")
    with open(ENV_PATH, "w", encoding="ascii", newline="\r\n") as handle:
        handle.write("\n".join(lines) + "\n")


def random_secret(length=40):
    alphabet = string.ascii_letters + string.digits
    return "".join(secrets.choice(alphabet) for _ in range(length))


class _NoExternalRedirect(urllib.request.HTTPRedirectHandler):
    """Sigue redirects solo dentro del laboratorio; registra el destino externo sin visitarlo."""

    allowed = ("localhost:18080", "keycloak.localhost:19090", "127.0.0.1:18080")

    def redirect_request(self, req, fp, code, msg, headers, newurl):
        if urllib.parse.urlsplit(newurl).netloc not in self.allowed:
            self.last_external = newurl
            return None
        return super().redirect_request(req, fp, code, msg, headers, newurl)


def http(opener, method, url, body=None, headers=None, form=False):
    data = None
    headers = dict(headers or {})
    if body is not None:
        if form:
            data = urllib.parse.urlencode(body).encode()
            headers.setdefault("Content-Type", "application/x-www-form-urlencoded")
        else:
            data = json.dumps(body).encode()
            headers.setdefault("Content-Type", "application/json")
    request = urllib.request.Request(url, data=data, method=method, headers=headers)
    try:
        with opener.open(request, timeout=15) as response:
            raw = response.read()
            return response.status, dict(response.headers), raw
    except urllib.error.HTTPError as error:
        return error.code, dict(error.headers), error.read()


def plain_opener():
    return urllib.request.build_opener()


def kc_admin_token(env):
    status, _, raw = http(plain_opener(), "POST", f"{KEYCLOAK}/realms/master/protocol/openid-connect/token",
                          {"grant_type": "password", "client_id": "admin-cli",
                           "username": env["SECURITY_KC_ADMIN_USERNAME"],
                           "password": env["SECURITY_KC_ADMIN_PASSWORD"]}, form=True)
    if status != 200:
        raise SystemExit(f"Keycloak admin token HTTP {status}")
    return json.loads(raw)["access_token"]


def ensure_lab_user(env, admin_token, username, first, last):
    """Crea (si falta) un usuario sintético del realm del laboratorio y fija su contraseña local."""
    auth = {"Authorization": f"Bearer {admin_token}"}
    base = f"{KEYCLOAK}/admin/realms/{REALM}"
    key = "LAB_USER_" + re.sub(r"[^A-Z0-9]", "_", username.upper()) + "_PASSWORD"
    status, _, raw = http(plain_opener(), "GET", f"{base}/users?username={username}&exact=true", headers=auth)
    users = json.loads(raw)
    if not users:
        status, _, _ = http(plain_opener(), "POST", f"{base}/users", {
            "username": username, "enabled": True, "emailVerified": True,
            "email": f"{username}@lab.asistencias.invalid", "firstName": first, "lastName": last,
            "requiredActions": []}, headers=auth)
        if status != 201:
            raise SystemExit(f"crear {username}: HTTP {status}")
        _, _, raw = http(plain_opener(), "GET", f"{base}/users?username={username}&exact=true", headers=auth)
        users = json.loads(raw)
    user_id = users[0]["id"]
    password = env.get(key)
    if not password:
        password = random_secret()
        write_env_value(key, password)
        env[key] = password
    status, _, _ = http(plain_opener(), "PUT", f"{base}/users/{user_id}/reset-password",
                        {"type": "password", "value": password, "temporary": False}, headers=auth)
    if status != 204:
        raise SystemExit(f"password {username}: HTTP {status}")
    return user_id, key


class _LocalhostSecureContextPolicy(DefaultCookiePolicy):
    """Como un navegador: http://*.localhost es contexto seguro, así que se envían cookies Secure."""

    def return_ok_secure(self, cookie, request):
        host = urllib.parse.urlsplit(request.full_url).hostname or ""
        if cookie.secure and (host == "localhost" or host.endswith(".localhost")):
            return True
        return super().return_ok_secure(cookie, request)


class _FormParser(HTMLParser):
    def __init__(self):
        super().__init__()
        self.action = None

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if tag == "form" and attrs.get("id") == "kc-form-login":
            self.action = attrs.get("action")


def bff_session(env, username, password_key):
    """Login OIDC authorization-code contra el BFF del PDP; aprovisiona la identidad en el PDP."""
    jar = CookieJar(policy=_LocalhostSecureContextPolicy())
    redirect = _NoExternalRedirect()
    opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar), redirect)
    status, _, raw = http(opener, "GET", f"{PDP}/oauth2/authorization/keycloak")
    parser = _FormParser()
    parser.feed(raw.decode("utf-8", "replace"))
    if status != 200 or not parser.action:
        raise SystemExit(f"login page HTTP {status}; form={'yes' if parser.action else 'no'}")
    action = parser.action.replace("&amp;", "&")
    redirect.last_external = None
    status, _, _ = http(opener, "POST", action, {"username": username, "password": env[password_key],
                                                  "credentialId": ""}, form=True)
    landed = getattr(redirect, "last_external", None)
    status_session, _, raw = http(opener, "GET", f"{PDP}/api/v1/session")
    return opener, jar, status_session, json.loads(raw or b"{}"), landed


def xsrf_headers(jar):
    for cookie in jar:
        if cookie.name == "XSRF-TOKEN":
            return {"X-XSRF-TOKEN": cookie.value}
    return {}


def password_grant(env, username, password_key):
    """Token Bearer de usuario sintético (cliente BFF, direct access grants del realm upstream)."""
    status, _, raw = http(plain_opener(), "POST", f"{KEYCLOAK}/realms/{REALM}/protocol/openid-connect/token",
                          {"grant_type": "password", "client_id": "security-baseline-bff",
                           "client_secret": env["SECURITY_PDP_KEYCLOAK_CLIENT_SECRET"],
                           "username": username, "password": env[password_key], "scope": "openid"}, form=True)
    if status != 200:
        raise SystemExit(f"password grant {username}: HTTP {status}")
    return json.loads(raw)["access_token"]


def jwt_claims(token):
    import base64
    payload = token.split(".")[1]
    payload += "=" * (-len(payload) % 4)
    return json.loads(base64.urlsafe_b64decode(payload))
