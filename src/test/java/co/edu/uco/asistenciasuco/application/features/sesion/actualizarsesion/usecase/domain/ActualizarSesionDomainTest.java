package co.edu.uco.asistenciasuco.application.features.sesion.actualizarsesion.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActualizarSesionDomainTest {

    private static final UUID SESSION = UUID.randomUUID();
    private static final UUID TEACHER = UUID.randomUUID();
    private static final LocalDateTime START = LocalDateTime.of(2026, 9, 14, 8, 0);

    @Test
    void normalizesOptionalTextAndPreservesScheduleAndIdentity() {
        final ActualizarSesionDomain domain = create(SESSION, "  Tema  ", START, START.plusHours(1),
                "  A101  ", "  Descripción suficiente  ", TEACHER);
        assertEquals(SESSION, domain.getSesion());
        assertEquals(TEACHER, domain.getDocente());
        assertEquals("Tema", domain.getNombre());
        assertEquals("Descripción suficiente", domain.getDescripcion());
        assertEquals("A101", domain.getAula());
        assertEquals(START, domain.getFechaHoraInicio());
        assertEquals(START.plusHours(1), domain.getFechaHoraFin());

        assertNull(create(SESSION, "Tema", START, START.plusHours(1), null, "  ", TEACHER).getDescripcion());
    }

    @Test
    void requiresSessionTeacherAndChronologicalSchedule() {
        assertCode("ERR_SESION_REQUERIDA", () -> create(null, "Tema", START, START.plusHours(1), null, null, TEACHER));
        assertCode("ERR_DOCENTE_REQUERIDO", () -> create(SESSION, "Tema", START, START.plusHours(1), null, null, null));
        assertCode("ERR_RANGO_FECHAS_SESION_INVALIDO", () -> create(SESSION, "Tema", null, START, null, null, TEACHER));
        assertCode("ERR_RANGO_FECHAS_SESION_INVALIDO", () -> create(SESSION, "Tema", START, null, null, null, TEACHER));
        assertCode("ERR_RANGO_FECHAS_SESION_INVALIDO", () -> create(SESSION, "Tema", START, START, null, null, TEACHER));
        assertCode("ERR_RANGO_FECHAS_SESION_INVALIDO", () -> create(SESSION, "Tema", START, START.minusMinutes(1), null, null, TEACHER));
    }

    @Test
    void rejectsInvalidNameAndDescriptionLengths() {
        assertCode("ERR_TEMA_SESION_REQUERIDO", () -> create(SESSION, "  ", START, START.plusHours(1), null, null, TEACHER));
        assertCode("ERR_TEMA_SESION_LONGITUD_INVALIDA", () -> create(SESSION, "X".repeat(151), START, START.plusHours(1), null, null, TEACHER));
        assertCode("ERR_DESCRIPCION_SESION_LONGITUD_INVALIDA", () -> create(SESSION, "Tema", START, START.plusHours(1), null, "corta", TEACHER));
        assertCode("ERR_DESCRIPCION_SESION_LONGITUD_INVALIDA", () -> create(SESSION, "Tema", START, START.plusHours(1), null, "X".repeat(251), TEACHER));
        create(SESSION, "X".repeat(150), START, START.plusHours(1), null, "X".repeat(250), TEACHER);
    }

    private static ActualizarSesionDomain create(final UUID session, final String name, final LocalDateTime start,
                                                  final LocalDateTime end, final String classroom,
                                                  final String description, final UUID teacher) {
        return new ActualizarSesionDomain(session, name, start, end, classroom, description, teacher);
    }

    private static void assertCode(final String code, final org.junit.jupiter.api.function.Executable action) {
        assertEquals(code, assertThrows(ValidationException.class, action).getCode());
    }
}
