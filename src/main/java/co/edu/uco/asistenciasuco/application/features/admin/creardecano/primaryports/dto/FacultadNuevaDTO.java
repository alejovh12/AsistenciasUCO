package co.edu.uco.asistenciasuco.application.features.admin.creardecano.primaryports.dto;

import java.util.UUID;

/** Facultad que se crea junto con el decano (DEC-065 (5)). */
public record FacultadNuevaDTO(String nombre, String codigo, UUID idInstitucion) {
}
