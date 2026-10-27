package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification.contract;

import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Suite abstracta de contrato para cualquier implementacion de {@link NotificacionPort}
 * (TEST_PLAN §3.1: CT-01..CT-08).
 */
public abstract class NotificacionPortContractTest {

    protected abstract NotificacionPort puerto();

    protected abstract void simularFalloInfraestructura();

    @Test
    @DisplayName("CT-01: acepta una notificacion completa sin lanzar")
    void CT01_acepta_notificacion_completa() {
        final Notificacion notificacion = new Notificacion(
                Notificacion.TIPO_RETIRO_ESTUDIANTE_GRUPO,
                "Asunto prueba",
                "Mensaje de prueba",
                List.of(new Notificacion.Destinatario("ESTUDIANTE", UUID.randomUUID(), "Juan Perez", "juan@uco.edu.co")),
                Map.of("grupoId", UUID.randomUUID().toString())
        );

        assertDoesNotThrow(() -> puerto().notificar(notificacion));
    }

    @Test
    @DisplayName("CT-02: notificacion nula lanza NullPointerException")
    void CT02_notificacion_nula_lanza_npe() {
        assertThrows(NullPointerException.class, () -> puerto().notificar(null));
    }

    @Test
    @DisplayName("CT-03: sin destinatarios no lanza")
    void CT03_sin_destinatarios_no_lanza() {
        final Notificacion notificacion = new Notificacion(
                Notificacion.TIPO_RETIRO_ESTUDIANTE_GRUPO,
                "Asunto",
                "Mensaje",
                List.of(),
                Map.of("clave", "valor")
        );

        assertDoesNotThrow(() -> puerto().notificar(notificacion));
    }

    @Test
    @DisplayName("CT-04: datos vacio y asunto/mensaje nulos no lanzan")
    void CT04_datos_vacio_y_asunto_mensaje_nulos_no_lanzan() {
        final Notificacion notificacion = new Notificacion(
                Notificacion.TIPO_RETIRO_ESTUDIANTE_GRUPO,
                null,
                null,
                List.of(new Notificacion.Destinatario("DOCENTE", UUID.randomUUID(), "Docente", "doc@uco.edu.co")),
                Map.of()
        );

        assertDoesNotThrow(() -> puerto().notificar(notificacion));
    }

    @Test
    @DisplayName("CT-05: con la infraestructura subyacente fallando nunca lanza")
    void CT05_con_infraestructura_fallando_nunca_lanza() {
        simularFalloInfraestructura();
        final Notificacion notificacion = new Notificacion(
                Notificacion.TIPO_RETIRO_ESTUDIANTE_GRUPO,
                "Asunto",
                "Mensaje",
                List.of(new Notificacion.Destinatario("ESTUDIANTE", UUID.randomUUID(), "Persona", "correo@uco.edu.co")),
                Map.of("id", "123")
        );

        assertDoesNotThrow(() -> puerto().notificar(notificacion));
    }

    @Test
    @DisplayName("CT-07: es segura bajo concurrencia (50 hilos x 20 llamadas)")
    void CT07_segura_bajo_concurrencia() throws Exception {
        final int hilos = 50;
        final int llamadasPorHilo = 20;
        final ExecutorService executor = Executors.newFixedThreadPool(hilos);

        try {
            final List<Callable<Void>> tasks = new ArrayList<>();
            for (int i = 0; i < hilos; i++) {
                tasks.add(() -> {
                    for (int j = 0; j < llamadasPorHilo; j++) {
                        puerto().notificar(new Notificacion(
                                Notificacion.TIPO_RETIRO_ESTUDIANTE_GRUPO,
                                "Asunto",
                                "Mensaje",
                                List.of(new Notificacion.Destinatario("ESTUDIANTE", UUID.randomUUID(), "N", "c@uco.edu.co")),
                                Map.of("i", String.valueOf(j))
                        ));
                    }
                    return null;
                });
            }

            final List<Future<Void>> futures = executor.invokeAll(tasks);
            for (final Future<Void> future : futures) {
                assertDoesNotThrow(() -> future.get());
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("CT-08: dos llamadas con el mismo objeto producen dos avisos independientes")
    void CT08_dos_llamadas_independientes() {
        final Notificacion notificacion = new Notificacion(
                Notificacion.TIPO_RETIRO_ESTUDIANTE_GRUPO,
                "Asunto",
                "Mensaje",
                List.of(new Notificacion.Destinatario("ESTUDIANTE", UUID.randomUUID(), "N", "c@uco.edu.co")),
                Map.of("k", "v")
        );

        assertDoesNotThrow(() -> {
            puerto().notificar(notificacion);
            puerto().notificar(notificacion);
        });
    }
}
