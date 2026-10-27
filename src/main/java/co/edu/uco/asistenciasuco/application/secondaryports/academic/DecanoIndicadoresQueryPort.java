package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AforoGrupoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ComparativaProgramaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.KpisDecanoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PaginaAforoGruposProjection;
import java.util.List;
import java.util.UUID;

/** Consultas de KPIs, comparativa y aforo de la facultad del decano: el calculo y el ambito viven en la DB. */
public interface DecanoIndicadoresQueryPort {

    KpisDecanoProjection consultarKpis(UUID actorUsuarioId, UUID idPeriodoAcademico);

    List<ComparativaProgramaProjection> consultarComparativa(UUID actorUsuarioId, UUID idPeriodoAcademico);

    PaginaAforoGruposProjection consultarAforo(UUID actorUsuarioId, UUID idPeriodoAcademico, UUID idPrograma, boolean soloHabilitados, int pagina, int tamano);
}
