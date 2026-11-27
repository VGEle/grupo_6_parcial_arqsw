"""Servidor estático + proxy /graphql.

Evita problemas de CORS: el navegador habla solo con localhost:5500 y este
servidor reenvía las peticiones al backend de Spring en localhost:8080.
Solo usa la biblioteca estándar de Python.
"""

import json
import os
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

BACKEND = os.environ.get("GRAPHQL_BACKEND", "http://localhost:8080/graphql")
PORT = int(os.environ.get("PORT", "5500"))
DIRECTORY = os.path.dirname(os.path.abspath(__file__))


class Handler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=DIRECTORY, **kwargs)

    def do_POST(self):
        if self.path.split("?")[0] != "/graphql":
            self.send_error(404, "Solo se permite POST /graphql")
            return

        longitud = int(self.headers.get("Content-Length", "0"))
        cuerpo = self.rfile.read(longitud)
        peticion = Request(
            BACKEND,
            data=cuerpo,
            method="POST",
            headers={"Content-Type": "application/json"},
        )

        try:
            with urlopen(peticion, timeout=60) as respuesta:
                datos = respuesta.read()
                estado = respuesta.status
        except HTTPError as error:
            datos = error.read()
            estado = error.code
        except URLError as error:
            datos = json.dumps(
                {"errors": [{"message": f"No se pudo conectar con el backend: {error.reason}"}]}
            ).encode("utf-8")
            estado = 502

        self.send_response(estado)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(datos)))
        self.end_headers()
        self.wfile.write(datos)


if __name__ == "__main__":
    servidor = ThreadingHTTPServer(("0.0.0.0", PORT), Handler)
    print(f"Frontend: http://localhost:{PORT}")
    print(f"GraphQL : {BACKEND}")
    print("Ctrl+C para detener.")
    servidor.serve_forever()
