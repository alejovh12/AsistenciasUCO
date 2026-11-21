package co.edu.uco.asistenciasuco.application.features.admin.consultarcatalogossistema;

import co.edu.uco.asistenciasuco.application.features.admin.consultarcatalogossistema.primaryports.interactor.ConsultarCatalogosSistemaInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.consultarcatalogossistema.usecase.impl.ConsultarCatalogosSistemaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CatalogosSistemaConsultaPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.CategoriaJustificacionCatalogoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.MotivoCancelacionCatalogoProjection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultarCatalogosSistemaTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final CatalogosSistemaConsultaPort port = mock(CatalogosSistemaConsultaPort.class);
    private final ConsultarCatalogosSistemaInteractor interactor = new ConsultarCatalogosSistemaInteractor(new ConsultarCatalogosSistemaUseCaseImpl(port));

    @Test
    void categorias_recorren_caso_de_uso_e_interactor() {
        when(port.consultarCategoriasJustificacion()).thenReturn(List.of(new CategoriaJustificacionCatalogoProjection(ID, "MED", "Médico", "Desc", true)));
        final var dtos = interactor.consultarCategoriasJustificacion();
        assertEquals(1, dtos.size());
        assertEquals(ID, dtos.get(0).id());
        assertEquals("MED", dtos.get(0).codigo());
        assertEquals("Médico", dtos.get(0).nombre());
        assertEquals("Desc", dtos.get(0).descripcion());
        assertEquals(true, dtos.get(0).requiereSoporte());
    }

    @Test
    void motivos_recorren_caso_de_uso_e_interactor() {
        when(port.consultarMotivosCancelacion()).thenReturn(List.of(new MotivoCancelacionCatalogoProjection(ID, "OTRO", "Otro", 5, false)));
        final var dtos = interactor.consultarMotivosCancelacion();
        assertEquals(1, dtos.size());
        assertEquals("OTRO", dtos.get(0).codigo());
        assertEquals(5, dtos.get(0).orden());
        assertEquals(false, dtos.get(0).estaActivo());
    }

    @Test
    void constructores_rechazan_dependencias_nulas() {
        assertThrows(NullPointerException.class, () -> new ConsultarCatalogosSistemaUseCaseImpl(null));
        assertThrows(NullPointerException.class, () -> new ConsultarCatalogosSistemaInteractor(null));
    }
}
