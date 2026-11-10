package co.edu.uco.asistenciasuco.application.features.estudiante.consultarplanestudio.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.estudiante.consultarplanestudio.primaryports.ConsultarPlanEstudioEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarplanestudio.primaryports.dto.AsignaturaPlanEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarplanestudio.primaryports.dto.PlanEstudioEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarplanestudio.primaryports.dto.SemestrePlanEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarplanestudio.usecase.ConsultarPlanEstudioEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarplanestudio.usecase.domain.AsignaturaPlanEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarplanestudio.usecase.domain.PlanEstudioEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarplanestudio.usecase.domain.SemestrePlanEstudianteDomain;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** HU018: adapta el plan de estudios del estudiante al contrato de salida (los totales se calculan una sola vez, aqui). */
public final class ConsultarPlanEstudioEstudianteInteractor implements ConsultarPlanEstudioEstudianteInputPort {

    private final ConsultarPlanEstudioEstudianteUseCase useCase;

    public ConsultarPlanEstudioEstudianteInteractor(final ConsultarPlanEstudioEstudianteUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public List<PlanEstudioEstudianteDTO> execute(final UUID actorUsuarioId) {
        return useCase.execute(actorUsuarioId).stream().map(ConsultarPlanEstudioEstudianteInteractor::toDTO).toList();
    }

    private static PlanEstudioEstudianteDTO toDTO(final PlanEstudioEstudianteDomain plan) {
        return new PlanEstudioEstudianteDTO(plan.idPlanEstudio(), plan.inp(), plan.idPrograma(), plan.nombrePrograma(),
                plan.totalSemestres(), plan.totalAsignaturas(), plan.totalCreditos(), plan.asignaturasCursando(),
                plan.creditosCursando(),
                plan.semestres().stream().map(ConsultarPlanEstudioEstudianteInteractor::toDTO).toList());
    }

    private static SemestrePlanEstudianteDTO toDTO(final SemestrePlanEstudianteDomain semestre) {
        return new SemestrePlanEstudianteDTO(semestre.idSemestrePlanEstudio(), semestre.numeroSemestre(),
                semestre.totalAsignaturas(), semestre.totalCreditos(),
                semestre.asignaturas().stream().map(ConsultarPlanEstudioEstudianteInteractor::toDTO).toList());
    }

    private static AsignaturaPlanEstudianteDTO toDTO(final AsignaturaPlanEstudianteDomain asignatura) {
        return new AsignaturaPlanEstudianteDTO(asignatura.idAsignatura(), asignatura.codigo(), asignatura.nombre(),
                asignatura.creditos(), asignatura.tipoAsignatura(), asignatura.codigoCaracter(), asignatura.nombreCaracter(),
                asignatura.cursando(), asignatura.idGrupoCursando(), asignatura.nombreGrupoCursando());
    }
}
