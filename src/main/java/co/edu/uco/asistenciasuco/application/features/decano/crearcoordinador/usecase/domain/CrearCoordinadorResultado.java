package co.edu.uco.asistenciasuco.application.features.decano.crearcoordinador.usecase.domain;

import java.util.UUID;

/** Resultado del alta del coordinador; el programa es el creado o el vinculado (nulo si no se indico). */
public record CrearCoordinadorResultado(UUID idCoordinador, UUID idPrograma, String nombrePrograma) {
}
