"""SEC-BASELINE-002 — PEP de inyección de fallos, SOLO para el caso 11 (respuesta sin decisión válida).

Uso: python -I pep_fault_stub.py [puerto=18089]
     y arrancar el backend con: start-backend-pep-lab.ps1 -IdentityMode SharedLab -PepUrl http://127.0.0.1:18089

Imita el endpoint POST /internal/v1/embedded-access-decisions y responde de forma anómala según el
`path` evaluado (ver lab_matrix.FAULTS). No certifica al proveedor real: solo prueba que el backend
real cierra (503) ante respuestas que el PEP v0.1.6 podría producir o que un intermediario podría
alterar. Escucha solo en 127.0.0.1; no registra cabeceras, credenciales ni tokens.
"""
import json
import sys
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

GROUP_OWN = "de000005-0000-4000-8000-000000000001"
GROUP_FOREIGN = "de000005-0000-4000-8000-000000000002"
BEHAVIOURS = {
    "/api/v1/docente/horarios": "200-empty",
    "/api/v1/realtime/stream": "204-no-decision",
    f"/api/v1/grupos/{GROUP_OWN}/estudiantes": "204-invalid-decision",
    f"/api/v2/sesiones/grupo/{GROUP_OWN}": "500",
    f"/api/v1/grupos/{GROUP_FOREIGN}/estudiantes": "302",
    f"/api/v2/sesiones/grupo/{GROUP_FOREIGN}": "200-with-decision",
    "/api/v1/docente/asignaturas": "timeout",
}


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def do_POST(self):  # noqa: N802
        length = int(self.headers.get("Content-Length") or 0)
        try:
            body = json.loads(self.rfile.read(length) or b"{}")
        except ValueError:
            body = {}
        behaviour = BEHAVIOURS.get(body.get("path"), "403")
        print(json.dumps({"path": body.get("path"), "method": body.get("method"), "behaviour": behaviour,
                          "credentialPresent": bool(self.headers.get("X-Application-Credential")),
                          "bearerPresent": (self.headers.get("Authorization") or "").startswith("Bearer "),
                          "correlationId": self.headers.get("X-Correlation-Id")}), flush=True)
        if self.path != "/internal/v1/embedded-access-decisions":
            behaviour = "404"
        if behaviour == "timeout":
            time.sleep(8)  # > SECURITY_BASELINE_PEP_REQUEST_TIMEOUT (5 s por defecto)
            behaviour = "204-valid"
        status, headers = {
            "200-empty": (200, {}),
            "204-no-decision": (204, {}),
            "204-invalid-decision": (204, {"X-Decision-Id": "bad decision id!"}),
            "500": (500, {}),
            "302": (302, {"Location": "http://127.0.0.1:18081/internal/v1/embedded-access-decisions"}),
            "200-with-decision": (200, {"X-Decision-Id": "11111111-2222-4333-8444-555555555555"}),
            "204-valid": (204, {"X-Decision-Id": "66666666-7777-4888-9999-000000000000"}),
            "403": (403, {}),
            "404": (404, {}),
        }[behaviour]
        self.send_response(status)
        for key, value in headers.items():
            self.send_header(key, value)
        self.send_header("Content-Length", "0")
        self.end_headers()

    def log_message(self, *args):  # silencia el log por defecto (incluiría la línea de petición)
        pass


if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 18089
    ThreadingHTTPServer(("127.0.0.1", port), Handler).serve_forever()
