package co.edu.uco.asistenciasuco.application.features.tomaasistencia.avisarfaltassesion.primaryports;

/**
 * ATA D1 (HU175): el job del servidor avisa por correo, una vez por estudiante y sesion, las faltas (SJC) de las sesiones que ya
 * terminaron. Devuelve cuantas sesiones quedaron marcadas como avisadas en la pasada.
 */
public interface AvisarFaltasSesionInputPort {

    int execute();
}
