package co.edu.uco.asistenciasuco.application.features.estudiante.retirarsolicitudingreso.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.estudiante.retirarsolicitudingreso.primaryports.dto.RetirarSolicitudIngresoDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.retirarsolicitudingreso.primaryports.interactor.RetirarSolicitudIngresoInteractor;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** HU053/HU055 (estudiante): retirar la solicitud propia abierta, con aviso al docente titular (RW-01..RW-03). */
class RetirarSolicitudIngresoUseCaseImplTest {

    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID SOLICITUD = UUID.randomUUID();
    private static final UUID USUARIO_DOCENTE = UUID.randomUUID();

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final SolicitudIngresoGrupoPort port = mock(SolicitudIngresoGrupoPort.class);
    private final NotificacionPort notificacionPort = mock(NotificacionPort.class);
    private RetirarSolicitudIngresoInteractor interactor;

    @BeforeEach
    void preparar() {
        interactor = new RetirarSolicitudIngresoInteractor(new RetirarSolicitudIngresoUseCaseImpl(scopePort, port, notificacionPort));
        when(scopePort.findEstudianteIdByUsuario(USUARIO)).thenReturn(Optional.of(UUID.randomUUID()));
    }

    @Test
    void RW01_retira_y_avisa_al_docente() {
        when(port.consultarDestinatarios(SOLICITUD, USUARIO)).thenReturn(List.of(
                new DestinatarioSolicitudIngresoProjection("ESTUDIANTE", USUARIO, "Ana", "a@uco.edu.co", UUID.randomUUID(), 1, "Grupo 1", "Calculo"),
                new DestinatarioSolicitudIngresoProjection("DOCENTE", USUARIO_DOCENTE, "Doc", "d@uco.edu.co", UUID.randomUUID(), 1, "Grupo 1", "Calculo")));

        interactor.execute(new RetirarSolicitudIngresoDTO(USUARIO, SOLICITUD));

        verify(port).retirarSolicitud(SOLICITUD, USUARIO);
        final ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionPort).notificar(captor.capture());
        assertEquals(Notificacion.TIPO_SOLICITUD_INGRESO_RETIRADA, captor.getValue().tipo());
        assertEquals(List.of(USUARIO_DOCENTE),
                captor.getValue().destinatarios().stream().map(Notificacion.Destinatario::idUsuario).toList());
    }

    @Test
    void RW02_ids_obligatorios() {
        assertThrows(ValidationException.class, () -> interactor.execute(new RetirarSolicitudIngresoDTO(USUARIO, null)));
        assertThrows(ValidationException.class, () -> interactor.execute(new RetirarSolicitudIngresoDTO(null, SOLICITUD)));
        verifyNoInteractions(port, notificacionPort);
    }

    @Test
    void RW03_usuario_sin_estudiante_es_403() {
        final UUID docente = UUID.randomUUID();
        when(scopePort.findEstudianteIdByUsuario(docente)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> interactor.execute(new RetirarSolicitudIngresoDTO(docente, SOLICITUD)));
        verifyNoInteractions(port, notificacionPort);
    }
}
