package co.edu.uco.asistenciasuco.application.features.identidad.accesocuenta;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.identidad.accesocuenta.primaryports.AccesoCuentaInputPort;
import co.edu.uco.asistenciasuco.application.features.identidad.accesocuenta.primaryports.dto.AccesoCuentaDTO;
import co.edu.uco.asistenciasuco.application.features.identidad.accesocuenta.primaryports.dto.AccesoRolDTO;
import co.edu.uco.asistenciasuco.application.features.identidad.accesocuenta.primaryports.dto.EstadoAccesoCuenta;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort.IdentityProviderException;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.UsuarioSincronizacionIdentidadPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.security.AccessProfilePort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.AccessProfilePort.Estado;
import co.edu.uco.asistenciasuco.application.secondaryports.security.AccessProfilePort.Resultado;
import co.edu.uco.asistenciasuco.application.secondaryports.security.ExternalIdentityLinkPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Estado de acceso de una cuenta y reconciliación idempotente entre tres capas: la base institucional (fuente de
 * verdad de los roles), el proveedor de identidad (client roles) y el motor de autorización (perfiles).
 *
 * <p>Una cuenta solo es {@code ACTIVA} si las tres coinciden. Cualquier falla de una capa externa se traduce en un
 * estado pendiente con el desfase registrado, nunca en acceso por defecto. El alcance del actor limita qué roles se
 * leen y se modifican: administrador todos; decano coordinador/docente/estudiante de su facultad; coordinador
 * docente/estudiante de su programa.</p>
 *
 * <p>POJO sin anotaciones de framework: se cablea con {@code @Bean} en la configuración de infraestructura.</p>
 */
public final class AccesoCuentaService implements AccesoCuentaInputPort {

    static final String OPERACION_RECONCILIAR = "RECONCILIAR_ACCESO";

    private static final Set<InstitutionalRole> ALCANCE_DECANO =
            EnumSet.of(InstitutionalRole.COORDINADOR, InstitutionalRole.DOCENTE, InstitutionalRole.ESTUDIANTE);
    private static final Set<InstitutionalRole> ALCANCE_COORDINADOR =
            EnumSet.of(InstitutionalRole.DOCENTE, InstitutionalRole.ESTUDIANTE);

    private final InstitutionalScopePort scope;
    private final IdentityProviderPort identityProvider;
    private final AccessProfilePort accessProfiles;
    private final UsuarioSincronizacionIdentidadPort desfases;
    private final ExternalIdentityLinkPort links;

    public AccesoCuentaService(
            final InstitutionalScopePort scope,
            final IdentityProviderPort identityProvider,
            final AccessProfilePort accessProfiles,
            final UsuarioSincronizacionIdentidadPort desfases,
            final ExternalIdentityLinkPort links
    ) {
        this.scope = Objects.requireNonNull(scope, "InstitutionalScopePort es obligatorio.");
        this.identityProvider = Objects.requireNonNull(identityProvider, "IdentityProviderPort es obligatorio.");
        this.accessProfiles = Objects.requireNonNull(accessProfiles, "AccessProfilePort es obligatorio.");
        this.desfases = Objects.requireNonNull(desfases, "UsuarioSincronizacionIdentidadPort es obligatorio.");
        this.links = Objects.requireNonNull(links, "ExternalIdentityLinkPort es obligatorio.");
    }

    @Override
    public AccesoCuentaDTO consultar(final UUID actorUsuario, final String correo) {
        return evaluar(resolver(actorUsuario, correo));
    }

    @Override
    public AccesoCuentaDTO reconciliar(final UUID actorUsuario, final String correo) {
        final Contexto contexto = resolver(actorUsuario, correo);
        final Optional<String> idExterno = buscarCuenta(contexto);
        idExterno.ifPresent(externo -> aplicarProveedor(contexto, externo));
        for (final InstitutionalRole rol : contexto.alcance()) {
            final Resultado resultado = contexto.esperado().contains(rol)
                    ? accessProfiles.sincronizar(contexto.correo(), rol)
                    : accessProfiles.revocar(contexto.correo(), rol);
            if (esPendienteOProblema(resultado.estado())) {
                registrarDesfase(contexto.usuario(), rol + ":" + resultado.estado() + ":" + resultado.detalle());
            }
        }
        return evaluar(contexto);
    }

    private Contexto resolver(final UUID actorUsuario, final String correo) {
        Objects.requireNonNull(actorUsuario, "El usuario autenticado es obligatorio.");
        if (correo == null || correo.isBlank()) {
            throw new ResourceNotFoundException("La cuenta indicada no existe.");
        }
        final Set<InstitutionalRole> alcance = alcanceDelActor(actorUsuario);
        if (alcance.isEmpty()) {
            throw new ForbiddenException("El usuario autenticado no puede gestionar cuentas de acceso.");
        }
        final String correoNormalizado = correo.trim().toLowerCase(Locale.ROOT);
        final UUID destino = scope.findUsuarioIdByEmail(correoNormalizado)
                .orElseThrow(() -> new ResourceNotFoundException("La cuenta indicada no existe."));
        if (!scope.canGestionarCuenta(actorUsuario, destino)) {
            throw new ForbiddenException("La cuenta indicada esta fuera del alcance del usuario autenticado.");
        }
        final Set<InstitutionalRole> esperado = EnumSet.noneOf(InstitutionalRole.class);
        scope.findRolesActivosByUsuario(destino).stream().filter(alcance::contains).forEach(esperado::add);
        return new Contexto(destino, correoNormalizado, alcance, esperado, scope.isUsuarioActivo(destino));
    }

    private Set<InstitutionalRole> alcanceDelActor(final UUID actor) {
        if (scope.isAdministradorActivo(actor)) {
            return EnumSet.allOf(InstitutionalRole.class);
        }
        final Set<InstitutionalRole> alcance = EnumSet.noneOf(InstitutionalRole.class);
        if (scope.findFacultadIdByDecanoUsuario(actor).isPresent()) {
            alcance.addAll(ALCANCE_DECANO);
        }
        if (scope.findProgramaIdByCoordinadorUsuario(actor).isPresent()) {
            alcance.addAll(ALCANCE_COORDINADOR);
        }
        return alcance;
    }

    private Optional<String> buscarCuenta(final Contexto contexto) {
        try {
            return ubicarCuenta(contexto.usuario());
        } catch (final IdentityProviderException exception) {
            registrarDesfase(contexto.usuario(), "BUSCAR_CUENTA:" + exception.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Ubica la cuenta en el proveedor: primero por el vínculo vigente {@code issuer + subject → Usuario.id} (autoritativo,
     * no depende de atributos que otras cuentas puedan repetir); si la persona aún no inició sesión, por el atributo
     * institucional con coincidencia exacta.
     */
    private Optional<String> ubicarCuenta(final UUID usuario) {
        final List<String> sujetos = links.findSujetosVigentes(usuario);
        if (sujetos.size() == 1) {
            return Optional.of(sujetos.getFirst());
        }
        return identityProvider.buscarCuentaPorIdUsuario(usuario).map(CuentaIdentidadDTO::idExterno);
    }

    /** Alinea los client roles del proveedor con la base institucional y deshabilita la cuenta sin roles activos. */
    private void aplicarProveedor(final Contexto contexto, final String idExterno) {
        try {
            final Set<InstitutionalRole> actuales = identityProvider.rolesDeCuenta(idExterno);
            for (final InstitutionalRole rol : contexto.alcance()) {
                final boolean esperado = contexto.esperado().contains(rol);
                if (esperado && !actuales.contains(rol)) {
                    identityProvider.asignarRol(idExterno, rol);
                } else if (!esperado && actuales.contains(rol)) {
                    identityProvider.retirarRol(idExterno, rol);
                }
            }
            if (!contexto.usuarioActivo()) {
                identityProvider.deshabilitarCuenta(idExterno);
            } else if (!contexto.esperado().isEmpty()) {
                identityProvider.habilitarCuenta(idExterno);
            }
        } catch (final IdentityProviderException exception) {
            registrarDesfase(contexto.usuario(), "PROVEEDOR:" + exception.getMessage());
        }
    }

    private AccesoCuentaDTO evaluar(final Contexto contexto) {
        Optional<String> idExterno = Optional.empty();
        Set<InstitutionalRole> enProveedor = EnumSet.noneOf(InstitutionalRole.class);
        String detalleProveedor = null;
        try {
            idExterno = ubicarCuenta(contexto.usuario());
            if (idExterno.isPresent()) {
                enProveedor = identityProvider.rolesDeCuenta(idExterno.get());
            }
        } catch (final IdentityProviderException exception) {
            detalleProveedor = "El proveedor de identidad no respondio: " + exception.getMessage();
        }

        final List<AccesoRolDTO> filas = new ArrayList<>();
        boolean pendiente = detalleProveedor != null;
        boolean problema = false;
        final StringBuilder detalle = new StringBuilder(detalleProveedor == null ? "" : detalleProveedor + ". ");
        if (detalleProveedor == null && idExterno.isEmpty() && !contexto.esperado().isEmpty()) {
            problema = true;
            detalle.append("La cuenta no existe en el proveedor de identidad. ");
        }
        for (final InstitutionalRole rol : contexto.alcance()) {
            final boolean esperado = contexto.esperado().contains(rol);
            final boolean presente = enProveedor.contains(rol);
            final Resultado perfil = accessProfiles.consultar(contexto.correo(), rol);
            filas.add(new AccesoRolDTO(rol.name(), esperado, presente, perfil.estado().name()));
            if (esperado && detalleProveedor == null && idExterno.isPresent() && !presente) {
                pendiente = true;
                detalle.append(rol).append(": falta el rol en el proveedor de identidad. ");
            }
            if (!esperado && presente) {
                pendiente = true;
                detalle.append(rol).append(": rol por retirar en el proveedor de identidad. ");
            }
            switch (perfil.estado()) {
                case ASIGNADO -> {
                    if (!esperado) {
                        pendiente = true;
                        detalle.append(rol).append(": perfil por revocar en el motor de autorizacion. ");
                    }
                }
                case NO_ASIGNADO -> {
                    if (esperado) {
                        pendiente = true;
                        detalle.append(rol).append(": perfil por asignar en el motor de autorizacion. ");
                    }
                }
                case PENDIENTE_PERSONA_NO_REGISTRADA -> {
                    if (esperado) {
                        pendiente = true;
                        detalle.append(rol).append(": ").append(perfil.detalle()).append(". ");
                    }
                }
                case PENDIENTE_NO_DISPONIBLE -> {
                    pendiente = true;
                    detalle.append(rol).append(": ").append(perfil.detalle()).append(". ");
                }
                case RECHAZADO -> {
                    problema = true;
                    detalle.append(rol).append(": ").append(perfil.detalle()).append(". ");
                }
                case NO_APLICA -> { }
            }
        }
        final EstadoAccesoCuenta estado;
        if (problema) {
            estado = EstadoAccesoCuenta.PROBLEMA;
        } else if (pendiente) {
            estado = EstadoAccesoCuenta.PENDIENTE_SINCRONIZACION;
        } else if (contexto.esperado().isEmpty()) {
            estado = EstadoAccesoCuenta.INACTIVA;
        } else {
            estado = EstadoAccesoCuenta.ACTIVA;
        }
        return new AccesoCuentaDTO(contexto.usuario(), estado, detalle.toString().trim(), List.copyOf(filas));
    }

    private static boolean esPendienteOProblema(final Estado estado) {
        return estado == Estado.PENDIENTE_NO_DISPONIBLE
                || estado == Estado.PENDIENTE_PERSONA_NO_REGISTRADA
                || estado == Estado.RECHAZADO;
    }

    private void registrarDesfase(final UUID usuario, final String detalle) {
        try {
            desfases.registrar(usuario, OPERACION_RECONCILIAR, detalle);
        } catch (final RuntimeException exception) {
            // Registrar el desfase nunca oculta el resultado de la reconciliacion.
        }
    }

    private record Contexto(
            UUID usuario,
            String correo,
            Set<InstitutionalRole> alcance,
            Set<InstitutionalRole> esperado,
            boolean usuarioActivo
    ) {
    }
}
