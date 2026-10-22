package co.edu.uco.asistenciasuco.application.features.decano.cambiarestadocoordinador.primaryports.dto;

import java.util.UUID;

public record CambiarEstadoCoordinadorDTO(UUID idCoordinador, boolean estaActivo, UUID usuario) {
}
