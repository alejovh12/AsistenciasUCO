package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

/** Filtro del directorio. {@code idDocente} e {@code idUsuario} sirven para releer un vinculo concreto. */
public record DocentesProgramaFiltro(
        UUID idPrograma,
        int pagina,
        int tamano,
        String busqueda,
        Boolean estaActivo,
        UUID idTipoVinculacion,
        UUID idDocente,
        UUID idUsuario
) {
}
