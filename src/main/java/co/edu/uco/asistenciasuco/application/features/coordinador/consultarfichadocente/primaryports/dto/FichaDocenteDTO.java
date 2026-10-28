package co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.primaryports.dto;

import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.DocenteProgramaDTO;

import java.util.List;

public record FichaDocenteDTO(
        DocenteProgramaDTO docente,
        ResumenCargaDocenteDTO resumen,
        List<GrupoHistorialDocenteDTO> historial,
        long totalHistorial
) {
}
