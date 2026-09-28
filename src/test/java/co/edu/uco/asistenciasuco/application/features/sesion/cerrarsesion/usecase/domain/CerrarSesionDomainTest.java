package co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CerrarSesionDomainTest {

    private static final UUID SESION = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID DOCENTE = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID USUARIO_EJECUTOR = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Test
    void construye_domain_valido_y_normaliza_observacion() {
        final CerrarSesionDomain domain =
                new CerrarSesionDomain(SESION, DOCENTE, "  Se finaliza por cierre programado  ", USUARIO_EJECUTOR);

        assertEquals(SESION, domain.getSesion());
        assertEquals("Se finaliza por cierre programado", domain.getObservacionCierre());
        assertEquals(USUARIO_EJECUTOR, domain.getUsuarioEjecutor());
    }

    @Test
    void rechaza_sesion_docente_usuario_y_observacion_invalidos() {
        assertThrows(ValidationException.class,
                () -> new CerrarSesionDomain(null, DOCENTE, "Observacion valida", USUARIO_EJECUTOR));
        assertThrows(ValidationException.class,
                () -> new CerrarSesionDomain(SESION, null, "Observacion valida", USUARIO_EJECUTOR));
        assertThrows(ValidationException.class,
                () -> new CerrarSesionDomain(SESION, DOCENTE, "Observacion valida", null));
        assertThrows(ValidationException.class, () -> new CerrarSesionDomain(SESION, DOCENTE, null, USUARIO_EJECUTOR));
        assertThrows(ValidationException.class, () -> new CerrarSesionDomain(SESION, DOCENTE, "corta", USUARIO_EJECUTOR));
        assertThrows(ValidationException.class,
                () -> new CerrarSesionDomain(SESION, DOCENTE, "a".repeat(251), USUARIO_EJECUTOR));
    }
}
