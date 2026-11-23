package co.edu.uco.asistenciasuco.application.features.admin.consultarcatalogossistema.primaryports;

import co.edu.uco.asistenciasuco.application.features.admin.consultarcatalogossistema.primaryports.dto.CategoriaJustificacionCatalogoDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcatalogossistema.primaryports.dto.MotivoCancelacionCatalogoDTO;

import java.util.List;

/** Consulta de los catalogos definidos por el sistema (categorias de justificacion y motivos de cancelacion de sesion); el ADMINISTRADOR solo los lee. */
public interface ConsultarCatalogosSistemaInputPort {

    List<CategoriaJustificacionCatalogoDTO> consultarCategoriasJustificacion();

    List<MotivoCancelacionCatalogoDTO> consultarMotivosCancelacion();
}
