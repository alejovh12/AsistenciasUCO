package co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.domain.RegistrarAsistenciasSesionDomain;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.domain.RegistroAsistenciaSesionDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciasSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RegistrarAsistenciasSesionUseCaseImplTest {

    private static final UUID SESION = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final UUID USUARIO_EJECUTOR = UUID.randomUUID();

    private final AsistenciaRepositoryPort asistenciaRepositoryPort = mock(AsistenciaRepositoryPort.class);
    private final SesionRepositoryPort sesionRepositoryPort = mock(SesionRepositoryPort.class);
    private final InstitutionalScopePort institutionalScopePort = mock(InstitutionalScopePort.class);
    private final RealtimePublisherPort realtimePublisherPort = mock(RealtimePublisherPort.class);
    private final RegistrarAsistenciasSesionUseCaseImpl useCase = new RegistrarAsistenciasSesionUseCaseImpl(
            asistenciaRepositoryPort, sesionRepositoryPort, institutionalScopePort, realtimePublisherPort);

    @Test
    void ejecucion_exitosa_persiste_y_luego_publica_en_ese_orden_exacto() {
        darTitularidadAlDocente();

        useCase.execute(dominioValido());

        final InOrder orden = inOrder(asistenciaRepositoryPort, realtimePublisherPort);
        orden.verify(asistenciaRepositoryPort).registrarAsistenciasSesion(any(RegistrarAsistenciasSesionRepositoryDTO.class));
        orden.verify(realtimePublisherPort).publish(any(RealtimeEvent.class));
    }

    @Test
    void evento_publicado_tiene_el_type_y_payload_canonicos() {
        darTitularidadAlDocente();

        useCase.execute(dominioValido());

        final ArgumentCaptor<RealtimeEvent> captor = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(realtimePublisherPort).publish(captor.capture());
        final RealtimeEvent event = captor.getValue();
        assertEquals("ASISTENCIAS_SESION_ACTUALIZADAS", event.type());
        assertEquals(GRUPO.toString(), event.payload().get("grupo"));
        assertEquals(SESION.toString(), event.payload().get("sesion"));
        assertEquals(1, event.payload().get("totalRegistros"));
    }

    @Test
    void si_la_persistencia_falla_no_se_publica_evento_alguno() {
        darTitularidadAlDocente();
        doThrow(new CrosscuttingException("fallo de persistencia"))
                .when(asistenciaRepositoryPort).registrarAsistenciasSesion(any());

        assertThrows(CrosscuttingException.class, () -> useCase.execute(dominioValido()));

        verify(realtimePublisherPort, never()).publish(any());
    }

    @Test
    void docente_ajeno_al_grupo_de_la_sesion_es_rechazado_sin_persistir_ni_publicar() {
        when(sesionRepositoryPort.consultarSesion(any(ConsultarSesionRepositoryDTO.class)))
                .thenReturn(sesionProjection());
        when(institutionalScopePort.canDocenteAccessGrupo(USUARIO_EJECUTOR, GRUPO)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> useCase.execute(dominioValido()));

        verifyNoInteractions(asistenciaRepositoryPort);
        verify(realtimePublisherPort, never()).publish(any());
    }

    @Test
    void sesion_inexistente_es_rechazada_con_notFound_sin_persistir_ni_publicar() {
        when(sesionRepositoryPort.consultarSesion(any(ConsultarSesionRepositoryDTO.class))).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(dominioValido()));

        verifyNoInteractions(asistenciaRepositoryPort);
        verify(realtimePublisherPort, never()).publish(any());
    }

    @Test
    void propaga_exactamente_el_usuarioEjecutor_del_dominio_al_repositoryDTO() {
        darTitularidadAlDocente();

        useCase.execute(dominioValido());

        final ArgumentCaptor<RegistrarAsistenciasSesionRepositoryDTO> captor =
                ArgumentCaptor.forClass(RegistrarAsistenciasSesionRepositoryDTO.class);
        verify(asistenciaRepositoryPort).registrarAsistenciasSesion(captor.capture());
        assertEquals(USUARIO_EJECUTOR, captor.getValue().usuarioEjecutor());
    }

    private void darTitularidadAlDocente() {
        when(sesionRepositoryPort.consultarSesion(any(ConsultarSesionRepositoryDTO.class)))
                .thenReturn(sesionProjection());
        when(institutionalScopePort.canDocenteAccessGrupo(USUARIO_EJECUTOR, GRUPO)).thenReturn(true);
    }

    private SesionRepositoryProjection sesionProjection() {
        return new SesionRepositoryProjection(SESION, GRUPO, "Sesion 1", 1, "S01", 1, "G01", "Grupo 1", null, null);
    }

    private RegistrarAsistenciasSesionDomain dominioValido() {
        return new RegistrarAsistenciasSesionDomain(
                SESION,
                List.of(new RegistroAsistenciaSesionDomain(ESTUDIANTE, "AN")),
                USUARIO_EJECUTOR
        );
    }
}
