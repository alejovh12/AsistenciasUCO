package co.edu.uco.asistenciasuco.application.features.sesion.cerrarsesion.usecase.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CerrarSesionDomainTest {

    private static final UUID SESION = UUID.fromString("11111111-1111-1111-1111-111111111111");
    @Test
    void construye_domain_valido_y_normaliza_observacion() {
        final CerrarSesionDomain domain =
                new CerrarSesionDomain(SESION, "  Se finaliza por cierre programado  ");

        assertEquals(SESION, domain.getSesion());
        assertEquals("Se finaliza por cierre programado", domain.getObservacionCierre());
    }

    @Test
    void rechaza_sesion_y_observacion_invalidas() {
        assertThrows(IllegalArgumentException.class, () -> new CerrarSesionDomain(null, "Observacion valida"));
        assertThrows(IllegalArgumentException.class, () -> new CerrarSesionDomain(SESION, null));
        assertThrows(IllegalArgumentException.class, () -> new CerrarSesionDomain(SESION, "corta"));
        assertThrows(IllegalArgumentException.class, () -> new CerrarSesionDomain(SESION, "a".repeat(251)));
    }
}
