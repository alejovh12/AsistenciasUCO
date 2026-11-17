package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.baseline;



import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorCommandPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResultadoGestionCoordinadorProjection;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.repository.CoordinadorJpaRepository;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.Objects;
import java.util.UUID;

/**
 * Adaptador SQL Server de coordinadores.
 *
 * <p>COMMAND (alta de coordinador) delega en {@link CoordinadorJpaRepository} sobre {@code EntityManager}
 * (LB-008 JPA-03). La QUERY sigue en JDBC temporal hasta JPA-05.</p>
 */
public final class CoordinadorJdbcBaseline implements CoordinadorCommandPort {

    private final NamedParameterJdbcOperations jdbcOperations;
    private final CoordinadorJpaRepository commands;

    public CoordinadorJdbcBaseline(
            final NamedParameterJdbcOperations jdbcOperations,
            final CoordinadorJpaRepository commands
    ) {
        this.jdbcOperations = Objects.requireNonNull(jdbcOperations, "NamedParameterJdbcOperations es obligatorio.");
        this.commands = Objects.requireNonNull(commands, "El command JPA de coordinador es obligatorio.");
    }

    @Override
    public void crearCoordinador(
            final UUID idCoordinador,
            final String numeroIdentificacion,
            final String primerNombre,
            final String segundoNombre,
            final String primerApellido,
            final String segundoApellido,
            final String correo,
            final UUID idPrograma,
            final UUID idFacultad,
            final String password,
            final UUID usuarioEjecutor,
            final UUID idTipoIdIdentificacion
    ) {
        commands.crearCoordinador(idCoordinador, numeroIdentificacion, primerNombre, segundoNombre, primerApellido,
                segundoApellido, correo, idPrograma, idFacultad, password, usuarioEjecutor, idTipoIdIdentificacion);
    }

    // HU121/HU122: los comandos nuevos tambien delegan en el command JPA (no existe baseline JDBC de ellos).
    @Override
    public ResultadoGestionCoordinadorProjection actualizarCoordinador(
            final UUID idCoordinador, final String primerNombre, final String segundoNombre, final String primerApellido,
            final String segundoApellido, final String correo, final UUID idFacultad, final UUID usuarioEjecutor,
            final co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CorreccionDocumentoDTO documento
    ) {
        return commands.actualizarCoordinador(idCoordinador, primerNombre, segundoNombre, primerApellido,
                segundoApellido, correo, idFacultad, usuarioEjecutor, documento);
    }

    @Override
    public ResultadoGestionCoordinadorProjection cambiarEstadoCoordinador(
            final UUID idCoordinador, final boolean estaActivo, final UUID idFacultad, final UUID usuarioEjecutor
    ) {
        return commands.cambiarEstadoCoordinador(idCoordinador, estaActivo, idFacultad, usuarioEjecutor);
    }

    @Override
    public ResultadoCoordinadorConPrograma crearCoordinadorConPrograma(final CrearCoordinadorConProgramaCommand command) {
        return commands.crearCoordinadorConPrograma(command);
    }
}
