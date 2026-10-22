package co.edu.uco.asistenciasuco.application.features.tipoprograma.consultarcatalogosprograma.usecase;

import co.edu.uco.asistenciasuco.application.features.tipoprograma.consultarcatalogosprograma.usecase.domain.CatalogoProgramaDomain;
import java.util.List;

public interface ConsultarCatalogosProgramaUseCase {

    List<CatalogoProgramaDomain> tiposPrograma();

    List<CatalogoProgramaDomain> modalidades();

    List<CatalogoProgramaDomain> jornadas();
}
