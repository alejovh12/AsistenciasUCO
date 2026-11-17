package co.edu.uco.asistenciasuco.application.secondaryports.repository;

import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;

import java.util.List;
import java.util.UUID;

/**
 * CIERRE-W3M (punto 1): matriz estudiantes x sesiones del grupo del docente titular, ya cruzada en la base
 * ({@code usp_consultar_matriz_asistencia_grupo_docente_paginado}). Pagina y busqueda se aplican a los estudiantes; el ambito
 * (titularidad) lo valida el SP y un grupo ajeno o inexistente se traduce a la excepcion de catalogo correspondiente.
 */
public interface MatrizAsistenciaQueryPort {

    /** Estado de un estudiante en una sesion segun {@code uv_asistencia_sesion_horas} (o SIN_REGISTRO / NO_DISPONIBLE). */
    record CeldaMatriz(UUID idSesion, String codigoEstado, int minutosAsistidos, int minutosSesion) {
    }

    /** Una matricula de la pagina con una celda por sesion del grupo (orden por numero de sesion). */
    record FilaMatriz(UUID idEstudianteGrupo, UUID idEstudiante, String nombreCompleto, String documento,
                      String codigoEstadoEstudiante, List<CeldaMatriz> celdas) {

        public FilaMatriz {
            celdas = celdas == null ? List.of() : List.copyOf(celdas);
        }
    }

    PaginaResultado<FilaMatriz> consultarMatrizGrupoDocente(UUID grupoId, int numeroPagina, int tamanoPagina, String busqueda,
                                                           UUID usuarioEjecutor);
}
