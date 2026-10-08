package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import java.util.UUID;

public interface PlanEstudioCommandPort {

    void registrarOActualizarPlanEstudio(UUID idPlanEstudio, UUID idPrograma, Integer inp);
}
