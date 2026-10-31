package co.edu.uco.asistenciasuco.application.features.configuracion.consultarregistroestudiante.primaryports;

/** IDENT-001: puerto de entrada de la configuracion del registro manual de estudiantes. */
public interface ConsultarConfiguracionRegistroEstudianteInputPort {

    /** @return el dominio institucional vigente del correo del estudiante. */
    String execute();
}
