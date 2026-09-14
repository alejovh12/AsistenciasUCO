package co.edu.uco.asistenciasuco.application.secondaryports.repository;

import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CerrarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ConsultarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.CrearSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.ActualizarSesionRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.GenerarSesionesGrupoRepositoryDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionRepositoryProjection;

/**
 * Puerto secundario para persistencia relacionada con sesiones.
 */
public interface SesionRepositoryPort {

    void crearSesion(CrearSesionRepositoryDTO dto);

    void actualizarSesion(ActualizarSesionRepositoryDTO dto);

    SesionRepositoryProjection consultarSesion(ConsultarSesionRepositoryDTO dto);

    void cerrarSesion(CerrarSesionRepositoryDTO dto);

    void generarSesionesGrupo(GenerarSesionesGrupoRepositoryDTO dto);
}
