package co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.primaryports.dto;

import java.util.UUID;

public record GuardarAsignaturaDTO(
        UUID idAsignatura,
        UUID idPlanEstudio,
        String codigo,
        String nombre,
        Integer creditos,
        Integer semestreNumero,
        String nombreArea,
        String nombreComponente
) {
}
