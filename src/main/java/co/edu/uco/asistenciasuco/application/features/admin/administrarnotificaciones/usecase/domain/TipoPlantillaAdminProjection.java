package co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain;

import java.time.Instant;
import java.util.List;

public record TipoPlantillaAdminProjection(
        String tipo,
        String idioma,
        int versionActiva,
        String asuntoActivo,
        String cuerpoActivo,
        Instant actualizadoEn,
        List<MarcadorPlantillaProjection> marcadores,
        List<VersionPlantillaProjection> versiones,
        String descripcion
) {
    public TipoPlantillaAdminProjection {
        marcadores = marcadores == null ? List.of() : List.copyOf(marcadores);
        versiones = versiones == null ? List.of() : List.copyOf(versiones);
        descripcion = descripcion == null ? "" : descripcion;
    }

    /** Sin descripcion (la que entrega el Servicio de Notificaciones para la pantalla de administracion). */
    public TipoPlantillaAdminProjection(
            final String tipo,
            final String idioma,
            final int versionActiva,
            final String asuntoActivo,
            final String cuerpoActivo,
            final Instant actualizadoEn,
            final List<MarcadorPlantillaProjection> marcadores,
            final List<VersionPlantillaProjection> versiones
    ) {
        this(tipo, idioma, versionActiva, asuntoActivo, cuerpoActivo, actualizadoEn, marcadores, versiones, "");
    }
}
