package co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.primaryports;

import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.primaryports.dto.ConsultasAsistenciaProgramaDTO.AsistenciaSesionDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.primaryports.dto.ConsultasAsistenciaProgramaDTO.GrupoProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.primaryports.dto.ConsultasAsistenciaProgramaDTO.PaginaDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.primaryports.dto.ConsultasAsistenciaProgramaDTO.SesionGrupoDTO;

import java.time.LocalDate;
import java.util.UUID;

/** HU064 (grupos), HU060 (registros de asistencia por sesion) y HU061 (detalle) del programa del coordinador. */
public interface ConsultarAsistenciaProgramaInputPort {

    PaginaDTO<GrupoProgramaDTO> consultarGrupos(UUID usuario, UUID idPeriodoAcademico, String busqueda, Integer pagina,
                                                 Integer tamano);

    PaginaDTO<SesionGrupoDTO> consultarSesionesGrupo(UUID usuario, UUID idGrupo, LocalDate desde, LocalDate hasta,
                                                      Integer pagina, Integer tamano);

    PaginaDTO<AsistenciaSesionDTO> consultarAsistenciasSesion(UUID usuario, UUID idSesion, Boolean asistio,
                                                               Integer pagina, Integer tamano);
}
