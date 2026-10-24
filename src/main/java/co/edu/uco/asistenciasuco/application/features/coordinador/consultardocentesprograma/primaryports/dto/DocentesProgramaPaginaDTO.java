package co.edu.uco.asistenciasuco.application.features.coordinador.consultardocentesprograma.primaryports.dto;

import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.DocenteProgramaDTO;

import java.util.List;

public record DocentesProgramaPaginaDTO(List<DocenteProgramaDTO> elementos, long total) {
}
