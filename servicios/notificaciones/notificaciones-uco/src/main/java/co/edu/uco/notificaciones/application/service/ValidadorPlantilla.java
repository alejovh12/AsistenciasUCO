package co.edu.uco.notificaciones.application.service;

import co.edu.uco.notificaciones.domain.ErrorNegocio;
import co.edu.uco.notificaciones.domain.Marcador;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reglas de una nueva version de plantilla: largos, marcadores obligatorios presentes y solo marcadores permitidos. */
public final class ValidadorPlantilla {

    public static final int MAX_ASUNTO = 200;
    public static final int MAX_CUERPO = 4000;

    private static final Pattern TOKEN = Pattern.compile("\\{([^{}]*)}");

    private ValidadorPlantilla() {
    }

    public static void validar(final List<Marcador> marcadores, final String asunto, final String cuerpo) {
        if (asunto == null || asunto.isBlank() || asunto.length() > MAX_ASUNTO) {
            throw ErrorNegocio.validacion("PLANTILLA_ASUNTO_INVALIDO",
                    "El asunto es obligatorio y admite hasta " + MAX_ASUNTO + " caracteres.");
        }
        if (cuerpo == null || cuerpo.isBlank() || cuerpo.length() > MAX_CUERPO) {
            throw ErrorNegocio.validacion("PLANTILLA_CUERPO_INVALIDO",
                    "El cuerpo es obligatorio y admite hasta " + MAX_CUERPO + " caracteres.");
        }
        if (marcadores.isEmpty()) {
            throw ErrorNegocio.validacion("PLANTILLA_SIN_MARCADORES", "El tipo no tiene marcadores declarados.");
        }
        final String texto = asunto + "\n" + cuerpo;
        final Set<String> permitidos = new HashSet<>();
        for (final Marcador m : marcadores) {
            permitidos.add(m.marcador());
            if (m.obligatorio() && !texto.contains("{" + m.marcador() + "}")) {
                throw ErrorNegocio.validacion("PLANTILLA_MARCADOR_OBLIGATORIO_AUSENTE",
                        "Falta el marcador obligatorio {" + m.marcador() + "}.");
            }
        }
        final Matcher matcher = TOKEN.matcher(texto);
        final StringBuilder sinTokens = new StringBuilder();
        int cursor = 0;
        while (matcher.find()) {
            final String nombre = matcher.group(1);
            if (!permitidos.contains(nombre)) {
                throw ErrorNegocio.validacion("PLANTILLA_MARCADOR_NO_PERMITIDO",
                        "El marcador {" + nombre + "} no esta permitido para este tipo.");
            }
            sinTokens.append(texto, cursor, matcher.start());
            cursor = matcher.end();
        }
        sinTokens.append(texto.substring(cursor));
        if (sinTokens.indexOf("{") >= 0 || sinTokens.indexOf("}") >= 0) {
            throw ErrorNegocio.validacion("PLANTILLA_LLAVES_INVALIDAS", "Las llaves de la plantilla no estan balanceadas.");
        }
    }
}
