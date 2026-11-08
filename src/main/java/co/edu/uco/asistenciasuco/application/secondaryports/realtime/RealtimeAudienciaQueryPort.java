package co.edu.uco.asistenciasuco.application.secondaryports.realtime;

import java.util.UUID;

/** CULM-001 C.1 (F-TRX-006): audiencias de un usuario sobre un grupo, calculadas en la DB ({@code usp_consultar_audiencias_realtime_grupo}). */
public interface RealtimeAudienciaQueryPort {

    Audiencias consultar(UUID usuarioId, UUID grupoId);

    record Audiencias(boolean esDocenteTitular, boolean esEstudianteActivo, boolean esAmbitoLectura) {
    }
}
