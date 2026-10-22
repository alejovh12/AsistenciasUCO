package co.edu.uco.asistenciasuco.application.features.decano.consultargrupos.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.usecase.entity.GrupoEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.GrupoFacultadQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.GrupoRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Listado de grupos del decano: solo los de su facultad. El backend no usa las vistas {@code uv_auth_*}, asi que el
 * ambito se fuerza aqui con {@link InstitutionalScopePort}. {@code GET /grupos} (institucional) sigue sin abrirse al decano.
 */
class ConsultarGruposDecanoUseCaseImplTest {

    private static final UUID DECANO = UUID.randomUUID();
    private static final UUID FACULTAD = UUID.randomUUID();

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final GrupoFacultadQueryPort queryPort = mock(GrupoFacultadQueryPort.class);
    private final ConsultarGruposDecanoUseCaseImpl useCase = new ConsultarGruposDecanoUseCaseImpl(scopePort, queryPort);

    private static GrupoRepositoryProjection grupo(final String codigo) {
        return new GrupoRepositoryProjection(UUID.randomUUID(), codigo, "Grupo " + codigo, UUID.randomUUID(), "Algebra",
                UUID.randomUUID(), 35, 20, 15, true, LocalDate.of(2026, 1, 15), LocalDate.of(2026, 6, 15));
    }

    @Test
    void consulta_los_grupos_de_la_facultad_del_decano() {
        when(scopePort.findFacultadIdByDecanoUsuario(DECANO)).thenReturn(Optional.of(FACULTAD));
        when(queryPort.consultarGruposPorFacultad(FACULTAD)).thenReturn(List.of(grupo("G1"), grupo("G2")));

        final List<GrupoEntity> resultado = useCase.execute(DECANO);

        assertEquals(2, resultado.size());
        assertEquals("G1", resultado.getFirst().getCodigo());
        verify(queryPort).consultarGruposPorFacultad(FACULTAD);
    }

    @Test
    void sin_facultad_de_decano_se_rechaza_sin_consultar() {
        when(scopePort.findFacultadIdByDecanoUsuario(DECANO)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(DECANO));

        verifyNoInteractions(queryPort);
    }

    @Test
    void un_actor_nulo_es_error_de_programacion() {
        assertThrows(CrosscuttingException.class, () -> useCase.execute(null));
    }
}
