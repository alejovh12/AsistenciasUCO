package co.edu.uco.asistenciasuco.application.secondaryports.identity.projection;

import java.util.List;
import java.util.UUID;

/** Perfil del usuario autenticado (HU006): datos personales de la DB y sus vinculos institucionales. Sin contrasena. */
public record PerfilUsuarioProjection(
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
        List<VinculoUsuarioProjection> vinculos
) {

    public PerfilUsuarioProjection {
        vinculos = vinculos == null ? List.of() : List.copyOf(vinculos);
    }
}
