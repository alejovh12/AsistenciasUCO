package co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.usecase.domain;

import java.util.List;

public record BitacoraPaginaDomain(List<BitacoraSesionDomain> sesiones, int totalRegistros) {
}
