package co.edu.uco.notificaciones.application.model;

import co.edu.uco.notificaciones.domain.Marcador;
import co.edu.uco.notificaciones.domain.VersionPlantilla;

import java.time.Instant;
import java.util.List;

/** Tipo de plantilla con su descripcion (viene del servicio), marcadores permitidos e historial de versiones. */
public record TipoPlantillaAdmin(
        String tipo,
        String descripcion,
        String idioma,
        int versionActiva,
        String asuntoActivo,
        String cuerpoActivo,
        Instant actualizadoEn,
        List<Marcador> marcadores,
        List<VersionPlantilla> versiones
) {

    public TipoPlantillaAdmin {
        marcadores = marcadores == null ? List.of() : List.copyOf(marcadores);
        versiones = versiones == null ? List.of() : List.copyOf(versiones);
    }
}
