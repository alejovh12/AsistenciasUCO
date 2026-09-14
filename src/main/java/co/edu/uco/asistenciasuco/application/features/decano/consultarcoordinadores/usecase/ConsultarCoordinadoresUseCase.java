package co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.usecase;

import co.edu.uco.asistenciasuco.application.features.decano.consultarcoordinadores.usecase.domain.CoordinadorDomain;

import java.util.List;
import java.util.UUID;

public interface ConsultarCoordinadoresUseCase { List<CoordinadorDomain> execute(UUID actorUsuarioId); }
