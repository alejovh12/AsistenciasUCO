package co.edu.uco.asistenciasuco.application.features.tipoprograma.consultarcatalogosprograma;

import co.edu.uco.asistenciasuco.application.features.tipoprograma.consultarcatalogosprograma.primaryports.dto.CatalogoProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.tipoprograma.consultarcatalogosprograma.primaryports.interactor.ConsultarCatalogosProgramaInteractor;
import co.edu.uco.asistenciasuco.application.features.tipoprograma.consultarcatalogosprograma.usecase.impl.ConsultarCatalogosProgramaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CatalogoProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CatalogoProgramaProjection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** T-023-01: lectura de los catalogos de tipo de programa, modalidad y jornada (solo activos, via el puerto). */
class ConsultarCatalogosProgramaUseCaseImplTest {

    private static final UUID ID = UUID.fromString("88888888-8888-8888-8888-888888888888");

    private final CatalogoProgramaQueryPort port = mock(CatalogoProgramaQueryPort.class);
    private final ConsultarCatalogosProgramaInteractor interactor =
            new ConsultarCatalogosProgramaInteractor(new ConsultarCatalogosProgramaUseCaseImpl(port));

    @Test
    void tipos_modalidades_y_jornadas_se_mapean_a_dto() {
        when(port.consultarTiposPrograma()).thenReturn(List.of(new CatalogoProgramaProjection(ID, "PRE", "pregrado")));
        when(port.consultarModalidades()).thenReturn(List.of(new CatalogoProgramaProjection(ID, "VIR", "Virtual")));
        when(port.consultarJornadas()).thenReturn(List.of(new CatalogoProgramaProjection(ID, "NOC", "Nocturna")));

        assertEquals(List.of(new CatalogoProgramaDTO(ID, "PRE", "pregrado")), interactor.tiposPrograma());
        assertEquals(List.of(new CatalogoProgramaDTO(ID, "VIR", "Virtual")), interactor.modalidades());
        assertEquals(List.of(new CatalogoProgramaDTO(ID, "NOC", "Nocturna")), interactor.jornadas());
    }

    @Test
    void catalogos_vacios_devuelven_lista_vacia() {
        when(port.consultarTiposPrograma()).thenReturn(List.of());
        when(port.consultarModalidades()).thenReturn(List.of());
        when(port.consultarJornadas()).thenReturn(List.of());

        assertTrue(interactor.tiposPrograma().isEmpty());
        assertTrue(interactor.modalidades().isEmpty());
        assertTrue(interactor.jornadas().isEmpty());
    }

    @Test
    void el_puerto_es_obligatorio() {
        assertThrows(NullPointerException.class, () -> new ConsultarCatalogosProgramaUseCaseImpl(null));
    }
}
