package co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import org.junit.jupiter.api.Test;

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
                new ResolverSolicitudRevisionAsistenciaDomain(SOLICITUD, "  APROBAR  ", "  Aceptada  ", USUARIO);

        assertEquals(SOLICITUD, domain.getSolicitud());
        assertEquals("APROBAR", domain.getAccion());
        assertEquals("Aceptada", domain.getRespuestaDocente());
        assertEquals(USUARIO, domain.getUsuario());
    }

    @Test
    void constructor_con_respuesta_nula_permite_valor_nulo() {
        final ResolverSolicitudRevisionAsistenciaDomain domain =
                new ResolverSolicitudRevisionAsistenciaDomain(SOLICITUD, "APROBAR", null, USUARIO);

        assertNull(domain.getRespuestaDocente());
    }

    @Test
    void constructor_rechaza_solicitud_nula() {
        assertThrows(ValidationException.class,
                () -> new ResolverSolicitudRevisionAsistenciaDomain(null, "APROBAR", null, USUARIO));
    }

    @Test
    void constructor_rechaza_usuario_nulo() {
        assertThrows(ValidationException.class,
                () -> new ResolverSolicitudRevisionAsistenciaDomain(SOLICITUD, "APROBAR", null, null));
    }

    @Test
    void constructor_rechaza_accion_en_blanco() {
        assertThrows(ValidationException.class,
                () -> new ResolverSolicitudRevisionAsistenciaDomain(SOLICITUD, "   ", null, USUARIO));
    }
}
