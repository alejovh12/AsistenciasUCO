package co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.primaryports.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AforoGrupoDTO(
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
