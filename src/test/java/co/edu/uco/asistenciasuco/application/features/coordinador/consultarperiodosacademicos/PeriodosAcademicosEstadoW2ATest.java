package co.edu.uco.asistenciasuco.application.features.coordinador.consultarperiodosacademicos;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarperiodosacademicos.primaryports.interactor.ConsultarPeriodosAcademicosInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarperiodosacademicos.usecase.impl.ConsultarPeriodosAcademicosUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.PeriodoAcademicoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PeriodoAcademicoProjection;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** CIERRE-W2A (punto 4): el periodo siempre trae el estado de la BD y se puede filtrar por estado. */
class PeriodosAcademicosEstadoW2ATest {

    private static PeriodoAcademicoProjection periodo(final String nombre, final String estado) {
        return new PeriodoAcademicoProjection(UUID.randomUUID(), UUID.randomUUID(), "UCO", nombre, "1",
                LocalDate.of(2026, 1, 20), LocalDate.of(2026, 6, 5), 2026, estado);
    }

    private final PeriodoAcademicoQueryPort port = mock(PeriodoAcademicoQueryPort.class);
    private final ConsultarPeriodosAcademicosInteractor interactor =
            new ConsultarPeriodosAcademicosInteractor(new ConsultarPeriodosAcademicosUseCaseImpl(port));

    @Test
    void el_dto_conserva_el_estado_resuelto_en_la_base() {
        when(port.consultarPeriodosAcademicos()).thenReturn(List.of(periodo("2026-1", "CERRADO"), periodo("2026-2", "ACTIVO"),
                periodo("2027-1", "PLANEACION")));

        final var periodos = interactor.execute();

        assertEquals(List.of("CERRADO", "ACTIVO", "PLANEACION"), periodos.stream().map(p -> p.estado()).toList());
    }

    @Test
    void con_estado_consulta_a_la_base_con_el_estado_normalizado() {
        when(port.consultarPeriodosAcademicosPorEstado("ACTIVO")).thenReturn(List.of(periodo("2026-2", "ACTIVO")));

        final var periodos = interactor.execute("  activo ");

        assertEquals(1, periodos.size());
        assertEquals("ACTIVO", periodos.getFirst().estado());
        verify(port, never()).consultarPeriodosAcademicos();
    }

    @Test
    void sin_estado_o_en_blanco_devuelve_todos() {
        when(port.consultarPeriodosAcademicos()).thenReturn(List.of(periodo("2026-1", "CERRADO")));

        assertEquals(1, interactor.execute((String) null).size());
        assertEquals(1, interactor.execute("   ").size());
        verify(port, never()).consultarPeriodosAcademicosPorEstado(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void un_estado_desconocido_no_devuelve_filas() {
        when(port.consultarPeriodosAcademicosPorEstado("INVENTADO")).thenReturn(List.of());

        assertEquals(0, interactor.execute("inventado").size());
    }
}
