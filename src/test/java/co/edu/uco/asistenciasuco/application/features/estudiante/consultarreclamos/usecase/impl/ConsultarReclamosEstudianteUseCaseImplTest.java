package co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.primaryports.dto.ConsultarReclamosEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.primaryports.dto.ReclamoEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarreclamos.primaryports.interactor.ConsultarReclamosEstudianteInteractor;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.SolicitudRevisionEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SolicitudRevisionEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * HU010/HU011 (consulta): el estudiante ve SOLO sus solicitudes de revision. Behavioral matrix en el TEST_PLAN
 * (UC-01..UC-07): rol sin estudiante institucional, normalizacion de filtros y mapeo del soporte.
 */
class ConsultarReclamosEstudianteUseCaseImplTest {

    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final UUID SOLICITUD = UUID.randomUUID();
    private static final UUID ARCHIVO = UUID.randomUUID();
    private static final Instant SESION_UTC = Instant.parse("2026-10-19T15:30:45Z");
    private static final Instant SOPORTE_UTC = Instant.parse("2026-10-20T10:00:00Z");

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final SolicitudRevisionEstudianteQueryPort queryPort = mock(SolicitudRevisionEstudianteQueryPort.class);
    private ConsultarReclamosEstudianteInteractor interactor;

    @BeforeEach
    void preparar() {
        interactor = new ConsultarReclamosEstudianteInteractor(new ConsultarReclamosEstudianteUseCaseImpl(scopePort, queryPort));
        when(scopePort.findEstudianteIdByUsuario(USUARIO)).thenReturn(Optional.of(ESTUDIANTE));
    }

    @Test
    void constructores_rechazan_dependencias_nulas() {
        assertThrows(NullPointerException.class, () -> new ConsultarReclamosEstudianteUseCaseImpl(null, queryPort));
        assertThrows(NullPointerException.class, () -> new ConsultarReclamosEstudianteUseCaseImpl(scopePort, null));
        assertThrows(NullPointerException.class, () -> new ConsultarReclamosEstudianteInteractor(null));
    }

    @Test
    void UC01_usuario_sin_estudiante_institucional_es_403_y_no_consulta() {
        final UUID docente = UUID.randomUUID();
        when(scopePort.findEstudianteIdByUsuario(docente)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class,
                () -> interactor.execute(new ConsultarReclamosEstudianteDTO(docente, null, null, null)));
        verifyNoInteractions(queryPort);
    }

    @Test
    void UC02_usuario_autenticado_obligatorio() {
        assertThrows(ValidationException.class,
                () -> interactor.execute(new ConsultarReclamosEstudianteDTO(null, null, null, null)));
        verifyNoInteractions(queryPort);
    }

    @Test
    void UC03_valores_por_defecto_y_total_de_la_primera_fila() {
        when(queryPort.consultarSolicitudesEstudiante(USUARIO, null, 1, 10)).thenReturn(List.of(fila(SOLICITUD, ARCHIVO, 7)));

        final var pagina = interactor.execute(new ConsultarReclamosEstudianteDTO(USUARIO, null, null, null));

        assertEquals(7, pagina.totalRegistros());
        assertEquals(1, pagina.reclamos().size());
    }

    @Test
    void UC04_estado_por_nombre_o_codigo_y_paginacion_acotada() {
        when(queryPort.consultarSolicitudesEstudiante(any(), any(), anyInt(), anyInt())).thenReturn(List.of());

        interactor.execute(new ConsultarReclamosEstudianteDTO(USUARIO, " aprobada ", 0, 500));
        verify(queryPort).consultarSolicitudesEstudiante(USUARIO, "A", 1, 100);

        interactor.execute(new ConsultarReclamosEstudianteDTO(USUARIO, "r", 3, 0));
        verify(queryPort).consultarSolicitudesEstudiante(USUARIO, "R", 3, 10);
    }

    @Test
    void UC05_estado_desconocido_es_400() {
        assertThrows(ValidationException.class,
                () -> interactor.execute(new ConsultarReclamosEstudianteDTO(USUARIO, "BORRADA", 1, 10)));
        verifyNoInteractions(queryPort);
    }

    @Test
    void UC06_mapea_estado_legible_y_soporte_con_url_autenticada() {
        when(queryPort.consultarSolicitudesEstudiante(USUARIO, null, 1, 10)).thenReturn(List.of(fila(SOLICITUD, ARCHIVO, 1)));

        final ReclamoEstudianteDTO reclamo = interactor.execute(
                new ConsultarReclamosEstudianteDTO(USUARIO, null, null, null)).reclamos().getFirst();

        assertEquals(SOLICITUD, reclamo.id());
        assertEquals("PENDIENTE", reclamo.estadoSolicitud());
        assertEquals(SESION_UTC, reclamo.fechaSesionUtc());
        assertEquals("Calculo", reclamo.materiaNombre());
        assertEquals(ARCHIVO, reclamo.soporteAdjunto().fileId());
        assertEquals("/api/v1/archivos/" + ARCHIVO, reclamo.soporteAdjunto().url());
        assertEquals(SOPORTE_UTC, reclamo.soporteAdjunto().fechaSubidaUtc());
        assertEquals(2048L, reclamo.soporteAdjunto().tamanioBytes());
    }

    @Test
    void UC07_sin_soporte_y_sin_filas() {
        when(queryPort.consultarSolicitudesEstudiante(USUARIO, null, 1, 10)).thenReturn(List.of(fila(SOLICITUD, null, 1)));
        assertNull(interactor.execute(new ConsultarReclamosEstudianteDTO(USUARIO, null, null, null))
                .reclamos().getFirst().soporteAdjunto());

        when(queryPort.consultarSolicitudesEstudiante(USUARIO, "P", 1, 10)).thenReturn(List.of());
        final var vacia = interactor.execute(new ConsultarReclamosEstudianteDTO(USUARIO, "P", null, null));
        assertEquals(0, vacia.totalRegistros());
        assertTrue(vacia.reclamos().isEmpty());
    }

    private static SolicitudRevisionEstudianteProjection fila(final UUID id, final UUID archivo, final int total) {
        return new SolicitudRevisionEstudianteProjection(id, "RAD-1", LocalDate.of(2026, 10, 20), "P", "Pendiente",
                "Cita medica", "", UUID.randomUUID(), 3, SESION_UTC, "UTC_V2", UUID.randomUUID(), 1, "Grupo 1",
                UUID.randomUUID(), "MAT-1", "Calculo", archivo, archivo == null ? null : "incapacidad.pdf",
                archivo == null ? null : "application/pdf", archivo == null ? null : 2048L,
                archivo == null ? null : SOPORTE_UTC, total);
    }
}
