package co.edu.uco.asistenciasuco.application.features.docente.retirarestudiantegrupo.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.docente.retirarestudiantegrupo.primaryports.dto.RetirarEstudianteGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.docente.retirarestudiantegrupo.primaryports.interactor.RetirarEstudianteGrupoInteractor;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.RetiroEstudianteGrupoPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DestinatarioRetiroProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * HU051: el DOCENTE titular retira a un estudiante de su grupo con motivo obligatorio y se avisa al estudiante, al
 * docente y al coordinador del programa (UC-01..UC-08 del TEST_PLAN). La titularidad, el periodo y el cupo los
 * revalida el SP; Application exige un docente institucional y un motivo valido antes de tocar la DB.
 */
class RetirarEstudianteGrupoUseCaseImplTest {

    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID DOCENTE = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final RetiroEstudianteGrupoPort retiroPort = mock(RetiroEstudianteGrupoPort.class);
    private final NotificacionPort notificacionPort = mock(NotificacionPort.class);
    private RetirarEstudianteGrupoInteractor interactor;

    @BeforeEach
    void preparar() {
        interactor = new RetirarEstudianteGrupoInteractor(
                new RetirarEstudianteGrupoUseCaseImpl(scopePort, retiroPort, notificacionPort));
        when(scopePort.findDocenteIdByUsuario(USUARIO)).thenReturn(Optional.of(DOCENTE));
        when(retiroPort.consultarDestinatarios(GRUPO, ESTUDIANTE, USUARIO)).thenReturn(List.of(
                destinatario("ESTUDIANTE"), destinatario("DOCENTE"), destinatario("COORDINADOR")));
    }

    @Test
    void constructores_rechazan_dependencias_nulas() {
        assertThrows(NullPointerException.class, () -> new RetirarEstudianteGrupoUseCaseImpl(null, retiroPort, notificacionPort));
        assertThrows(NullPointerException.class, () -> new RetirarEstudianteGrupoUseCaseImpl(scopePort, null, notificacionPort));
        assertThrows(NullPointerException.class, () -> new RetirarEstudianteGrupoUseCaseImpl(scopePort, retiroPort, null));
        assertThrows(NullPointerException.class, () -> new RetirarEstudianteGrupoInteractor(null));
    }

    @Test
    void UC01_retira_con_motivo_normalizado_y_luego_notifica() {
        interactor.execute(new RetirarEstudianteGrupoDTO(USUARIO, GRUPO, ESTUDIANTE, "  Inasistencia reiterada  "));

        final InOrder orden = inOrder(retiroPort, notificacionPort);
        orden.verify(retiroPort).retirarEstudiante(GRUPO, ESTUDIANTE, "Inasistencia reiterada", USUARIO);
        orden.verify(retiroPort).consultarDestinatarios(GRUPO, ESTUDIANTE, USUARIO);
        orden.verify(notificacionPort).notificar(any(Notificacion.class));
    }

    @Test
    void UC02_aviso_a_estudiante_docente_y_coordinador_con_el_motivo() {
        interactor.execute(new RetirarEstudianteGrupoDTO(USUARIO, GRUPO, ESTUDIANTE, "Inasistencia reiterada"));

        final ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionPort).notificar(captor.capture());
        final Notificacion aviso = captor.getValue();
        assertEquals(Notificacion.TIPO_RETIRO_ESTUDIANTE_GRUPO, aviso.tipo());
        assertEquals(List.of("ESTUDIANTE", "DOCENTE", "COORDINADOR"),
                aviso.destinatarios().stream().map(Notificacion.Destinatario::rol).toList());
        assertTrue(aviso.mensaje().contains("Inasistencia reiterada"), "El aviso incluye el motivo");
        assertTrue(aviso.mensaje().contains("Calculo") && aviso.mensaje().contains("Grupo 1"), "El aviso identifica el grupo");
        assertEquals("Inasistencia reiterada", aviso.datos().get("motivo"));
        assertEquals(GRUPO.toString(), aviso.datos().get("idGrupo"));
        assertEquals(ESTUDIANTE.toString(), aviso.datos().get("idEstudiante"));
    }

    @Test
    void UC03_usuario_sin_docente_institucional_es_403_sin_tocar_la_db() {
        final UUID estudiante = UUID.randomUUID();
        when(scopePort.findDocenteIdByUsuario(estudiante)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class,
                () -> interactor.execute(new RetirarEstudianteGrupoDTO(estudiante, GRUPO, ESTUDIANTE, "Motivo")));
        verifyNoInteractions(retiroPort, notificacionPort);
    }

    @Test
    void UC04_motivo_obligatorio_y_maximo_500() {
        for (final String motivo : new String[]{null, "", "   ", "x".repeat(501)}) {
            assertThrows(ValidationException.class,
                    () -> interactor.execute(new RetirarEstudianteGrupoDTO(USUARIO, GRUPO, ESTUDIANTE, motivo)));
        }
        assertDoesNotThrow(() -> interactor.execute(new RetirarEstudianteGrupoDTO(USUARIO, GRUPO, ESTUDIANTE, "x".repeat(500))));
        verify(retiroPort).retirarEstudiante(GRUPO, ESTUDIANTE, "x".repeat(500), USUARIO);
    }

    @Test
    void UC05_actor_grupo_y_estudiante_obligatorios() {
        assertThrows(ValidationException.class,
                () -> interactor.execute(new RetirarEstudianteGrupoDTO(null, GRUPO, ESTUDIANTE, "Motivo")));
        assertThrows(ValidationException.class,
                () -> interactor.execute(new RetirarEstudianteGrupoDTO(USUARIO, null, ESTUDIANTE, "Motivo")));
        assertThrows(ValidationException.class,
                () -> interactor.execute(new RetirarEstudianteGrupoDTO(USUARIO, GRUPO, null, "Motivo")));
        assertThrows(ValidationException.class, () -> interactor.execute(null));
        verifyNoInteractions(retiroPort, notificacionPort);
    }

    @Test
    void UC06_si_la_db_rechaza_el_retiro_no_se_notifica() {
        doThrow(new IllegalStateException("RET_003")).when(retiroPort)
                .retirarEstudiante(GRUPO, ESTUDIANTE, "Motivo", USUARIO);

        assertThrows(IllegalStateException.class,
                () -> interactor.execute(new RetirarEstudianteGrupoDTO(USUARIO, GRUPO, ESTUDIANTE, "Motivo")));
        verifyNoInteractions(notificacionPort);
    }

    @Test
    void UC07_un_fallo_del_aviso_no_revierte_ni_falla_el_retiro() {
        doThrow(new IllegalStateException("canal caido")).when(notificacionPort).notificar(any(Notificacion.class));

        assertDoesNotThrow(() -> interactor.execute(new RetirarEstudianteGrupoDTO(USUARIO, GRUPO, ESTUDIANTE, "Motivo")));
        verify(retiroPort).retirarEstudiante(GRUPO, ESTUDIANTE, "Motivo", USUARIO);
    }

    @Test
    void UC08_sin_destinatarios_no_se_envia_aviso() {
        when(retiroPort.consultarDestinatarios(GRUPO, ESTUDIANTE, USUARIO)).thenReturn(List.of());

        interactor.execute(new RetirarEstudianteGrupoDTO(USUARIO, GRUPO, ESTUDIANTE, "Motivo"));

        verify(retiroPort).retirarEstudiante(GRUPO, ESTUDIANTE, "Motivo", USUARIO);
        verifyNoInteractions(notificacionPort);
    }

    private static DestinatarioRetiroProjection destinatario(final String rol) {
        return new DestinatarioRetiroProjection(rol, UUID.randomUUID(), "Nombre " + rol, rol.toLowerCase() + "@uco.edu.co",
                GRUPO, 1, "Grupo 1", "Calculo");
    }
}
