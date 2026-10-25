package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.math.BigDecimal;
import java.util.UUID;

public record AforoGrupoProjection(
        UUID idGrupo,
        int codigoGrupo,
        UUID idAsignatura,
        String nombreAsignatura,
        UUID idPrograma,
        String nombrePrograma,
        int cupoMaximo,
        int matriculadosActivos,
        int cuposDisponibles,
        BigDecimal tasaAforo,
        boolean grupoEstaHabilitado
) {
}
