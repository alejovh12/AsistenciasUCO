package co.edu.uco.notificaciones.application.port.out;

import co.edu.uco.notificaciones.domain.Marcador;
import co.edu.uco.notificaciones.domain.TipoNotificacion;
import co.edu.uco.notificaciones.domain.VersionPlantilla;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Puerto de salida: tipos, marcadores permitidos y versiones de plantillas. */
public interface PlantillaRepositoryPort {

    /** Tipos administrables (sin los internos), ordenados por nombre. */
    List<TipoNotificacion> listarTipos();

    /** Cualquier tipo, interno o no. */
    Optional<TipoNotificacion> buscarTipo(String tipo);

    List<Marcador> marcadores(String tipo);

    Map<String, List<Marcador>> marcadoresPorTipo();

    /** Historial de un tipo, de la mas reciente a la mas antigua. */
    List<VersionPlantilla> versiones(String tipo, String idioma);

    Map<String, List<VersionPlantilla>> versionesPorTipo(String idioma);

    Optional<VersionPlantilla> activa(String tipo, String idioma);

    Optional<VersionPlantilla> version(String tipo, String idioma, int version);

    /** Crea la version siguiente y la deja activa (desactiva la anterior), de forma atomica. */
    VersionPlantilla crearYActivar(String tipo, String idioma, String asunto, String cuerpo, String creadoPor, Instant ahora);

    /** @return la version activada, o vacio si no existe. */
    Optional<VersionPlantilla> activar(String tipo, String idioma, int version);
}
