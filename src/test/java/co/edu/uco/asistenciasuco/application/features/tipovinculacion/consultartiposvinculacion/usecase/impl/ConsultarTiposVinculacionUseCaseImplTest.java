package co.edu.uco.asistenciasuco.application.features.tipovinculacion.consultartiposvinculacion.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.tipovinculacion.consultartiposvinculacion.primaryports.dto.TipoVinculacionDTO;
import co.edu.uco.asistenciasuco.application.features.tipovinculacion.consultartiposvinculacion.primaryports.interactor.ConsultarTiposVinculacionInteractor;
import co.edu.uco.asistenciasuco.application.features.tipovinculacion.consultartiposvinculacion.usecase.domain.TipoVinculacionDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.TipoVinculacionQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.TipoVinculacionProjection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** HU090: catalogo de tipos de vinculacion activos. */
class ConsultarTiposVinculacionUseCaseImplTest {

    private final TipoVinculacionQueryPort queryPort = mock(TipoVinculacionQueryPort.class);
    private final ConsultarTiposVinculacionUseCaseImpl useCase = new ConsultarTiposVinculacionUseCaseImpl(queryPort);

    @Test
    void constructor_rechaza_puerto_nulo() {
        assertThrows(NullPointerException.class, () -> new ConsultarTiposVinculacionUseCaseImpl(null));
        assertThrows(NullPointerException.class, () -> new ConsultarTiposVinculacionInteractor(null));
    }

    @Test
    void T_090_01_devuelve_los_tipos_activos_mapeados_y_el_interactor_los_convierte_a_dto() {
        final UUID id = UUID.randomUUID();
        when(queryPort.consultarActivos()).thenReturn(List.of(new TipoVinculacionProjection(id, "PLA", "Planta")));

        final List<TipoVinculacionDomain> dominio = useCase.execute();
        assertEquals(new TipoVinculacionDomain(id, "PLA", "Planta"), dominio.get(0));

        final List<TipoVinculacionDTO> dtos = new ConsultarTiposVinculacionInteractor(useCase).execute();
        assertEquals(new TipoVinculacionDTO(id, "PLA", "Planta"), dtos.get(0));
    }
}
