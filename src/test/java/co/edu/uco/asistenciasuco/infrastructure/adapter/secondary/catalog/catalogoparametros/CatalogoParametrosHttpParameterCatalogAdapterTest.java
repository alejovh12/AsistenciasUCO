package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.catalog.catalogoparametros;

import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort.ParameterCatalogException;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort.ParameterNotFoundException;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.CatalogoParametrosProviderProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class CatalogoParametrosHttpParameterCatalogAdapterTest {

    private static final UUID TOKEN = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final String SECRETO = "valor-secreto-de-prueba";
    private static final String JSON = """
            {"mensajes":[],"ambiente":{"id":"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa","nombre":"Develop"},"parametros":[
             {"parametro":{"nombre":"REGISTRO.DOMINIO_CORREO_ESTUDIANTE","activo":true},
              "metadatos":[{"metadato":{"valor":null}},{"metadato":{"valor":"uco.edu.co"}},{"metadato":{"valor":"otro"}}]},
             {"parametro":{"nombre":"asistencias.limite","activo":true},"metadatos":[{"metadato":{"valor":"20"}}]},
             {"parametro":{"nombre":"ASISTENCIAS.EXACTO","activo":true},"metadatos":[{"metadato":{"valor":"si"}}]},
             {"parametro":{"nombre":"G.INACTIVO","activo":false},"metadatos":[{"metadato":{"valor":"x"}}]},
             {"parametro":{"nombre":"G.SINMETA","activo":true},"metadatos":[]},
             {"parametro":{"nombre":"G.SOLONULO","activo":true},"metadatos":[{"metadato":{}},{"metadato":{"valor":null}}]},
             {"parametro":{"nombre":"  ","activo":true},"metadatos":[{"metadato":{"valor":"x"}}]},
             {"parametro":{"activo":true},"metadatos":[{"metadato":{"valor":"x"}}]},
             {"parametro":{"nombre":"G.SINACTIVO"},"metadatos":[{"metadato":{"valor":"presente"}}]},
             {"parametro":{"nombre":"N.ENTERO","activo":true},"metadatos":[{"metadato":{"valor":" 7 "}}]},
             {"parametro":{"nombre":"N.LARGO","activo":true},"metadatos":[{"metadato":{"valor":"9000000000"}}]},
             {"parametro":{"nombre":"N.BOOL","activo":true},"metadatos":[{"metadato":{"valor":"true"}}]},
             {"parametro":{"nombre":"N.DOBLE","activo":true},"metadatos":[{"metadato":{"valor":"2.5"}}]}
            ]}""";

    private final HttpClient http = mock(HttpClient.class);

    private static CatalogoParametrosProviderProperties props(final Duration ttl) {
        return new CatalogoParametrosProviderProperties("http://localhost:8081/catalogo-parametros/api/v1/", TOKEN,
                SECRETO, null, null, Duration.ofSeconds(2), ttl, ".");
    }

    private CatalogoParametrosHttpParameterCatalogAdapter adaptor(final Duration ttl) {
        return new CatalogoParametrosHttpParameterCatalogAdapter(http, props(ttl));
    }

    private void responde(final int status, final String body) throws Exception {
        final HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
    }

    @Test
    void resuelve_por_grupo_y_clave_con_el_primer_valor_no_nulo() throws Exception {
        responde(200, JSON);
        final var a = adaptor(Duration.ofMinutes(5));

        assertEquals("uco.edu.co", a.getParameter("REGISTRO", "DOMINIO_CORREO_ESTUDIANTE").orElseThrow());
        assertEquals("uco.edu.co", a.getParameter(" REGISTRO ", " DOMINIO_CORREO_ESTUDIANTE ").orElseThrow());
        assertEquals("presente", a.getParameter("G", "SINACTIVO").orElseThrow());
    }

    @Test
    void usa_la_variante_en_mayusculas_y_prefiere_la_exacta() throws Exception {
        responde(200, JSON);
        final var a = adaptor(Duration.ofMinutes(5));

        assertEquals("20", a.getParameter("asistencias", "limite").orElseThrow());
        assertEquals("si", a.getParameter("asistencias", "exacto").orElseThrow());
    }

    @Test
    void ignora_inactivos_sin_nombre_y_sin_valor() throws Exception {
        responde(200, JSON);
        final var a = adaptor(Duration.ofMinutes(5));

        assertTrue(a.getParameter("G", "INACTIVO").isEmpty());
        assertTrue(a.getParameter("G", "SINMETA").isEmpty());
        assertTrue(a.getParameter("G", "SOLONULO").isEmpty());
        assertTrue(a.getParameter("G", "AUSENTE").isEmpty());
    }

    @Test
    void argumentos_nulos_o_vacios_devuelven_vacio_sin_llamar() {
        final var a = adaptor(Duration.ofMinutes(5));

        assertTrue(a.getParameter(null, "k").isEmpty());
        assertTrue(a.getParameter("g", null).isEmpty());
        assertTrue(a.getParameter(" ", "k").isEmpty());
        assertTrue(a.getParameter("g", "").isEmpty());
        org.mockito.Mockito.verifyNoInteractions(http);
    }

    @Test
    void required_ausente_lanza_not_found() throws Exception {
        responde(200, JSON);

        assertThrows(ParameterNotFoundException.class, () -> adaptor(Duration.ofMinutes(5)).getRequiredParameter("G", "NOPE"));
    }

    @Test
    void respuesta_sin_arreglo_de_parametros_da_catalogo_vacio() throws Exception {
        responde(200, "{\"mensajes\":[]}");

        assertTrue(adaptor(Duration.ofMinutes(5)).getParameter("G", "K").isEmpty());
    }

    @Test
    void status_no_2xx_lanza_excepcion_sin_secreto() throws Exception {
        for (final int status : new int[]{401, 500, 199, 300}) {
            responde(status, "{\"detalle\":\"" + SECRETO + "\"}");
            final var e = assertThrows(ParameterCatalogException.class, () -> adaptor(Duration.ofMinutes(5)).getParameter("G", "K"));
            assertTrue(e.getMessage().contains(String.valueOf(status)));
            assertFalse(e.getMessage().contains(SECRETO));
        }
    }

    @Test
    void io_exception_se_traduce_a_excepcion_del_catalogo() throws Exception {
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenThrow(new IOException("caida " + "red"));

        final var e = assertThrows(ParameterCatalogException.class, () -> adaptor(Duration.ofMinutes(5)).getParameter("G", "K"));
        assertFalse(e.getMessage().contains(SECRETO));
    }

    @Test
    void interrupcion_se_traduce_y_restaura_la_bandera() throws Exception {
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenThrow(new InterruptedException());
        try {
            assertThrows(ParameterCatalogException.class, () -> adaptor(Duration.ofMinutes(5)).getParameter("G", "K"));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void json_invalido_lanza_excepcion_del_catalogo() throws Exception {
        responde(200, "{no es json");

        assertThrows(ParameterCatalogException.class, () -> adaptor(Duration.ofMinutes(5)).getParameter("G", "K"));
    }

    @Test
    void la_segunda_lectura_dentro_del_ttl_usa_la_cache() throws Exception {
        responde(200, JSON);
        final var a = adaptor(Duration.ofMinutes(5));

        a.getParameter("G", "SINACTIVO");
        a.getParameter("N", "ENTERO");

        verify(http, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void invalidate_all_e_invalidate_parameter_fuerzan_nueva_llamada() throws Exception {
        responde(200, JSON);
        final var a = adaptor(Duration.ofMinutes(5));

        a.getParameter("G", "SINACTIVO");
        a.invalidateAll();
        a.getParameter("G", "SINACTIVO");
        a.invalidateParameter("G", "SINACTIVO");
        a.getParameter("G", "SINACTIVO");

        verify(http, times(3)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void ttl_cero_consulta_en_cada_lectura() throws Exception {
        responde(200, JSON);
        final var a = adaptor(Duration.ZERO);

        a.getParameter("G", "SINACTIVO");
        a.getParameter("G", "SINACTIVO");

        verify(http, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void cache_vencida_con_fallo_propaga_la_excepcion() throws Exception {
        responde(200, JSON);
        final var a = adaptor(Duration.ofMillis(1));
        a.getParameter("G", "SINACTIVO");
        Thread.sleep(10);
        responde(500, "");

        assertThrows(ParameterCatalogException.class, () -> a.getParameter("G", "SINACTIVO"));
    }

    @Test
    void la_peticion_lleva_bearer_accept_y_url_del_token() throws Exception {
        responde(200, JSON);
        adaptor(Duration.ofMinutes(5)).getParameter("G", "SINACTIVO");

        final ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(captor.capture(), any(HttpResponse.BodyHandler.class));
        final HttpRequest request = captor.getValue();
        assertEquals("Bearer " + SECRETO, request.headers().firstValue("Authorization").orElseThrow());
        assertEquals("application/json", request.headers().firstValue("Accept").orElseThrow());
        assertEquals("GET", request.method());
        assertEquals("http://localhost:8081/catalogo-parametros/api/v1/tokens/" + TOKEN + "/parametros",
                request.uri().toString());
        assertEquals(Duration.ofSeconds(2), request.timeout().orElseThrow());
    }

    @Test
    void get_parameter_as_convierte_los_tipos_soportados() throws Exception {
        responde(200, JSON);
        final var a = adaptor(Duration.ofMinutes(5));

        assertEquals("presente", a.getParameterAs("G", "SINACTIVO", String.class));
        assertEquals(7, a.getParameterAs("N", "ENTERO", Integer.class));
        assertEquals(7, a.getParameterAs("N", "ENTERO", int.class));
        assertEquals(9000000000L, a.getParameterAs("N", "LARGO", Long.class));
        assertEquals(9000000000L, a.getParameterAs("N", "LARGO", long.class));
        assertEquals(true, a.getParameterAs("N", "BOOL", Boolean.class));
        assertEquals(true, a.getParameterAs("N", "BOOL", boolean.class));
        assertEquals(2.5, a.getParameterAs("N", "DOBLE", Double.class));
        assertEquals(2.5, a.getParameterAs("N", "DOBLE", double.class));
        assertThrows(IllegalArgumentException.class, () -> a.getParameterAs("N", "ENTERO", java.math.BigDecimal.class));
        assertThrows(ParameterNotFoundException.class, () -> a.getParameterAs("N", "NOPE", Integer.class));
    }
}
