package co.edu.uco.asistenciasuco.application.features.asistencia.common.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * CULM-001 C.1 (CONTRACT 3.2 "marca", HU026C/HU041): alertas vigentes de inasistencia y estado de la revision de cancelacion de
 * la matricula en la sabana del docente. {@code revision} es {@code null} si no hay revision.
 */
public record MarcaInasistenciaDTO(boolean advertenciaVigente, Instant fechaAdvertenciaUtc, boolean perdidaVigente,
                                   Instant fechaPerdidaUtc, RevisionMarcaDTO revision) {

    public record RevisionMarcaDTO(UUID id, String estado, String nombreEstado, boolean requiereAccionDocente,
                                   String perfilResolutorActual, List<String> accionesPermitidas) {

        public RevisionMarcaDTO {
            accionesPermitidas = accionesPermitidas == null ? List.of() : List.copyOf(accionesPermitidas);
        }
    }
}
