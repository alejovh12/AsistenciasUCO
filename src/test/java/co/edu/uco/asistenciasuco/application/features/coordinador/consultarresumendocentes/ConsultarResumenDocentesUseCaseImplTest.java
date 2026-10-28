package co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes.primaryports.dto.ResumenDocentesProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes.primaryports.interactor.ConsultarResumenDocentesInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes.usecase.domain.ResumenDocentesProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarresumendocentes.usecase.impl.ConsultarResumenDocentesUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.ResumenDocentesProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResumenTipoVinculacionProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** HU095: consolidado del directorio docente del programa (RD-01..RD-05). */
class ConsultarResumenDocentesUseCaseImplTest {

    private static final UUID ACTOR = UUID.randomUUID();

    private final InstitutionalScopePort scope = mock(InstitutionalScopePort.class);
    private final ResumenDocentesProgramaQueryPort port = mock(ResumenDocentesProgramaQueryPort.class);
    private final ConsultarResumenDocentesUseCaseImpl useCase = new ConsultarResumenDocentesUseCaseImpl(scope, port);

    @Test
    void RD01_suma_activos_e_inactivos_de_todos_los_tipos() {
        when(scope.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(UUID.randomUUID()));
        when(port.consultarResumen(ACTOR)).thenReturn(List.of(
                new ResumenTipoVinculacionProjection(UUID.randomUUID(), "CAT", "Catedra", 2, 1),
                new ResumenTipoVinculacionProjection(UUID.randomUUID(), "PLA", "Planta", 1, 0),
                new ResumenTipoVinculacionProjection(UUID.randomUUID(), "OCA", "Ocasional", 0, 0)));

        final ResumenDocentesProgramaDomain resumen = useCase.execute(ACTOR);

        assertEquals(4, resumen.totalDocentes());
        assertEquals(3, resumen.totalActivos());
        assertEquals(1, resumen.totalInactivos());
        assertEquals(3, resumen.porTipoVinculacion().size());
        assertEquals(3, resumen.porTipoVinculacion().getFirst().total());
    }

    @Test
    void RD02_programa_sin_docentes_devuelve_ceros() {
        when(scope.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(UUID.randomUUID()));
        when(port.consultarResumen(ACTOR)).thenReturn(List.of());

        final ResumenDocentesProgramaDomain resumen = useCase.execute(ACTOR);

        assertEquals(0, resumen.totalDocentes());
        assertTrue(resumen.porTipoVinculacion().isEmpty());
    }

    @Test
    void RD03_sin_programa_a_cargo_responde_403_sin_consultar() {
        when(scope.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(ACTOR));
        verifyNoInteractions(port);
    }

    @Test
    void RD04_interactor_mapea_a_dto_con_total_por_tipo() {
        when(scope.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(UUID.randomUUID()));
        when(port.consultarResumen(ACTOR)).thenReturn(List.of(
                new ResumenTipoVinculacionProjection(UUID.randomUUID(), "MTI", "Medio tiempo", 1, 2)));

        final ResumenDocentesProgramaDTO dto = new ConsultarResumenDocentesInteractor(useCase).execute(ACTOR);

        assertEquals(3, dto.totalDocentes());
        assertEquals("MTI", dto.porTipoVinculacion().getFirst().codigoTipoVinculacion());
        assertEquals(1, dto.porTipoVinculacion().getFirst().totalActivos());
        assertEquals(2, dto.porTipoVinculacion().getFirst().totalInactivos());
        assertEquals(3, dto.porTipoVinculacion().getFirst().total());
    }

    @Test
    void RD05_constructores_rechazan_dependencias_nulas() {
        assertThrows(NullPointerException.class, () -> new ConsultarResumenDocentesUseCaseImpl(null, port));
        assertThrows(NullPointerException.class, () -> new ConsultarResumenDocentesUseCaseImpl(scope, null));
        assertThrows(NullPointerException.class, () -> new ConsultarResumenDocentesInteractor(null));
        assertTrue(new ResumenDocentesProgramaDomain(0, 0, 0, null).porTipoVinculacion().isEmpty());
    }
}
