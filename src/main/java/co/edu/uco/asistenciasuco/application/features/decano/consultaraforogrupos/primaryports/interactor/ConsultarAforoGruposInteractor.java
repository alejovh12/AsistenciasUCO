package co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.primaryports.ConsultarAforoGruposInputPort;
import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.primaryports.dto.ConsultarAforoGruposDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.primaryports.dto.AforoGruposPaginaDTO;

import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.primaryports.mapper.ConsultarAforoGruposMapper;
import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.usecase.ConsultarAforoGruposUseCase;
import java.util.Objects;

public class ConsultarAforoGruposInteractor implements ConsultarAforoGruposInputPort {

    private final ConsultarAforoGruposUseCase useCase;

    public ConsultarAforoGruposInteractor(final ConsultarAforoGruposUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public AforoGruposPaginaDTO execute(final ConsultarAforoGruposDTO dto) {
        return ConsultarAforoGruposMapper.toDto(useCase.execute(ConsultarAforoGruposMapper.toDomain(dto)));
    }
}
