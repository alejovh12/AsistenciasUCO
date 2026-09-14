package co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegistroAsistenciaSesionDomainTest {

    @Test
    void constructor_con_datos_validos_normaliza_estado() {
        final UUID estudiante = UUID.randomUUID();

        final RegistroAsistenciaSesionDomain domain = new RegistroAsistenciaSesionDomain(estudiante, "  ASISTIO  ");

        assertEquals(estudiante, domain.getEstudiante());
        assertEquals("ASISTIO", domain.getEstado());
    }

    @Test
    void constructor_rechaza_estudiante_nulo() {
        assertThrows(ValidationException.class, () -> new RegistroAsistenciaSesionDomain(null, "ASISTIO"));
    }

    @Test
    void constructor_rechaza_estado_en_blanco() {
        assertThrows(ValidationException.class, () -> new RegistroAsistenciaSesionDomain(UUID.randomUUID(), "   "));
    }
}
