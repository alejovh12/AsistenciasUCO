package co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common;

import java.math.BigDecimal;
import java.util.UUID;

public record DestinatarioRevision(String rol, String codigoPerfil, UUID idUsuario, String nombreCompleto, String correo, UUID idPrograma,
                            boolean esEscalamiento, UUID idGrupo, int codigoGrupo, String nombreGrupo, String nombreAsignatura,
                            String nombreEstudiante, String nombreDocente, String estado, BigDecimal porcentajeActual,
                            BigDecimal umbralPerdida, String motivoDocente, String motivoResolucion, String nombreResolutor) {
}
