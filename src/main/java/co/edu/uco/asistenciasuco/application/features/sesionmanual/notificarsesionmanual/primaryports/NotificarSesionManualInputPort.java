package co.edu.uco.asistenciasuco.application.features.sesionmanual.notificarsesionmanual.primaryports;

import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CULM-001 / DEC-064 (HU047): tras crear una sesion manualmente, se avisa de inmediato a los estudiantes activos del grupo y al
 * coordinador del programa (correo) y se publica el evento {@code SESION_MANUAL_CREADA}. Es de mejor esfuerzo: nunca falla la
 * creacion de la sesion.
 */
public interface NotificarSesionManualInputPort
        extends InteractorWithReturn<NotificarSesionManualInputPort.Comando, NotificarSesionManualInputPort.Resultado> {

    /** Datos con los que se creo la sesion; sirven para localizarla (grupo + nombre + horas tal como se guardaron). */
    record Comando(UUID grupoId, String nombre, LocalDateTime fechaHoraInicio, LocalDateTime fechaHoraFin, UUID docenteUsuarioId) {
    }

    /** {@code sesionId} es {@code null} si no se pudo localizar la sesion; los conteos son avisos encolados. */
    record Resultado(UUID sesionId, int estudiantesNotificados, int coordinadoresNotificados, boolean eventoPublicado) {

        public static Resultado sinSesion() {
            return new Resultado(null, 0, 0, false);
        }
    }
}
