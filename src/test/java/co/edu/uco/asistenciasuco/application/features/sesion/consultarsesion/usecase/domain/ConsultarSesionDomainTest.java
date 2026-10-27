package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConsultarSesionDomainTest {

    private static final UUID SESION = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USUARIO = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void construye_domain_valido() {
        final ConsultarSesionDomain domain = new ConsultarSesionDomain(SESION, USUARIO);

        assertEquals(SESION, domain.getSesion());
        assertEquals(USUARIO, domain.getUsuarioEjecutor());
    }

    @Test
    void rechaza_sesion_obligatoria() {
        assertThrows(ValidationException.class, () -> new ConsultarSesionDomain(null, USUARIO));
    }

    @Test
    void rechaza_usuario_ejecutor_obligatorio() {
        assertThrows(ValidationException.class, () -> new ConsultarSesionDomain(SESION, null));
    }
}
