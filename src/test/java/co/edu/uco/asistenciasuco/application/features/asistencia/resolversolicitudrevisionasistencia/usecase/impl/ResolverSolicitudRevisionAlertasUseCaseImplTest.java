package co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.EvaluadorAlertasInasistencia;
import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.usecase.domain.ResolverSolicitudRevisionAsistenciaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DestinatarioReclamoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DestinatariosReclamoPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** CULM-001 C.1 (SUPUESTO-C-04): solo la aprobacion de un reclamo reevalua las alertas del grupo, y un fallo de la evaluacion no deshace la resolucion. */
class ResolverSolicitudRevisionAlertasUseCaseImplTest {

    private static final UUID SOLICITUD = UUID.randomUUID();
    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID DOCENTE = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();

    private final AsistenciaRepositoryPort repository = mock(AsistenciaRepositoryPort.class);
    private final InstitutionalScopePort scope = mock(InstitutionalScopePort.class);
    private final NotificacionPort notificaciones = mock(NotificacionPort.class);
    private final DestinatariosReclamoPort destinatarios = mock(DestinatariosReclamoPort.class);
    private final EvaluadorAlertasInasistencia evaluador = mock(EvaluadorAlertasInasistencia.class);
    private final ResolverSolicitudRevisionAsistenciaUseCaseImpl useCase =
            new ResolverSolicitudRevisionAsistenciaUseCaseImpl(repository, scope, notificaciones, destinatarios, evaluador);

    private static DestinatarioReclamoProjection destinatario(final UUID grupo) {
        return new DestinatarioReclamoProjection("ESTUDIANTE", UUID.randomUUID(), "Est Uno", "est@uco.edu.co", grupo, "G01", "2026-11-08");
    }

    private void resolver(final String accion) {
        when(scope.findDocenteIdByUsuario(USUARIO)).thenReturn(Optional.of(DOCENTE));
        useCase.execute(new ResolverSolicitudRevisionAsistenciaDomain(SOLICITUD, accion, "Respuesta del docente.", USUARIO));
    }

    @Test
    void aprobar_reevalua_las_alertas_del_grupo_del_primer_destinatario() {
        when(destinatarios.consultarDestinatariosPorSolicitud(SOLICITUD)).thenReturn(List.of(destinatario(GRUPO), destinatario(UUID.randomUUID())));

        resolver("APROBADA");

        verify(evaluador).evaluarYAvisar(GRUPO, null);
    }

    @Test
    void rechazar_no_reevalua_porque_el_porcentaje_no_cambia() {
        when(destinatarios.consultarDestinatariosPorSolicitud(SOLICITUD)).thenReturn(List.of(destinatario(GRUPO)));

        resolver("RECHAZADA");

        verify(evaluador, never()).evaluarYAvisar(any(), any());
    }

    @Test
    void aprobar_sin_destinatarios_o_sin_grupo_no_reevalua() {
        when(destinatarios.consultarDestinatariosPorSolicitud(SOLICITUD)).thenReturn(List.of());
        resolver("APROBADA");
        when(destinatarios.consultarDestinatariosPorSolicitud(SOLICITUD)).thenReturn(null);
        resolver("APROBADA");
        when(destinatarios.consultarDestinatariosPorSolicitud(SOLICITUD)).thenReturn(List.of(destinatario(null)));
        resolver("APROBADA");

        verify(evaluador, never()).evaluarYAvisar(any(), any());
    }

    @Test
    void si_la_evaluacion_falla_la_resolucion_ya_confirmada_no_se_deshace() {
        when(destinatarios.consultarDestinatariosPorSolicitud(SOLICITUD)).thenReturn(List.of(destinatario(GRUPO)));
        org.mockito.Mockito.doThrow(new IllegalStateException("evaluador caido")).when(evaluador).evaluarYAvisar(eq(GRUPO), any());

        resolver("APROBADA");

        verify(repository).resolverSolicitudRevisionAsistencia(any());
        verify(evaluador).evaluarYAvisar(GRUPO, null);
    }

    @Test
    void sin_evaluador_o_sin_puerto_de_destinatarios_solo_se_resuelve() {
        final ResolverSolicitudRevisionAsistenciaUseCaseImpl sinEvaluador =
                new ResolverSolicitudRevisionAsistenciaUseCaseImpl(repository, scope, notificaciones, destinatarios);
        when(scope.findDocenteIdByUsuario(USUARIO)).thenReturn(Optional.of(DOCENTE));
        sinEvaluador.execute(new ResolverSolicitudRevisionAsistenciaDomain(SOLICITUD, "APROBADA", "ok", USUARIO));
        final ResolverSolicitudRevisionAsistenciaUseCaseImpl sinDestinatarios =
                new ResolverSolicitudRevisionAsistenciaUseCaseImpl(repository, scope, notificaciones, null, evaluador);
        sinDestinatarios.execute(new ResolverSolicitudRevisionAsistenciaDomain(SOLICITUD, "APROBADA", "ok", USUARIO));

        verify(evaluador, never()).evaluarYAvisar(any(), any());
        verify(repository, org.mockito.Mockito.times(2)).resolverSolicitudRevisionAsistencia(any());
    }
}
