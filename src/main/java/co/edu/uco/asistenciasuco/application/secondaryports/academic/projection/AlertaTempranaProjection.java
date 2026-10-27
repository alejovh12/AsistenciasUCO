package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.math.BigDecimal;
import java.util.UUID;

/** Una persona de usp_consultar_alerta_temprana_programa (HU066). */
public record AlertaTempranaProjection(
        UUID idEstudiante,
        String nombreCompleto,
        Integer numeroIdentificacion,
        String correo,
        int totalMatriculasActivas,
        BigDecimal porcentajeInasistencia,
        int faltasSinJustaCausa,
        String codigoPermanencia,
        String semaforo
) {
}
