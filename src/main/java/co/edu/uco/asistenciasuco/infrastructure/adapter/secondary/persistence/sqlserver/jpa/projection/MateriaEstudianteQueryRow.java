package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection;

import java.util.UUID;

/** Fila JPQL de materias del estudiante (LB-008 JPA-05). */
public record MateriaEstudianteQueryRow(UUID idAsignatura, String nombreAsignatura, UUID idGrupo,
                                        String nombreGrupo) {
}
