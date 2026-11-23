package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response;

import co.edu.uco.asistenciasuco.application.features.catalogo.resolvermensajeusuario.primaryports.ResolverMensajeUsuarioInputPort;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** P3 F5: las confirmaciones salen del catalogo por su codigo OK_* y caen al respaldo, con la misma convencion de marcadores. */
class MensajesExitoTest {

    @Test
    void usa_el_texto_del_catalogo_por_el_codigo_ok() {
        final ResolverMensajeUsuarioInputPort catalogo = codigo ->
                "OK_SESION_001".equals(codigo) ? Optional.of("Sesión cerrada (catálogo).") : Optional.empty();

        assertEquals("Sesión cerrada (catálogo).", new MensajesExito(catalogo).de(MensajesExitoHttp.SESION_CERRADA));
    }

    @Test
    void cae_al_respaldo_sin_catalogo_sin_codigo_o_con_texto_en_blanco() {
        assertEquals("Sesión cerrada correctamente.", MensajesExito.soloRespaldo().de(MensajesExitoHttp.SESION_CERRADA));
        assertEquals("Sesión cerrada correctamente.",
                new MensajesExito(codigo -> Optional.empty()).de(MensajesExitoHttp.SESION_CERRADA));
        assertEquals("Sesión cerrada correctamente.",
                new MensajesExito(codigo -> Optional.of("  ")).de(MensajesExitoHttp.SESION_CERRADA));
    }

    @Test
    void el_marcador_posicional_del_catalogo_y_el_del_respaldo_dan_el_mismo_texto_sin_separador_de_miles() {
        final ResolverMensajeUsuarioInputPort catalogo = codigo -> Optional.of("Evento reactivo emitido a {0} suscriptores.");

        assertEquals("Evento reactivo emitido a 1234 suscriptores.",
                new MensajesExito(catalogo).de(MensajesExitoHttp.EVENTO_EMITIDO, 1234));
        assertEquals("Evento reactivo emitido a 1234 suscriptores.",
                MensajesExito.soloRespaldo().de(MensajesExitoHttp.EVENTO_EMITIDO, 1234));
    }

    @Test
    void admite_marcadores_secuenciales_y_una_plantilla_invalida_se_devuelve_tal_cual() {
        assertEquals("Emitido a 3 y 4.",
                new MensajesExito(codigo -> Optional.of("Emitido a {} y {}.")).de(MensajesExitoHttp.EVENTO_EMITIDO, 3, 4));
        assertEquals("Emitido a {0",
                new MensajesExito(codigo -> Optional.of("Emitido a {0")).de(MensajesExitoHttp.EVENTO_EMITIDO, 3));
    }

    @Test
    void los_codigos_coinciden_con_los_sembrados_en_la_bd() {
        assertEquals("OK_ADMIN_001", MensajesExitoHttp.CIERRE_MASIVO_SOLICITADO.codigo());
        assertEquals("OK_ASISTENCIA_001", MensajesExitoHttp.SOLICITUD_REVISION_REGISTRADA.codigo());
        assertEquals("OK_ASISTENCIA_002", MensajesExitoHttp.ASISTENCIAS_SESION_REGISTRADAS.codigo());
        assertEquals("OK_SESION_001", MensajesExitoHttp.SESION_CERRADA.codigo());
        assertEquals("OK_SESION_002", MensajesExitoHttp.SESION_CREADA.codigo());
        assertEquals("OK_SESION_003", MensajesExitoHttp.SESIONES_GRUPO_GENERADAS.codigo());
        assertEquals("OK_REALTIME_001", MensajesExitoHttp.EVENTO_EMITIDO.codigo());
    }
}
