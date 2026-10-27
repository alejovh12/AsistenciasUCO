package co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichaestudiante.usecase.domain;

import java.util.List;
import java.util.UUID;

public record FichaEstudianteDomain(
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
        List<ResumenAsignaturaDomain> resumenPorAsignatura
) {
}
