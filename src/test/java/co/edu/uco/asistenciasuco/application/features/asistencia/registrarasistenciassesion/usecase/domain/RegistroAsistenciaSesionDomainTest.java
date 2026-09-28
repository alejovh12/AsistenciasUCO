package co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegistroAsistenciaSesionDomainTest {

    @Test
    void constructor_con_datos_validos_normaliza_estado() {
        final UUID estudiante = UUID.randomUUID();

        final RegistroAsistenciaSesionDomain domain = new RegistroAsistenciaSesionDomain(estudiante, "  an  ");

        assertEquals(estudiante, domain.getEstudiante());
        assertEquals("AN", domain.getEstado());
    }

    @ParameterizedTest
    @ValueSource(strings = {"AN", "SJC", "EX"})
    void constructor_acepta_exclusivamente_el_contrato_canonico(final String estado) {
        final RegistroAsistenciaSesionDomain domain = new RegistroAsistenciaSesionDomain(UUID.randomUUID(), estado);

        assertEquals(estado, domain.getEstado());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC", "ASISTIO", "PRESENTE", "AUSENTE", "A", "F", "T", "J"})
    void constructor_rechaza_estados_fuera_del_contrato_canonico(final String estado) {
        assertThrows(ValidationException.class, () -> new RegistroAsistenciaSesionDomain(UUID.randomUUID(), estado));
    }

    @Test
    void constructor_rechaza_estudiante_nulo() {
        assertThrows(ValidationException.class, () -> new RegistroAsistenciaSesionDomain(null, "AN"));
    }

    @Test
    void constructor_rechaza_estado_en_blanco() {
        assertThrows(ValidationException.class, () -> new RegistroAsistenciaSesionDomain(UUID.randomUUID(), "   "));
    }
}
