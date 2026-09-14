package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import java.util.UUID;

public interface AsignaturaCommandPort {

    void crearAsignatura(UUID idAsignatura, String codigo, String nombre, Integer creditos, UUID idPlanEstudio,
                         Integer semestreNumero, String nombreArea, String nombreComponente);

    void actualizarAsignatura(UUID idAsignatura, String codigo, String nombre, Integer creditos, UUID idPlanEstudio,
                              Integer semestreNumero, String nombreArea, String nombreComponente);

    void toggleEstadoAsignatura(UUID idAsignatura);
}
