package co.edu.uco.notificaciones.application.port.in;

import co.edu.uco.notificaciones.application.model.TipoPlantillaAdmin;
import co.edu.uco.notificaciones.application.model.VistaPrevia;
import co.edu.uco.notificaciones.domain.VersionPlantilla;

import java.util.List;

/** Puerto de entrada: administracion de plantillas (versionado, activacion y vista previa). */
public interface PlantillasUseCase {

    List<TipoPlantillaAdmin> consultar(String idioma);

    VersionPlantilla crearVersion(String tipo, String idioma, String asunto, String cuerpo, String creadoPor);

    VersionPlantilla activarVersion(String tipo, String idioma, int version);

    VistaPrevia vistaPrevia(String tipo, String idioma, String asunto, String cuerpo);
}
