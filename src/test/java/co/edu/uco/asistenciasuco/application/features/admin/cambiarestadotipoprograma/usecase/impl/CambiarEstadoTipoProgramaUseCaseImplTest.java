package co.edu.uco.asistenciasuco.application.features.admin.cambiarestadotipoprograma.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadotipoprograma.usecase.domain.CambiarEstadoTipoProgramaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.TipoProgramaCommandPort;
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

class CambiarEstadoTipoProgramaUseCaseImplTest {

    private static final UUID ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID ACTOR_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private TipoProgramaCommandPort commandPort;
    private CambiarEstadoTipoProgramaUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        commandPort = mock(TipoProgramaCommandPort.class);
        useCase = new CambiarEstadoTipoProgramaUseCaseImpl(commandPort);
    }

    @Test
    void inactivar_y_activar_delegan_al_puerto() {
        useCase.execute(new CambiarEstadoTipoProgramaDomain(ID, false, ACTOR_ID));
        useCase.execute(new CambiarEstadoTipoProgramaDomain(ID, true, ACTOR_ID));

        final ArgumentCaptor<TipoProgramaCommandPort.CambiarEstadoTipoProgramaCommand> captor = ArgumentCaptor.forClass(TipoProgramaCommandPort.CambiarEstadoTipoProgramaCommand.class);
        verify(commandPort, times(2)).cambiarEstadoTipoPrograma(captor.capture());
        assertFalse(captor.getAllValues().get(0).activo());
        assertTrue(captor.getAllValues().get(1).activo());
        assertEquals(ID, captor.getValue().id());
        assertEquals(ACTOR_ID, captor.getValue().actorUsuarioId());
    }

    @Test
    void domain_nulo_lanza_validationException() {
        assertThrows(ValidationException.class, () -> useCase.execute(null));
        verify(commandPort, never()).cambiarEstadoTipoPrograma(any());
    }

    @Test
    void id_nulo_lanza_validationException() {
        final CambiarEstadoTipoProgramaDomain domain = new CambiarEstadoTipoProgramaDomain(null, true, ACTOR_ID);
        assertThrows(ValidationException.class, () -> useCase.execute(domain));
        verify(commandPort, never()).cambiarEstadoTipoPrograma(any());
    }

    @Test
    void estado_deseado_nulo_lanza_validationException() {
        final CambiarEstadoTipoProgramaDomain domain = new CambiarEstadoTipoProgramaDomain(ID, null, ACTOR_ID);
        assertThrows(ValidationException.class, () -> useCase.execute(domain));
        verify(commandPort, never()).cambiarEstadoTipoPrograma(any());
    }

    @Test
    void errores_del_puerto_se_propagan() {
        doThrow(new ConflictException("Conflicto")).when(commandPort).cambiarEstadoTipoPrograma(any());
        final CambiarEstadoTipoProgramaDomain domain = new CambiarEstadoTipoProgramaDomain(ID, true, ACTOR_ID);
        assertThrows(ConflictException.class, () -> useCase.execute(domain));
    }

    @Test
    void puerto_obligatorio() {
        assertThrows(NullPointerException.class, () -> new CambiarEstadoTipoProgramaUseCaseImpl(null));
    }
}
