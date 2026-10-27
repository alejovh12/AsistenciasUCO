package co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.usecase.domain;

import java.util.List;

public record AlertasPaginaDomain(List<AlertaTempranaDomain> alertas, int totalRegistros) {
}
