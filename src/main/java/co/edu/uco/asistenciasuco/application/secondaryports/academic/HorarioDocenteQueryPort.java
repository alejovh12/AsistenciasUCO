package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.HorarioDocenteProjection;

import java.util.List;
import java.util.UUID;

public interface HorarioDocenteQueryPort {

    List<HorarioDocenteProjection> consultarHorarioDocente(UUID idDocente);
}
