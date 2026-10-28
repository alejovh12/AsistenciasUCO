package co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.usecase.domain;

public record ResumenCargaDocenteDomain(
        int totalGrupos,
        int totalPeriodos,
        int gruposVigentes,
        int estudiantesActivosVigentes
) {
}
