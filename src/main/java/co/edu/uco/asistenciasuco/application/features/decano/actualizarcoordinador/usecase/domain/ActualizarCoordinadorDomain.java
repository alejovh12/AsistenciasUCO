package co.edu.uco.asistenciasuco.application.features.decano.actualizarcoordinador.usecase.domain;

import java.util.UUID;

/**
 * Dominio de la operacion editar coordinador (HU121). El documento de identidad no forma parte: no se edita.
 */
public record ActualizarCoordinadorDomain(
        UUID idCoordinador,
        String primerNombre,
        String segundoNombre,
        String primerApellido,
        String segundoApellido,
        String correo,
        UUID usuario
) {
}
