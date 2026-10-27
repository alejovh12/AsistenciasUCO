"""Obtiene un access token del cliente público PKCE `asistencias-uco-spa` para un usuario de prueba.

Reproduce lo que hace el navegador (Authorization Code + PKCE S256, sin password grant) para poder ejercitar el
backend en pruebas E2E sin UI. Nunca imprime contraseñas ni tokens: `get_token` devuelve el token al llamador.

Uso como módulo: `import spa_token; token = spa_token.get_token(env, "uco-docente")`.
"""
import base64
import hashlib
import json
import os
import secrets
import sys
import urllib.parse
import urllib.request
from http.cookiejar import CookieJar

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import lab_pdp as L  # noqa: E402

CLIENT = "asistencias-uco-spa"
REDIRECT = "http://localhost:4200/auth/callback"


def _pkce():
    verifier = base64.urlsafe_b64encode(secrets.token_bytes(48)).rstrip(b"=").decode()
    challenge = base64.urlsafe_b64encode(hashlib.sha256(verifier.encode()).digest()).rstrip(b"=").decode()
    return verifier, challenge


def get_token(env, username, password=None, client_id=CLIENT, prompt=None):
    key = "LAB_USER_" + username.upper().replace("-", "_") + "_PASSWORD"
    password = password or env[key]
    verifier, challenge = _pkce()
    state = secrets.token_urlsafe(16)
    jar = CookieJar(policy=L._LocalhostSecureContextPolicy())
    redirect = L._NoExternalRedirect()
    opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar), redirect)
    query = urllib.parse.urlencode({
        "client_id": client_id, "response_type": "code", "scope": "openid email profile", "redirect_uri": REDIRECT,
        "code_challenge": challenge, "code_challenge_method": "S256", "state": state,
        **({"prompt": prompt} if prompt else {})})
    status, _, raw = L.http(opener, "GET", f"{L.KEYCLOAK}/realms/{L.REALM}/protocol/openid-connect/auth?{query}")
    parser = L._FormParser()
    parser.feed(raw.decode("utf-8", "replace"))
    if status != 200 or not parser.action:
        raise RuntimeError(f"login Keycloak: HTTP {status}, formulario={'si' if parser.action else 'no'}")
    redirect.last_external = None
    status, headers, _ = L.http(opener, "POST", parser.action.replace("&amp;", "&"),
                                {"username": username, "password": password, "credentialId": ""}, form=True)
    location = getattr(redirect, "last_external", None) or headers.get("Location") or ""
    params = urllib.parse.parse_qs(urllib.parse.urlsplit(location).query)
    if not location.startswith(REDIRECT) or "code" not in params or params.get("state") != [state]:
        raise RuntimeError(f"login sin código de autorización (HTTP {status})")
    status, _, raw = L.http(L.plain_opener(), "POST", f"{L.KEYCLOAK}/realms/{L.REALM}/protocol/openid-connect/token", {
        "grant_type": "authorization_code", "client_id": client_id, "code": params["code"][0],
        "redirect_uri": REDIRECT, "code_verifier": verifier}, form=True)
    if status != 200:
        raise RuntimeError(f"intercambio de código: HTTP {status}")
    return json.loads(raw)
