package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.catalogoparametros;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort.ParameterCatalogException;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogSourcePort.ParametroExterno;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.CatalogoParametrosProviderProperties;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** INT-004: mapeo de la respuesta M2M, proveedor de token y fuente de lectura puntual. */
@SuppressWarnings("unchecked")
class CatalogoParametrosM2mPiezasTest {

    private static final String SECRETO = "secreto-de-prueba-no-real";
    private static final CatalogoParametrosProviderProperties PROPS = new CatalogoParametrosProviderProperties(
            "http://localhost:8081/catalogo-parametros/api/v1", URI.create("http://localhost:19090/token"), "svc-asistencias",
            SECRETO, null, null, Duration.ofSeconds(2), Duration.ofMinutes(5), ".");

    private static String fixture(final String nombre) throws IOException {
        try (var in = CatalogoParametrosM2mPiezasTest.class.getResourceAsStream("/fixtures/catalogo-parametros/" + nombre)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static HttpResponse<String> respuesta(final int status, final String body) {
        final HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        return response;
    }

    /** Encadena respuestas ya construidas (crear un mock dentro de thenReturn rompe el stubbing en curso). */
    private static void encadena(final HttpClient http, final Object... pasos) throws Exception {
        org.mockito.stubbing.OngoingStubbing actual = when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)));
        for (final Object paso : pasos) {
            actual = paso instanceof Throwable t ? actual.thenThrow(t) : actual.thenReturn(paso);
        }
    }

    // ---- mapper ----

    @Test
    void mapea_la_respuesta_real_del_catalogo_0_1_17() throws Exception {
        final Map<String, String> valores = CatalogoParametrosM2mMapper.valoresPorNombre(fixture("respuesta-m2m-ejemplo.json"));

        assertEquals(Map.of("ARCHIVOS.LIMITE_SUBIDAS_POR_MINUTO", "10", "ASISTENCIA_QR.HABILITADA", "true",
                "ASISTENCIA_QR.LIMITE_ESTUDIANTE_POR_MINUTO", "5"), valores);
    }

    @Test
    void mapea_la_forma_plana_la_raiz_arreglo_y_valores_json() {
        final Map<String, String> valores = CatalogoParametrosM2mMapper.valoresPorNombre("""
                [{"nombre":"A.B","activo":true,"valor":"1"},
                 {"nombre":"A.C","metadatos":[{"valor":null},{"valor":"2"}]},
                 {"nombre":"A.D","valor":{"emails":["x@uco.edu.co"]}},
                 {"nombre":"A.E","activo":false,"valor":"3"},
                 {"nombre":" ","valor":"4"},
                 {"nombre":"A.F"},
                 {"parametro":{"nombre":"A.G"},"metadatos":[{"metadato":{"valor":null}}]}]""");

        assertEquals(Map.of("A.B", "1", "A.C", "2", "A.D", "{\"emails\":[\"x@uco.edu.co\"]}"), valores);
    }

    @Test
    void sin_arreglo_de_parametros_da_vacio_y_json_invalido_falla() {
        assertTrue(CatalogoParametrosM2mMapper.valoresPorNombre("{\"parametros\":[]}").isEmpty());
        assertTrue(CatalogoParametrosM2mMapper.valoresPorNombre("{\"mensajes\":[]}").isEmpty());
        assertThrows(ParameterCatalogException.class, () -> CatalogoParametrosM2mMapper.valoresPorNombre("{no es json"));
    }

    // ---- fuente puntual ----

    @Test
    void lista_parte_el_nombre_por_el_primer_separador_y_omite_nombres_o_valores_inutilizables() throws Exception {
        final HttpClient http = mock(HttpClient.class);
        final CatalogoParametrosTokenProvider tokens = mock(CatalogoParametrosTokenProvider.class);
        when(tokens.token()).thenReturn("tok");
        final HttpResponse<String> cuerpoLista = respuesta(200, """
                {"parametros":[{"parametro":{"nombre":"RED.PROXIES.X"},"metadatos":[{"metadato":{"valor":"1"}}]},
                 {"parametro":{"nombre":"SIN_SEPARADOR"},"metadatos":[{"metadato":{"valor":"2"}}]},
                 {"parametro":{"nombre":".CLAVE"},"metadatos":[{"metadato":{"valor":"3"}}]},
                 {"parametro":{"nombre":"G."},"metadatos":[{"metadato":{"valor":"4"}}]},
                 {"parametro":{"nombre":"G.VACIO"},"metadatos":[{"metadato":{"valor":"  "}}]},
                 {"parametro":{"nombre":"REGISTRO.DOMINIO"},"metadatos":[{"metadato":{"valor":"uco.edu.co"}}]}]}""");
        encadena(http, cuerpoLista);

        final List<ParametroExterno> lista = new CatalogoParametrosM2mSourceAdapter(http, PROPS, tokens).listar();

        assertEquals(List.of(new ParametroExterno("RED", "PROXIES.X", "1"), new ParametroExterno("REGISTRO", "DOMINIO", "uco.edu.co")),
                lista);
        final var captor = forClass(HttpRequest.class);
        verify(http).send(captor.capture(), any(HttpResponse.BodyHandler.class));
        assertEquals("http://localhost:8081/catalogo-parametros/api/v1/parametros/consulta/m2m", captor.getValue().uri().toString());
        assertEquals("Bearer tok", captor.getValue().headers().firstValue("Authorization").orElseThrow());
    }

    @Test
    void obtener_consulta_por_nombre_codificado_y_filtra_la_coincidencia_exacta() throws Exception {
        final HttpClient http = mock(HttpClient.class);
        final CatalogoParametrosTokenProvider tokens = mock(CatalogoParametrosTokenProvider.class);
        when(tokens.token()).thenReturn("tok");
        final HttpResponse<String> porNombre = respuesta(200, """
                {"parametros":[{"parametro":{"nombre":"REGISTRO.DOMINIO_OTRO"},"metadatos":[{"metadato":{"valor":"otro"}}]},
                 {"parametro":{"nombre":"registro.dominio"},"metadatos":[{"metadato":{"valor":"uco.edu.co"}}]}]}""");
        encadena(http, porNombre);
        final var fuente = new CatalogoParametrosM2mSourceAdapter(http, PROPS, tokens);

        assertEquals(Optional.of(new ParametroExterno("registro", "dominio", "uco.edu.co")), fuente.obtener(" REGISTRO.DOMINIO "));
        assertEquals(Optional.empty(), fuente.obtener(" "));
        assertEquals(Optional.empty(), fuente.obtener(null));

        final var captor = forClass(HttpRequest.class);
        verify(http).send(captor.capture(), any(HttpResponse.BodyHandler.class));
        assertEquals("http://localhost:8081/catalogo-parametros/api/v1/parametros/consulta/m2m?nombre=REGISTRO.DOMINIO",
                captor.getValue().uri().toString());
    }

    @Test
    void obtener_de_un_nombre_ausente_devuelve_vacio_y_un_fallo_se_propaga() throws Exception {
        final HttpClient http = mock(HttpClient.class);
        final CatalogoParametrosTokenProvider tokens = mock(CatalogoParametrosTokenProvider.class);
        when(tokens.token()).thenReturn("tok");
        final HttpResponse<String> vacia = respuesta(200, "{\"parametros\":[]}");
        final HttpResponse<String> caida = respuesta(503, "{}");
        encadena(http, vacia, caida);
        final var fuente = new CatalogoParametrosM2mSourceAdapter(http, PROPS, tokens);

        assertTrue(fuente.obtener("G.K").isEmpty());
        final var e = assertThrows(ParameterCatalogException.class, () -> fuente.obtener("G.K"));
        assertTrue(e.getMessage().contains("503"));
        assertFalse(e.getMessage().contains(SECRETO));
    }

    // ---- token ----

    private static final class Reloj extends Clock {
        Instant ahora = Instant.parse("2026-11-22T12:00:00Z");

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(final java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }

    private static CatalogoParametrosM2mTokenProvider proveedor(final HttpClient http, final Reloj reloj) {
        return new CatalogoParametrosM2mTokenProvider(PROPS, http, JsonMapper.builder().build(), reloj);
    }

    @Test
    void el_token_se_pide_con_client_credentials_se_guarda_y_se_renueva_30_segundos_antes_de_vencer() throws Exception {
        final HttpClient http = mock(HttpClient.class);
        final HttpResponse<String> r1 = respuesta(200, "{\"access_token\":\"t1\",\"expires_in\":300}");
        final HttpResponse<String> r2 = respuesta(200, "{\"access_token\":\"t2\",\"expires_in\":300}");
        encadena(http, r1, r2);
        final Reloj reloj = new Reloj();
        final var tokens = proveedor(http, reloj);

        assertTrue(tokens.tokenEnCache().isEmpty());
        assertEquals("t1", tokens.token());
        assertEquals("t1", tokens.token());
        assertEquals(Optional.of("t1"), tokens.tokenEnCache());
        verify(http, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));

        reloj.ahora = reloj.ahora.plusSeconds(269);
        assertEquals("t1", tokens.token());
        reloj.ahora = reloj.ahora.plusSeconds(1);
        assertTrue(tokens.tokenEnCache().isEmpty());
        assertEquals("t2", tokens.token());

        final var captor = forClass(HttpRequest.class);
        verify(http, times(2)).send(captor.capture(), any(HttpResponse.BodyHandler.class));
        final HttpRequest peticion = captor.getAllValues().get(0);
        assertEquals("POST", peticion.method());
        assertEquals("http://localhost:19090/token", peticion.uri().toString());
        assertEquals("application/x-www-form-urlencoded", peticion.headers().firstValue("Content-Type").orElseThrow());
    }

    @Test
    void invalidar_fuerza_un_token_nuevo() throws Exception {
        final HttpClient http = mock(HttpClient.class);
        final HttpResponse<String> r1 = respuesta(200, "{\"access_token\":\"t1\",\"expires_in\":300}");
        final HttpResponse<String> r2 = respuesta(200, "{\"access_token\":\"t2\"}");
        encadena(http, r1, r2);
        final var tokens = proveedor(http, new Reloj());

        assertEquals("t1", tokens.token());
        tokens.invalidar();
        assertEquals("t2", tokens.token());
    }

    @Test
    void sin_expires_in_usa_60_segundos_menos_el_margen() throws Exception {
        final HttpClient http = mock(HttpClient.class);
        final HttpResponse<String> r1 = respuesta(200, "{\"access_token\":\"t1\"}");
        final HttpResponse<String> r2 = respuesta(200, "{\"access_token\":\"t2\"}");
        encadena(http, r1, r2);
        final Reloj reloj = new Reloj();
        final var tokens = proveedor(http, reloj);

        assertEquals("t1", tokens.token());
        reloj.ahora = reloj.ahora.plusSeconds(CatalogoParametrosM2mTokenProvider.VIGENCIA_POR_DEFECTO_SEGUNDOS
                - CatalogoParametrosM2mTokenProvider.MARGEN_SEGUNDOS);
        assertEquals("t2", tokens.token());
    }

    @Test
    void los_fallos_del_token_son_excepciones_del_catalogo_sin_secreto() throws Exception {
        final HttpClient http = mock(HttpClient.class);
        final HttpResponse<String> noAutorizado = respuesta(401, "{\"error\":\"" + SECRETO + "\"}");
        final HttpResponse<String> noJson = respuesta(200, "{no es json");
        final HttpResponse<String> sinToken = respuesta(200, "{\"expires_in\":5}");
        encadena(http, noAutorizado, noJson, sinToken, new IOException("red caida " + SECRETO), new InterruptedException());
        final var tokens = proveedor(http, new Reloj());

        final var e401 = assertThrows(ParameterCatalogException.class, tokens::token);
        assertTrue(e401.getMessage().contains("401"));
        assertFalse(e401.getMessage().contains(SECRETO));
        assertThrows(ParameterCatalogException.class, tokens::token);
        assertThrows(ParameterCatalogException.class, tokens::token);
        final var eio = assertThrows(ParameterCatalogException.class, tokens::token);
        assertFalse(eio.getMessage().contains(SECRETO));
        try {
            assertThrows(ParameterCatalogException.class, tokens::token);
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }
}
