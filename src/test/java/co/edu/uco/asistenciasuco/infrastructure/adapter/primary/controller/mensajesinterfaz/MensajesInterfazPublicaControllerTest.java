package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.mensajesinterfaz;

import co.edu.uco.asistenciasuco.application.features.catalogo.consultarmensajeinterfaz.primaryports.ConsultarMensajeInterfazInputPort;
import co.edu.uco.asistenciasuco.application.features.catalogo.consultarmensajeinterfaz.primaryports.ConsultarMensajeInterfazInputPort.MensajeInterfaz;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MensajesInterfazPublicaControllerTest {

    @Test
    void responde_el_texto_en_api_data_response() {
        final ConsultarMensajeInterfazInputPort puerto = mock(ConsultarMensajeInterfazInputPort.class);
        when(puerto.execute("UI012")).thenReturn(new MensajeInterfaz("UI012", "Texto del catalogo."));

        final var respuesta = new MensajesInterfazPublicaController(puerto).consultar("UI012");

        assertEquals(200, respuesta.getStatusCode().value());
        assertTrue(respuesta.getBody().exitoso());
        assertEquals("UI012", respuesta.getBody().datos().codigo());
        assertEquals("Texto del catalogo.", respuesta.getBody().datos().contenido());
    }

    @Test
    void exige_el_puerto_de_entrada() {
        assertThrows(NullPointerException.class, () -> new MensajesInterfazPublicaController(null));
    }
}
