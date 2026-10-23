package co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.primaryports;

import co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.primaryports.dto.TipoProgramaDTO;

import java.util.List;

public interface ConsultarTiposProgramaInputPort {

    /** Catalogo completo, incluidos los inactivos (administracion). */
    List<TipoProgramaDTO> consultarTodos();

    /** Solo los tipos activos (consumo transversal). */
    List<TipoProgramaDTO> consultarActivos();
}
