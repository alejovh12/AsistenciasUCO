package co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.usecase;

import co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.usecase.domain.TipoProgramaDomain;

import java.util.List;

public interface ConsultarTiposProgramaUseCase {

    List<TipoProgramaDomain> execute(boolean soloActivos);
}
