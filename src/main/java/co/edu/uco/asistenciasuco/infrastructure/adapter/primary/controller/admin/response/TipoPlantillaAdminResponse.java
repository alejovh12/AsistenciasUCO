package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.admin.response;

import java.time.Instant;
import java.util.List;

public record TipoPlantillaAdminResponse(
        String tipo,
        String idioma,
        int versionActiva,
        String asuntoActivo,
        String cuerpoActivo,
        Instant actualizadoEn,
        List<MarcadorPlantillaResponse> marcadores,
        List<VersionPlantillaResponse> versiones,
        String descripcion
) {
    public TipoPlantillaAdminResponse {
        marcadores = marcadores == null ? List.of() : List.copyOf(marcadores);
        versiones = versiones == null ? List.of() : List.copyOf(versiones);
        descripcion = descripcion == null ? "" : descripcion;
    }

    /** Sin descripcion: conserva la forma anterior del DTO (campo nuevo solo al final, compatible hacia atras). */
    public TipoPlantillaAdminResponse(
            final String tipo,
            final String idioma,
            final int versionActiva,
            final String asuntoActivo,
            final String cuerpoActivo,
            final Instant actualizadoEn,
            final List<MarcadorPlantillaResponse> marcadores,
            final List<VersionPlantillaResponse> versiones
    ) {
        this(tipo, idioma, versionActiva, asuntoActivo, cuerpoActivo, actualizadoEn, marcadores, versiones, "");
    }
}
