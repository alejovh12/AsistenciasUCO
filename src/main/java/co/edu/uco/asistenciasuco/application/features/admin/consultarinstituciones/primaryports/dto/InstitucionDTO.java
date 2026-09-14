package co.edu.uco.asistenciasuco.application.features.admin.consultarinstituciones.primaryports.dto;

import java.util.UUID;

public record InstitucionDTO(UUID id, String nombre, boolean estaActivaInstitucion, String estaActivaTextoInstitucion) {
}
