package co.edu.uco.asistenciasuco.application.features.admin.actualizartipoprograma.primaryports;

import co.edu.uco.asistenciasuco.application.features.admin.actualizartipoprograma.primaryports.dto.ActualizarTipoProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.actualizartipoprograma.primaryports.interactor.ActualizarTipoProgramaInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.actualizartipoprograma.primaryports.mapper.ActualizarTipoProgramaMapper;
import co.edu.uco.asistenciasuco.application.features.admin.actualizartipoprograma.usecase.ActualizarTipoProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.actualizartipoprograma.usecase.domain.ActualizarTipoProgramaDomain;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ActualizarTipoProgramaPrimaryPortsTest {

    private static final UUID ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID ACTOR = UUID.fromString("12345678-1234-1234-1234-123456789012");

    @Test
    void mapper_preserva_el_contrato_de_entrada() {
        assertEquals(new ActualizarTipoProgramaDomain(ID, "Nombre Valido", ACTOR),
                ActualizarTipoProgramaMapper.toDomain(new ActualizarTipoProgramaDTO(ID, "Nombre Valido", ACTOR)));
    }

    @Test
    void mapper_devuelve_null_para_entrada_nula() {
        assertNull(ActualizarTipoProgramaMapper.toDomain(null));
    }

    @Test
    void interactor_mapea_y_delega_al_caso_de_uso() {
        final ActualizarTipoProgramaUseCase useCase = mock(ActualizarTipoProgramaUseCase.class);
        new ActualizarTipoProgramaInteractor(useCase).execute(new ActualizarTipoProgramaDTO(ID, "Nombre Valido", ACTOR));
        verify(useCase).execute(new ActualizarTipoProgramaDomain(ID, "Nombre Valido", ACTOR));
    }

    @Test
    void interactor_requiere_caso_de_uso() {
        assertThrows(NullPointerException.class, () -> new ActualizarTipoProgramaInteractor(null));
    }
}
