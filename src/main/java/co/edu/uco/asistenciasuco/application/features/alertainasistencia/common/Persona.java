package co.edu.uco.asistenciasuco.application.features.alertainasistencia.common;

import java.util.UUID;

public record Persona(UUID idUsuario, String nombreCompleto, String correo) {
}
