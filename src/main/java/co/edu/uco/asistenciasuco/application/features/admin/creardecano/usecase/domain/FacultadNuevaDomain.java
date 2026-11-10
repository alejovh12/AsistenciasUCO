package co.edu.uco.asistenciasuco.application.features.admin.creardecano.usecase.domain;

import java.util.UUID;

/** Facultad que se crea en la misma transaccion que el decano (DEC-065 (5)). */
public record FacultadNuevaDomain(String nombre, String codigo, UUID idInstitucion) {
}
