package co.edu.uco.notificaciones.integracion;

import co.edu.uco.notificaciones.application.service.Despachador;
import co.edu.uco.notificaciones.infrastructure.adapter.out.persistence.NotificacionJpaRepository;
import jakarta.mail.BodyPart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Recorrido completo por HTTP contra SQL Server y un SMTP real: recepcion, entrega, idempotencia, plantillas y reintento. */
@SpringBootTest
@AutoConfigureMockMvc
class FlujoCompletoTest extends BaseIntegracion {

    private static final String CLAVE = "clave-integracion";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private Despachador despachador;
    @Autowired
    private JsonMapper json;
    @Autowired
    private NotificacionJpaRepository cola;

    @BeforeEach
    void limpiar() throws Exception {
        smtp.purgeEmailFromAllMailboxes();
        cola.deleteAll();
    }

    /** Texto plano decodificado del primer cuerpo text/plain del mensaje (recorre los multipart). */
    private static String textoPlano(final Part parte) throws Exception {
        if (parte.isMimeType("text/plain")) {
            return parte.getContent().toString();
        }
        if (parte.getContent() instanceof MimeMultipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                final BodyPart hijo = multipart.getBodyPart(i);
                final String texto = textoPlano(hijo);
                if (!texto.isEmpty()) {
                    return texto;
                }
            }
        }
        return "";
    }

    private static MockHttpServletRequestBuilder api(final MockHttpServletRequestBuilder b) {
        return b.header("X-Api-Key", CLAVE).contentType(MediaType.APPLICATION_JSON);
    }

    private static String solicitud(final String clave, final String correo, final String nombre) {
        return "{\"aplicacion\":\"flujo\",\"idempotencyKey\":\"" + clave + "\",\"tipo\":\"RETIRO_ESTUDIANTE_GRUPO\","
                + "\"destinatarios\":[{\"correo\":\"" + correo + "\",\"nombre\":\"Ana\"}],"
                + "\"datos\":{\"grupo\":\"G01\",\"estudiante\":\"" + nombre + "\"},\"idCorrelacion\":\"corr-flujo\"}";
    }

    private JsonNode llamar(final MockHttpServletRequestBuilder b, final int estado) throws Exception {
        final MvcResult r = mvc.perform(b).andExpect(status().is(estado)).andReturn();
        return json.readTree(r.getResponse().getContentAsString());
    }

    @Test
    void recibeEntregaYNoDuplicaConLaMismaClave() throws Exception {
        final JsonNode aceptada = llamar(api(post("/api/v1/notificaciones")).content(solicitud("c-" + UUID.randomUUID(), "ana@uco.edu.co", "Ana Pérez")), 202);
        final String id = aceptada.get("id").asString();
        assertEquals("PENDIENTE", aceptada.get("estado").asString());

        assertEquals(1, despachador.ejecutarCiclo());
        final MimeMessage[] recibidos = smtp.getReceivedMessages();
        assertEquals(2, recibidos.length, "To (remitente) y BCC (destinatario)");
        assertEquals("Retiro del grupo G01", recibidos[0].getSubject());
        assertTrue(textoPlano(recibidos[0]).contains("Se registró el retiro de Ana Pérez del grupo G01."), textoPlano(recibidos[0]));

        final String clave = "c-" + UUID.randomUUID();
        final JsonNode primera = llamar(api(post("/api/v1/notificaciones")).content(solicitud(clave, "ana@uco.edu.co", "Ana Pérez")), 202);
        final JsonNode segunda = llamar(api(post("/api/v1/notificaciones")).content(solicitud(clave, "ana@uco.edu.co", "Ana Pérez")), 200);
        assertEquals(primera.get("id").asString(), segunda.get("id").asString());
        assertTrue(segunda.get("duplicada").asBoolean());
        smtp.purgeEmailFromAllMailboxes();
        assertEquals(1, despachador.ejecutarCiclo());
        assertEquals(2, smtp.getReceivedMessages().length, "la clave repetida no genero un segundo envio");

        mvc.perform(api(get("/api/v1/notificaciones")).param("aplicacion", "flujo").param("estado", "ENTREGADA"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.resumen.entregadas").value(2))
                .andExpect(jsonPath("$.elementos[0].cantidadDestinatarios").value(1));
        assertFalse(id.isBlank());
    }

    @Test
    void laApiExigeLaClaveYValidaLasSolicitudes() throws Exception {
        mvc.perform(get("/api/v1/notificaciones")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/notificaciones").header("X-Api-Key", "mala")).andExpect(status().isUnauthorized());
        mvc.perform(api(post("/api/v1/notificaciones")).content(solicitud("k", "ana@uco.edu.co", "A").replace("RETIRO_ESTUDIANTE_GRUPO", "NO_EXISTE")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("TIPO_NO_SOPORTADO"));
        mvc.perform(api(post("/api/v1/notificaciones")).content(solicitud("k", "malo", "A")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("DESTINATARIO_INVALIDO"));
        mvc.perform(api(post("/api/v1/notificaciones/" + UUID.randomUUID() + "/reintento"))).andExpect(status().isNotFound());
    }

    @Test
    void administrarPlantillasConVistaPreviaYVersionActiva() throws Exception {
        final JsonNode tipos = llamar(api(get("/api/v1/plantillas")).param("idioma", "es"), 200);
        assertEquals(22, tipos.size());
        JsonNode retiro = null;
        for (final JsonNode t : tipos) {
            assertFalse(t.get("descripcion").asString().isBlank());
            if ("RETIRO_ESTUDIANTE_GRUPO".equals(t.get("tipo").asString())) {
                retiro = t;
            }
        }
        assertEquals(1, retiro.get("versionActiva").asInt());
        assertEquals(3, retiro.get("marcadores").size());

        mvc.perform(api(post("/api/v1/plantillas/vista-previa")).content(
                        "{\"tipo\":\"RETIRO_ESTUDIANTE_GRUPO\",\"asunto\":\"Baja {grupo}\",\"cuerpo\":\"{estudiante} salio de {grupo}\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.asunto").value("Baja G01 de Ingeniería de Software"))
                .andExpect(jsonPath("$.origen").value("preview"));
        mvc.perform(api(post("/api/v1/plantillas/vista-previa")).content("{\"tipo\":\"NO_EXISTE\",\"asunto\":\"a\",\"cuerpo\":\"b\"}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("TIPO_NO_ENCONTRADO"));

        mvc.perform(api(post("/api/v1/plantillas/RETIRO_ESTUDIANTE_GRUPO/versiones")).content("{\"asunto\":\"Baja {grupo}\",\"cuerpo\":\"sin marcadores\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PLANTILLA_MARCADOR_OBLIGATORIO_AUSENTE"));
        final JsonNode version = llamar(api(post("/api/v1/plantillas/RETIRO_ESTUDIANTE_GRUPO/versiones"))
                .content("{\"asunto\":\"Baja de {estudiante}\",\"cuerpo\":\"{estudiante} salio de {grupo}. Gracias.\",\"creadoPor\":\"admin\"}"), 201);
        assertTrue(version.get("activa").asBoolean());
        final int numero = version.get("version").asInt();

        smtp.purgeEmailFromAllMailboxes();
        llamar(api(post("/api/v1/notificaciones")).content(solicitud("plantilla-" + UUID.randomUUID(), "ana@uco.edu.co", "Luis")), 202);
        despachador.ejecutarCiclo();
        assertEquals("Baja de Luis", smtp.getReceivedMessages()[0].getSubject());

        mvc.perform(api(post("/api/v1/plantillas/RETIRO_ESTUDIANTE_GRUPO/versiones/1/activacion"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1)).andExpect(jsonPath("$.activa").value(true));
        mvc.perform(api(post("/api/v1/plantillas/RETIRO_ESTUDIANTE_GRUPO/versiones/" + (numero + 50) + "/activacion"))).andExpect(status().isNotFound());
        smtp.purgeEmailFromAllMailboxes();
        llamar(api(post("/api/v1/notificaciones")).content(solicitud("plantilla-" + UUID.randomUUID(), "ana@uco.edu.co", "Luis")), 202);
        despachador.ejecutarCiclo();
        assertEquals("Retiro del grupo G01", smtp.getReceivedMessages()[0].getSubject());
    }

    @Test
    void conElSmtpCaidoReintentaYLuegoEntrega() throws Exception {
        smtp.stop();
        final JsonNode aceptada = llamar(api(post("/api/v1/notificaciones")).content(solicitud("caido-" + UUID.randomUUID(), "ana@uco.edu.co", "Ana")), 202);
        final String id = aceptada.get("id").asString();
        try {
            despachador.ejecutarCiclo();
        } finally {
            smtp.start();
        }
        mvc.perform(api(get("/api/v1/notificaciones")).param("estado", "FALLIDA_REINTENTABLE").param("aplicacion", "flujo"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.elementos[0].id").value(id))
                .andExpect(jsonPath("$.elementos[0].codigoError").value("FALLO_REINTENTABLE"))
                .andExpect(jsonPath("$.elementos[0].intentos").value(1));
        mvc.perform(api(get("/api/v1/estado-canal"))).andExpect(status().isOk()).andExpect(jsonPath("$.ultimoError").value("FALLO_REINTENTABLE"))
                .andExpect(jsonPath("$.password").doesNotExist());

        mvc.perform(api(post("/api/v1/notificaciones/" + id + "/reintento"))).andExpect(status().isAccepted());
        assertEquals(1, despachador.ejecutarCiclo());
        mvc.perform(api(get("/api/v1/notificaciones")).param("estado", "ENTREGADA").param("aplicacion", "flujo")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
        mvc.perform(api(post("/api/v1/notificaciones/" + id + "/reintento"))).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ESTADO_NO_REINTENTABLE"));
    }

    @Test
    void correoDePruebaYEstadoDelCanal() throws Exception {
        smtp.purgeEmailFromAllMailboxes();
        llamar(api(post("/api/v1/correos-prueba")).content("{\"tipo\":\"RETIRO_ESTUDIANTE_GRUPO\",\"correoDestino\":\"admin@uco.edu.co\",\"solicitadoPor\":\"admin\"}"), 202);
        llamar(api(post("/api/v1/correos-prueba")).content("{\"correoDestino\":\"admin@uco.edu.co\"}"), 202);
        assertEquals(2, despachador.ejecutarCiclo());
        assertEquals(4, smtp.getReceivedMessages().length);
        final StringBuilder todos = new StringBuilder();
        for (final MimeMessage m : smtp.getReceivedMessages()) {
            todos.append(m.getSubject()).append('|');
        }
        final String asuntos = todos.toString();
        assertTrue(asuntos.contains("Retiro del grupo G01 de Ingeniería de Software"), asuntos);
        assertTrue(asuntos.contains("Prueba de canal de notificación institucional (PRUEBA_CANAL_NOTIFICACION)"), asuntos);
        mvc.perform(api(post("/api/v1/correos-prueba")).content("{\"correoDestino\":\"malo\"}")).andExpect(status().isBadRequest());
        mvc.perform(api(post("/api/v1/correos-prueba")).content("{\"tipo\":\"NO_EXISTE\",\"correoDestino\":\"a@uco.edu.co\"}")).andExpect(status().isNotFound());
        mvc.perform(api(get("/api/v1/estado-canal"))).andExpect(status().isOk()).andExpect(jsonPath("$.canal").value("smtp"))
                .andExpect(jsonPath("$.disponible").value(true)).andExpect(jsonPath("$.ultimoEnvioUtc").isNotEmpty());
    }
}
