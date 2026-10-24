package co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.usecase.domain;

import co.edu.uco.asistenciasuco.application.features.coordinador.common.domain.DocenteProgramaDomain;

import java.util.List;

public record DocentesProgramaPaginaDomain(List<DocenteProgramaDomain> elementos, long total) {
}
