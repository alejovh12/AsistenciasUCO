package co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SolicitarRevisionAsistenciaDomainTest {

    private static final UUID ASISTENCIA = UUID.fromString("11111111-1111-1111-1111-111111111111");
    @Test
    void construye_domain_valido_y_normaliza_motivo() {
        final SolicitarRevisionAsistenciaDomain domain =
                new SolicitarRevisionAsistenciaDomain(ASISTENCIA, "  Llegue despues del registro  ");

        assertEquals(ASISTENCIA, domain.getAsistencia());
        assertEquals("Llegue despues del registro", domain.getMotivo());
    }

    @Test
    void rechaza_asistencia_y_motivo_invalidos() {
        assertThrows(ValidationException.class, () -> new SolicitarRevisionAsistenciaDomain(null, "motivo valido"));
        assertThrows(ValidationException.class, () -> new SolicitarRevisionAsistenciaDomain(ASISTENCIA, null));
        assertThrows(ValidationException.class, () -> new SolicitarRevisionAsistenciaDomain(ASISTENCIA, "corto"));
        assertThrows(ValidationException.class, () -> new SolicitarRevisionAsistenciaDomain(ASISTENCIA, "a".repeat(301)));
    }
}
