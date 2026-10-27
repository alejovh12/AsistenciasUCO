package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsolicitudesingreso.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsolicitudesingreso.primaryports.dto.ConsultarSolicitudesIngresoEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsolicitudesingreso.primaryports.interactor.ConsultarSolicitudesIngresoEstudianteInteractor;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.SolicitudIngresoGrupoPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SolicitudIngresoEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** HU053/HU055 (estudiante): consulta de las solicitudes de ingreso propias (CE-01..CE-03). */
class ConsultarSolicitudesIngresoEstudianteUseCaseImplTest {

    private static final UUID USUARIO = UUID.randomUUID();

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final SolicitudIngresoGrupoPort port = mock(SolicitudIngresoGrupoPort.class);
    private ConsultarSolicitudesIngresoEstudianteInteractor interactor;

    @BeforeEach
    void preparar() {
        interactor = new ConsultarSolicitudesIngresoEstudianteInteractor(new ConsultarSolicitudesIngresoEstudianteUseCaseImpl(scopePort, port));
        when(scopePort.findEstudianteIdByUsuario(USUARIO)).thenReturn(Optional.of(UUID.randomUUID()));
    }

    @Test
    void CE01_mapea_estado_legible_y_motivo() {
        when(port.consultarSolicitudesEstudiante("R", 1, 10, USUARIO)).thenReturn(List.of(
                new SolicitudIngresoEstudianteProjection(UUID.randomUUID(), UUID.randomUUID(), 1, "Grupo 1", UUID.randomUUID(),
                        "Calculo", "Doc Uno", "R", "rechazada", Instant.parse("2026-10-27T13:00:00Z"),
                        Instant.parse("2026-10-27T14:00:00Z"), "Cupo reservado", 1)));

        final var pagina = interactor.execute(new ConsultarSolicitudesIngresoEstudianteDTO(USUARIO, "RECHAZADA", null, null));

        assertEquals(1, pagina.totalRegistros());
        assertEquals("RECHAZADA", pagina.solicitudes().getFirst().estado());
        assertEquals("Cupo reservado", pagina.solicitudes().getFirst().motivoRechazo());
        assertEquals("Calculo", pagina.solicitudes().getFirst().asignaturaNombre());
    }

    @Test
    void CE02_retirada_se_filtra_por_nombre_y_desconocido_es_400() {
        when(port.consultarSolicitudesEstudiante("X", 2, 5, USUARIO)).thenReturn(List.of());
        assertEquals(0, interactor.execute(new ConsultarSolicitudesIngresoEstudianteDTO(USUARIO, "retirada", 2, 5)).totalRegistros());
        assertThrows(ValidationException.class,
                () -> interactor.execute(new ConsultarSolicitudesIngresoEstudianteDTO(USUARIO, "OTRA", 1, 10)));
    }

    @Test
    void CE03_usuario_sin_estudiante_es_403() {
        final UUID otro = UUID.randomUUID();
        when(scopePort.findEstudianteIdByUsuario(otro)).thenReturn(Optional.empty());
        assertThrows(ForbiddenException.class,
                () -> interactor.execute(new ConsultarSolicitudesIngresoEstudianteDTO(otro, null, 1, 10)));
        verifyNoInteractions(port);
    }
}
