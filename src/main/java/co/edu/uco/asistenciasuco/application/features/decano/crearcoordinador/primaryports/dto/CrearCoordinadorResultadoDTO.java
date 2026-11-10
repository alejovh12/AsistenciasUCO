package co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.primaryports.dto;

import java.util.UUID;

/** Resultado del alta del coordinador: su id y el programa vinculado o creado (nulos si no se indico programa). */
public record CrearCoordinadorResultadoDTO(UUID idCoordinador, UUID idPrograma, String nombrePrograma) {
}
