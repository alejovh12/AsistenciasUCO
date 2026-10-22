package co.edu.uco.asistenciasuco.application.features.admin.cambiarestadotipoprograma.primaryports;

import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadotipoprograma.primaryports.dto.CambiarEstadoTipoProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadotipoprograma.primaryports.interactor.CambiarEstadoTipoProgramaInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadotipoprograma.primaryports.mapper.CambiarEstadoTipoProgramaMapper;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadotipoprograma.usecase.CambiarEstadoTipoProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadotipoprograma.usecase.domain.CambiarEstadoTipoProgramaDomain;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CambiarEstadoTipoProgramaPrimaryPortsTest {

    private static final UUID ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID ACTOR = UUID.fromString("12345678-1234-1234-1234-123456789012");

    @Test
    void mapper_preserva_el_contrato_de_entrada() {
        assertEquals(new CambiarEstadoTipoProgramaDomain(ID, true, ACTOR),
                CambiarEstadoTipoProgramaMapper.toDomain(new CambiarEstadoTipoProgramaDTO(ID, true, ACTOR)));
    }

    @Test
    void mapper_devuelve_null_para_entrada_nula() {
        assertNull(CambiarEstadoTipoProgramaMapper.toDomain(null));
    }

    @Test
    void interactor_mapea_y_delega_al_caso_de_uso() {
        final CambiarEstadoTipoProgramaUseCase useCase = mock(CambiarEstadoTipoProgramaUseCase.class);
        new CambiarEstadoTipoProgramaInteractor(useCase).execute(new CambiarEstadoTipoProgramaDTO(ID, true, ACTOR));
        verify(useCase).execute(new CambiarEstadoTipoProgramaDomain(ID, true, ACTOR));
    }

    @Test
    void interactor_requiere_caso_de_uso() {
        assertThrows(NullPointerException.class, () -> new CambiarEstadoTipoProgramaInteractor(null));
    }
}
