package co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.usecase.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record AforoGrupoDomain(
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
