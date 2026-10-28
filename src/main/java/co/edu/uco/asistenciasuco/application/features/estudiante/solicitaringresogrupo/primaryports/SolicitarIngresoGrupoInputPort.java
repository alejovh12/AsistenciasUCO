package co.edu.uco.asistenciasuco.application.features.estudiante.solicitaringresogrupo.primaryports;

import co.edu.uco.asistenciasuco.application.features.estudiante.solicitaringresogrupo.primaryports.dto.SolicitarIngresoGrupoDTO;

import java.util.UUID;

public interface SolicitarIngresoGrupoInputPort {

    /** @return id de la solicitud creada (PENDIENTE). */
    UUID execute(SolicitarIngresoGrupoDTO dto);
}
