package co.edu.uco.notificaciones.application.service;

import co.edu.uco.notificaciones.application.model.ConfiguracionCanal;
import co.edu.uco.notificaciones.application.model.PaginaNotificaciones;
import co.edu.uco.notificaciones.application.model.TipoPlantillaAdmin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelosAplicacionTest {

    @Test
    void lasColeccionesNulasSeNormalizan() {
        assertTrue(new PaginaNotificaciones(null, 1, 1, 0, 1, null).elementos().isEmpty());
        assertTrue(new TipoPlantillaAdmin("T", "d", "es", 0, "", "", null, null, null).marcadores().isEmpty());
        assertTrue(new TipoPlantillaAdmin("T", "d", "es", 0, "", "", null, null, null).versiones().isEmpty());
        assertTrue(new ConfiguracionCanal("log", "", 25, "", false, "a@b.co", "n", false, false, 1, 1, 1, "", false, null).testModeRedirectTo().isEmpty());
    }
}
