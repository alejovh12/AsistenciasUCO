package co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarsolicitudesdocente.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarsolicitudes.primaryports.ConsultarSolicitudesSesionInputPort.Resultado;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarsolicitudes.primaryports.ConsultarSolicitudesSesionInputPort.Solicitud;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarsolicitudesdocente.primaryports.ConsultarSolicitudesSesionDocenteInputPort.Comando;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort.PaginaSolicitudesSesion;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort.SolicitudSesion;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** ATA D6: la lista del docente traduce los DATETIME2 en UTC a instantes, aplica la pagina por defecto y no usa la consulta del coordinador. */
class ConsultarSolicitudesSesionDocenteUseCaseImplTest {

    private static final UUID DOCENTE = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();

    private final SesionManualRepositoryPort repositorio = mock(SesionManualRepositoryPort.class);
    private final ConsultarSolicitudesSesionDocenteUseCaseImpl useCase = new ConsultarSolicitudesSesionDocenteUseCaseImpl(repositorio);

    @Test
    void traduce_las_filas_a_instantes_utc_y_conserva_el_total() {
        final UUID sesion = UUID.randomUUID();
        final UUID programa = UUID.randomUUID();
        when(repositorio.consultarSolicitudesDocente(DOCENTE, false, GRUPO, 2, 5)).thenReturn(new PaginaSolicitudesSesion(List.of(
                new SolicitudSesion(sesion, "Repaso", "S-9", GRUPO, "G01", "Calculo", programa, "Ingenieria", UUID.randomUUID(), "Maria Docente",
                        LocalDateTime.of(2026, 11, 10, 13, 0), LocalDateTime.of(2026, 11, 10, 15, 0), "RECHAZADA", "Rechazada",
                        LocalDateTime.of(2026, 11, 9, 14, 0), LocalDateTime.of(2026, 11, 9, 15, 0), "Ya existe"),
                new SolicitudSesion(UUID.randomUUID(), "Otra", "S-10", GRUPO, "G01", "Calculo", programa, "Ingenieria", UUID.randomUUID(),
                        "Maria Docente", null, null, "PENDIENTE", "Pendiente", null, null, null)), 7));

        final Resultado resultado = useCase.execute(new Comando(DOCENTE, false, GRUPO, 2, 5));

        assertEquals(7, resultado.total());
        assertEquals(2, resultado.pagina());
        assertEquals(5, resultado.tamano());
        final Solicitud primera = resultado.elementos().get(0);
        assertEquals(sesion, primera.idSesion());
        assertEquals(Instant.parse("2026-11-10T13:00:00Z"), primera.fechaHoraInicio());
        assertEquals(Instant.parse("2026-11-10T15:00:00Z"), primera.fechaHoraFin());
        assertEquals(Instant.parse("2026-11-09T14:00:00Z"), primera.fechaSolicitudUtc());
        assertEquals(Instant.parse("2026-11-09T15:00:00Z"), primera.fechaResolucionUtc());
        assertEquals("RECHAZADA", primera.estadoSolicitud());
        assertEquals("Ya existe", primera.observacion());
        assertNull(resultado.elementos().get(1).fechaHoraInicio());
        assertNull(resultado.elementos().get(1).fechaResolucionUtc());
        verify(repositorio, never()).consultarSolicitudes(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyBoolean(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void sin_pagina_ni_tamano_usa_1_y_20() {
        when(repositorio.consultarSolicitudesDocente(DOCENTE, true, null, 1, 20)).thenReturn(new PaginaSolicitudesSesion(List.of(), 0));

        assertEquals(new Resultado(List.of(), 0, 1, 20), useCase.execute(new Comando(DOCENTE, true, null, null, null)));
    }

    @Test
    void los_errores_de_rol_y_de_pagina_se_propagan() {
        when(repositorio.consultarSolicitudesDocente(DOCENTE, true, null, 1, 20))
                .thenThrow(new ForbiddenException("SEC_001", "Sin acceso al recurso solicitado."));
        when(repositorio.consultarSolicitudesDocente(DOCENTE, true, null, 0, 20))
                .thenThrow(new ValidationException(MensajeUsuarioErrorCode.PAG_001));

        assertThrows(ForbiddenException.class, () -> useCase.execute(new Comando(DOCENTE, true, null, null, null)));
        assertThrows(ValidationException.class, () -> useCase.execute(new Comando(DOCENTE, true, null, 0, null)));
    }

    @Test
    void el_constructor_y_el_comando_son_obligatorios() {
        assertThrows(NullPointerException.class, () -> new ConsultarSolicitudesSesionDocenteUseCaseImpl(null));
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }
}
