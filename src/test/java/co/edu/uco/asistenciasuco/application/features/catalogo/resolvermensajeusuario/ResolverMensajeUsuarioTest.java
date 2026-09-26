package co.edu.uco.asistenciasuco.application.features.catalogo.resolvermensajeusuario;

import co.edu.uco.asistenciasuco.application.features.catalogo.resolvermensajeusuario.primaryports.ResolverMensajeUsuarioInputPort;
import co.edu.uco.asistenciasuco.application.features.catalogo.resolvermensajeusuario.primaryports.interactor.ResolverMensajeUsuarioInteractor;
import co.edu.uco.asistenciasuco.application.features.catalogo.resolvermensajeusuario.usecase.ResolverMensajeUsuarioUseCase;
import co.edu.uco.asistenciasuco.application.features.catalogo.resolvermensajeusuario.usecase.impl.ResolverMensajeUsuarioUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResolverMensajeUsuarioTest {

    private static final String CODIGO = "ERR_CORREO_FORMATO_INVALIDO";

    private final MessageCatalogPort catalog = mock(MessageCatalogPort.class);
    private final ResolverMensajeUsuarioUseCase useCase = new ResolverMensajeUsuarioUseCaseImpl(catalog);
    private final ResolverMensajeUsuarioInputPort inputPort = new ResolverMensajeUsuarioInteractor(useCase);

    @Test
    void catalogo_con_mensaje_retorna_mensaje() {
        when(catalog.findUserMessage(CODIGO)).thenReturn(Optional.of("Mensaje de catalogo"));

        assertEquals(Optional.of("Mensaje de catalogo"), inputPort.execute(CODIGO));
    }

    @Test
    void catalogo_sin_mensaje_retorna_empty() {
        when(catalog.findUserMessage(CODIGO)).thenReturn(Optional.empty());

        assertEquals(Optional.empty(), inputPort.execute(CODIGO));
    }

    @Test
    void catalogo_con_mensaje_blank_retorna_empty() {
        when(catalog.findUserMessage(CODIGO)).thenReturn(Optional.of("   "));

        assertEquals(Optional.empty(), inputPort.execute(CODIGO));
    }

    @Test
    void catalogo_que_lanza_excepcion_no_propaga_y_retorna_empty() {
        when(catalog.findUserMessage(CODIGO)).thenThrow(new IllegalStateException("fallo tecnico del provider"));

        assertEquals(Optional.empty(), inputPort.execute(CODIGO));
    }

    @Test
    void catalogo_que_retorna_null_retorna_empty() {
        when(catalog.findUserMessage(CODIGO)).thenReturn(null);

        assertEquals(Optional.empty(), inputPort.execute(CODIGO));
    }

    @Test
    void codigo_nulo_o_blank_retorna_empty_sin_consultar_catalogo() {
        assertEquals(Optional.empty(), inputPort.execute(null));
        assertEquals(Optional.empty(), inputPort.execute(" "));
        verify(catalog, never()).findUserMessage(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void dependencias_obligatorias() {
        assertThrows(NullPointerException.class, () -> new ResolverMensajeUsuarioUseCaseImpl(null));
        assertThrows(NullPointerException.class, () -> new ResolverMensajeUsuarioInteractor(null));
    }
}
