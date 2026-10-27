package co.edu.uco.asistenciasuco.application.features.docente.consultarsolicitudesingreso.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.docente.consultarsolicitudesingreso.primaryports.dto.ConsultarSolicitudesIngresoDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarsolicitudesingreso.primaryports.interactor.ConsultarSolicitudesIngresoDocenteInteractor;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.SolicitudIngresoGrupoPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SolicitudIngresoDocenteProjection;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** HU053/HU055 (docente): bandeja de solicitudes de un grupo con cupo y lista de espera (CD-01..CD-04). */
class ConsultarSolicitudesIngresoDocenteUseCaseImplTest {

    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final SolicitudIngresoGrupoPort port = mock(SolicitudIngresoGrupoPort.class);
    private ConsultarSolicitudesIngresoDocenteInteractor interactor;

    @BeforeEach
    void preparar() {
        interactor = new ConsultarSolicitudesIngresoDocenteInteractor(new ConsultarSolicitudesIngresoDocenteUseCaseImpl(scopePort, port));
        when(scopePort.findDocenteIdByUsuario(USUARIO)).thenReturn(Optional.of(UUID.randomUUID()));
    }

    @Test
    void CD01_mapea_estado_legible_posicion_y_cupo() {
        final UUID solicitud = UUID.randomUUID();
        when(port.consultarSolicitudesGrupo(GRUPO, "E", 1, 10, USUARIO)).thenReturn(List.of(
                new SolicitudIngresoDocenteProjection(solicitud, UUID.randomUUID(), "Beto Qa", 970005302, "E", "en espera",
                        Instant.parse("2026-10-27T13:00:00Z"), null, null, 1, 30, 0, 4)));

        final var pagina = interactor.execute(new ConsultarSolicitudesIngresoDocenteDTO(USUARIO, GRUPO, "en_espera", null, null));

        assertEquals(4, pagina.totalRegistros());
        final var fila = pagina.solicitudes().getFirst();
        assertEquals(solicitud, fila.id());
        assertEquals("EN_ESPERA", fila.estado());
        assertEquals(1, fila.posicionEspera());
        assertEquals(0, fila.cuposDisponibles());
        assertEquals(30, fila.capacidadGrupo());
    }

    @Test
    void CD02_estado_por_codigo_y_paginacion_acotada() {
        when(port.consultarSolicitudesGrupo(GRUPO, "P", 1, 100, USUARIO)).thenReturn(List.of());
        assertEquals(0, interactor.execute(new ConsultarSolicitudesIngresoDocenteDTO(USUARIO, GRUPO, "p", 0, 500)).totalRegistros());
        verify(port).consultarSolicitudesGrupo(GRUPO, "P", 1, 100, USUARIO);
    }

    @Test
    void CD03_estado_desconocido_o_ids_nulos_son_400() {
        assertThrows(ValidationException.class,
                () -> interactor.execute(new ConsultarSolicitudesIngresoDocenteDTO(USUARIO, GRUPO, "VENCIDA", 1, 10)));
        assertThrows(ValidationException.class,
                () -> interactor.execute(new ConsultarSolicitudesIngresoDocenteDTO(USUARIO, null, null, 1, 10)));
        verifyNoInteractions(port);
    }

    @Test
    void CD04_usuario_no_docente_es_403() {
        final UUID otro = UUID.randomUUID();
        when(scopePort.findDocenteIdByUsuario(otro)).thenReturn(Optional.empty());
        assertThrows(ForbiddenException.class,
                () -> interactor.execute(new ConsultarSolicitudesIngresoDocenteDTO(otro, GRUPO, null, 1, 10)));
        verifyNoInteractions(port);
    }
}
