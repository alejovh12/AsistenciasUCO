package co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarsolicitudes.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarsolicitudes.primaryports.ConsultarSolicitudesSesionInputPort.Comando;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarsolicitudes.primaryports.ConsultarSolicitudesSesionInputPort.Resultado;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarsolicitudes.primaryports.ConsultarSolicitudesSesionInputPort.Solicitud;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort.PaginaSolicitudesSesion;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort.SolicitudSesion;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** DEC-065 (2): la bandeja traduce los DATETIME2 en UTC a instantes y aplica la pagina por defecto. */
class ConsultarSolicitudesSesionUseCaseImplTest {

    private static final UUID COORDINADOR = UUID.randomUUID();
    private static final UUID PROGRAMA = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();

    private final SesionManualRepositoryPort repositorio = mock(SesionManualRepositoryPort.class);
    private final ConsultarSolicitudesSesionUseCaseImpl useCase = new ConsultarSolicitudesSesionUseCaseImpl(repositorio);

    @Test
    void traduce_las_filas_a_instantes_utc_y_conserva_el_total() {
        final UUID sesion = UUID.randomUUID();
        final UUID docente = UUID.randomUUID();
        when(repositorio.consultarSolicitudes(COORDINADOR, false, PROGRAMA, GRUPO, 2, 5)).thenReturn(new PaginaSolicitudesSesion(List.of(
                new SolicitudSesion(sesion, "Repaso", "S-9", GRUPO, "G01", "Calculo", PROGRAMA, "Ingenieria", docente, "Maria Docente",
                        LocalDateTime.of(2026, 11, 10, 13, 0), LocalDateTime.of(2026, 11, 10, 15, 0), "APROBADA", "Aprobada",
                        LocalDateTime.of(2026, 11, 9, 14, 0), LocalDateTime.of(2026, 11, 9, 15, 0), "Adelante"),
                new SolicitudSesion(UUID.randomUUID(), "Otra", "S-10", GRUPO, "G01", "Calculo", PROGRAMA, "Ingenieria", docente, "Maria Docente",
                        null, null, "PENDIENTE", "Pendiente", null, null, null)), 7));

        final Resultado resultado = useCase.execute(new Comando(COORDINADOR, false, PROGRAMA, GRUPO, 2, 5));

        assertEquals(7, resultado.total());
        assertEquals(2, resultado.pagina());
        assertEquals(5, resultado.tamano());
        final Solicitud primera = resultado.elementos().get(0);
        assertEquals(sesion, primera.idSesion());
        assertEquals(Instant.parse("2026-11-10T13:00:00Z"), primera.fechaHoraInicio());
        assertEquals(Instant.parse("2026-11-10T15:00:00Z"), primera.fechaHoraFin());
        assertEquals(Instant.parse("2026-11-09T14:00:00Z"), primera.fechaSolicitudUtc());
        assertEquals(Instant.parse("2026-11-09T15:00:00Z"), primera.fechaResolucionUtc());
        assertEquals("Adelante", primera.observacion());
        assertEquals("APROBADA", primera.estadoSolicitud());
        final Solicitud segunda = resultado.elementos().get(1);
        assertEquals(null, segunda.fechaHoraInicio());
        assertEquals(null, segunda.fechaResolucionUtc());
    }

    @Test
    void sin_pagina_ni_tamano_usa_1_y_20() {
        when(repositorio.consultarSolicitudes(COORDINADOR, true, null, null, 1, 20)).thenReturn(new PaginaSolicitudesSesion(List.of(), 0));

        final Resultado resultado = useCase.execute(new Comando(COORDINADOR, true, null, null, null, null));

        assertEquals(new Resultado(List.of(), 0, 1, 20), resultado);
    }

    @Test
    void los_errores_del_ambito_se_propagan() {
        when(repositorio.consultarSolicitudes(COORDINADOR, true, null, null, 1, 20))
                .thenThrow(new ForbiddenException("SEC_001", "Sin acceso al recurso solicitado."));

        assertThrows(ForbiddenException.class, () -> useCase.execute(new Comando(COORDINADOR, true, null, null, null, null)));
    }

    @Test
    void el_constructor_y_el_comando_son_obligatorios() {
        assertThrows(NullPointerException.class, () -> new ConsultarSolicitudesSesionUseCaseImpl(null));
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }
}
