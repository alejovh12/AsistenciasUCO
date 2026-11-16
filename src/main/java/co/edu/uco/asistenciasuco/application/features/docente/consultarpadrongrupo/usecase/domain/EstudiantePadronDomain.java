package co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.usecase.domain;

import java.util.UUID;

/** Matricula de un estudiante en el padron de un grupo (estado de la matricula incluido). */
public record EstudiantePadronDomain(
        UUID id,
        UUID idEstudiante,
        String documento,
        String nombreCompleto,
        String correo,
        String codigoEstado,
        String nombreEstado
) {
}
