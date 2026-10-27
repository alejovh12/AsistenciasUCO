package co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.primaryports.dto;

public record ResumenCargaDocenteDTO(
        int totalGrupos,
        int totalPeriodos,
        int gruposVigentes,
        int estudiantesActivosVigentes
) {
}
