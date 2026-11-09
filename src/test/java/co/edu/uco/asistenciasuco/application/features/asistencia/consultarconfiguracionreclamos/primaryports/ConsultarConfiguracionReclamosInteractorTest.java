package co.edu.uco.asistenciasuco.application.features.asistencia.consultarconfiguracionreclamos.primaryports;

import co.edu.uco.asistenciasuco.application.features.asistencia.consultarconfiguracionreclamos.primaryports.dto.ConfiguracionReclamosDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarconfiguracionreclamos.primaryports.interactor.ConsultarConfiguracionReclamosInteractor;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarconfiguracionreclamos.usecase.ConsultarConfiguracionReclamosUseCase;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarconfiguracionreclamos.usecase.domain.ConfiguracionReclamosDomain;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultarConfiguracionReclamosInteractorTest {

    @Test
    void convierte_el_dominio_en_el_dto_de_entrada_del_controlador() {
        final UUID actor = UUID.randomUUID();
        final ConsultarConfiguracionReclamosUseCase useCase = mock(ConsultarConfiguracionReclamosUseCase.class);
        when(useCase.execute(actor)).thenReturn(
                new ConfiguracionReclamosDomain(3, 5_242_880L, List.of("application/pdf", "image/png"), 10, 5));

        final ConfiguracionReclamosDTO dto = new ConsultarConfiguracionReclamosInteractor(useCase).execute(actor);

        assertEquals(new ConfiguracionReclamosDTO(3, 5_242_880L, List.of("application/pdf", "image/png"), 10, 5), dto);
    }

    @Test
    void exige_el_caso_de_uso() {
        assertThrows(NullPointerException.class, () -> new ConsultarConfiguracionReclamosInteractor(null));
    }
}
