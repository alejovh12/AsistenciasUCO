package co.edu.uco.asistenciasuco.application.features.seguridad.registroeventoasignacion.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.seguridad.registroeventoasignacion.common.dto.EventoAsignacionSeguridad;
import co.edu.uco.asistenciasuco.application.features.seguridad.registroeventoasignacion.primaryports.RegistrarEventoAsignacionInputPort.EventoAsignacionInvalidoException;
import co.edu.uco.asistenciasuco.application.features.seguridad.registroeventoasignacion.primaryports.RegistrarEventoAsignacionInputPort.ResultadoRegistro;
import co.edu.uco.asistenciasuco.application.secondaryports.security.RegistroEventoAsignacionPort;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** SEC-BASELINE-020: valida schemaVersion/applicationId y delega el registro idempotente. */
class RegistrarEventoAsignacionUseCaseImplTest {

    private static final UUID APPLICATION_ID_PROPIO = UUID.fromString("a0b42637-39f8-4ddd-bec2-0781ba08e779");

    private final RegistroEventoAsignacionPort repositorio = mock(RegistroEventoAsignacionPort.class);
    private final RegistrarEventoAsignacionUseCaseImpl useCase =
            new RegistrarEventoAsignacionUseCaseImpl(repositorio, APPLICATION_ID_PROPIO);

    private EventoAsignacionSeguridad evento(final int schemaVersion, final UUID applicationId) {
        return new EventoAsignacionSeguridad(
                UUID.randomUUID(), schemaVersion, "ROLE_ASSIGNED", OffsetDateTime.now(), "universidad-uco",
                applicationId, "asistencias-uco", "user-demo", UUID.randomUUID().toString(),
                "assignment-demo-001", "role-demo-001", "DEMO_ROLE", null, null, null);
    }

    @Test
    void schema_version_no_soportada_se_rechaza_sin_llamar_al_repositorio() {
        final EventoAsignacionSeguridad evento = evento(2, APPLICATION_ID_PROPIO);
        assertThrows(EventoAsignacionInvalidoException.class, () -> useCase.procesar(evento));
        verifyNoInteractions(repositorio);
    }

    @Test
    void application_id_ajeno_se_rechaza_sin_llamar_al_repositorio() {
        final EventoAsignacionSeguridad evento = evento(1, UUID.randomUUID());
        assertThrows(EventoAsignacionInvalidoException.class, () -> useCase.procesar(evento));
        verifyNoInteractions(repositorio);
    }

    @Test
    void evento_sin_event_id_se_rechaza_sin_llamar_al_repositorio() {
        final EventoAsignacionSeguridad evento = new EventoAsignacionSeguridad(
                null, 1, "ROLE_ASSIGNED", OffsetDateTime.now(), "universidad-uco", APPLICATION_ID_PROPIO,
                "asistencias-uco", "user-demo", null, "assignment-demo-001", "role-demo-001", "DEMO_ROLE",
                null, null, null);
        assertThrows(EventoAsignacionInvalidoException.class, () -> useCase.procesar(evento));
        verifyNoInteractions(repositorio);
    }

    @Test
    void evento_valido_nuevo_devuelve_procesado() {
        final EventoAsignacionSeguridad evento = evento(1, APPLICATION_ID_PROPIO);
        when(repositorio.registrarSiNuevo(evento)).thenReturn(true);
        assertEquals(ResultadoRegistro.PROCESADO, useCase.procesar(evento));
    }

    @Test
    void evento_valido_repetido_devuelve_duplicado() {
        final EventoAsignacionSeguridad evento = evento(1, APPLICATION_ID_PROPIO);
        when(repositorio.registrarSiNuevo(evento)).thenReturn(false);
        assertEquals(ResultadoRegistro.DUPLICADO, useCase.procesar(evento));
    }

    @Test
    void falla_del_repositorio_se_propaga_sin_envolver() {
        final EventoAsignacionSeguridad evento = evento(1, APPLICATION_ID_PROPIO);
        when(repositorio.registrarSiNuevo(any())).thenThrow(new RuntimeException("BD caida"));
        final RuntimeException excepcion = assertThrows(RuntimeException.class, () -> useCase.procesar(evento));
        assertEquals("BD caida", excepcion.getMessage());
        verify(repositorio).registrarSiNuevo(evento);
    }
}
