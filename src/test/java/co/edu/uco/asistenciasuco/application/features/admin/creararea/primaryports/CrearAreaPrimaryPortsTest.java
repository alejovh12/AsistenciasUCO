package co.edu.uco.asistenciasuco.application.features.admin.creararea.primaryports;

import co.edu.uco.asistenciasuco.application.features.admin.creararea.primaryports.dto.CrearAreaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.creararea.primaryports.interactor.CrearAreaInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.creararea.primaryports.mapper.CrearAreaMapper;
import co.edu.uco.asistenciasuco.application.features.admin.creararea.usecase.CrearAreaUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.creararea.usecase.domain.CrearAreaDomain;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CrearAreaPrimaryPortsTest {

    private static final UUID ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID ACTOR = UUID.fromString("12345678-1234-1234-1234-123456789012");

    @Test
    void mapper_preserva_el_contrato_de_entrada() {
        assertEquals(new CrearAreaDomain("PR1", "Nombre Valido", ACTOR),
                CrearAreaMapper.toDomain(new CrearAreaDTO("PR1", "Nombre Valido", ACTOR)));
    }

    @Test
    void mapper_devuelve_null_para_entrada_nula() {
        assertNull(CrearAreaMapper.toDomain(null));
    }

    @Test
    void interactor_mapea_y_delega_al_caso_de_uso() {
        final CrearAreaUseCase useCase = mock(CrearAreaUseCase.class);
        new CrearAreaInteractor(useCase).execute(new CrearAreaDTO("PR1", "Nombre Valido", ACTOR));
        verify(useCase).execute(new CrearAreaDomain("PR1", "Nombre Valido", ACTOR));
    }

    @Test
    void interactor_requiere_caso_de_uso() {
        assertThrows(NullPointerException.class, () -> new CrearAreaInteractor(null));
    }
}
