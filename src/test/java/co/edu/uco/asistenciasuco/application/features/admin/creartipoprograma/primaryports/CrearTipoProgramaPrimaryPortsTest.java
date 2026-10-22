package co.edu.uco.asistenciasuco.application.features.admin.creartipoprograma.primaryports;

import co.edu.uco.asistenciasuco.application.features.admin.creartipoprograma.primaryports.dto.CrearTipoProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.creartipoprograma.primaryports.interactor.CrearTipoProgramaInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.creartipoprograma.primaryports.mapper.CrearTipoProgramaMapper;
import co.edu.uco.asistenciasuco.application.features.admin.creartipoprograma.usecase.CrearTipoProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.creartipoprograma.usecase.domain.CrearTipoProgramaDomain;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CrearTipoProgramaPrimaryPortsTest {

    private static final UUID ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID ACTOR = UUID.fromString("12345678-1234-1234-1234-123456789012");

    @Test
    void mapper_preserva_el_contrato_de_entrada() {
        assertEquals(new CrearTipoProgramaDomain("PR1", "Nombre Valido", ACTOR),
                CrearTipoProgramaMapper.toDomain(new CrearTipoProgramaDTO("PR1", "Nombre Valido", ACTOR)));
    }

    @Test
    void mapper_devuelve_null_para_entrada_nula() {
        assertNull(CrearTipoProgramaMapper.toDomain(null));
    }

    @Test
    void interactor_mapea_y_delega_al_caso_de_uso() {
        final CrearTipoProgramaUseCase useCase = mock(CrearTipoProgramaUseCase.class);
        new CrearTipoProgramaInteractor(useCase).execute(new CrearTipoProgramaDTO("PR1", "Nombre Valido", ACTOR));
        verify(useCase).execute(new CrearTipoProgramaDomain("PR1", "Nombre Valido", ACTOR));
    }

    @Test
    void interactor_requiere_caso_de_uso() {
        assertThrows(NullPointerException.class, () -> new CrearTipoProgramaInteractor(null));
    }
}
