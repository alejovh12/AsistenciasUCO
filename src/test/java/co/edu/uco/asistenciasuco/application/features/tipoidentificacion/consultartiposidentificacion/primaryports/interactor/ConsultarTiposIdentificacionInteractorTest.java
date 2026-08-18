package co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.primaryports.dto.TipoIdentificacionDTO;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.usecase.ConsultarTiposIdentificacionUseCase;
import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.domain.TipoIdentificacionDomain;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultarTiposIdentificacionInteractorTest {

    @Test
    void execute_retorna_dtos_correctos_sin_domain_artificial() {
        final AtomicBoolean useCaseInvocado = new AtomicBoolean(false);
        final ConsultarTiposIdentificacionUseCase useCase = () -> {
            useCaseInvocado.set(true);
            return List.of(
                    TipoIdentificacionDomain.reconstruir(
                            UUID.fromString("13641bab-e3cd-485c-b275-47e7b731e18c"),
                            "CC",
                            "Cédula de ciudadanía"
                    ),
                    TipoIdentificacionDomain.reconstruir(
                            UUID.fromString("796a6c0f-f9fd-4327-a322-efd4d90bb81d"),
                            "PA",
                            "Pasaporte"
                    ),
                    TipoIdentificacionDomain.reconstruir(
                            UUID.fromString("67a3d514-ab1b-4ce7-b661-1a99ea18fd9d"),
                            "TI",
                            "Tarjeta de identidad"
                    )
            );
        };

        final ConsultarTiposIdentificacionInteractor interactor =
                new ConsultarTiposIdentificacionInteractor(useCase);

        final List<TipoIdentificacionDTO> resultado = interactor.execute();

        assertTrue(useCaseInvocado.get());
        assertNotNull(resultado);
        assertEquals(3, resultado.size());
        assertEquals("CC", resultado.get(0).getTipoIdentificacion());
        assertEquals("PA", resultado.get(1).getTipoIdentificacion());
        assertEquals("TI", resultado.get(2).getTipoIdentificacion());
    }
}
