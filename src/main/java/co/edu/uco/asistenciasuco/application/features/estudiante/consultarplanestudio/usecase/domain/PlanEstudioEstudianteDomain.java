package co.edu.uco.asistenciasuco.application.features.estudiante.consultarplanestudio.usecase.domain;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/** HU018: plan de estudios activo de un programa del estudiante; los totales se derivan de sus semestres. */
public record PlanEstudioEstudianteDomain(UUID idPlanEstudio, Integer inp, UUID idPrograma, String nombrePrograma,
                                          List<SemestrePlanEstudianteDomain> semestres) {

    public PlanEstudioEstudianteDomain {
        semestres = List.copyOf(semestres);
    }

    /** El semestre mas alto del plan: la misma regla (MAX) del coordinador y del decano. */
    public int totalSemestres() {
        return semestres.stream().mapToInt(s -> s.numeroSemestre() == null ? 0 : s.numeroSemestre()).max().orElse(0);
    }

    public int totalAsignaturas() {
        return semestres.stream().mapToInt(SemestrePlanEstudianteDomain::totalAsignaturas).sum();
    }

    public int totalCreditos() {
        return semestres.stream().mapToInt(SemestrePlanEstudianteDomain::totalCreditos).sum();
    }

    public int asignaturasCursando() {
        return (int) cursadas().count();
    }

    public int creditosCursando() {
        return cursadas().mapToInt(a -> a.creditos() == null ? 0 : a.creditos()).sum();
    }

    private Stream<AsignaturaPlanEstudianteDomain> cursadas() {
        return semestres.stream().flatMap(s -> s.asignaturas().stream()).filter(AsignaturaPlanEstudianteDomain::cursando);
    }
}
