package co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.usecase.domain;

import java.util.UUID;

public record ConsultarDocentesProgramaDomain(int pagina, int tamano, String busqueda, Boolean estaActivo, UUID idTipoVinculacion) {
}
