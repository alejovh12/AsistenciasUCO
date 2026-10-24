package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.FichaEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.KpisCoordinadorProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PaginaAlertasProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PaginaBitacoraProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResumenAsignaturaProjection;
import java.util.List;
import java.util.UUID;

/** Consultas de KPIs, alerta temprana, bitacora y ficha del coordinador: el calculo y el ambito viven en la DB. */
public interface CoordinadorIndicadoresQueryPort {

    KpisCoordinadorProjection consultarKpis(UUID actorUsuarioId, UUID idPeriodoAcademico);

    PaginaAlertasProjection consultarAlertas(UUID actorUsuarioId, UUID idPeriodoAcademico, String semaforo, String nombre, int pagina, int tamano);

    PaginaBitacoraProjection consultarBitacora(UUID actorUsuarioId, UUID idEstudiante, UUID idPeriodoAcademico, int pagina, int tamano);

    FichaEstudianteProjection consultarFicha(UUID actorUsuarioId, UUID idEstudiante);

    List<ResumenAsignaturaProjection> consultarResumenAsistencia(UUID actorUsuarioId, UUID idEstudiante, UUID idPeriodoAcademico);
}
