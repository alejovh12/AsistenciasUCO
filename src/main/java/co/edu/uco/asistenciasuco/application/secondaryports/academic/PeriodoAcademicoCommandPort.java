package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import java.time.LocalDate;
import java.util.UUID;

/**
 * HU096/HU097/HU098: comandos de periodos academicos. {@code idUsuarioEjecutor} es el ADMINISTRADOR
 * autenticado; el provider DB revalida el perfil y resuelve la institucion del ejecutor.
 */
public interface PeriodoAcademicoCommandPort {

    void crearPeriodoAcademico(UUID idPeriodo, String nombre, LocalDate fechaInicio, LocalDate fechaFin,
                               UUID idUsuarioEjecutor);

    void actualizarPeriodoAcademico(UUID idPeriodo, String nombre, LocalDate fechaInicio, LocalDate fechaFin,
                                    UUID idUsuarioEjecutor);

    void eliminarPeriodoAcademico(UUID idPeriodo, UUID idUsuarioEjecutor);
}
