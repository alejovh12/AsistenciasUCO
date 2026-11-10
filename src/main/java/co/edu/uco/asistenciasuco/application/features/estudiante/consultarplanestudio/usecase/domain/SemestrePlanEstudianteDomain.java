package co.edu.uco.asistenciasuco.application.features.estudiante.consultarplanestudio.usecase.domain;

import java.util.List;
import java.util.UUID;

/** HU018: semestre del plan con sus asignaturas; los totales se derivan de la lista. */
public record SemestrePlanEstudianteDomain(UUID idSemestrePlanEstudio, Integer numeroSemestre,
                                           List<AsignaturaPlanEstudianteDomain> asignaturas) {

    public SemestrePlanEstudianteDomain {
        asignaturas = List.copyOf(asignaturas);
    }

    public int totalAsignaturas() {
        return asignaturas.size();
    }

    public int totalCreditos() {
        return asignaturas.stream().mapToInt(a -> a.creditos() == null ? 0 : a.creditos()).sum();
    }
}
