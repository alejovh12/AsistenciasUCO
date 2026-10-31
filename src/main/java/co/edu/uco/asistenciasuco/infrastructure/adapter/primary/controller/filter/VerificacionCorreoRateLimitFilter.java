package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.filter;

import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.SecurityErrorCode;
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
 * IDENT-001 (CA-07/D9): limite de intentos en memoria por IP sobre {@code POST /api/v1/public/verificar-correo}, la
 * unica ruta publica nueva. Cuenta solo {@code remoteAddr}: {@code X-Forwarded-For} se ignora porque es falsificable
 * (si en el futuro hay un proxy de confianza se resuelve en un paso explicito aparte). La memoria esta acotada: al
 * superar {@code maxClaves} se desaloja la clave menos reciente. En un despliegue con varias instancias el limite es por
 * instancia; es una defensa adicional y no reemplaza al token de alta entropia de un solo uso.
 */
public final class VerificacionCorreoRateLimitFilter extends OncePerRequestFilter {

    public static final String RUTA = "/api/v1/public/verificar-correo";
    private static final long VENTANA_SEGUNDOS = 60;

    private final int limitePorMinuto;
    private final int maxClaves;
    private final Clock clock;
    private final SecurityErrorResponseWriter responseWriter;
    private final Map<String, Deque<Instant>> intentosPorIp;

    public VerificacionCorreoRateLimitFilter(
            final int limitePorMinuto,
            final int maxClaves,
            final Clock clock,
            final SecurityErrorResponseWriter responseWriter
    ) {
        if (limitePorMinuto < 1) {
            throw new IllegalArgumentException("El limite de intentos por minuto debe ser al menos 1.");
        }
        if (maxClaves < 1) {
            throw new IllegalArgumentException("El maximo de claves del limitador debe ser al menos 1.");
        }
        this.limitePorMinuto = limitePorMinuto;
        this.maxClaves = maxClaves;
        this.clock = Objects.requireNonNull(clock, "Clock es obligatorio.");
        this.responseWriter = Objects.requireNonNull(responseWriter, "SecurityErrorResponseWriter es obligatorio.");
        this.intentosPorIp = new LinkedHashMap<>(16, 0.75f, true);
    }

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod()) || !RUTA.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain
    ) throws ServletException, IOException {
        if (!registrarIntento(request.getRemoteAddr())) {
            responseWriter.write(request, response, HttpStatus.TOO_MANY_REQUESTS, SecurityErrorCode.TOO_MANY_REQUESTS);
            return;
        }
        filterChain.doFilter(request, response);
    }

    /** Numero de IPs con intentos recientes en memoria (visible para pruebas). */
    synchronized int clavesActivas() {
        return intentosPorIp.size();
    }

    private synchronized boolean registrarIntento(final String ip) {
        final String clave = ip == null ? "desconocida" : ip;
        final Instant ahora = clock.instant();
        final Instant desde = ahora.minusSeconds(VENTANA_SEGUNDOS);

        final Deque<Instant> intentos = intentosPorIp.computeIfAbsent(clave, k -> new ArrayDeque<>());
        while (!intentos.isEmpty() && !intentos.peekFirst().isAfter(desde)) {
            intentos.pollFirst();
        }
        final boolean permitido = intentos.size() < limitePorMinuto;
        if (permitido) {
            intentos.addLast(ahora);
        }
        while (intentosPorIp.size() > maxClaves) {
            final String masAntigua = intentosPorIp.keySet().iterator().next();
            intentosPorIp.remove(masAntigua);
        }
        return permitido;
    }
}
