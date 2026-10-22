package co.edu.uco.asistenciasuco.application.features.tipovinculacion.consultartiposvinculacion.usecase;

import co.edu.uco.asistenciasuco.application.features.tipovinculacion.consultartiposvinculacion.usecase.domain.TipoVinculacionDomain;

import java.util.List;

public interface ConsultarTiposVinculacionUseCase {

    List<TipoVinculacionDomain> execute();
}
