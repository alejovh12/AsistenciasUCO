package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.security.catalog;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.mock.web.MockServletContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Inventario de los endpoints que Spring MVC registra de verdad: levanta un {@link RequestMappingHandlerMapping}
 * sobre los controladores de producción (definiciones perezosas: no se instancian ni necesitan sus puertos) y lee
 * {@code getHandlerMethods()}. Es la referencia contra la que se verifican el catálogo de acceso y el extractor.
 */
final class SpringMvcEndpointInventory {

    private static final String BASE_PACKAGE = "co.edu.uco.asistenciasuco";
    private static final Pattern VARIABLE_WITH_REGEX = Pattern.compile("\\{([^}:]+):[^}]*}");

    record Endpoint(String method, String path) implements Comparable<Endpoint> {

        @Override
        public int compareTo(final Endpoint other) {
            return Comparator.comparing(Endpoint::path).thenComparing(Endpoint::method).compare(this, other);
        }
    }

    private SpringMvcEndpointInventory() {
    }

    static Set<Endpoint> discover() {
        try (GenericWebApplicationContext context = new GenericWebApplicationContext(new MockServletContext())) {
            for (final Class<?> controller : productionControllers()) {
                final RootBeanDefinition definition = new RootBeanDefinition(controller);
                definition.setLazyInit(true);
                context.registerBeanDefinition(controller.getName(), definition);
            }
            context.registerBeanDefinition("requestMappingHandlerMapping",
                    new RootBeanDefinition(RequestMappingHandlerMapping.class));
            context.refresh();
            final Set<Endpoint> endpoints = new TreeSet<>();
            for (final RequestMappingInfo info : context.getBean(RequestMappingHandlerMapping.class)
                    .getHandlerMethods().keySet()) {
                final Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
                final List<String> verbs = methods.isEmpty()
                        ? List.of("ANY")
                        : methods.stream().map(Enum::name).sorted().toList();
                for (final String pattern : info.getPathPatternsCondition().getPatternValues()) {
                    final String path = VARIABLE_WITH_REGEX.matcher(pattern).replaceAll("{$1}");
                    verbs.forEach(verb -> endpoints.add(new Endpoint(verb, path)));
                }
            }
            return endpoints;
        }
    }

    private static List<Class<?>> productionControllers() {
        final ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));
        final List<Class<?>> controllers = new ArrayList<>();
        for (final BeanDefinition candidate : scanner.findCandidateComponents(BASE_PACKAGE)) {
            final Class<?> type = load(candidate.getBeanClassName());
            if (isProductionClass(type)) {
                controllers.add(type);
            }
        }
        controllers.sort(Comparator.comparing(Class::getName));
        return controllers;
    }

    /** Excluye los controladores sondeo de las pruebas ({@code target/test-classes}). */
    private static boolean isProductionClass(final Class<?> type) {
        final String location = type.getProtectionDomain().getCodeSource().getLocation().getPath();
        return !location.contains("test-classes");
    }

    private static Class<?> load(final String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("No se pudo cargar el controlador " + name, exception);
        }
    }
}
