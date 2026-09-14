package co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.usecase;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.usecase.domain.EstudianteProgramaDomain;

import java.util.List; import java.util.UUID;

public interface ConsultarEstudiantesProgramaUseCase { List<EstudianteProgramaDomain> execute(UUID actorUsuarioId); }
