package co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciaautonoma.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegistrarAsistenciaAutonomaDomainTest {

    private static final UUID SESION = UUID.randomUUID();
    private static final UUID USUARIO = UUID.randomUUID();

    @Test
    void constructor_con_datos_validos_normaliza_codigo() {
        final RegistrarAsistenciaAutonomaDomain domain =
                new RegistrarAsistenciaAutonomaDomain(SESION, "  123456  ", USUARIO);

        assertEquals(SESION, domain.getSesion());
        assertEquals("123456", domain.getCodigoVerificacion());
        assertEquals(USUARIO, domain.getUsuario());
    }

    @Test
    void constructor_rechaza_sesion_nula() {
        assertThrows(ValidationException.class,
                () -> new RegistrarAsistenciaAutonomaDomain(null, "123456", USUARIO));
    }

    @Test
    void constructor_rechaza_usuario_nulo() {
        assertThrows(ValidationException.class,
                () -> new RegistrarAsistenciaAutonomaDomain(SESION, "123456", null));
    }

    @Test
    void constructor_rechaza_codigo_en_blanco() {
        assertThrows(ValidationException.class,
                () -> new RegistrarAsistenciaAutonomaDomain(SESION, "   ", USUARIO));
    }
}
