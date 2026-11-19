package co.edu.uco.asistenciasuco.application.features.tomaasistencia.avisarsesionessintoma.primaryports;

/**
 * NOTOMA-001: el job del servidor avisa por correo a la coordinacion, una vez por sesion, que el docente no tomo asistencia en una
 * sesion que ya termino. Devuelve cuantas sesiones quedaron marcadas como avisadas en la pasada.
 */
public interface AvisarSesionesSinTomaInputPort {

    int execute();
}
