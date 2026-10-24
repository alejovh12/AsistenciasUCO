package co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.primaryports.dto;

import java.util.List;
import java.util.UUID;

public record FichaEstudianteDTO(
        UUID idEstudiante,
        String tipoIdentificacion,
        Integer numeroIdentificacion,
        String primerNombre,
        String segundoNombre,
        String primerApellido,
        String segundoApellido,
        String nombreCompleto,
        String correo,
        boolean estaActivoUsuario,
        UUID idPrograma,
        String nombrePrograma,
        List<ResumenAsignaturaDTO> resumenPorAsignatura
) {
}
