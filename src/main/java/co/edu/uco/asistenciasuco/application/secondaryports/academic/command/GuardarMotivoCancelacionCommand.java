package co.edu.uco.asistenciasuco.application.secondaryports.academic.command;

import java.util.UUID;

/** Datos para crear o actualizar un registro del catalogo de motivos de cancelacion de sesion (migracion 65 de la BD). */
public record GuardarMotivoCancelacionCommand(UUID id, String codigo, String nombre, Integer orden, UUID usuario) {
}
