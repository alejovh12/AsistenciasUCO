package co.edu.uco.asistenciasuco.application.features.configuracion.consultarregistroestudiante.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.configuracion.consultarregistroestudiante.usecase.ConsultarConfiguracionRegistroEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.usuario.correoinstitucional.CorreoInstitucionalPolicy;

import java.util.Objects;

/** IDENT-001 D3: entrega al frontend el dominio institucional vigente para validar el correo antes de enviar. */
public final class ConsultarConfiguracionRegistroEstudianteUseCaseImpl implements ConsultarConfiguracionRegistroEstudianteUseCase {

    private final CorreoInstitucionalPolicy politica;

    public ConsultarConfiguracionRegistroEstudianteUseCaseImpl(final CorreoInstitucionalPolicy politica) {
        this.politica = Objects.requireNonNull(politica, "CorreoInstitucionalPolicy es obligatoria.");
    }

    @Override
    public String execute() {
        return politica.dominio();
    }
}
