package co.edu.uco.asistenciasuco.application.features.docente.retirarestudiantegrupo.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.docente.retirarestudiantegrupo.usecase.RetirarEstudianteGrupoUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.retirarestudiantegrupo.usecase.domain.RetirarEstudianteGrupoDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.RetiroEstudianteGrupoPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DestinatarioRetiroProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * HU051: el docente titular retira a un estudiante de su grupo. El rol DOCENTE no basta: Application exige un docente
 * institucional y el SP revalida la titularidad, el periodo vigente y la matricula activa. Tras el retiro se avisa al
 * estudiante, al docente y al coordinador del programa; el aviso es de mejor esfuerzo y nunca revierte el retiro.
 */
public final class RetirarEstudianteGrupoUseCaseImpl implements RetirarEstudianteGrupoUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetirarEstudianteGrupoUseCaseImpl.class);

    private final InstitutionalScopePort scopePort;
    private final RetiroEstudianteGrupoPort retiroPort;
    private final NotificacionPort notificacionPort;

    public RetirarEstudianteGrupoUseCaseImpl(
            final InstitutionalScopePort scopePort,
            final RetiroEstudianteGrupoPort retiroPort,
            final NotificacionPort notificacionPort
    ) {
        this.scopePort = Objects.requireNonNull(scopePort, "InstitutionalScopePort es obligatorio.");
        this.retiroPort = Objects.requireNonNull(retiroPort, "RetiroEstudianteGrupoPort es obligatorio.");
        this.notificacionPort = Objects.requireNonNull(notificacionPort, "NotificacionPort es obligatorio.");
    }

    @Override
    public void execute(final RetirarEstudianteGrupoDomain domain) {
        scopePort.findDocenteIdByUsuario(domain.actorUsuarioId())
                .orElseThrow(() -> new ForbiddenException("No fue posible resolver el docente autenticado."));

        retiroPort.retirarEstudiante(domain.grupoId(), domain.estudianteId(), domain.motivo(), domain.actorUsuarioId());
        avisar(domain);
    }

    private void avisar(final RetirarEstudianteGrupoDomain domain) {
        try {
            final List<DestinatarioRetiroProjection> destinatarios =
                    retiroPort.consultarDestinatarios(domain.grupoId(), domain.estudianteId(), domain.actorUsuarioId());
            if (destinatarios.isEmpty()) {
                return;
            }
            notificacionPort.notificar(construirAviso(domain, destinatarios));
        } catch (final RuntimeException exception) {
            // El retiro ya quedo confirmado en la DB: un fallo del aviso se registra pero no se propaga.
            LOGGER.warn("No fue posible enviar el aviso de retiro del grupo {}: {}", domain.grupoId(),
                    exception.getClass().getSimpleName());
        }
    }

    private static Notificacion construirAviso(
            final RetirarEstudianteGrupoDomain domain,
            final List<DestinatarioRetiroProjection> destinatarios
    ) {
        final DestinatarioRetiroProjection contexto = destinatarios.getFirst();
        final String estudiante = destinatarios.stream()
                .filter(destinatario -> "ESTUDIANTE".equals(destinatario.rol()))
                .map(DestinatarioRetiroProjection::nombreCompleto)
                .findFirst()
                .orElse("el estudiante");
        final String grupo = contexto.nombreGrupo() + " de " + contexto.nombreAsignatura();
        final String asunto = "Retiro del grupo " + grupo;
        final String mensaje = "Se retiro a " + estudiante + " del grupo " + grupo + ". Motivo: " + domain.motivo();
        return new Notificacion(
                Notificacion.TIPO_RETIRO_ESTUDIANTE_GRUPO,
                asunto,
                mensaje,
                destinatarios.stream()
                        .map(destinatario -> new Notificacion.Destinatario(destinatario.rol(), destinatario.idUsuario(),
                                destinatario.nombreCompleto(), destinatario.correo()))
                        .toList(),
                Map.of("idGrupo", domain.grupoId().toString(),
                        "idEstudiante", domain.estudianteId().toString(),
                        "motivo", domain.motivo()));
    }
}
