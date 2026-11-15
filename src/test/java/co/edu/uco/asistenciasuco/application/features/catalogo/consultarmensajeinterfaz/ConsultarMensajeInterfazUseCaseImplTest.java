package co.edu.uco.asistenciasuco.application.features.catalogo.consultarmensajeinterfaz;

import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.features.catalogo.consultarmensajeinterfaz.primaryports.ConsultarMensajeInterfazInputPort.MensajeInterfaz;
import co.edu.uco.asistenciasuco.application.features.catalogo.consultarmensajeinterfaz.primaryports.interactor.ConsultarMensajeInterfazInteractor;
import co.edu.uco.asistenciasuco.application.features.catalogo.consultarmensajeinterfaz.usecase.impl.ConsultarMensajeInterfazUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** INT-003 (paso 12): solo codigos UI### de la interfaz; cualquier otro responde como inexistente (GEN_001). */
class ConsultarMensajeInterfazUseCaseImplTest {

    private final MessageCatalogPort catalogo = mock(MessageCatalogPort.class);
    private final ConsultarMensajeInterfazUseCaseImpl useCase = new ConsultarMensajeInterfazUseCaseImpl(catalogo);

    @Test
    void entrega_el_texto_del_catalogo_recortado() {
        when(catalogo.findUserMessage("UI012")).thenReturn(Optional.of("  Ocurrio un error inesperado.  "));

        final MensajeInterfaz mensaje = useCase.execute(" UI012 ");

        assertEquals(new MensajeInterfaz("UI012", "Ocurrio un error inesperado."), mensaje);
    }

    @Test
    void conserva_los_marcadores_para_que_los_llene_la_spa() {
        when(catalogo.findUserMessage("UI120")).thenReturn(Optional.of("Referencia del fallo: {id}"));

        assertEquals("Referencia del fallo: {id}", useCase.execute("UI120").contenido());
    }

    @Test
    void codigo_sin_texto_en_el_catalogo_responde_gen_001() {
        when(catalogo.findUserMessage("UI999")).thenReturn(Optional.empty());

        final ResourceNotFoundException error = assertThrows(ResourceNotFoundException.class, () -> useCase.execute("UI999"));

        assertEquals(Optional.of(MensajeUsuarioErrorCode.GEN_001), error.getErrorDefinition());
        assertEquals(java.util.List.of("mensaje"), error.getMessageArgs());
    }

    @Test
    void texto_vacio_en_el_catalogo_responde_como_inexistente() {
        when(catalogo.findUserMessage("UI013")).thenReturn(Optional.of("   "));

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute("UI013"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"GEN_001", "VAL_APP_001", "ui012", "UI12", "UI123456789", "UI012X", "MSG_UI012", "  "})
    void codigos_fuera_de_la_interfaz_no_consultan_el_catalogo(final String codigo) {
        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(codigo));
        verify(catalogo, never()).findUserMessage(anyString());
    }

    @Test
    void el_interactor_delega_en_el_caso_de_uso() {
        when(catalogo.findUserMessage("UI001")).thenReturn(Optional.of("Texto"));

        assertEquals("Texto", new ConsultarMensajeInterfazInteractor(useCase).execute("UI001").contenido());
    }

    @Test
    void exige_sus_dependencias() {
        assertThrows(NullPointerException.class, () -> new ConsultarMensajeInterfazUseCaseImpl(null));
        assertThrows(NullPointerException.class, () -> new ConsultarMensajeInterfazInteractor(null));
    }
}
