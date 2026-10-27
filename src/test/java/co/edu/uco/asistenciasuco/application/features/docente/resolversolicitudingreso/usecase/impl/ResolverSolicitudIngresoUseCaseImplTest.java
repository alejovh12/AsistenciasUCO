package co.edu.uco.asistenciasuco.application.features.docente.resolversolicitudingreso.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.docente.resolversolicitudingreso.primaryports.dto.ResolverSolicitudIngresoDTO;
import co.edu.uco.asistenciasuco.application.features.docente.resolversolicitudingreso.primaryports.interactor.ResolverSolicitudIngresoInteractor;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * HU053/HU054/HU055: el docente titular acepta (con cupo matricula; sin cupo queda EN ESPERA) o rechaza con motivo una
 * solicitud de ingreso, y se avisa al estudiante (RS-01..RS-07). El cupo con bloqueo y la titularidad los valida el SP.
 */
class ResolverSolicitudIngresoUseCaseImplTest {

    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID SOLICITUD = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID USUARIO_ESTUDIANTE = UUID.randomUUID();

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final SolicitudIngresoGrupoPort port = mock(SolicitudIngresoGrupoPort.class);
    private final NotificacionPort notificacionPort = mock(NotificacionPort.class);
    private ResolverSolicitudIngresoInteractor interactor;

    @BeforeEach
    void preparar() {
        interactor = new ResolverSolicitudIngresoInteractor(
                new ResolverSolicitudIngresoUseCaseImpl(scopePort, port, notificacionPort));
        when(scopePort.findDocenteIdByUsuario(USUARIO)).thenReturn(Optional.of(UUID.randomUUID()));
        when(port.consultarDestinatarios(SOLICITUD, USUARIO)).thenReturn(List.of(
                new DestinatarioSolicitudIngresoProjection("ESTUDIANTE", USUARIO_ESTUDIANTE, "Ana Perez", "ana@uco.edu.co",
                        GRUPO, 1, "Grupo 1", "Calculo"),
                new DestinatarioSolicitudIngresoProjection("DOCENTE", USUARIO, "Doc", "doc@uco.edu.co",
                        GRUPO, 1, "Grupo 1", "Calculo")));
    }

    @Test
    void constructores_rechazan_dependencias_nulas() {
        assertThrows(NullPointerException.class, () -> new ResolverSolicitudIngresoUseCaseImpl(null, port, notificacionPort));
        assertThrows(NullPointerException.class, () -> new ResolverSolicitudIngresoUseCaseImpl(scopePort, null, notificacionPort));
        assertThrows(NullPointerException.class, () -> new ResolverSolicitudIngresoUseCaseImpl(scopePort, port, null));
        assertThrows(NullPointerException.class, () -> new ResolverSolicitudIngresoInteractor(null));
    }

    @Test
    void RS01_aceptar_con_cupo_devuelve_ACEPTADA_y_avisa_solo_al_estudiante() {
        when(port.resolverSolicitud(SOLICITUD, "ACEPTAR", null, USUARIO)).thenReturn("A");

        final var resultado = interactor.execute(new ResolverSolicitudIngresoDTO(USUARIO, SOLICITUD, " aceptar ", null));

        assertEquals("ACEPTADA", resultado.estadoResultante());
        final ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionPort).notificar(captor.capture());
        assertEquals(Notificacion.TIPO_SOLICITUD_INGRESO_RESUELTA, captor.getValue().tipo());
        assertEquals(List.of("ESTUDIANTE"), captor.getValue().destinatarios().stream().map(Notificacion.Destinatario::rol).toList());
        assertEquals("ACEPTADA", captor.getValue().datos().get("estado"));
        assertTrue(captor.getValue().mensaje().contains("Calculo"));
    }

    @Test
    void RS02_aceptar_sin_cupo_devuelve_EN_ESPERA() {
        when(port.resolverSolicitud(SOLICITUD, "ACEPTAR", null, USUARIO)).thenReturn("E");

        assertEquals("EN_ESPERA",
                interactor.execute(new ResolverSolicitudIngresoDTO(USUARIO, SOLICITUD, "ACEPTAR", null)).estadoResultante());
    }

    @Test
    void RS03_rechazar_exige_motivo_y_lo_incluye_en_el_aviso() {
        assertThrows(ValidationException.class,
                () -> interactor.execute(new ResolverSolicitudIngresoDTO(USUARIO, SOLICITUD, "RECHAZAR", "  ")));
        assertThrows(ValidationException.class,
                () -> interactor.execute(new ResolverSolicitudIngresoDTO(USUARIO, SOLICITUD, "RECHAZAR", "x".repeat(501))));
        when(port.resolverSolicitud(SOLICITUD, "RECHAZAR", "Cupo reservado", USUARIO)).thenReturn("R");

        final var resultado = interactor.execute(new ResolverSolicitudIngresoDTO(USUARIO, SOLICITUD, "RECHAZAR", " Cupo reservado "));

        assertEquals("RECHAZADA", resultado.estadoResultante());
        final ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionPort).notificar(captor.capture());
        assertTrue(captor.getValue().mensaje().contains("Cupo reservado"));
    }

    @Test
    void RS04_accion_invalida_actor_o_solicitud_nulos_son_400_sin_db() {
        assertThrows(ValidationException.class,
                () -> interactor.execute(new ResolverSolicitudIngresoDTO(USUARIO, SOLICITUD, "BORRAR", null)));
        assertThrows(ValidationException.class,
                () -> interactor.execute(new ResolverSolicitudIngresoDTO(null, SOLICITUD, "ACEPTAR", null)));
        assertThrows(ValidationException.class,
                () -> interactor.execute(new ResolverSolicitudIngresoDTO(USUARIO, null, "ACEPTAR", null)));
        verifyNoInteractions(port, notificacionPort);
    }

    @Test
    void RS05_usuario_no_docente_es_403() {
        final UUID otro = UUID.randomUUID();
        when(scopePort.findDocenteIdByUsuario(otro)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class,
                () -> interactor.execute(new ResolverSolicitudIngresoDTO(otro, SOLICITUD, "ACEPTAR", null)));
        verifyNoInteractions(port, notificacionPort);
    }

    @Test
    void RS06_si_la_db_rechaza_no_se_avisa() {
        doThrow(new IllegalStateException("SIG_013")).when(port).resolverSolicitud(SOLICITUD, "ACEPTAR", null, USUARIO);

        assertThrows(IllegalStateException.class,
                () -> interactor.execute(new ResolverSolicitudIngresoDTO(USUARIO, SOLICITUD, "ACEPTAR", null)));
        verifyNoInteractions(notificacionPort);
    }

    @Test
    void RS07_un_fallo_del_aviso_no_falla_la_resolucion() {
        when(port.resolverSolicitud(SOLICITUD, "ACEPTAR", null, USUARIO)).thenReturn("A");
        doThrow(new IllegalStateException("canal caido")).when(notificacionPort).notificar(any(Notificacion.class));

        assertDoesNotThrow(() -> interactor.execute(new ResolverSolicitudIngresoDTO(USUARIO, SOLICITUD, "ACEPTAR", null)));
    }
}
