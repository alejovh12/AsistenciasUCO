package co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Contrato TARGET (LB-001B.4A): ActualizarSesionDomain(sesion, nombre, fechaHoraInicio,
 * fechaHoraFin, usuarioEjecutor). Sin docente. Codigos: ERR_NOMBRE_SESION_REQUERIDO /
 * ERR_NOMBRE_SESION_LONGITUD_INVALIDA. RED esperado: falla la compilacion (constructor de 5 parametros).
 */
class ActualizarSesionDomainTest {

    private static final UUID SESSION = UUID.randomUUID();
    private static final UUID USUARIO_EJECUTOR = UUID.randomUUID();
    private static final LocalDateTime START = LocalDateTime.of(2026, 9, 14, 8, 0);

    @Test
    void normalizesNameAndPreservesScheduleAndIdentity() {
        final ActualizarSesionDomain domain = create(SESSION, "  Tema  ", START, START.plusHours(1), USUARIO_EJECUTOR);

        assertEquals(SESSION, domain.getSesion());
        assertEquals("Tema", domain.getNombre());
        assertEquals(START, domain.getFechaHoraInicio());
        assertEquals(START.plusHours(1), domain.getFechaHoraFin());
        assertEquals(USUARIO_EJECUTOR, domain.getUsuarioEjecutor());
    }

    @Test
    void requiresSessionActorAndChronologicalSchedule() {
        assertCode("ERR_SESION_REQUERIDA", () -> create(null, "Tema", START, START.plusHours(1), USUARIO_EJECUTOR));
        assertCode("ERR_USUARIO_REQUERIDO", () -> create(SESSION, "Tema", START, START.plusHours(1), null));
        assertCode("ERR_RANGO_FECHAS_SESION_INVALIDO", () -> create(SESSION, "Tema", null, START, USUARIO_EJECUTOR));
        assertCode("ERR_RANGO_FECHAS_SESION_INVALIDO", () -> create(SESSION, "Tema", START, null, USUARIO_EJECUTOR));
        assertCode("ERR_RANGO_FECHAS_SESION_INVALIDO", () -> create(SESSION, "Tema", START, START, USUARIO_EJECUTOR));
        assertCode("ERR_RANGO_FECHAS_SESION_INVALIDO",
                () -> create(SESSION, "Tema", START, START.minusMinutes(1), USUARIO_EJECUTOR));
    }

    @Test
    void rejectsInvalidNameLengthsWithNombreCodes() {
        assertCode("ERR_NOMBRE_SESION_REQUERIDO", () -> create(SESSION, null, START, START.plusHours(1), USUARIO_EJECUTOR));
        assertCode("ERR_NOMBRE_SESION_REQUERIDO", () -> create(SESSION, "  ", START, START.plusHours(1), USUARIO_EJECUTOR));
        assertCode("ERR_NOMBRE_SESION_LONGITUD_INVALIDA",
                () -> create(SESSION, "X".repeat(51), START, START.plusHours(1), USUARIO_EJECUTOR));
        create(SESSION, "X".repeat(50), START, START.plusHours(1), USUARIO_EJECUTOR);
    }

    /** LB-001B.4B (TD-048): dbo.Sesion.nombre = NVARCHAR(50) -> acepta 1..50, rechaza 51+. */
    @Test
    void nameAcceptsBounds1And50AndRejects51WithMessage50() {
        assertEquals("A", create(SESSION, "A", START, START.plusHours(1), USUARIO_EJECUTOR).getNombre());
        assertEquals("X".repeat(50),
                create(SESSION, "X".repeat(50), START, START.plusHours(1), USUARIO_EJECUTOR).getNombre());
        final ValidationException ex = assertThrows(ValidationException.class,
                () -> create(SESSION, "X".repeat(51), START, START.plusHours(1), USUARIO_EJECUTOR));
        assertEquals("ERR_NOMBRE_SESION_LONGITUD_INVALIDA", ex.getCode());
        assertEquals("El nombre de la sesion debe tener entre 1 y 50 caracteres.", ex.getMessage());
    }

    private static ActualizarSesionDomain create(final UUID session, final String name, final LocalDateTime start,
                                                  final LocalDateTime end, final UUID actor) {
        return new ActualizarSesionDomain(session, name, start, end, actor);
    }

    private static void assertCode(final String code, final org.junit.jupiter.api.function.Executable action) {
        assertEquals(code, assertThrows(ValidationException.class, action).getCode());
    }
}
