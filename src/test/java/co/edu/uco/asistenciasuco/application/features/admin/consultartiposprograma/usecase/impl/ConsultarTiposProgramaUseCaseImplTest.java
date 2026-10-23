package co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.usecase.domain.TipoProgramaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.TipoProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.TipoProgramaProjection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultarTiposProgramaUseCaseImplTest {

    private static final UUID ACTIVO = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID INACTIVO = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final TipoProgramaQueryPort queryPort = mock(TipoProgramaQueryPort.class);
    private final ConsultarTiposProgramaUseCaseImpl useCase = new ConsultarTiposProgramaUseCaseImpl(queryPort);

    private void dadoUnCatalogoMixto() {
        when(queryPort.consultarTiposPrograma()).thenReturn(List.of(
                new TipoProgramaProjection(ACTIVO, "PRE", "pregrado", true),
                new TipoProgramaProjection(INACTIVO, "ESP", "especializacion", false)));
    }

    @Test
    void administrador_recibe_todos_los_tipos_incluidos_inactivos() {
        dadoUnCatalogoMixto();

        final List<TipoProgramaDomain> result = useCase.execute(false);

        assertEquals(List.of(
                new TipoProgramaDomain(ACTIVO, "PRE", "pregrado", true),
                new TipoProgramaDomain(INACTIVO, "ESP", "especializacion", false)), result);
    }

    @Test
    void consumo_transversal_recibe_solo_los_activos() {
        dadoUnCatalogoMixto();

        assertEquals(List.of(new TipoProgramaDomain(ACTIVO, "PRE", "pregrado", true)), useCase.execute(true));
    }

    @Test
    void puerto_obligatorio() {
        assertThrows(NullPointerException.class, () -> new ConsultarTiposProgramaUseCaseImpl(null));
    }
}
