package co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common;

import java.math.BigDecimal;
import java.util.UUID;

public record ResultadoDecision(String mensajeUsuario, UUID idRevision, String estadoAnterior, String estadoNuevo, String decision,
                         boolean sinCambios, UUID idGrupo, UUID idEstudianteGrupo, UUID idEstudiante, BigDecimal porcentajeActual,
                         String tipoPlantillaNotificacion, String perfilResolutor, boolean escalamiento) {
}
