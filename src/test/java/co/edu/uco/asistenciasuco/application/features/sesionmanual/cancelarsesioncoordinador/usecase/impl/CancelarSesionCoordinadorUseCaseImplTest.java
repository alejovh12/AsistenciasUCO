package co.edu.uco.asistenciasuco.application.features.sesionmanual.cancelarsesioncoordinador.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.sesion.common.dto.CancelacionSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesion.common.entity.EstadoSesion;
import co.edu.uco.asistenciasuco.application.features.sesion.exception.SesionErrorCode;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.cancelarsesioncoordinador.primaryports.CancelarSesionCoordinadorInputPort.Comando;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.cancelarsesioncoordinador.primaryports.CancelarSesionCoordinadorInputPort.Resultado;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionCancelacionRepositoryPort.SesionCancelada;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.CancelacionSesionProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionProcedenciaRepositoryProjection;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * CULM-001 / DEC-064 (HU047): el coordinador cancela una sesion manual. Exito con la cancelacion leida de la DB y el evento
 * {@code SESION_CANCELADA} (tipo COORDINADOR); sesion inexistente antes del SP; errores de la DB (ambito, no manual, ya cancelada)
 * sin evento; el evento es de mejor esfuerzo.
 */
class CancelarSesionCoordinadorUseCaseImplTest {

    private static final UUID SESION = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID GRUPO = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID COORDINADOR = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final LocalDateTime INICIO_UTC = LocalDateTime.of(2026, 11, 10, 13, 0);
    private static final LocalDateTime CANCELADA_UTC = LocalDateTime.of(2026, 11, 9, 14, 0);
    private static final String MOTIVO_TEXTO = "Sesión duplicada: Ya existe la del lunes";
    private static final Comando COMANDO = new Comando(SESION, "DUPLICADA", "Ya existe la del lunes", COORDINADOR);

    private final SesionProcedenciaQueryPort sesiones = mock(SesionProcedenciaQueryPort.class);
    private final SesionManualRepositoryPort repositorio = mock(SesionManualRepositoryPort.class);
    private final RealtimePublisherPort realtime = mock(RealtimePublisherPort.class);
    private final CancelarSesionCoordinadorUseCaseImpl useCase = new CancelarSesionCoordinadorUseCaseImpl(sesiones, repositorio, realtime);

    @Test
    void cancela_devuelve_el_motivo_guardado_y_publica_el_evento_del_grupo() {
        when(sesiones.consultarSesion(SESION)).thenReturn(Optional.of(sesion("UTC_V2", null)), Optional.of(sesion("UTC_V2", MOTIVO_TEXTO)));
        when(repositorio.cancelarPorCoordinador(SESION, "DUPLICADA", "Ya existe la del lunes", COORDINADOR))
                .thenReturn(new SesionCancelada(SESION, GRUPO, CANCELADA_UTC, 38));

        final Resultado resultado = useCase.execute(COMANDO);

        assertEquals(new Resultado(SESION, GRUPO, EstadoSesion.CANCELADA,
                new CancelacionSesionDTO("COORDINADOR", MOTIVO_TEXTO, null, Instant.parse("2026-11-09T14:00:00Z")), 38), resultado);

        final ArgumentCaptor<RealtimeEvent> evento = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(realtime).publish(evento.capture());
        assertEquals("SESION_CANCELADA", evento.getValue().type());
        assertEquals(Map.of("grupo", GRUPO.toString(), "sesion", SESION.toString(), "tipo", "COORDINADOR",
                "fechaHoraInicioUtc", "2026-11-10T13:00:00Z"), evento.getValue().payload());
        assertFalse(evento.getValue().payload().containsKey("motivo"));
    }

    @Test
    void si_la_sesion_no_tiene_horario_confirmado_el_evento_no_lleva_hora_y_sin_cancelacion_leida_el_motivo_es_nulo() {
        when(sesiones.consultarSesion(SESION)).thenReturn(Optional.of(sesion(null, null)));
        when(repositorio.cancelarPorCoordinador(any(), any(), any(), any())).thenReturn(new SesionCancelada(SESION, GRUPO, null, 0));

        final Resultado resultado = useCase.execute(new Comando(SESION, "OTRO", null, COORDINADOR));

        assertNull(resultado.cancelacion().motivo());
        assertNull(resultado.cancelacion().fechaCancelacionUtc());
        final ArgumentCaptor<RealtimeEvent> evento = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(realtime).publish(evento.capture());
        assertFalse(evento.getValue().payload().containsKey("fechaHoraInicioUtc"));
    }

    @Test
    void una_sesion_inexistente_responde_404_sin_llamar_al_sp() {
        when(sesiones.consultarSesion(SESION)).thenReturn(Optional.empty());

        final ResourceNotFoundException error = assertThrows(ResourceNotFoundException.class, () -> useCase.execute(COMANDO));

        assertEquals(SesionErrorCode.ERR_SESION_NO_EXISTE.code(), error.getCode());
        verifyNoInteractions(repositorio, realtime);
    }

    @Test
    void los_errores_de_la_db_se_propagan_sin_evento() {
        when(sesiones.consultarSesion(SESION)).thenReturn(Optional.of(sesion("UTC_V2", null)));
        when(repositorio.cancelarPorCoordinador(any(), any(), any(), any()))
                .thenThrow(new ForbiddenException("sin ambito"))
                .thenThrow(new ConflictException(SesionErrorCode.SES_018));

        assertThrows(ForbiddenException.class, () -> useCase.execute(COMANDO));
        assertThrows(ConflictException.class, () -> useCase.execute(COMANDO));
        verifyNoInteractions(realtime);
    }

    @Test
    void un_fallo_del_publicador_no_cambia_la_respuesta() {
        when(sesiones.consultarSesion(SESION)).thenReturn(Optional.of(sesion("UTC_V2", null)));
        when(repositorio.cancelarPorCoordinador(any(), any(), any(), any())).thenReturn(new SesionCancelada(SESION, GRUPO, CANCELADA_UTC, 2));
        doThrow(new IllegalStateException("sse")).when(realtime).publish(any());

        assertEquals(2, useCase.execute(COMANDO).totalEstudiantesActivos());
    }

    @Test
    void el_constructor_y_el_comando_son_obligatorios() {
        assertThrows(NullPointerException.class, () -> new CancelarSesionCoordinadorUseCaseImpl(null, repositorio, realtime));
        assertThrows(NullPointerException.class, () -> new CancelarSesionCoordinadorUseCaseImpl(sesiones, null, realtime));
        assertThrows(NullPointerException.class, () -> new CancelarSesionCoordinadorUseCaseImpl(sesiones, repositorio, null));
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }

    private static SesionProcedenciaRepositoryProjection sesion(final String procedencia, final String motivoGuardado) {
        final CancelacionSesionProjection cancelacion = motivoGuardado == null ? null
                : new CancelacionSesionProjection("DOCENTE", motivoGuardado, null, CANCELADA_UTC);
        return new SesionProcedenciaRepositoryProjection(SESION, GRUPO, "Sesion extra", 5, "COD5", 1, "01", "G01",
                INICIO_UTC, INICIO_UTC.plusHours(2), procedencia, cancelacion);
    }
}
