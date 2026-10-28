package co.edu.uco.asistenciasuco.application.features.estudiante.solicitaringresogrupo.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.estudiante.solicitaringresogrupo.primaryports.dto.SolicitarIngresoGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.solicitaringresogrupo.primaryports.interactor.SolicitarIngresoGrupoInteractor;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.SolicitudIngresoGrupoPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DestinatarioSolicitudIngresoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * HU053/HU055 (estudiante): solicitar el ingreso a un grupo, con aviso al docente titular (SI-01..SI-03). Las reglas de grupo, periodo, matricula y duplicados las valida el SP.
 */
class SolicitarIngresoGrupoUseCaseImplTest {

    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID USUARIO_DOCENTE = UUID.randomUUID();

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final SolicitudIngresoGrupoPort port = mock(SolicitudIngresoGrupoPort.class);
    private final NotificacionPort notificacionPort = mock(NotificacionPort.class);
    private SolicitarIngresoGrupoInteractor interactor;

    @BeforeEach
    void preparar() {
        interactor = new SolicitarIngresoGrupoInteractor(new SolicitarIngresoGrupoUseCaseImpl(scopePort, port, notificacionPort));
        when(scopePort.findEstudianteIdByUsuario(USUARIO)).thenReturn(Optional.of(UUID.randomUUID()));
    }

    @Test
    void SI01_genera_el_id_registra_y_avisa_al_docente() {
        when(port.consultarDestinatarios(any(), eq(USUARIO))).thenReturn(List.of(
                new DestinatarioSolicitudIngresoProjection("ESTUDIANTE", USUARIO, "Ana", "a@uco.edu.co", GRUPO, 1, "Grupo 1", "Calculo"),
                new DestinatarioSolicitudIngresoProjection("DOCENTE", USUARIO_DOCENTE, "Doc", "d@uco.edu.co", GRUPO, 1, "Grupo 1", "Calculo")));

        final UUID id = interactor.execute(new SolicitarIngresoGrupoDTO(USUARIO, GRUPO));

        assertNotNull(id);
        verify(port).registrarSolicitud(id, GRUPO, USUARIO);
        final ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionPort).notificar(captor.capture());
        assertEquals(Notificacion.TIPO_SOLICITUD_INGRESO_REGISTRADA, captor.getValue().tipo());
        assertEquals(List.of(USUARIO_DOCENTE),
                captor.getValue().destinatarios().stream().map(Notificacion.Destinatario::idUsuario).toList());
    }

    @Test
    void SI02_usuario_sin_estudiante_institucional_es_403() {
        final UUID docente = UUID.randomUUID();
        when(scopePort.findEstudianteIdByUsuario(docente)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> interactor.execute(new SolicitarIngresoGrupoDTO(docente, GRUPO)));
        verifyNoInteractions(port, notificacionPort);
    }

    @Test
    void SI03_actor_y_grupo_obligatorios() {
        assertThrows(ValidationException.class, () -> interactor.execute(new SolicitarIngresoGrupoDTO(null, GRUPO)));
        assertThrows(ValidationException.class, () -> interactor.execute(new SolicitarIngresoGrupoDTO(USUARIO, null)));
        assertThrows(ValidationException.class, () -> interactor.execute(null));
        verifyNoInteractions(port, notificacionPort);
    }
}
