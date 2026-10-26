package co.edu.uco.asistenciasuco.application.features.coordinador.gestionarplanestudio.usecase;

import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarplanestudio.usecase.domain.PlanEstudioDomain;

import java.util.UUID;

public interface GestionarPlanEstudioUseCase {

    void guardar(PlanEstudioDomain domain);

    void cambiarEstado(UUID idPlanEstudio, UUID usuario);

    void agregarSemestre(UUID idPlanEstudio, UUID usuario);

    void quitarSemestre(UUID idPlanEstudio, Integer numeroSemestre, UUID usuario);
}
