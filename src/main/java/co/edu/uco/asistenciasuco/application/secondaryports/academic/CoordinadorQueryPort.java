package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CoordinadorFacultadProjection;

import java.util.List;
import java.util.UUID;

public interface CoordinadorQueryPort {

    /**
     * CIERRE-W1B: coordinadores de la facultad a cargo del decano ejecutor, con filtros opcionales y paginacion en el
     * procedimiento almacenado ({@code usp_consultar_coordinadores_facultad_paginado}). El procedimiento valida el
     * perfil DECANO (SEC_001) y limita las filas a su facultad. {@code busqueda} compara nombre, documento, correo y
     * programa; {@code estaActivo} filtra por el estado efectivo; {@code sinPrograma} deja solo los que no tienen
     * programa ({@code true}) o solo los que si ({@code false}).
     */
    Pagina<CoordinadorFacultadProjection> consultarCoordinadoresFacultad(UUID idUsuarioEjecutor, String busqueda,
                                                                         Boolean estaActivo, Boolean sinPrograma,
                                                                         int pagina, int tamano);

    record Pagina<T>(List<T> elementos, int total) {
    }
}
