package co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common;

import java.math.BigDecimal;
import java.util.UUID;

public record ResultadoResolucion(String mensajeUsuario, UUID idRevision, String estadoAnterior, String estadoNuevo, String decision,
                           boolean sinCambios, boolean cambioMatricula, UUID idGrupo, UUID idEstudianteGrupo, UUID idEstudiante,
                           UUID idPrograma, String perfilResolucion, BigDecimal porcentajeActual, String tipoPlantillaNotificacion) {
}
