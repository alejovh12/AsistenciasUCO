package co.edu.uco.asistenciasuco.application.features.admin.gestionarmotivocancelacion.usecase.domain;

import java.util.UUID;

/** Datos de entrada para crear o actualizar un registro del catalogo de motivos de cancelacion de sesion. */
public record MotivoCancelacionDomain(UUID id, String codigo, String nombre, Integer orden, UUID usuario) {
}
