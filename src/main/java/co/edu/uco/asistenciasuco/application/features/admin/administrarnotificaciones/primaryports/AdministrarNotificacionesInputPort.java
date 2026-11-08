package co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.primaryports;

import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.EstadoCanalNotificacionProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.EstadoCanalPublicoProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.PaginaColaNotificacionProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.TipoPlantillaAdminProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.VersionPlantillaProjection;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.VistaPreviaPlantillaProjection;

import java.util.List;
import java.util.UUID;

/**
 * Puerto primario para administrar plantillas de correo y la cola outbox (NOTIF-002B, TD-069).
 */
public interface AdministrarNotificacionesInputPort {

    EstadoCanalNotificacionProjection consultarEstadoCanal();

    /** CULM-001 D.9-a (F-ADM-029): estado del canal sin host, puerto ni usuario de la cuenta tecnica. */
    EstadoCanalPublicoProjection consultarEstadoCanalPublico(UUID idUsuarioEjecutor);

    List<TipoPlantillaAdminProjection> consultarPlantillas(String idioma, UUID idUsuarioEjecutor);

    VersionPlantillaProjection crearVersionPlantilla(String tipo, String idioma, String asunto, String cuerpo, UUID creadoPor);

    void activarVersionPlantilla(String tipo, String idioma, int version, UUID activadoPor);

    VistaPreviaPlantillaProjection generarVistaPrevia(String tipo, String idioma, String asunto, String cuerpo);

    PaginaColaNotificacionProjection consultarCola(String estado, String tipo, int pagina, int tamano, UUID idUsuarioEjecutor);

    void reintentarNotificacion(UUID idNotificacion, UUID usuarioEjecutor);

    void enviarCorreoPrueba(UUID idUsuarioEjecutor, String tipo, String correoDestino);
}
