package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.core;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarAsistenciasPorGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciaAutonomaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciasSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ResolverSolicitudRevisionAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.SolicitarRevisionAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.AsistenciaRepositoryProjection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/** JPA-Q-010/011: la query va a la persistencia seleccionada y TODOS los commands a JDBC. */
class AsistenciaRepositoryHybridSqlServerAdapterTest {

    private final AsistenciaRepositoryPort jdbcCommands = mock(AsistenciaRepositoryPort.class);
    private final AsistenciaQueryPersistence query = mock(AsistenciaQueryPersistence.class);
    private final AsistenciaRepositoryHybridSqlServerAdapter adapter =
            new AsistenciaRepositoryHybridSqlServerAdapter(jdbcCommands, query);

    @Test
    void query_delega_solo_a_la_persistencia_de_query_y_nunca_a_jdbc() {
        final ConsultarAsistenciasPorGrupoRepositoryDTO dto =
                new ConsultarAsistenciasPorGrupoRepositoryDTO(UUID.randomUUID(), UUID.randomUUID());
        final List<AsistenciaRepositoryProjection> expected = List.of();
        when(query.consultarAsistenciasPorGrupo(dto)).thenReturn(expected);

        assertSame(expected, adapter.consultarAsistenciasPorGrupo(dto));

        verifyNoInteractions(jdbcCommands);
    }

    @Test
    void todos_los_commands_delegan_a_jdbc_y_no_tocan_la_query_seleccionada() {
        final RegistrarAsistenciaRepositoryDTO individual = mock(RegistrarAsistenciaRepositoryDTO.class);
        final RegistrarAsistenciasSesionRepositoryDTO lote = mock(RegistrarAsistenciasSesionRepositoryDTO.class);
        final RegistrarAsistenciaAutonomaRepositoryDTO autonoma = mock(RegistrarAsistenciaAutonomaRepositoryDTO.class);
        final SolicitarRevisionAsistenciaRepositoryDTO solicitud = mock(SolicitarRevisionAsistenciaRepositoryDTO.class);
        final ResolverSolicitudRevisionAsistenciaRepositoryDTO resolucion =
                mock(ResolverSolicitudRevisionAsistenciaRepositoryDTO.class);

        adapter.registrarAsistencia(individual);
        adapter.registrarAsistenciasSesion(lote);
        adapter.registrarAsistenciaAutonoma(autonoma);
        adapter.solicitarRevisionAsistencia(solicitud);
        adapter.resolverSolicitudRevisionAsistencia(resolucion);

        verify(jdbcCommands).registrarAsistencia(individual);
        verify(jdbcCommands).registrarAsistenciasSesion(lote);
        verify(jdbcCommands).registrarAsistenciaAutonoma(autonoma);
        verify(jdbcCommands).solicitarRevisionAsistencia(solicitud);
        verify(jdbcCommands).resolverSolicitudRevisionAsistencia(resolucion);
        verifyNoMoreInteractions(jdbcCommands);
        verifyNoInteractions(query);
    }

    @Test
    void exige_colaboradores() {
        assertThrows(NullPointerException.class, () -> new AsistenciaRepositoryHybridSqlServerAdapter(null, query));
        assertThrows(NullPointerException.class,
                () -> new AsistenciaRepositoryHybridSqlServerAdapter(jdbcCommands, null));
    }
}
