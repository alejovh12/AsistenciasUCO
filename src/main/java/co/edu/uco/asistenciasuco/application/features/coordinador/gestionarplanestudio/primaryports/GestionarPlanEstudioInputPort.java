package co.edu.uco.asistenciasuco.application.features.coordinador.gestionarplanestudio.primaryports;

import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarplanestudio.primaryports.dto.GuardarPlanEstudioDTO;

public interface GestionarPlanEstudioInputPort {

    void guardar(GuardarPlanEstudioDTO dto);
}
