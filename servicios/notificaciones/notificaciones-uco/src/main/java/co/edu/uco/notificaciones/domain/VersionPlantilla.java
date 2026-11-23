package co.edu.uco.notificaciones.domain;

import java.time.Instant;
import java.util.UUID;

/** Version de una plantilla (tipo + idioma). Solo una por tipo e idioma esta activa. */
public record VersionPlantilla(
        UUID id,
        String tipo,
        String idioma,
        int version,
        String asunto,
        String cuerpo,
        boolean activa,
        String creadoPor,
        Instant creadoEn
) {
}
