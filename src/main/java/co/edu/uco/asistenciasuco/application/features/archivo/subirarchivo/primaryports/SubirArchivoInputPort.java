package co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.primaryports;

import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.primaryports.dto.SubirArchivoDTO;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.primaryports.dto.SubirArchivoResultado;
import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

/**
 * Puerto de entrada para subir un soporte de revision.
 */
public interface SubirArchivoInputPort extends InteractorWithReturn<SubirArchivoDTO, SubirArchivoResultado> {
}
