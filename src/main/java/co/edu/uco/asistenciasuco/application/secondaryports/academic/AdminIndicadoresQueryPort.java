package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ComparativaFacultadProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.KpisInstitucionProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PuntoTendenciaProjection;
import java.util.List;
import java.util.UUID;

/** Consultas de KPIs, comparativa de facultades y tendencia de la institucion del administrador: el calculo y el ambito viven en la DB. */
public interface AdminIndicadoresQueryPort {

    KpisInstitucionProjection consultarKpis(UUID actorUsuarioId, UUID idPeriodoAcademico);

    List<ComparativaFacultadProjection> consultarComparativa(UUID actorUsuarioId, UUID idPeriodoAcademico);

    List<PuntoTendenciaProjection> consultarTendencia(UUID actorUsuarioId, UUID idPeriodoAcademico, String granularidad);
}
