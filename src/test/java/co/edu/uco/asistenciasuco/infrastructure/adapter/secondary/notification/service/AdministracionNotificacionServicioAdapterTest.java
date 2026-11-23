package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.service;

import co.edu.uco.asistenciasuco.application.exception.ApplicationException;
import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.internal.InternalApplicationException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.EstadoCanalNotificacionProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.PaginaColaNotificacionProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.TipoPlantillaAdminProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.VersionPlantillaProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.VistaPreviaPlantillaProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.CommonErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.contact.ContactoDestinatario;
import co.edu.uco.asistenciasuco.infrastructure.config.properties.providers.NotificacionesServiceProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdministracionNotificacionServicioAdapterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String PLANTILLAS = """
            [{"tipo":"SESION_CANCELADA","descripcion":"Clase cancelada por el docente titular.","idioma":"es","versionActiva":2,
              "asuntoActivo":"Clase cancelada","cuerpoActivo":"Hola {estudiante}","actualizadoEn":"2026-11-20T10:15:30Z",
              "marcadores":[{"marcador":"estudiante","descripcion":"Nombre","ejemplo":"Ana","obligatorio":true}],
              "versiones":[{"id":"22222222-2222-2222-2222-222222222222","tipo":"SESION_CANCELADA","idioma":"es","version":2,
                            "asunto":"Clase cancelada","cuerpo":"Hola {estudiante}","activa":true,"creadoPor":null,
                            "creadoEn":"2026-11-20T10:15:30Z"}]}]
            """;

    private ServidorNotificacionesFalso servidor;
    private AdministracionNotificacionServicioAdapter adapter;
    private final UUID admin = UUID.randomUUID();
    private List<ContactoDestinatario> contactos = List.of();

    @BeforeEach
    void preparar() throws Exception {
        servidor = new ServidorNotificacionesFalso();
        adapter = adapter(servidor.url());
    }

    @AfterEach
    void cerrar() {
        servidor.close();
    }

    private AdministracionNotificacionServicioAdapter adapter(final String url) {
        final NotificacionesServiceProperties props = new NotificacionesServiceProperties(
                url, "clave", Duration.ofSeconds(1), Duration.ofSeconds(3), "asistencias", "es", true, "http://localhost:4200");
        final NotificacionesServiceClient cliente = new NotificacionesServiceClient(HttpClient.newHttpClient(), props,
                CircuitBreaker.of("admin-" + UUID.randomUUID(), CircuitBreakerConfig.ofDefaults()), MAPPER);
        return new AdministracionNotificacionServicioAdapter(cliente, props, ids -> contactos, MAPPER);
    }

    private JsonNode cuerpoEnviado() throws Exception {
        return MAPPER.readTree(servidor.peticiones.getLast().cuerpo());
    }

    @Test
    void consulta_las_plantillas_con_la_descripcion_marcadores_y_versiones_del_servicio() {
        servidor.responder(200, PLANTILLAS);

        final List<TipoPlantillaAdminProjection> tipos = adapter.consultarPlantillas("es", admin);

        assertEquals("/api/v1/plantillas", servidor.peticiones.getFirst().ruta());
        assertEquals("idioma=es", servidor.peticiones.getFirst().consulta());
        final TipoPlantillaAdminProjection tipo = tipos.getFirst();
        assertEquals("SESION_CANCELADA", tipo.tipo());
        assertEquals("Clase cancelada por el docente titular.", tipo.descripcion());
        assertEquals(2, tipo.versionActiva());
        assertEquals("Hola {estudiante}", tipo.cuerpoActivo());
        assertEquals(Instant.parse("2026-11-20T10:15:30Z"), tipo.actualizadoEn());
        assertEquals("estudiante", tipo.marcadores().getFirst().marcador());
        assertTrue(tipo.marcadores().getFirst().obligatorio());
        assertEquals(UUID.fromString("22222222-2222-2222-2222-222222222222"), tipo.versiones().getFirst().id());
        assertTrue(tipo.versiones().getFirst().activa());
    }

    @Test
    void crea_una_version_con_el_autor_y_codifica_el_tipo_en_la_ruta() throws Exception {
        servidor.responder(201, """
                {"id":"33333333-3333-3333-3333-333333333333","tipo":"SESION_CANCELADA","idioma":"es","version":3,
                 "asunto":"Nuevo","cuerpo":"Hola {estudiante}","activa":true,"creadoEn":"2026-11-21T08:00:00Z"}""");

        final VersionPlantillaProjection version = adapter.crearVersionPlantilla("SESION CANCELADA", "es", " Nuevo ", " Hola {estudiante} ", admin);

        assertEquals("/api/v1/plantillas/SESION%20CANCELADA/versiones", servidor.peticiones.getFirst().ruta());
        assertEquals(3, version.version());
        assertTrue(version.activa());
        final JsonNode enviado = cuerpoEnviado();
        assertEquals("Nuevo", enviado.get("asunto").asText());
        assertEquals("Hola {estudiante}", enviado.get("cuerpo").asText());
        assertEquals(admin.toString(), enviado.get("creadoPor").asText());
        assertEquals("es", enviado.get("idioma").asText());
    }

    @Test
    void activa_una_version_por_su_numero_e_idioma() {
        servidor.responder(200, "{}");

        adapter.activarVersionPlantilla("SESION_CANCELADA", "es", 1, admin);

        assertEquals("POST", servidor.peticiones.getFirst().metodo());
        assertEquals("/api/v1/plantillas/SESION_CANCELADA/versiones/1/activacion", servidor.peticiones.getFirst().ruta());
        assertEquals("idioma=es", servidor.peticiones.getFirst().consulta());
    }

    @Test
    void genera_la_vista_previa_del_borrador() throws Exception {
        servidor.responder(200, "{\"asunto\":\"A\",\"textoPlano\":\"T\",\"html\":\"<p>H</p>\",\"origen\":\"preview\"}");

        final VistaPreviaPlantillaProjection vista = adapter.generarVistaPrevia("SESION_CANCELADA", null, "A", "C");

        assertEquals("/api/v1/plantillas/vista-previa", servidor.peticiones.getFirst().ruta());
        assertEquals("<p>H</p>", vista.html());
        assertEquals("preview", vista.origen());
        assertEquals("es", cuerpoEnviado().get("idioma").asText());
        assertEquals("SESION_CANCELADA", cuerpoEnviado().get("tipo").asText());
    }

    @Test
    void consulta_la_cola_con_filtros_paginacion_y_resumen() {
        final String id = UUID.randomUUID().toString();
        final String correlacion = UUID.randomUUID().toString();
        servidor.responder(200, """
                {"elementos":[{"id":"%s","aplicacion":"asistencias","tipo":"SESION_CANCELADA","asunto":"Clase cancelada",
                  "codigoEstado":"FALLIDA_DEFINITIVA","nombreEstado":"Fallida definitiva","esTerminal":true,
                  "cantidadDestinatarios":3,"intentos":5,"creadoEn":"2026-11-20T10:00:00Z","proximoIntentoEn":null,
                  "entregadoEn":null,"codigoError":"SMTP_RECHAZADO","detalleError":"rechazado","idCorrelacion":"%s"}],
                 "numeroPagina":2,"tamanoPagina":10,"totalElementos":11,"totalPaginas":2,
                 "resumen":{"pendientes":1,"reservadas":2,"entregadas":3,"fallidasReintentables":4,"fallidasDefinitivas":5}}
                """.formatted(id, correlacion));

        final PaginaColaNotificacionProjection pagina = adapter.consultarCola("FALLIDA_DEFINITIVA", "SESION_CANCELADA", 2, 10, admin);

        assertEquals("estado=FALLIDA_DEFINITIVA&tipo=SESION_CANCELADA&pagina=2&tamano=10",
                servidor.peticiones.getFirst().consulta());
        assertEquals(11, pagina.totalElementos());
        assertEquals(2, pagina.totalPaginas());
        assertEquals(5, pagina.resumen().fallidasDefinitivas());
        final var item = pagina.elementos().getFirst();
        assertEquals(UUID.fromString(id), item.id());
        assertEquals("Fallida definitiva", item.nombreEstado());
        assertTrue(item.esTerminal());
        assertEquals(3, item.cantidadDestinatarios());
        assertEquals("SMTP_RECHAZADO", item.codigoError());
        assertNull(item.proximoIntentoEn());
        assertEquals(UUID.fromString(correlacion), item.referenciaTraza());
        assertEquals("asistencias", item.aplicacion());
    }

    @Test
    void normaliza_pagina_y_tamano_fuera_de_rango() {
        servidor.responder(200, "{\"elementos\":[]}");

        adapter.consultarCola(null, " ", 0, 500, admin);

        assertEquals("pagina=1&tamano=20", servidor.peticiones.getFirst().consulta());
    }

    @Test
    void reintenta_una_notificacion_por_el_id_de_la_cola_del_servicio() {
        final UUID id = UUID.randomUUID();
        servidor.responder(202, "{\"estado\":\"PENDIENTE\"}");

        adapter.reintentarNotificacion(id, admin);

        assertEquals("/api/v1/notificaciones/" + id + "/reintento", servidor.peticiones.getFirst().ruta());
    }

    @Test
    void el_correo_de_prueba_lleva_tipo_destino_y_solicitante() throws Exception {
        servidor.responder(202, "{\"id\":\"1\"}");

        adapter.enviarCorreoPrueba(admin, "SESION_CANCELADA", " ana@uco.edu.co ");

        assertEquals("/api/v1/correos-prueba", servidor.peticiones.getFirst().ruta());
        final JsonNode enviado = cuerpoEnviado();
        assertEquals("SESION_CANCELADA", enviado.get("tipo").asText());
        assertEquals("ana@uco.edu.co", enviado.get("correoDestino").asText());
        assertEquals(admin.toString(), enviado.get("solicitadoPor").asText());
    }

    @Test
    void el_correo_de_prueba_generico_sin_destino_usa_el_correo_del_administrador() throws Exception {
        contactos = List.of(new ContactoDestinatario(admin, "admin@uco.edu.co", true));
        servidor.responder(202, "{\"id\":\"1\"}");

        adapter.enviarCorreoPrueba(admin, "prueba@uco.edu.co");
        adapter.enviarCorreoPrueba(admin, null, null);

        assertEquals("prueba@uco.edu.co", MAPPER.readTree(servidor.peticiones.getFirst().cuerpo()).get("correoDestino").asText());
        assertEquals("admin@uco.edu.co", cuerpoEnviado().get("correoDestino").asText());
        assertFalse(cuerpoEnviado().has("tipo"));
    }

    @Test
    void sin_destino_y_sin_correo_del_administrador_es_un_error_de_validacion() {
        assertThrows(ValidationException.class, () -> adapter.enviarCorreoPrueba(admin, null, " "));
        assertThrows(ValidationException.class, () -> adapter.enviarCorreoPrueba(null, null, null));
        assertTrue(servidor.peticiones.isEmpty());
    }

    @Test
    void el_estado_del_canal_expone_el_proveedor_servicio_y_la_configuracion_del_servicio() {
        servidor.responder(200, """
                {"canalActivo":true,"dispatcherEnabled":true,"disponible":true,"canal":"smtp","host":"smtp.uco.edu.co","port":587,
                 "username":"cuenta","tienePasswordConfigurado":true,"fromAddress":"no-reply@uco.edu.co","fromName":"UCO",
                 "starttls":true,"ssl":false,"maxBcc":50,"ratePerMinute":60,"ratePerDay":500,"platformUrl":"http://x",
                 "testModeEnabled":true,"testModeRedirectCount":2}""");

        final EstadoCanalNotificacionProjection estado = adapter.consultarEstadoCanal();

        assertEquals("servicio", estado.provider());
        assertEquals("smtp", estado.sender());
        assertTrue(estado.dispatcherEnabled());
        assertTrue(estado.disponible());
        assertEquals("smtp.uco.edu.co", estado.host());
        assertEquals(587, estado.port());
        assertTrue(estado.tienePasswordConfigurado());
        assertTrue(estado.requireConfirmedEmail());
        assertTrue(estado.testModeEnabled());
        assertEquals(2, estado.testModeRedirectCount());
    }

    @Test
    void los_instantes_del_servicio_se_recortan_a_lo_que_admite_UtcInstantResponse() {
        // El servicio serializa Instant con nanosegundos (9 decimales); el contrato UtcInstantResponse admite como
        // maximo 7 y el frontend rechaza el valor (mostraba «—» en «Último envío»).
        servidor.responder(200, """
                {"canalActivo":true,"dispatcherEnabled":true,"disponible":true,"canal":"smtp",
                 "ultimoEnvioUtc":"2026-11-23T02:24:02.076351200Z","ultimoError":null}""");

        final EstadoCanalNotificacionProjection estado = adapter.consultarEstadoCanal();

        assertEquals(java.time.Instant.parse("2026-11-23T02:24:02.076351Z"), estado.ultimoEnvioUtc());
        assertEquals(0, estado.ultimoEnvioUtc().getNano() % 1_000, "sin fraccion por debajo del microsegundo");
    }

    @Test
    void el_estado_del_canal_mapea_el_ultimo_envio_y_el_ultimo_error_del_servicio() {
        servidor.responder(200, """
                {"canalActivo":true,"dispatcherEnabled":true,"disponible":true,"canal":"smtp","ultimoEnvioUtc":"2026-11-23T00:53:35Z",
                 "ultimoError":"SMTP_TIMEOUT"}""");

        final EstadoCanalNotificacionProjection estado = adapter.consultarEstadoCanal();

        assertEquals(Instant.parse("2026-11-23T00:53:35Z"), estado.ultimoEnvioUtc());
        assertEquals("SMTP_TIMEOUT", estado.ultimoError());
    }

    @Test
    void el_estado_del_canal_sin_ultimo_envio_ni_error_los_deja_nulos() {
        servidor.responder(200, "{\"canalActivo\":true,\"dispatcherEnabled\":true,\"disponible\":true,\"ultimoEnvioUtc\":null}");

        final EstadoCanalNotificacionProjection estado = adapter.consultarEstadoCanal();

        assertEquals(null, estado.ultimoEnvioUtc());
        assertEquals(null, estado.ultimoError());
    }

    @Test
    void el_correo_de_prueba_envia_la_correlacion_en_la_cabecera_y_el_administrador_como_solicitante() throws Exception {
        final UUID correlacion = UUID.fromString("f7c16a61-3c64-4598-ab40-7ba3be13ddd4");
        co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext.set(correlacion);
        try {
            servidor.responder(202, "{\"id\":\"1\"}");

            adapter.enviarCorreoPrueba(admin, "SESION_CANCELADA", "ana@uco.edu.co");

            assertEquals(correlacion.toString(), servidor.peticiones.getFirst().cabecera("X-Correlation-Id"));
            assertEquals(admin.toString(), cuerpoEnviado().get("solicitadoPor").asText());
        } finally {
            co.edu.uco.asistenciasuco.infrastructure.observability.correlation.CorrelationIdContext.clear();
        }
    }

    @Test
    void el_estado_del_canal_responde_no_disponible_si_el_servicio_no_contesta() {
        final String urlCerrada = servidor.url();
        servidor.close();

        final EstadoCanalNotificacionProjection estado = adapter(urlCerrada).consultarEstadoCanal();

        assertFalse(estado.disponible());
        assertFalse(estado.dispatcherEnabled());
        assertEquals("servicio", estado.provider());
    }

    @Test
    void traduce_los_errores_del_servicio_a_los_codigos_que_ya_conoce_el_frontend() {
        servidor.responder(404, "{\"code\":\"TIPO_NO_ENCONTRADO\",\"message\":\"No existe el tipo\"}");
        final ResourceNotFoundException tipo = assertThrows(ResourceNotFoundException.class,
                () -> adapter.generarVistaPrevia("X", "es", "a", "b"));
        assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND.code(), tipo.getCode());
        assertEquals("NTF_021", tipo.getErrorDefinition().orElseThrow().messageCode());

        servidor.responder(404, "{\"code\":\"VERSION_NO_ENCONTRADA\",\"message\":\"x\"}");
        assertEquals("NTF_005", assertThrows(ResourceNotFoundException.class,
                () -> adapter.activarVersionPlantilla("A", "es", 9, admin)).getErrorDefinition().orElseThrow().messageCode());

        servidor.responder(404, "{\"code\":\"NOTIFICACION_NO_ENCONTRADA\",\"message\":\"x\"}");
        assertEquals("NTF_020", assertThrows(ResourceNotFoundException.class,
                () -> adapter.reintentarNotificacion(UUID.randomUUID(), admin)).getErrorDefinition().orElseThrow().messageCode());

        servidor.responder(409, "{\"code\":\"ESTADO_NO_REINTENTABLE\",\"message\":\"Solo fallidas\"}");
        final ConflictException conflicto = assertThrows(ConflictException.class,
                () -> adapter.reintentarNotificacion(UUID.randomUUID(), admin));
        assertEquals("NTF_008", conflicto.getErrorDefinition().orElseThrow().messageCode());

        servidor.responder(400, "{\"code\":\"PLANTILLA_MARCADOR_OBLIGATORIO_AUSENTE\",\"message\":\"Falta el marcador {estudiante}\"}");
        final ValidationException plantilla = assertThrows(ValidationException.class,
                () -> adapter.crearVersionPlantilla("A", "es", "a", "b", admin));
        assertEquals("NTF_SERVICIO_PLANTILLA_MARCADOR_OBLIGATORIO_AUSENTE", plantilla.getErrorDefinition().orElseThrow().messageCode());
        assertEquals("Falta el marcador {estudiante}", plantilla.getMessage());

        servidor.responder(400, "{\"code\":\"VALIDACION_FALLIDA\",\"message\":\"x\"}");
        assertEquals("NTF_001", assertThrows(ValidationException.class,
                () -> adapter.crearVersionPlantilla("A", "es", "a", "b", admin)).getErrorDefinition().orElseThrow().messageCode());
    }

    @Test
    void un_5xx_la_clave_rechazada_y_la_caida_son_503() {
        for (final int status : new int[] {500, 503, 401, 403}) {
            servidor.responder(status, "{\"code\":\"X\",\"message\":\"x\"}");
            final InternalApplicationException e = assertThrows(InternalApplicationException.class,
                    () -> adapter.consultarPlantillas("es", admin));
            assertEquals(ErrorKind.SERVICE_UNAVAILABLE, e.getErrorDefinition().orElseThrow().kind());
            // P3 F5: codigo propio del catalogo (antes reutilizaba el texto de ERR_BD_NO_DISPONIBLE).
            assertEquals("ERR_NOTIFICACIONES_NO_DISPONIBLE", e.getErrorDefinition().orElseThrow().messageCode());
            assertEquals("El servicio de notificaciones no está disponible en este momento. Intenta de nuevo en unos minutos.",
                    e.getErrorDefinition().orElseThrow().defaultMessage());
        }
        final String urlCerrada = servidor.url();
        servidor.close();
        final ApplicationException e = assertThrows(ApplicationException.class, () -> adapter(urlCerrada).consultarCola(null, null, 1, 20, admin));
        assertInstanceOf(InternalApplicationException.class, e);
        assertNotNull(e.getCode());
    }
}
