package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.coordinador.response;

import co.edu.uco.asistenciasuco.application.features.coordinador.gestionarasignatura.primaryports.dto.EstadoAsignaturaDTO;

import java.util.UUID;

/**
 * HU074: datos de {@code PATCH /coordinador/asignaturas/{id}/estado}. {@code estaActivaAsignatura} es el estado en que quedo la
 * asignatura (mismo nombre que en la malla, {@code AsignaturaPlanDTO}); el texto de confirmacion va en {@code mensajeUsuario} de
 * {@code ApiDataMensajeResponse}.
 */
public record EstadoAsignaturaResponse(UUID idAsignatura, boolean estaActivaAsignatura) {

    public static EstadoAsignaturaResponse from(final EstadoAsignaturaDTO dto) {
        return new EstadoAsignaturaResponse(dto.idAsignatura(), dto.estaActivaAsignatura());
    }
}
