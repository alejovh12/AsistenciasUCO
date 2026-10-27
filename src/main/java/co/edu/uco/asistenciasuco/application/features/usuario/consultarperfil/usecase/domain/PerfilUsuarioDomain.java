package co.edu.uco.asistenciasuco.application.features.usuario.consultarperfil.usecase.domain;

import java.util.List;
import java.util.UUID;

public record PerfilUsuarioDomain(
        UUID idUsuario,
        String codigoTipoIdentificacion,
        String nombreTipoIdentificacion,
        Integer numeroIdentificacion,
        String primerNombre,
        String segundoNombre,
        String primerApellido,
        String segundoApellido,
        String nombreCompleto,
        String correo,
        boolean correoConfirmado,
        boolean estaActivo,
        List<VinculoUsuarioDomain> vinculos
) {

    public PerfilUsuarioDomain {
        vinculos = vinculos == null ? List.of() : List.copyOf(vinculos);
    }
}
