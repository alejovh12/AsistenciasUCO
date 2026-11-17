package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SesionMateriaEstudianteProjection;

import java.util.UUID;

public interface SesionMateriaEstudianteQueryPort {

    /**
     * Sesiones de la asignatura del estudiante autenticado (CULM-001 D.1): el ambito (matricula inscrita del usuario),
     * la paginacion y la fecha limite de justificacion se resuelven dentro del procedimiento almacenado.
     */
    default PaginaResultado<SesionMateriaEstudianteProjection> consultarSesionesMateria(final UUID usuarioEjecutor, final UUID idAsignatura,
                                                                                         final int numeroPagina, final int tamanoPagina) {
        return consultarSesionesMateria(usuarioEjecutor, idAsignatura, null, numeroPagina, tamanoPagina);
    }

    /**
     * CIERRE-W2B: igual que la anterior con el grupo opcional ({@code null} = todos los grupos inscritos de la asignatura).
     * El orden (inicio, numero, id) y el total de la pagina los garantiza el procedimiento almacenado.
     */
    PaginaResultado<SesionMateriaEstudianteProjection> consultarSesionesMateria(UUID usuarioEjecutor, UUID idAsignatura, UUID idGrupo,
                                                                                int numeroPagina, int tamanoPagina);
}
