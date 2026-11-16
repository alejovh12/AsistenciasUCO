package co.edu.uco.asistenciasuco.application.features.admin.admitirestudiante.usecase.domain;

import java.util.UUID;

/**
 * Datos de la admision de un estudiante por el administrador (CULM-001 D.4, HU156). IDENT-002: sin contrasena; la persona
 * nueva la define al activar su cuenta. La representacion de texto no imprime datos personales (OBS-12).
 */
public record AdmitirEstudianteDomain(
        UUID tipoIdentificacionId,
        Integer numeroIdentificacion,
        String primerNombre,
        String segundoNombre,
        String primerApellido,
        String segundoApellido,
        String correo,
        UUID idPrograma,
        UUID usuarioEjecutor
) {

    @Override
    public String toString() {
        return "AdmitirEstudianteDomain[idPrograma=" + idPrograma + ", usuarioEjecutor=" + usuarioEjecutor + "]";
    }
}
