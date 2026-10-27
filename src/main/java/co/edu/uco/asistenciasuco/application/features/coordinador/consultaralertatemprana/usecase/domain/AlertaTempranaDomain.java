package co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.usecase.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record AlertaTempranaDomain(
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
