package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ProgramaAcademicoPaginaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ProgramaAcademicoProjection;
import java.util.Optional;
import java.util.UUID;

/** Consultas del programa academico: listado paginado con ambito en la DB y lectura puntual por facultad. */
public interface ProgramaAcademicoQueryPort {

    ProgramaAcademicoPaginaProjection consultar(ConsultaProgramas consulta);

    Optional<ProgramaAcademicoProjection> consultarPorId(UUID idPrograma, UUID idFacultad);

    record ConsultaProgramas(
            UUID actorUsuarioId,
            String nombre,
            Boolean estaActivo,
            UUID idTipoPrograma,
            int pagina,
            int tamano
    ) {
    }
}
