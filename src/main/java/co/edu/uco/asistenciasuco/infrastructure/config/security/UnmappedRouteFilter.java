package co.edu.uco.asistenciasuco.infrastructure.config.security;

import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.CommonErrorCode;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.handler.SecurityErrorResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;

import java.io.IOException;
import java.util.Objects;

/**
 * CULM-001 D.9-a (CONTRACT D §11.1, F-EST-024, OBS-17): una ruta que ningun {@code @RequestMapping} atiende responde 404
 * {@code RESOURCE_NOT_FOUND} sin consultar al PDP. Se registra despues de autenticar el Bearer y ANTES del PEP: asi una ruta
 * retirada (p. ej. {@code GET /estudiante/materias/{id}/prerrequisitos}) deja de anunciarse como 403 por caer bajo un comodin
 * del catalogo de acceso.
 *
 * <p>Sin credencial no decide nada: la cadena sigue y responde 401 (una ruta inexistente no revela si existe a un anonimo). Una
 * ruta existente con otro metodo, tipo de contenido o parametros incorrectos tiene handler y sigue su camino normal (405/415/400).
 * El manejador de recursos estaticos ({@code /**}) no cuenta como handler: solo los {@code @RequestMapping}.</p>
 *
 * <p>Consulta directamente los beans {@link RequestMappingHandlerMapping} del contexto MVC (antes via
 * {@code HandlerMappingIntrospector}, deprecado en Spring Framework 7). Si el contexto no tiene ninguno, no decide nada y la
 * cadena sigue: el PEP conserva la autorizacion.</p>
 */
public class UnmappedRouteFilter extends OncePerRequestFilter {

    private final ObjectProvider<RequestMappingHandlerMapping> handlerMappings;
    private final SecurityErrorResponseWriter responseWriter;

    public UnmappedRouteFilter(final ObjectProvider<RequestMappingHandlerMapping> handlerMappings,
            final SecurityErrorResponseWriter responseWriter) {
        this.handlerMappings = Objects.requireNonNull(handlerMappings, "Los RequestMappingHandlerMapping son obligatorios.");
        this.responseWriter = Objects.requireNonNull(responseWriter, "SecurityErrorResponseWriter es obligatorio.");
    }

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod()) || SecurityPublicEndpoints.pepExempt().matches(request);
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain)
            throws ServletException, IOException {
        if (!isAuthenticated() || tieneHandler(request)) {
            chain.doFilter(request, response);
            return;
        }
        responseWriter.write(request, response, HttpStatus.NOT_FOUND, CommonErrorCode.RESOURCE_NOT_FOUND);
    }

    private boolean tieneHandler(final HttpServletRequest request) {
        final boolean rutaParseada = ServletRequestPathUtils.hasParsedRequestPath(request);
        if (!rutaParseada) {
            ServletRequestPathUtils.parseAndCache(request);
        }
        try {
            boolean hayMappings = false;
            for (final RequestMappingHandlerMapping mapping : handlerMappings.orderedStream().toList()) {
                hayMappings = true;
                if (atiende(mapping, request)) {
                    return true;
                }
            }
            // Sin mappings MVC no hay base para afirmar que la ruta no existe: decide el resto de la cadena.
            return !hayMappings;
        } finally {
            if (!rutaParseada) {
                ServletRequestPathUtils.clearParsedRequestPath(request);
            }
        }
    }

    private static boolean atiende(final RequestMappingHandlerMapping mapping, final HttpServletRequest request) {
        try {
            return mapping.getHandler(request) != null;
        } catch (final Exception coincidenciaParcial) {
            // 405, 415, 406 o parametros ausentes: la ruta existe; el DispatcherServlet produce el error propio.
            return true;
        }
    }

    private static boolean isAuthenticated() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
