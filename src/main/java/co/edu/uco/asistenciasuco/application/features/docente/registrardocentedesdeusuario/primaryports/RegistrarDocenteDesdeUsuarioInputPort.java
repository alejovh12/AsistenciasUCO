package co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.primaryports;

import co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.primaryports.dto.RegistrarDocenteDesdeUsuarioDTO;
import co.edu.uco.asistenciasuco.application.features.docente.registrardocentedesdeusuario.primaryports.dto.RegistrarDocenteDesdeUsuarioResultadoDTO;
import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

public interface RegistrarDocenteDesdeUsuarioInputPort
        extends InteractorWithReturn<RegistrarDocenteDesdeUsuarioDTO, RegistrarDocenteDesdeUsuarioResultadoDTO> {
}
