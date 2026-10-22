package co.edu.uco.asistenciasuco.application.features.coordinador.common.dto;

import java.util.List;

public record DocentesProgramaPaginaDTO(List<DocenteProgramaDTO> elementos, long total) {
}
