package co.edu.uco.asistenciasuco.application.features.admin.cambiarestadoarea.primaryports;

import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadoarea.primaryports.dto.CambiarEstadoAreaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadoarea.primaryports.interactor.CambiarEstadoAreaInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadoarea.primaryports.mapper.CambiarEstadoAreaMapper;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadoarea.usecase.CambiarEstadoAreaUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.cambiarestadoarea.usecase.domain.CambiarEstadoAreaDomain;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CambiarEstadoAreaPrimaryPortsTest {

    private static final UUID ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID ACTOR = UUID.fromString("12345678-1234-1234-1234-123456789012");

    @Test
    void mapper_preserva_el_contrato_de_entrada() {
        assertEquals(new CambiarEstadoAreaDomain(ID, true, ACTOR),
                CambiarEstadoAreaMapper.toDomain(new CambiarEstadoAreaDTO(ID, true, ACTOR)));
    }

    @Test
    void mapper_devuelve_null_para_entrada_nula() {
        assertNull(CambiarEstadoAreaMapper.toDomain(null));
    }

    @Test
    void interactor_mapea_y_delega_al_caso_de_uso() {
        final CambiarEstadoAreaUseCase useCase = mock(CambiarEstadoAreaUseCase.class);
        new CambiarEstadoAreaInteractor(useCase).execute(new CambiarEstadoAreaDTO(ID, true, ACTOR));
        verify(useCase).execute(new CambiarEstadoAreaDomain(ID, true, ACTOR));
    }

    @Test
    void interactor_requiere_caso_de_uso() {
        assertThrows(NullPointerException.class, () -> new CambiarEstadoAreaInteractor(null));
    }
}
