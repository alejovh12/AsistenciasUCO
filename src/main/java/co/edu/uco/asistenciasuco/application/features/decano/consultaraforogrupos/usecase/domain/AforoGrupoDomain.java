package co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.usecase.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record AforoGrupoDomain(
        UUID idGrupo,
        int codigoGrupo,
        UUID idAsignatura,
        String nombreAsignatura,
        java.util.List<String> programas,   // CULM-001 B2NM: 1 fila por grupo con los nombres de sus programas
        int cupoMaximo,
        int matriculadosActivos,
        int cuposDisponibles,
        BigDecimal tasaAforo,
        boolean grupoEstaHabilitado
) {
}
