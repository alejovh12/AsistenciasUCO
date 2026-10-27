package co.edu.uco.asistenciasuco.application.features.admin.actualizararea.primaryports;

import co.edu.uco.asistenciasuco.application.features.admin.actualizararea.primaryports.dto.ActualizarAreaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.actualizararea.primaryports.interactor.ActualizarAreaInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.actualizararea.primaryports.mapper.ActualizarAreaMapper;
import co.edu.uco.asistenciasuco.application.features.admin.actualizararea.usecase.ActualizarAreaUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.actualizararea.usecase.domain.ActualizarAreaDomain;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ActualizarAreaPrimaryPortsTest {

    private static final UUID ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID ACTOR = UUID.fromString("12345678-1234-1234-1234-123456789012");

    @Test
    void mapper_preserva_el_contrato_de_entrada() {
        assertEquals(new ActualizarAreaDomain(ID, "Nombre Valido", ACTOR),
                ActualizarAreaMapper.toDomain(new ActualizarAreaDTO(ID, "Nombre Valido", ACTOR)));
    }

    @Test
    void mapper_devuelve_null_para_entrada_nula() {
        assertNull(ActualizarAreaMapper.toDomain(null));
    }

    @Test
    void interactor_mapea_y_delega_al_caso_de_uso() {
        final ActualizarAreaUseCase useCase = mock(ActualizarAreaUseCase.class);
        new ActualizarAreaInteractor(useCase).execute(new ActualizarAreaDTO(ID, "Nombre Valido", ACTOR));
        verify(useCase).execute(new ActualizarAreaDomain(ID, "Nombre Valido", ACTOR));
    }

    @Test
    void interactor_requiere_caso_de_uso() {
        assertThrows(NullPointerException.class, () -> new ActualizarAreaInteractor(null));
    }
}
