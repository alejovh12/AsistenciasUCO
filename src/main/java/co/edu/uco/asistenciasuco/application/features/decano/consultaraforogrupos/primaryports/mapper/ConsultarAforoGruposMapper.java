package co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.primaryports.dto.ConsultarAforoGruposDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.usecase.domain.ConsultarAforoGruposDomain;
import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.primaryports.dto.AforoGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.primaryports.dto.AforoGruposPaginaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.usecase.domain.AforoGruposPaginaDomain;
import java.util.List;

public final class ConsultarAforoGruposMapper {

    private ConsultarAforoGruposMapper() {
    }

    public static ConsultarAforoGruposDomain toDomain(final ConsultarAforoGruposDTO dto) {
        return new ConsultarAforoGruposDomain(dto.actorUsuarioId(), dto.idPeriodoAcademico(), dto.idPrograma(), dto.soloHabilitados(), dto.pagina(), dto.tamano());
    }

    public static AforoGruposPaginaDTO toDto(final AforoGruposPaginaDomain d) {
        final List<AforoGrupoDTO> grupos = d.grupos().stream()
                .map(g -> new AforoGrupoDTO(g.idGrupo(), g.codigoGrupo(), g.idAsignatura(), g.nombreAsignatura(), g.idPrograma(), g.nombrePrograma(), g.cupoMaximo(), g.matriculadosActivos(), g.cuposDisponibles(), g.tasaAforo(), g.grupoEstaHabilitado()))
                .toList();
        return new AforoGruposPaginaDTO(grupos, d.totalRegistros());
    }
}
