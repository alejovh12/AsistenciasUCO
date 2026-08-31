package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.adapter;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.TipoIdentificacionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.TipoIdentificacionRepositoryProjection;

import java.util.List;
import java.util.UUID;

/**
 * Adaptador mock temporal para consulta del catalogo de tipos de identificacion.
 */
public final class TipoIdentificacionRepositoryMockAdapter implements TipoIdentificacionRepositoryPort {

    private static final UUID CC = UUID.fromString("13641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID PA = UUID.fromString("796a6c0f-f9fd-4327-a322-efd4d90bb81d");
    private static final UUID TI = UUID.fromString("67a3d514-ab1b-4ce7-b661-1a99ea18fd9d");

    @Override
    public List<TipoIdentificacionRepositoryProjection> consultarTiposIdentificacion() {
        return List.of(
                new TipoIdentificacionRepositoryProjection(CC, "CC", "Cédula de ciudadanía"),
                new TipoIdentificacionRepositoryProjection(PA, "PA", "Pasaporte"),
                new TipoIdentificacionRepositoryProjection(TI, "TI", "Tarjeta de identidad")
        );
    }
}
