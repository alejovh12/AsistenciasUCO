package co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.usecase;

import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.usecase.domain.ProvisionarUsuarioDomain;
import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.usecase.entity.ProvisionarUsuarioResultadoEntity;

public interface ProvisionarUsuarioUseCase {

    ProvisionarUsuarioResultadoEntity execute(ProvisionarUsuarioDomain domain);
}
