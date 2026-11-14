package co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.asistencia.exception.AsistenciaErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResolverSolicitudRevisionAsistenciaDomainTest {

    private static final UUID SOLICITUD = UUID.randomUUID();
    private static final UUID USUARIO = UUID.randomUUID();

    @Test
    void constructor_con_datos_validos_normaliza_campos() {
        final ResolverSolicitudRevisionAsistenciaDomain domain =
                new ResolverSolicitudRevisionAsistenciaDomain(SOLICITUD, "  aprobada  ", "  Aceptada  ", USUARIO);

        assertEquals(SOLICITUD, domain.getSolicitud());
        assertEquals("APROBADA", domain.getAccion());
        assertEquals("Aceptada", domain.getRespuestaDocente());
        assertEquals(USUARIO, domain.getUsuario());
    }

    @Test
    void constructor_con_respuesta_nula_permite_valor_nulo() {
        final ResolverSolicitudRevisionAsistenciaDomain domain =
                new ResolverSolicitudRevisionAsistenciaDomain(SOLICITUD, "APROBADA", null, USUARIO);

        assertNull(domain.getRespuestaDocente());
    }

    @Test
    void constructor_rechaza_solicitud_nula() {
        assertThrows(ValidationException.class,
                () -> new ResolverSolicitudRevisionAsistenciaDomain(null, "APROBADA", null, USUARIO));
    }

    @Test
    void constructor_rechaza_usuario_nulo() {
        assertThrows(ValidationException.class,
                () -> new ResolverSolicitudRevisionAsistenciaDomain(SOLICITUD, "APROBADA", null, null));
    }

    @Test
    void constructor_rechaza_accion_en_blanco() {
        assertThrows(ValidationException.class,
                () -> new ResolverSolicitudRevisionAsistenciaDomain(SOLICITUD, "   ", null, USUARIO));
    }

    // SES-04: solo las acciones contractuales; ningun otro valor se interpreta como rechazo.
    @ParameterizedTest
    @ValueSource(strings = {"APROBAR", "ACEPTAR", "RECHAZAR", "APROBADO", "SI", "1"})
    void constructor_rechaza_acciones_fuera_del_contrato(final String accion) {
        final ValidationException exception = assertThrows(ValidationException.class,
                () -> new ResolverSolicitudRevisionAsistenciaDomain(SOLICITUD, accion, null, USUARIO));

        assertEquals(AsistenciaErrorCode.ERR_ACCION_REVISION_INVALIDA.code(), exception.getCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"APROBADA", "RECHAZADA", " rechazada "})
    void constructor_acepta_y_normaliza_las_acciones_contractuales(final String accion) {
        final ResolverSolicitudRevisionAsistenciaDomain domain =
                new ResolverSolicitudRevisionAsistenciaDomain(SOLICITUD, accion, "No presentó soporte.", USUARIO);

        assertEquals(accion.trim().toUpperCase(java.util.Locale.ROOT), domain.getAccion());
    }

    // PC (puntos de control): resultado de aprobar un reclamo de una toma (PRESENTE = AN, JUSTIFICADA = EX).
    @ParameterizedTest
    @ValueSource(strings = {"PRESENTE", " presente ", "JUSTIFICADA", "justificada"})
    void aprobada_con_resultado_valido_lo_normaliza_y_expone_el_codigo(final String resultado) {
        final ResolverSolicitudRevisionAsistenciaDomain domain =
                new ResolverSolicitudRevisionAsistenciaDomain(SOLICITUD, "APROBADA", null, resultado, USUARIO);

        assertEquals(resultado.strip().toUpperCase(), domain.getResultado());
        assertEquals(domain.getResultado().equals("PRESENTE") ? "AN" : "EX", domain.getCodigoResultadoAprobacion());
    }

    @Test
    void aprobada_sin_resultado_deja_que_la_base_de_datos_aplique_el_valor_por_defecto() {
        assertNull(new ResolverSolicitudRevisionAsistenciaDomain(SOLICITUD, "APROBADA", null, USUARIO).getCodigoResultadoAprobacion());
        assertNull(new ResolverSolicitudRevisionAsistenciaDomain(SOLICITUD, "APROBADA", null, "  ", USUARIO).getResultado());
    }

    @Test
    void aprobada_con_resultado_invalido_es_error_de_validacion() {
        final ValidationException exception = assertThrows(ValidationException.class,
                () -> new ResolverSolicitudRevisionAsistenciaDomain(SOLICITUD, "APROBADA", null, "AN", USUARIO));

        assertEquals(AsistenciaErrorCode.ERR_RESULTADO_APROBACION_INVALIDO.code(), exception.getCode());
    }

    @Test
    void rechazada_ignora_el_resultado() {
        final ResolverSolicitudRevisionAsistenciaDomain domain =
                new ResolverSolicitudRevisionAsistenciaDomain(SOLICITUD, "RECHAZADA", "No presentó soporte.", "PRESENTE", USUARIO);

        assertNull(domain.getResultado());
        assertNull(domain.getCodigoResultadoAprobacion());
    }

    // DEC-073: al rechazar la respuesta es obligatoria (10 a 500); al aprobar es opcional (hasta 500).
    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "Muy corta"})
    void DEC_073_rechazar_exige_una_respuesta_de_al_menos_diez_caracteres(final String respuesta) {
        final ValidationException exception = assertThrows(ValidationException.class,
                () -> new ResolverSolicitudRevisionAsistenciaDomain(SOLICITUD, "RECHAZADA", respuesta, USUARIO));

        assertEquals(AsistenciaErrorCode.ERR_RESPUESTA_RECLAMO_LONGITUD_INVALIDA.code(), exception.getCode());
    }

    @Test
    void DEC_073_rechazar_sin_respuesta_tambien_se_rechaza() {
        final ValidationException exception = assertThrows(ValidationException.class,
                () -> new ResolverSolicitudRevisionAsistenciaDomain(SOLICITUD, "RECHAZADA", null, USUARIO));

        assertEquals(AsistenciaErrorCode.ERR_RESPUESTA_RECLAMO_LONGITUD_INVALIDA.code(), exception.getCode());
    }

    @Test
    void DEC_073_rechazar_con_diez_caracteres_o_mas_es_valido_y_se_recorta() {
        final ResolverSolicitudRevisionAsistenciaDomain domain = new ResolverSolicitudRevisionAsistenciaDomain(
                SOLICITUD, "RECHAZADA", "  No presentó soporte.  ", USUARIO);

        assertEquals("No presentó soporte.", domain.getRespuestaDocente());
    }

    @ParameterizedTest
    @ValueSource(strings = {"APROBADA", "RECHAZADA"})
    void DEC_073_una_respuesta_de_mas_de_quinientos_caracteres_se_rechaza(final String accion) {
        final String larga = "A".repeat(501);

        final ValidationException exception = assertThrows(ValidationException.class,
                () -> new ResolverSolicitudRevisionAsistenciaDomain(SOLICITUD, accion, larga, USUARIO));

        assertEquals(AsistenciaErrorCode.ERR_RESPUESTA_RECLAMO_LONGITUD_INVALIDA.code(), exception.getCode());
    }

    @Test
    void DEC_073_aprobar_sin_respuesta_sigue_siendo_valido() {
        final ResolverSolicitudRevisionAsistenciaDomain domain =
                new ResolverSolicitudRevisionAsistenciaDomain(SOLICITUD, "APROBADA", "   ", USUARIO);

        assertEquals("APROBADA", domain.getAccion());
    }
}
