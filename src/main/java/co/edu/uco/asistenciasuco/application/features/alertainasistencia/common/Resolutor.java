package co.edu.uco.asistenciasuco.application.features.alertainasistencia.common;

import java.util.UUID;

public record Resolutor(UUID idUsuario, String nombreCompleto, String correo, String codigoPerfil, boolean esEscalamiento) {
}
