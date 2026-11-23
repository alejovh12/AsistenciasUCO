package co.edu.uco.notificaciones.infrastructure.adapter.in.rest;

import co.edu.uco.notificaciones.application.model.EstadoCanal;
import co.edu.uco.notificaciones.application.model.PaginaNotificaciones;
import co.edu.uco.notificaciones.application.model.ResultadoRegistro;
import co.edu.uco.notificaciones.application.model.ResumenCola;
import co.edu.uco.notificaciones.application.model.SolicitudNotificacion;
import co.edu.uco.notificaciones.application.model.TipoPlantillaAdmin;
import co.edu.uco.notificaciones.application.model.VistaPrevia;
import co.edu.uco.notificaciones.application.port.in.NotificacionesUseCase;
import co.edu.uco.notificaciones.application.port.in.PlantillasUseCase;
import co.edu.uco.notificaciones.domain.ErrorNegocio;
import co.edu.uco.notificaciones.domain.Marcador;
import co.edu.uco.notificaciones.domain.Notificacion;
import co.edu.uco.notificaciones.domain.VersionPlantilla;
import co.edu.uco.notificaciones.soporte.Datos;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {NotificacionesController.class, PlantillasController.class, CanalController.class},
        properties = "notificaciones.api-key=clave-de-prueba")
class ApiRestTest {

    private static final String SOLICITUD = """
            {"aplicacion":"asistencias","idempotencyKey":"clave-1","tipo":"SALUDO",
             "destinatarios":[{"correo":"ana@uco.edu.co","nombre":"Ana"}],"datos":{"nombre":"Ana"},"idCorrelacion":"corr-7"}""";

    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private NotificacionesUseCase notificaciones;
    @MockitoBean
    private PlantillasUseCase plantillas;

    private static MockHttpServletRequestBuilder conClave(final MockHttpServletRequestBuilder b) {
        return b.header("X-Api-Key", "clave-de-prueba");
    }

    @Test
    void sinApiKeyTodoRequiereAutenticacion() throws Exception {
        mvc.perform(get("/api/v1/notificaciones")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("NO_AUTORIZADO"))
                .andExpect(header().exists("X-Correlation-Id"));
        mvc.perform(post("/api/v1/correos-prueba").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
    }

    @Test
    void registraUnaNotificacionNuevaConAccepted() throws Exception {
        final Notificacion n = Datos.notificacion("SALUDO", Map.of(), "ana@uco.edu.co");
        when(notificaciones.registrar(any())).thenReturn(new ResultadoRegistro(n, true));
        mvc.perform(conClave(post("/api/v1/notificaciones")).header("X-Correlation-Id", "corr-header").header("traceparent",
                        "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01").contentType(MediaType.APPLICATION_JSON).content(SOLICITUD))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(n.id().toString()))
                .andExpect(jsonPath("$.estado").value("RESERVADA"))
                .andExpect(jsonPath("$.duplicada").value(false))
                .andExpect(header().string("X-Correlation-Id", "corr-header"));
        final ArgumentCaptor<SolicitudNotificacion> s = ArgumentCaptor.forClass(SolicitudNotificacion.class);
        verify(notificaciones).registrar(s.capture());
        assertEquals("corr-7", s.getValue().idCorrelacion());
        assertEquals("00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01", s.getValue().traceparent());
        assertEquals("ana@uco.edu.co", s.getValue().destinatarios().getFirst().correo());
    }

    @Test
    void unaClaveRepetidaResponde200ConLaMismaId() throws Exception {
        final Notificacion n = Datos.notificacion("SALUDO", Map.of(), "ana@uco.edu.co");
        when(notificaciones.registrar(any())).thenReturn(new ResultadoRegistro(n, false));
        mvc.perform(conClave(post("/api/v1/notificaciones")).header("X-Correlation-Id", "corr-header")
                        .contentType(MediaType.APPLICATION_JSON).content(SOLICITUD.replace(",\"idCorrelacion\":\"corr-7\"", "")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(n.id().toString()))
                .andExpect(jsonPath("$.duplicada").value(true));
        final ArgumentCaptor<SolicitudNotificacion> s = ArgumentCaptor.forClass(SolicitudNotificacion.class);
        verify(notificaciones).registrar(s.capture());
        assertEquals("corr-header", s.getValue().idCorrelacion());
    }

    @Test
    void laIdempotencyKeyEsObligatoriaYElErrorEsUniforme() throws Exception {
        mvc.perform(conClave(post("/api/v1/notificaciones")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aplicacion\":\"a\",\"tipo\":\"SALUDO\",\"destinatarios\":[{\"correo\":\"a@uco.edu.co\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDACION_FALLIDA"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("idempotencyKey")))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    @Test
    void erroresDeNegocioSeTraducenPorCategoria() throws Exception {
        when(notificaciones.registrar(any())).thenThrow(ErrorNegocio.validacion("TIPO_NO_SOPORTADO", "no soportado"));
        mvc.perform(conClave(post("/api/v1/notificaciones")).contentType(MediaType.APPLICATION_JSON).content(SOLICITUD))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("TIPO_NO_SOPORTADO")).andExpect(jsonPath("$.message").value("no soportado"));
        final UUID id = UUID.randomUUID();
        doThrow(ErrorNegocio.noEncontrado("NOTIFICACION_NO_ENCONTRADA", "no existe")).when(notificaciones).reintentar(id);
        mvc.perform(conClave(post("/api/v1/notificaciones/" + id + "/reintento"))).andExpect(status().isNotFound());
        final UUID otra = UUID.randomUUID();
        doThrow(ErrorNegocio.conflicto("ESTADO_NO_REINTENTABLE", "no")).when(notificaciones).reintentar(otra);
        mvc.perform(conClave(post("/api/v1/notificaciones/" + otra + "/reintento"))).andExpect(status().isConflict());
    }

    @Test
    void erroresDeProtocoloTienenCodigosPropios() throws Exception {
        mvc.perform(conClave(post("/api/v1/notificaciones")).contentType(MediaType.APPLICATION_JSON).content("{no es json"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("CUERPO_INVALIDO"));
        mvc.perform(conClave(get("/api/v1/notificaciones")).param("pagina", "abc")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PARAMETRO_INVALIDO"));
        mvc.perform(conClave(post("/api/v1/notificaciones/no-es-uuid/reintento"))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PARAMETRO_INVALIDO"));
        mvc.perform(conClave(put("/api/v1/notificaciones"))).andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METODO_NO_PERMITIDO"));
        mvc.perform(conClave(get("/api/v1/no-existe"))).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("RECURSO_NO_ENCONTRADO"));
        when(notificaciones.estadoCanal()).thenThrow(new IllegalStateException("detalle interno"));
        mvc.perform(conClave(get("/api/v1/estado-canal"))).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("ERROR_INTERNO"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("detalle interno"))));
    }

    @Test
    void consultaLaColaPaginadaSinExponerCorreos() throws Exception {
        final Notificacion n = Datos.notificacion("SALUDO", Map.of("nombre", "Ana"), "ana@uco.edu.co", "b@uco.edu.co");
        when(notificaciones.consultar(any(), anyInt(), anyInt())).thenReturn(new PaginaNotificaciones(List.of(n), 2, 10, 11, 2, new ResumenCola(1, 2, 3, 4, 5)));
        mvc.perform(conClave(get("/api/v1/notificaciones")).param("estado", "ENTREGADA").param("tipo", "SALUDO").param("aplicacion", "asistencias")
                        .param("pagina", "2").param("tamano", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroPagina").value(2))
                .andExpect(jsonPath("$.totalPaginas").value(2))
                .andExpect(jsonPath("$.resumen.fallidasDefinitivas").value(5))
                .andExpect(jsonPath("$.elementos[0].cantidadDestinatarios").value(2))
                .andExpect(jsonPath("$.elementos[0].codigoEstado").value("RESERVADA"))
                .andExpect(jsonPath("$.elementos[0].asunto").value("Asunto guardado"))
                .andExpect(jsonPath("$.elementos[0].destinatarios").doesNotExist());
    }

    @Test
    void reintentoResponde202() throws Exception {
        final UUID id = UUID.randomUUID();
        mvc.perform(conClave(post("/api/v1/notificaciones/" + id + "/reintento"))).andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(id.toString())).andExpect(jsonPath("$.estado").value("PENDIENTE"));
        verify(notificaciones).reintentar(id);
    }

    @Test
    void plantillasConsultarCrearActivarYVistaPrevia() throws Exception {
        final Marcador m = new Marcador("nombre", "Nombre", "Ana", true);
        final VersionPlantilla v = new VersionPlantilla(UUID.randomUUID(), "SALUDO", "es", 2, "A", "B", true, "admin", Instant.parse("2026-11-20T10:00:00Z"));
        when(plantillas.consultar("es")).thenReturn(List.of(new TipoPlantillaAdmin("SALUDO", "Saludo de prueba", "es", 2, "A", "B", v.creadoEn(), List.of(m), List.of(v))));
        mvc.perform(conClave(get("/api/v1/plantillas")).param("idioma", "es")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].descripcion").value("Saludo de prueba"))
                .andExpect(jsonPath("$[0].marcadores[0].marcador").value("nombre"))
                .andExpect(jsonPath("$[0].marcadores[0].obligatorio").value(true))
                .andExpect(jsonPath("$[0].versiones[0].version").value(2));

        when(plantillas.crearVersion("SALUDO", "es", "A", "B", "admin")).thenReturn(v);
        mvc.perform(conClave(post("/api/v1/plantillas/SALUDO/versiones")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idioma\":\"es\",\"asunto\":\"A\",\"cuerpo\":\"B\",\"creadoPor\":\"admin\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.version").value(2)).andExpect(jsonPath("$.activa").value(true));
        mvc.perform(conClave(post("/api/v1/plantillas/SALUDO/versiones")).contentType(MediaType.APPLICATION_JSON).content("{\"asunto\":\"\",\"cuerpo\":\"B\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDACION_FALLIDA"));

        when(plantillas.activarVersion("SALUDO", null, 1)).thenReturn(v);
        mvc.perform(conClave(post("/api/v1/plantillas/SALUDO/versiones/1/activacion"))).andExpect(status().isOk());
        when(plantillas.activarVersion(eq("SALUDO"), eq("es"), eq(9))).thenThrow(ErrorNegocio.noEncontrado("VERSION_NO_ENCONTRADA", "no"));
        mvc.perform(conClave(post("/api/v1/plantillas/SALUDO/versiones/9/activacion")).param("idioma", "es")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VERSION_NO_ENCONTRADA"));

        when(plantillas.vistaPrevia("SALUDO", "es", "A", "B")).thenReturn(new VistaPrevia("A", "t", "<p>h</p>", "preview"));
        mvc.perform(conClave(post("/api/v1/plantillas/vista-previa")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"SALUDO\",\"idioma\":\"es\",\"asunto\":\"A\",\"cuerpo\":\"B\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.origen").value("preview")).andExpect(jsonPath("$.html").value("<p>h</p>"));
    }

    @Test
    void estadoDelCanalYCorreoDePrueba() throws Exception {
        when(notificaciones.estadoCanal()).thenReturn(new EstadoCanal(true, true, true, "smtp", "smtp.local", 587, "u", true, "a@uco.edu.co", "UCO",
                true, false, 50, 60, 500, "http://x", false, 0, Instant.parse("2026-11-20T10:00:00Z"), null));
        mvc.perform(conClave(get("/api/v1/estado-canal"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.canalActivo").value(true))
                .andExpect(jsonPath("$.tienePasswordConfigurado").value(true))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.ultimoEnvioUtc").value("2026-11-20T10:00:00Z"));

        final Notificacion n = Datos.notificacion("SALUDO", Map.of(), "ana@uco.edu.co");
        when(notificaciones.enviarCorreoPrueba("SALUDO", "ana@uco.edu.co", "admin", "corr-xyz")).thenReturn(n);
        mvc.perform(conClave(post("/api/v1/correos-prueba")).header("X-Correlation-Id", "corr-xyz").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"SALUDO\",\"correoDestino\":\"ana@uco.edu.co\",\"solicitadoPor\":\"admin\"}"))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.id").value(n.id().toString()));
        mvc.perform(conClave(post("/api/v1/correos-prueba")).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}
