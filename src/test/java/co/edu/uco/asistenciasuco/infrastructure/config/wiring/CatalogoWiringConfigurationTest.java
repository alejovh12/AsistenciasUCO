package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.catalogo.resolvermensajeusuario.primaryports.ResolverMensajeUsuarioInputPort;
import co.edu.uco.asistenciasuco.application.features.catalogo.resolvermensajeusuario.primaryports.interactor.ResolverMensajeUsuarioInteractor;
import co.edu.uco.asistenciasuco.application.features.catalogo.resolvermensajeusuario.usecase.ResolverMensajeUsuarioUseCase;
import co.edu.uco.asistenciasuco.application.features.catalogo.resolvermensajeusuario.usecase.impl.ResolverMensajeUsuarioUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CatalogoWiringConfigurationTest {

    private final CatalogoWiringConfiguration config = new CatalogoWiringConfiguration();

    @Test
    void beans_se_construyen_y_resuelven_mensaje_con_el_catalogo_inyectado() {
        final MessageCatalogPort catalog = mock(MessageCatalogPort.class);
        when(catalog.findUserMessage("COD")).thenReturn(Optional.of("Mensaje"));

        final ResolverMensajeUsuarioUseCase useCase = config.resolverMensajeUsuarioUseCase(catalog);
        final ResolverMensajeUsuarioInputPort inputPort = config.resolverMensajeUsuarioInputPort(useCase);

        assertInstanceOf(ResolverMensajeUsuarioUseCaseImpl.class, useCase);
        assertInstanceOf(ResolverMensajeUsuarioInteractor.class, inputPort);
        assertEquals(Optional.of("Mensaje"), inputPort.execute("COD"));
    }
}
