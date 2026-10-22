package co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.primaryports;

import co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.primaryports.dto.TipoProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.primaryports.interactor.ConsultarTiposProgramaInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.primaryports.mapper.ConsultarTiposProgramaMapper;
import co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.usecase.ConsultarTiposProgramaUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.consultartiposprograma.usecase.domain.TipoProgramaDomain;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsultarTiposProgramaPrimaryPortsTest {

    private static final UUID ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final TipoProgramaDomain DOMAIN = new TipoProgramaDomain(ID, "PRE", "pregrado", true);
    private static final TipoProgramaDTO DTO = new TipoProgramaDTO(ID, "PRE", "pregrado", true);

    @Test
    void mapper_convierte_dominio_a_dto() {
        assertEquals(DTO, ConsultarTiposProgramaMapper.toDTO(DOMAIN));
    }

    @Test
    void interactor_consulta_todos_sin_filtrar() {
        final ConsultarTiposProgramaUseCase useCase = mock(ConsultarTiposProgramaUseCase.class);
        when(useCase.execute(false)).thenReturn(List.of(DOMAIN));

        assertEquals(List.of(DTO), new ConsultarTiposProgramaInteractor(useCase).consultarTodos());
        verify(useCase).execute(false);
    }

    @Test
    void interactor_consulta_solo_activos() {
        final ConsultarTiposProgramaUseCase useCase = mock(ConsultarTiposProgramaUseCase.class);
        when(useCase.execute(true)).thenReturn(List.of(DOMAIN));

        assertEquals(List.of(DTO), new ConsultarTiposProgramaInteractor(useCase).consultarActivos());
        verify(useCase).execute(true);
    }

    @Test
    void interactor_requiere_caso_de_uso() {
        assertThrows(NullPointerException.class, () -> new ConsultarTiposProgramaInteractor(null));
    }
}
