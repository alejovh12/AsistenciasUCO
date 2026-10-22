package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.command.ActualizarDocenteProgramaCommand;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.command.VincularDocenteProgramaCommand;

/** Puerto de salida de escritura para la adscripcion docente-programa (HU091). */
public interface DocenteProgramaCommandPort {

    void vincular(VincularDocenteProgramaCommand command);

    void actualizar(ActualizarDocenteProgramaCommand command);
}
