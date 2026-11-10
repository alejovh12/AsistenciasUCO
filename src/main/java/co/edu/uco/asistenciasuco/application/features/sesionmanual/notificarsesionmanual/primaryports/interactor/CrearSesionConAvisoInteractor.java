package co.edu.uco.asistenciasuco.application.features.sesionmanual.notificarsesionmanual.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.CrearSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.dto.CrearSesionDTO;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.notificarsesionmanual.primaryports.NotificarSesionManualInputPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * CULM-001 / DEC-064 (HU047): decorador del puerto de entrada de creacion de sesiones (el golden path no cambia). Crea la sesion con
 * el puerto original y, solo si la creacion termino bien, avisa a estudiantes y coordinador. El aviso es de mejor esfuerzo: un fallo
 * se registra y no revierte la sesion ni cambia la respuesta.
 */
public final class CrearSesionConAvisoInteractor implements CrearSesionInputPort {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrearSesionConAvisoInteractor.class);

    private final CrearSesionInputPort crearSesion;
    private final NotificarSesionManualInputPort notificarSesionManual;

    public CrearSesionConAvisoInteractor(final CrearSesionInputPort crearSesion, final NotificarSesionManualInputPort notificarSesionManual) {
        this.crearSesion = Objects.requireNonNull(crearSesion, "El puerto de creacion de sesiones es obligatorio.");
        this.notificarSesionManual = Objects.requireNonNull(notificarSesionManual, "El puerto de aviso de sesion manual es obligatorio.");
    }

    @Override
    public String execute(final CrearSesionDTO dto) {
        final String mensajeUsuario = crearSesion.execute(dto);
        try {
            notificarSesionManual.execute(new NotificarSesionManualInputPort.Comando(
                    dto.getGrupo(), dto.getNombre(), dto.getFechaHoraInicio(), dto.getFechaHoraFin(), dto.getUsuarioEjecutor()));
        } catch (final RuntimeException exception) {
            LOGGER.warn("No fue posible avisar la sesion manual creada en el grupo {}: {}", dto.getGrupo(), exception.getClass().getSimpleName());
        }
        return mensajeUsuario;
    }
}
