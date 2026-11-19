package co.edu.uco.asistenciasuco.application.features.sesioncalendario.consultarcalendario.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.sesioncalendario.consultarcalendario.primaryports.CalendarioSesiones.Comando;
import co.edu.uco.asistenciasuco.application.features.sesioncalendario.consultarcalendario.primaryports.CalendarioSesiones.SesionCalendario;
import co.edu.uco.asistenciasuco.application.features.sesioncalendario.exception.CalendarioErrorCode;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionCalendarioRepositoryPort;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** ATA D7: el calendario del estudiante y del docente traduce los DATETIME2 en UTC a instantes y propaga los rechazos de la DB. */
class ConsultarCalendarioUseCaseImplTest {

    private static final UUID USUARIO = UUID.randomUUID();
    private static final LocalDate DESDE = LocalDate.of(2026, 11, 16);
    private static final LocalDate HASTA = LocalDate.of(2026, 11, 22);

    private final SesionCalendarioRepositoryPort repositorio = mock(SesionCalendarioRepositoryPort.class);
    private final ConsultarCalendarioEstudianteUseCaseImpl estudiante = new ConsultarCalendarioEstudianteUseCaseImpl(repositorio);
    private final ConsultarCalendarioDocenteUseCaseImpl docente = new ConsultarCalendarioDocenteUseCaseImpl(repositorio);

    @Test
    void el_estudiante_recibe_las_sesiones_con_instantes_utc_y_sus_banderas() {
        final UUID sesion = UUID.randomUUID();
        final UUID grupo = UUID.randomUUID();
        final UUID asignatura = UUID.randomUUID();
        final UUID profesor = UUID.randomUUID();
        when(repositorio.consultarEstudiante(USUARIO, DESDE, HASTA)).thenReturn(List.of(
                new SesionCalendarioRepositoryPort.SesionCalendario(sesion, grupo, 3, "G03", asignatura, "MAT101", "Calculo", profesor,
                        "Maria Docente", LocalDateTime.of(2026, 11, 16, 13, 0), LocalDateTime.of(2026, 11, 16, 15, 0), true, true,
                        "Dia festivo", true, "Festivo X"),
                new SesionCalendarioRepositoryPort.SesionCalendario(UUID.randomUUID(), grupo, null, null, asignatura, null, null, profesor,
                        null, null, null, false, false, null, false, null)));

        final List<SesionCalendario> sesiones = estudiante.execute(new Comando(USUARIO, DESDE, HASTA));

        assertEquals(2, sesiones.size());
        assertEquals(new SesionCalendario(sesion, grupo, 3, "G03", asignatura, "MAT101", "Calculo", profesor, "Maria Docente",
                Instant.parse("2026-11-16T13:00:00Z"), Instant.parse("2026-11-16T15:00:00Z"), true, true, "Dia festivo", true, "Festivo X"),
                sesiones.getFirst());
        assertNull(sesiones.get(1).fechaHoraInicioUtc());
        assertNull(sesiones.get(1).fechaHoraFinUtc());
    }

    @Test
    void el_docente_usa_su_propia_consulta() {
        when(repositorio.consultarDocente(USUARIO, DESDE, HASTA)).thenReturn(List.of());

        assertTrue(docente.execute(new Comando(USUARIO, DESDE, HASTA)).isEmpty());
    }

    @Test
    void el_rango_invalido_de_la_db_se_propaga() {
        when(repositorio.consultarEstudiante(USUARIO, null, HASTA)).thenThrow(new ValidationException(CalendarioErrorCode.CAL_001));
        when(repositorio.consultarDocente(USUARIO, HASTA, DESDE)).thenThrow(new ValidationException(CalendarioErrorCode.CAL_001));

        assertEquals("CAL_001", assertThrows(ValidationException.class, () -> estudiante.execute(new Comando(USUARIO, null, HASTA))).getCode());
        assertEquals("CAL_001", assertThrows(ValidationException.class, () -> docente.execute(new Comando(USUARIO, HASTA, DESDE))).getCode());
    }

    @Test
    void los_constructores_y_el_comando_son_obligatorios() {
        assertThrows(NullPointerException.class, () -> new ConsultarCalendarioEstudianteUseCaseImpl(null));
        assertThrows(NullPointerException.class, () -> new ConsultarCalendarioDocenteUseCaseImpl(null));
        assertThrows(NullPointerException.class, () -> estudiante.execute(null));
        assertThrows(NullPointerException.class, () -> docente.execute(null));
    }
}
