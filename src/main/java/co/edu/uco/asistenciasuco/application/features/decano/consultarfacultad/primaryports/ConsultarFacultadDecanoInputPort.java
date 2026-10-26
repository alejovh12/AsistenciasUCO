package co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.primaryports;

import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.primaryports.dto.ConsultasFacultadDecanoDTO.EstudianteFacultadDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.primaryports.dto.ConsultasFacultadDecanoDTO.FacultadNodoDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.primaryports.dto.ConsultasFacultadDecanoDTO.FichaEstudianteDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.primaryports.dto.ConsultasFacultadDecanoDTO.PaginaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarfacultad.primaryports.dto.ConsultasFacultadDecanoDTO.SesionFacultadDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * HU104 (directorio de estudiantes), HU105 (busqueda y ficha), HU109/HU119 (estructura Facultad -> Programa -> Plan)
 * y HU145 (cronograma de sesiones) de la facultad del decano.
 */
public interface ConsultarFacultadDecanoInputPort {

    PaginaDTO<EstudianteFacultadDTO> consultarEstudiantes(UUID usuario, String busqueda, UUID idPrograma,
                                                          Boolean estaActivo, Integer pagina, Integer tamano);

    FichaEstudianteDTO consultarFichaEstudiante(UUID usuario, UUID idEstudiante);

    List<FacultadNodoDTO> consultarEstructura(UUID usuario);

    PaginaDTO<SesionFacultadDTO> consultarSesiones(UUID usuario, LocalDate desde, LocalDate hasta, UUID idPrograma,
                                                   Integer pagina, Integer tamano);
}
