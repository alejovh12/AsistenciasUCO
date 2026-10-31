package co.edu.uco.asistenciasuco.application.features.configuracion.consultarregistroestudiante.usecase;

/** IDENT-001: consulta la configuracion publica del registro manual de estudiantes. */
public interface ConsultarConfiguracionRegistroEstudianteUseCase {

    /** @return el dominio institucional vigente del correo del estudiante (incluye el {@code @}). */
    String execute();
}
