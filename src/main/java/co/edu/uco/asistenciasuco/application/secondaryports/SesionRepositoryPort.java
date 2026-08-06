package co.edu.uco.asistenciasuco.application.secondaryports;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CerrarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.entity.SesionRepositoryEntity;

/**
 * Puerto secundario para persistencia relacionada con sesiones.
 */
public interface SesionRepositoryPort {

    void crearSesion(CrearSesionRepositoryDTO dto);

    SesionRepositoryEntity consultarSesion(ConsultarSesionRepositoryDTO dto);

    void cerrarSesion(CerrarSesionRepositoryDTO dto);
}
