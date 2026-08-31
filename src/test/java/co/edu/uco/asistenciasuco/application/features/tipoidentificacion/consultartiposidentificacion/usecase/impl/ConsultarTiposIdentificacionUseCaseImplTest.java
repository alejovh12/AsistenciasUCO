package co.edu.uco.asistenciasuco.application.features.tipoidentificacion.consultartiposidentificacion.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.tipoidentificacion.domain.TipoIdentificacionDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.TipoIdentificacionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.TipoIdentificacionRepositoryProjection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultarTiposIdentificacionUseCaseImplTest {

    @Test
    void execute_transforma_los_resultados_del_repositorio() {
        final TipoIdentificacionRepositoryPort repositoryPort = () -> List.of(
                new TipoIdentificacionRepositoryProjection(
                        UUID.fromString("13641bab-e3cd-485c-b275-47e7b731e18c"),
                        "CC",
                        "Cédula de ciudadanía"
                ),
                new TipoIdentificacionRepositoryProjection(
                        UUID.fromString("796a6c0f-f9fd-4327-a322-efd4d90bb81d"),
                        "PA",
                        "Pasaporte"
                ),
                new TipoIdentificacionRepositoryProjection(
                        UUID.fromString("67a3d514-ab1b-4ce7-b661-1a99ea18fd9d"),
                        "TI",
                        "Tarjeta de identidad"
                )
        );

        final ConsultarTiposIdentificacionUseCaseImpl useCase =
                new ConsultarTiposIdentificacionUseCaseImpl(repositoryPort);

        final List<TipoIdentificacionDomain> resultado = useCase.execute();

        assertNotNull(resultado);
        assertEquals(3, resultado.size());
        assertEquals("CC", resultado.get(0).getTipoIdentificacion());
        assertEquals("Pasaporte", resultado.get(1).getNombre());
        assertEquals("TI", resultado.get(2).getTipoIdentificacion());
    }

    @Test
    void execute_retorna_lista_vacia_si_el_repositorio_retorna_null() {
        final TipoIdentificacionRepositoryPort repositoryPort = () -> null;

        final ConsultarTiposIdentificacionUseCaseImpl useCase =
                new ConsultarTiposIdentificacionUseCaseImpl(repositoryPort);

        final List<TipoIdentificacionDomain> resultado = useCase.execute();

        assertNotNull(resultado);
        assertEquals(0, resultado.size());
    }

    @Test
    void execute_consulta_el_repository_port() {
        final AtomicBoolean repositoryInvocado = new AtomicBoolean(false);
        final TipoIdentificacionRepositoryPort repositoryPort = () -> {
            repositoryInvocado.set(true);
            return List.of();
        };

        final ConsultarTiposIdentificacionUseCaseImpl useCase =
                new ConsultarTiposIdentificacionUseCaseImpl(repositoryPort);

        useCase.execute();

        assertTrue(repositoryInvocado.get());
    }
}
