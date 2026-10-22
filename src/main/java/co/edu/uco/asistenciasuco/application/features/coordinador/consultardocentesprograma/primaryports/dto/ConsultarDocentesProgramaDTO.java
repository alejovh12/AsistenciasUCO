package co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.primaryports.dto;

import java.util.UUID;

public record ConsultarDocentesProgramaDTO(int pagina, int tamano, String busqueda, Boolean estaActivo, UUID idTipoVinculacion) {
}
