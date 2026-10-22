package co.edu.uco.asistenciasuco.application.features.coordinador.common.domain;

import java.util.List;

public record DocentesProgramaPaginaDomain(List<DocenteProgramaDomain> elementos, long total) {
}
