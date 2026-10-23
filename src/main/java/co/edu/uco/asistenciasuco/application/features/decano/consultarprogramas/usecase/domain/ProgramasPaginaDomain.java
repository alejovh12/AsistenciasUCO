package co.edu.uco.asistenciasuco.application.features.decano.consultarprogramas.usecase.domain;

import co.edu.uco.asistenciasuco.application.features.decano.common.domain.ProgramaAcademicoDomain;
import java.util.List;

public record ProgramasPaginaDomain(List<ProgramaAcademicoDomain> programas, int totalRegistros) {
}
