package co.edu.uco.asistenciasuco.application.features.coordinador.consultarcandidatosdocente.usecase;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarcandidatosdocente.usecase.domain.DocenteCandidatoDomain;

import java.util.List;
import java.util.UUID;

public interface ConsultarCandidatosDocenteUseCase {

    List<DocenteCandidatoDomain> execute(UUID actorUsuarioId, String busqueda);
}
