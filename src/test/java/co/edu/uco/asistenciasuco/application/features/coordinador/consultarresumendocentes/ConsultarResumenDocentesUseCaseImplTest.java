package co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes.primaryports.dto.ResumenDocentesProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes.primaryports.interactor.ConsultarResumenDocentesInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes.usecase.domain.ResumenDocentesProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes.usecase.impl.ConsultarResumenDocentesUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ResumenDocentesProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResumenDocentesProgramaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * HU095: consolidado del directorio docente del programa (RD-01..RD-05). MEJORAS-SERGIO-2026-11-18: la DB entrega una sola fila
 * (activos e inactivos); ya no hay desglose por tipo de vinculacion.
 */
class ConsultarResumenDocentesUseCaseImplTest {

    private static final UUID ACTOR = UUID.randomUUID();

    private final InstitutionalScopePort scope = mock(InstitutionalScopePort.class);
    private final ResumenDocentesProgramaQueryPort port = mock(ResumenDocentesProgramaQueryPort.class);
    private final ConsultarResumenDocentesUseCaseImpl useCase = new ConsultarResumenDocentesUseCaseImpl(scope, port);

    @Test
    void RD01_total_es_la_suma_de_activos_e_inactivos() {
        when(scope.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(UUID.randomUUID()));
        when(port.consultarResumen(ACTOR)).thenReturn(new ResumenDocentesProgramaProjection(3, 1));

        final ResumenDocentesProgramaDomain resumen = useCase.execute(ACTOR);

        assertEquals(4, resumen.totalDocentes());
        assertEquals(3, resumen.totalActivos());
        assertEquals(1, resumen.totalInactivos());
    }

    @Test
    void RD02_programa_sin_docentes_o_sin_fila_devuelve_ceros() {
        when(scope.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(UUID.randomUUID()));
        when(port.consultarResumen(ACTOR)).thenReturn(ResumenDocentesProgramaProjection.vacio());
        assertEquals(new ResumenDocentesProgramaDomain(0, 0, 0), useCase.execute(ACTOR));

        when(port.consultarResumen(ACTOR)).thenReturn(null);
        assertEquals(new ResumenDocentesProgramaDomain(0, 0, 0), useCase.execute(ACTOR));
    }

    @Test
    void RD03_sin_programa_a_cargo_responde_403_sin_consultar() {
        when(scope.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(ACTOR));
        verifyNoInteractions(port);
    }

    @Test
    void RD04_interactor_mapea_a_dto_con_los_totales() {
        when(scope.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(UUID.randomUUID()));
        when(port.consultarResumen(ACTOR)).thenReturn(new ResumenDocentesProgramaProjection(1, 2));

        final ResumenDocentesProgramaDTO dto = new ConsultarResumenDocentesInteractor(useCase).execute(ACTOR);

        assertEquals(new ResumenDocentesProgramaDTO(3, 1, 2), dto);
    }

    @Test
    void RD05_constructores_rechazan_dependencias_nulas() {
        assertThrows(NullPointerException.class, () -> new ConsultarResumenDocentesUseCaseImpl(null, port));
        assertThrows(NullPointerException.class, () -> new ConsultarResumenDocentesUseCaseImpl(scope, null));
        assertThrows(NullPointerException.class, () -> new ConsultarResumenDocentesInteractor(null));
    }
}
