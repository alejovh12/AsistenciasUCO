package co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.usecase.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistrarAsistenciaDomainTest {

    private static final UUID ESTUDIANTE = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID GRUPO = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SESION = UUID.fromString("33333333-3333-3333-3333-333333333333");
    @Test
    void construye_domain_valido_con_presente_y_sin_observacion() {
        final RegistrarAsistenciaDomain domain =
                new RegistrarAsistenciaDomain(ESTUDIANTE, GRUPO, SESION, true, "   ");

        assertEquals(ESTUDIANTE, domain.getEstudiante());
        assertEquals(GRUPO, domain.getGrupo());
        assertEquals(SESION, domain.getSesion());
        assertTrue(domain.isPresente());
        assertNull(domain.getObservacion());
    }

    @Test
    void exige_observacion_cuando_no_asiste_y_aplica_trim() {
        final RegistrarAsistenciaDomain domain =
                new RegistrarAsistenciaDomain(ESTUDIANTE, GRUPO, SESION, false, "  Llego tarde  ");

        assertEquals("Llego tarde", domain.getObservacion());
        assertThrows(
                IllegalArgumentException.class,
                () -> new RegistrarAsistenciaDomain(ESTUDIANTE, GRUPO, SESION, false, null)
        );
    }

    @Test
    void rechaza_identificadores_y_presencia_obligatorios() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RegistrarAsistenciaDomain(null, GRUPO, SESION, true, null)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new RegistrarAsistenciaDomain(ESTUDIANTE, null, SESION, true, null)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new RegistrarAsistenciaDomain(ESTUDIANTE, GRUPO, null, true, null)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new RegistrarAsistenciaDomain(ESTUDIANTE, GRUPO, SESION, null, null)
        );
    }

    @Test
    void rechaza_observacion_fuera_de_rango() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RegistrarAsistenciaDomain(ESTUDIANTE, GRUPO, SESION, true, "abcd")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new RegistrarAsistenciaDomain(ESTUDIANTE, GRUPO, SESION, true, "a".repeat(251))
        );
    }
}
