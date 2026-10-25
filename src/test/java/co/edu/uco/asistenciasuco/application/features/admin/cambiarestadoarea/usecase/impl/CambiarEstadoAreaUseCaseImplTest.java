package co.edu.uco.asistenciasuco.application.features.admin.cambiarestadoarea.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadoarea.usecase.domain.CambiarEstadoAreaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AreaCommandPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class CambiarEstadoAreaUseCaseImplTest {

    private static final UUID ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID ACTOR_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private AreaCommandPort commandPort;
    private CambiarEstadoAreaUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        commandPort = mock(AreaCommandPort.class);
        useCase = new CambiarEstadoAreaUseCaseImpl(commandPort);
    }

    @Test
    void inactivar_y_activar_delegan_al_puerto() {
        useCase.execute(new CambiarEstadoAreaDomain(ID, false, ACTOR_ID));
        useCase.execute(new CambiarEstadoAreaDomain(ID, true, ACTOR_ID));

        final ArgumentCaptor<AreaCommandPort.CambiarEstadoAreaCommand> captor = ArgumentCaptor.forClass(AreaCommandPort.CambiarEstadoAreaCommand.class);
        verify(commandPort, times(2)).cambiarEstadoArea(captor.capture());
        assertFalse(captor.getAllValues().get(0).activa());
        assertTrue(captor.getAllValues().get(1).activa());
        assertEquals(ID, captor.getValue().id());
        assertEquals(ACTOR_ID, captor.getValue().actorUsuarioId());
    }

    @Test
    void domain_nulo_lanza_validationException() {
        assertThrows(ValidationException.class, () -> useCase.execute(null));
        verify(commandPort, never()).cambiarEstadoArea(any());
    }

    @Test
    void id_nulo_lanza_validationException() {
        final CambiarEstadoAreaDomain domain = new CambiarEstadoAreaDomain(null, true, ACTOR_ID);
        assertThrows(ValidationException.class, () -> useCase.execute(domain));
        verify(commandPort, never()).cambiarEstadoArea(any());
    }

    @Test
    void estado_deseado_nulo_lanza_validationException() {
        final CambiarEstadoAreaDomain domain = new CambiarEstadoAreaDomain(ID, null, ACTOR_ID);
        assertThrows(ValidationException.class, () -> useCase.execute(domain));
        verify(commandPort, never()).cambiarEstadoArea(any());
    }

    @Test
    void errores_del_puerto_se_propagan() {
        doThrow(new ConflictException("Conflicto")).when(commandPort).cambiarEstadoArea(any());
        final CambiarEstadoAreaDomain domain = new CambiarEstadoAreaDomain(ID, true, ACTOR_ID);
        assertThrows(ConflictException.class, () -> useCase.execute(domain));
    }

    @Test
    void puerto_obligatorio() {
        assertThrows(NullPointerException.class, () -> new CambiarEstadoAreaUseCaseImpl(null));
    }
}
