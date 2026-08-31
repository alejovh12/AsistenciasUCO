package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.repository.adapter;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CerrarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionRepositoryProjection;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.crosscutting.helpers.ObjectHelper;

import java.util.UUID;

/**
 * Adaptador mock temporal para persistencia de sesiones.
 */
public final class SesionRepositoryMockAdapter implements SesionRepositoryPort {

    private static final UUID SESION_ABIERTA = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private static final UUID SESION_CERRADA = UUID.fromString("00000000-0000-0000-0000-000000000302");
    private static final UUID GRUPO_1 = UUID.fromString("00000000-0000-0000-0000-000000000401");

    @Override
    public void crearSesion(final CrearSesionRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para crear sesion es obligatorio.");
        }
    }

    @Override
    public SesionRepositoryProjection consultarSesion(final ConsultarSesionRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para consultar sesion es obligatorio.");
        }

        final boolean sesionCerrada = SESION_CERRADA.equals(dto.getSesion());

        return new SesionRepositoryProjection(
                dto.getSesion(),
                GRUPO_1,
                sesionCerrada ? "Sesion de cierre de periodo" : "Sesion de seguimiento academico",
                sesionCerrada
                        ? "Sesion mock cerrada para validar flujo de consulta."
                        : "Sesion mock disponible para validar flujo de consulta.",
                sesionCerrada,
                sesionCerrada ? "La sesion se cerro sin novedades." : null
        );
    }

    @Override
    public void cerrarSesion(final CerrarSesionRepositoryDTO dto) {
        if (ObjectHelper.isNull(dto)) {
            throw new CrosscuttingException("El dominio para cerrar sesion es obligatorio.");
        }
    }
}
