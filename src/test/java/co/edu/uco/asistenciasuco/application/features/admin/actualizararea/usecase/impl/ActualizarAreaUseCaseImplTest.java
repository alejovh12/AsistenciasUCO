package co.edu.uco.asistenciasuco.application.features.admin.actualizararea.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.admin.actualizararea.usecase.domain.ActualizarAreaDomain;
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

class ActualizarAreaUseCaseImplTest {

    private static final UUID ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID ACTOR_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private AreaCommandPort commandPort;
    private ActualizarAreaUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        commandPort = mock(AreaCommandPort.class);
        useCase = new ActualizarAreaUseCaseImpl(commandPort);
    }

    @Test
    void normaliza_y_delega_al_puerto() {
        useCase.execute(new ActualizarAreaDomain(ID, "  Nombre Valido  ", ACTOR_ID));

        final ArgumentCaptor<AreaCommandPort.ActualizarAreaCommand> captor = ArgumentCaptor.forClass(AreaCommandPort.ActualizarAreaCommand.class);
        verify(commandPort).actualizarArea(captor.capture());
        assertEquals(ID, captor.getValue().id());
        assertEquals("Nombre Valido", captor.getValue().nombre());
        assertEquals(ACTOR_ID, captor.getValue().actorUsuarioId());
    }

    @Test
    void domain_nulo_lanza_validationException() {
        assertThrows(ValidationException.class, () -> useCase.execute(null));
        verify(commandPort, never()).actualizarArea(any());
    }

    @Test
    void id_nulo_lanza_validationException() {
        final ActualizarAreaDomain domain = new ActualizarAreaDomain(null, "Nombre Valido", ACTOR_ID);
        assertThrows(ValidationException.class, () -> useCase.execute(domain));
        verify(commandPort, never()).actualizarArea(any());
    }

    @Test
    void nombre_invalido_lanza_validationException() {
        for (final String nombre : new String[] {null, "", "ab", "x".repeat(51)}) {
            final ActualizarAreaDomain domain = new ActualizarAreaDomain(ID, nombre, ACTOR_ID);
            assertThrows(ValidationException.class, () -> useCase.execute(domain));
        }
        verify(commandPort, never()).actualizarArea(any());
    }

    @Test
    void errores_del_puerto_se_propagan() {
        doThrow(new ConflictException("Conflicto")).when(commandPort).actualizarArea(any());
        final ActualizarAreaDomain domain = new ActualizarAreaDomain(ID, "Nombre Valido", ACTOR_ID);
        assertThrows(ConflictException.class, () -> useCase.execute(domain));
    }

    @Test
    void puerto_obligatorio() {
        assertThrows(NullPointerException.class, () -> new ActualizarAreaUseCaseImpl(null));
    }
}
