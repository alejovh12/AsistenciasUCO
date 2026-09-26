package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsistenciasPorGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciaAutonomaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciasSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ResolverSolicitudRevisionAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.SolicitarRevisionAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.AsistenciaRepositoryProjection;

import java.util.List;
import java.util.Objects;

/**
 * Adapter hibrido (LB-002.1): la query de asistencia se resuelve con {@link AsistenciaQueryPersistence}
 * (JPA) y TODOS los commands delegan al adapter JDBC. Un solo provider por invocacion: no hay
 * lectura dual ni shadow traffic. La autorizacion sigue viviendo en Application.
 */
public final class AsistenciaRepositoryHybridSqlServerAdapter implements AsistenciaRepositoryPort {

    private final AsistenciaRepositoryPort commands;
    private final AsistenciaQueryPersistence query;

    public AsistenciaRepositoryHybridSqlServerAdapter(
            final AsistenciaRepositoryPort commands,
            final AsistenciaQueryPersistence query
    ) {
        this.commands = Objects.requireNonNull(commands, "El adapter JDBC de commands de asistencia es obligatorio.");
        this.query = Objects.requireNonNull(query, "La persistencia de la query de asistencia es obligatoria.");
    }

    @Override
    public void registrarAsistencia(final RegistrarAsistenciaRepositoryDTO dto) {
        commands.registrarAsistencia(dto);
    }

    @Override
    public void registrarAsistenciasSesion(final RegistrarAsistenciasSesionRepositoryDTO dto) {
        commands.registrarAsistenciasSesion(dto);
    }

    @Override
    public void registrarAsistenciaAutonoma(final RegistrarAsistenciaAutonomaRepositoryDTO dto) {
        commands.registrarAsistenciaAutonoma(dto);
    }

    @Override
    public List<AsistenciaRepositoryProjection> consultarAsistenciasPorGrupo(
            final ConsultarAsistenciasPorGrupoRepositoryDTO dto
    ) {
        return query.consultarAsistenciasPorGrupo(dto);
    }

    @Override
    public void solicitarRevisionAsistencia(final SolicitarRevisionAsistenciaRepositoryDTO dto) {
        commands.solicitarRevisionAsistencia(dto);
    }

    @Override
    public void resolverSolicitudRevisionAsistencia(final ResolverSolicitudRevisionAsistenciaRepositoryDTO dto) {
        commands.resolverSolicitudRevisionAsistencia(dto);
    }
}
