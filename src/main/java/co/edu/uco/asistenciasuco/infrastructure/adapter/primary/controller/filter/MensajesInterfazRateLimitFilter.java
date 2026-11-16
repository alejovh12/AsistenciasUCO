package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter;

import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.SecurityErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.mensajesinterfaz.MensajesInterfazPublicaController;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.SecurityErrorResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * INT-003: limite de peticiones en memoria por IP sobre {@code GET /api/v1/public/mensajes-interfaz/{codigo}}. Cada texto
 * que no esta en la cache del catalogo se consulta en MessageUcoLab; el limite evita que la ruta publica sirva para
 * cargarlo. La SPA pide cada codigo una sola vez por sesion del navegador, asi que el limite por defecto (240/min) deja
 * cargar los textos de la interfaz con holgura. Mismo criterio que {@link VerificacionCorreoRateLimitFilter}: cuenta solo
 * {@code remoteAddr} ({@code X-Forwarded-For} es falsificable) y la memoria esta acotada a {@code maxClaves}.
 */
public final class MensajesInterfazRateLimitFilter extends OncePerRequestFilter {

    public static final String RUTA_BASE = MensajesInterfazPublicaController.RUTA_BASE;
    private static final long VENTANA_SEGUNDOS = 60;

    private final int limitePorMinuto;
    private final int maxClaves;
    private final Clock clock;
    private final SecurityErrorResponseWriter responseWriter;
    private final Map<String, Deque<Instant>> peticionesPorIp;

    public MensajesInterfazRateLimitFilter(
            final int limitePorMinuto,
            final int maxClaves,
            final Clock clock,
            final SecurityErrorResponseWriter responseWriter
    ) {
        if (limitePorMinuto < 1) {
            throw new IllegalArgumentException("El limite de peticiones por minuto debe ser al menos 1.");
        }
        if (maxClaves < 1) {
            throw new IllegalArgumentException("El maximo de claves del limitador debe ser al menos 1.");
        }
        this.limitePorMinuto = limitePorMinuto;
        this.maxClaves = maxClaves;
        this.clock = Objects.requireNonNull(clock, "Clock es obligatorio.");
        this.responseWriter = Objects.requireNonNull(responseWriter, "SecurityErrorResponseWriter es obligatorio.");
        this.peticionesPorIp = new LinkedHashMap<>(16, 0.75f, true);
    }

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        final String uri = request.getRequestURI();
        return !HttpMethod.GET.matches(request.getMethod()) || uri == null || !uri.startsWith(RUTA_BASE + "/");
    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {
        if (!registrarPeticion(request.getRemoteAddr())) {
            responseWriter.write(request, response, HttpStatus.TOO_MANY_REQUESTS, SecurityErrorCode.TOO_MANY_REQUESTS);
            return;
        }
        filterChain.doFilter(request, response);
    }

    /** Numero de IPs con peticiones recientes en memoria (visible para pruebas). */
    synchronized int clavesActivas() {
        return peticionesPorIp.size();
    }

    private synchronized boolean registrarPeticion(final String ip) {
        final String clave = ip == null ? "desconocida" : ip;
        final Instant ahora = clock.instant();
        final Instant desde = ahora.minusSeconds(VENTANA_SEGUNDOS);

        final Deque<Instant> peticiones = peticionesPorIp.computeIfAbsent(clave, k -> new ArrayDeque<>());
        while (!peticiones.isEmpty() && !peticiones.peekFirst().isAfter(desde)) {
            peticiones.pollFirst();
        }
        final boolean permitida = peticiones.size() < limitePorMinuto;
        if (permitida) {
            peticiones.addLast(ahora);
        }
        while (peticionesPorIp.size() > maxClaves) {
            final String masAntigua = peticionesPorIp.keySet().iterator().next();
            peticionesPorIp.remove(masAntigua);
        }
        return permitida;
    }
}
