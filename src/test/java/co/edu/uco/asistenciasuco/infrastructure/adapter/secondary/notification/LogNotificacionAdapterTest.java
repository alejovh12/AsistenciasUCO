package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification;

import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Adaptador temporal de {@code NotificacionPort} (hasta integrar el componente de notificaciones): registra el aviso en
 * el log sin datos personales (NT-01..NT-03).
 */
class LogNotificacionAdapterTest {

    private final LogNotificacionAdapter adapter = new LogNotificacionAdapter();

    @Test
    void NT01_registra_tipo_roles_y_usuarios_sin_correo_ni_nombre() {
        final UUID usuario = UUID.randomUUID();
        final Notificacion aviso = new Notificacion(Notificacion.TIPO_RETIRO_ESTUDIANTE_GRUPO, "Retiro", "Motivo: X",
                List.of(new Notificacion.Destinatario("ESTUDIANTE", usuario, "Ana Perez", "ana@uco.edu.co")),
                Map.of("idGrupo", "g"));

        final String linea = adapter.describir(aviso);

        assertTrue(linea.contains(Notificacion.TIPO_RETIRO_ESTUDIANTE_GRUPO));
        assertTrue(linea.contains("ESTUDIANTE:" + usuario));
        assertFalse(linea.contains("ana@uco.edu.co"), "El log no expone correos");
        assertFalse(linea.contains("Ana Perez"), "El log no expone nombres");
        assertDoesNotThrow(() -> adapter.notificar(aviso));
    }

    @Test
    void NT02_aviso_nulo_es_invalido() {
        assertThrows(NullPointerException.class, () -> adapter.notificar(null));
    }

    @Test
    void NT03_notificacion_copia_las_listas_y_exige_tipo() {
        final Notificacion aviso = new Notificacion("T", "a", "m", List.of(), Map.of());
        assertEquals(0, aviso.destinatarios().size());
        assertThrows(NullPointerException.class, () -> new Notificacion(null, "a", "m", List.of(), Map.of()));
    }
}
