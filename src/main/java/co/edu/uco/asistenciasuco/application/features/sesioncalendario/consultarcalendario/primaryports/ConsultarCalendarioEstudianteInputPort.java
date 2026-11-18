package co.edu.uco.asistenciasuco.application.features.sesioncalendario.consultarcalendario.primaryports;

import co.edu.uco.asistenciasuco.application.features.sesioncalendario.consultarcalendario.primaryports.CalendarioSesiones.Comando;
import co.edu.uco.asistenciasuco.application.features.sesioncalendario.consultarcalendario.primaryports.CalendarioSesiones.SesionCalendario;
import co.edu.uco.asistenciasuco.application.primaryports.InteractorWithReturn;

import java.util.List;

/** ATA D7: sesiones del estudiante (grupos con matricula inscrita) en un rango de fechas locales, en orden de inicio. */
public interface ConsultarCalendarioEstudianteInputPort extends InteractorWithReturn<Comando, List<SesionCalendario>> {
}
