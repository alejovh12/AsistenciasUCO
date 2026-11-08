package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.cargas;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.exception.CargaMasivaErrorCode;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.secondaryports.CsvCargaUsuariosParserPort;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.ErrorFilaCargaDomain;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.FilaCsvParseada;
import co.edu.uco.asistenciasuco.application.features.cargamasiva.usecase.domain.PerfilDestinoCarga;
import co.edu.uco.asistenciasuco.application.features.usuario.domain.rules.PasswordRegistroRule;
import co.edu.uco.asistenciasuco.application.features.usuario.exception.UsuarioErrorCode;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CsvCargaUsuariosParser implements CsvCargaUsuariosParserPort {

    public static final int TAMANIO_MAXIMO_BYTES = 1048576; // 1 MiB
    public static final int MAXIMO_FILAS = 100;

    private static final Set<Character> PREFIJOS_FORMULA = Set.of('=', '+', '-', '@', '\t', '\r');

    @Override
    public List<FilaCsvParseada> parsear(final byte[] contenidoBytes, final PerfilDestinoCarga perfilDestino) {
        if (contenidoBytes == null || contenidoBytes.length == 0) {
            throw new ValidationException(CargaMasivaErrorCode.CMA_013);
        }
        if (contenidoBytes.length > TAMANIO_MAXIMO_BYTES) {
            throw new ValidationException(CargaMasivaErrorCode.CMA_002);
        }

        final String textoCsv = decodificarUtf8(contenidoBytes);
        final List<List<String>> lineasParseadas = parsearLineasRfc4180(textoCsv);

        // Filtrar lineas completamente vacias
        final List<LineaConNumero> lineasValidas = new ArrayList<>();
        for (int i = 0; i < lineasParseadas.size(); i++) {
            final List<String> columnas = lineasParseadas.get(i);
            if (!esLineaVacia(columnas)) {
                lineasValidas.add(new LineaConNumero(i + 1, columnas));
            }
        }

        if (lineasValidas.isEmpty()) {
            throw new ValidationException(CargaMasivaErrorCode.CMA_004);
        }

        final LineaConNumero lineaCabecera = lineasValidas.get(0);
        final Map<String, Integer> indiceCabeceras = procesarCabecera(lineaCabecera.columnas(), perfilDestino);

        final List<LineaConNumero> lineasDatos = lineasValidas.subList(1, lineasValidas.size());
        if (lineasDatos.size() > MAXIMO_FILAS) {
            throw new ValidationException(CargaMasivaErrorCode.CMA_001);
        }

        final List<FilaCsvParseada> resultado = new ArrayList<>();
        final Map<String, Integer> documentosVistos = new HashMap<>();
        final Map<String, Integer> correosVistos = new HashMap<>();

        for (final LineaConNumero lineaDato : lineasDatos) {
            final int numeroFila = lineaDato.numeroLinea();
            final List<String> celdas = lineaDato.columnas();
            final List<ErrorFilaCargaDomain> erroresLocales = new ArrayList<>();

            final String tipoDoc = valor(celdas, indiceCabeceras, "tipo_documento");
            final String numDoc = valor(celdas, indiceCabeceras, "numero_documento");
            final String primerNombre = valor(celdas, indiceCabeceras, "primer_nombre");
            final String segundoNombre = valor(celdas, indiceCabeceras, "segundo_nombre");
            final String primerApellido = valor(celdas, indiceCabeceras, "primer_apellido");
            final String segundoApellido = valor(celdas, indiceCabeceras, "segundo_apellido");
            final String correo = valor(celdas, indiceCabeceras, "correo");
            final String claveInicial = valor(celdas, indiceCabeceras, "clave_inicial");
            final String tipoVinc = valor(celdas, indiceCabeceras, "codigo_tipo_vinculacion");
            final String codProg = valor(celdas, indiceCabeceras, "codigo_programa");
            final String codFac = valor(celdas, indiceCabeceras, "codigo_facultad");

            // Validacion de formulas (excepto clave_inicial)
            validarFormula("tipo_documento", tipoDoc, erroresLocales);
            validarFormula("numero_documento", numDoc, erroresLocales);
            validarFormula("primer_nombre", primerNombre, erroresLocales);
            validarFormula("segundo_nombre", segundoNombre, erroresLocales);
            validarFormula("primer_apellido", primerApellido, erroresLocales);
            validarFormula("segundo_apellido", segundoApellido, erroresLocales);
            validarFormula("correo", correo, erroresLocales);
            validarFormula("codigo_tipo_vinculacion", tipoVinc, erroresLocales);
            validarFormula("codigo_programa", codProg, erroresLocales);
            validarFormula("codigo_facultad", codFac, erroresLocales);

            // Validacion numero de documento
            Integer numDocEntero = null;
            if (numDoc.isBlank()) {
                erroresLocales.add(new ErrorFilaCargaDomain("numero_documento", "CMA_005", "El número de documento es obligatorio."));
            } else {
                try {
                    final long n = Long.parseLong(numDoc.trim());
                    if (n <= 0 || n > Integer.MAX_VALUE) {
                        erroresLocales.add(new ErrorFilaCargaDomain("numero_documento", "CMA_005", "Número de documento inválido."));
                    } else {
                        numDocEntero = (int) n;
                    }
                } catch (NumberFormatException e) {
                    erroresLocales.add(new ErrorFilaCargaDomain("numero_documento", "CMA_005", "Número de documento inválido."));
                }
            }

            // Validacion nombres y apellidos
            if (primerNombre.isBlank()) {
                erroresLocales.add(new ErrorFilaCargaDomain("primer_nombre", "VAL_001", "El primer nombre es obligatorio."));
            }
            if (primerApellido.isBlank()) {
                erroresLocales.add(new ErrorFilaCargaDomain("primer_apellido", "VAL_001", "El primer apellido es obligatorio."));
            }

            // Validacion correo
            if (correo.isBlank()) {
                erroresLocales.add(new ErrorFilaCargaDomain("correo", "VAL_001", "El correo es obligatorio."));
            } else if (!correo.contains("@")) {
                erroresLocales.add(new ErrorFilaCargaDomain("correo", "VCE_001", "El correo no tiene un formato válido."));
            }

            // Validacion clave inicial si presente
            if (!claveInicial.isBlank()) {
                try {
                    PasswordRegistroRule.resolver(claveInicial, numDocEntero == null ? 1 : numDocEntero);
                } catch (final RuntimeException ex) {
                    erroresLocales.add(new ErrorFilaCargaDomain("clave_inicial", UsuarioErrorCode.ERR_PASSWORD_POLITICA_INVALIDA.code(), "La clave inicial no cumple la política de seguridad."));
                }
            }

            // Validacion campos por rol
            if (perfilDestino == PerfilDestinoCarga.DOCENTE && tipoVinc.isBlank()) {
                erroresLocales.add(new ErrorFilaCargaDomain("codigo_tipo_vinculacion", "VAL_001", "El código de tipo de vinculación es obligatorio para docentes."));
            }
            if (perfilDestino == PerfilDestinoCarga.COORDINADOR && codProg.isBlank()) {
                erroresLocales.add(new ErrorFilaCargaDomain("codigo_programa", "VAL_001", "El código de programa es obligatorio para coordinadores."));
            }

            // Deteccion de duplicados en el archivo (CMA_010)
            if (!numDoc.isBlank()) {
                final String docKey = tipoDoc.trim().toUpperCase(Locale.ROOT) + ":" + numDoc.trim();
                if (documentosVistos.containsKey(docKey)) {
                    final int primeraAparicion = documentosVistos.get(docKey);
                    erroresLocales.add(new ErrorFilaCargaDomain("numero_documento", "CMA_010", "Fila repetida en el archivo (línea " + primeraAparicion + ")."));
                } else {
                    documentosVistos.put(docKey, numeroFila);
                }
            }
            if (!correo.isBlank()) {
                final String correoKey = correo.trim().toLowerCase(Locale.ROOT);
                if (correosVistos.containsKey(correoKey)) {
                    final int primeraAparicion = correosVistos.get(correoKey);
                    erroresLocales.add(new ErrorFilaCargaDomain("correo", "CMA_010", "Fila repetida en el archivo (línea " + primeraAparicion + ")."));
                } else {
                    correosVistos.put(correoKey, numeroFila);
                }
            }

            resultado.add(new FilaCsvParseada(
                    numeroFila,
                    tipoDoc.trim(),
                    numDoc.trim(),
                    primerNombre.trim(),
                    segundoNombre.trim(),
                    primerApellido.trim(),
                    segundoApellido.trim(),
                    correo.trim(),
                    tipoVinc.trim(),
                    codProg.trim(),
                    codFac.trim(),
                    claveInicial,
                    erroresLocales
            ));
        }

        return resultado;
    }

    private static String decodificarUtf8(final byte[] bytes) {
        final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            final String texto = decoder.decode(java.nio.ByteBuffer.wrap(bytes)).toString();
            if (texto.startsWith("\uFEFF")) {
                return texto.substring(1);
            }
            return texto;
        } catch (CharacterCodingException e) {
            throw new ValidationException(CargaMasivaErrorCode.CMA_013);
        }
    }

    private static boolean esLineaVacia(final List<String> celdas) {
        return celdas.stream().allMatch(String::isBlank);
    }

    private static void validarFormula(final String columna, final String valor, final List<ErrorFilaCargaDomain> errores) {
        if (valor != null && !valor.isEmpty()) {
            final char primerCaracter = valor.charAt(0);
            if (PREFIJOS_FORMULA.contains(primerCaracter)) {
                errores.add(new ErrorFilaCargaDomain(columna, "CMA_011", "Valor no permitido en la columna " + columna + "."));
            }
        }
    }

    private static String valor(final List<String> celdas, final Map<String, Integer> indice, final String columna) {
        final Integer pos = indice.get(columna);
        if (pos == null || pos >= celdas.size()) {
            return "";
        }
        final String val = celdas.get(pos);
        return val == null ? "" : val;
    }

    private static Map<String, Integer> procesarCabecera(final List<String> cabeceras, final PerfilDestinoCarga perfilDestino) {
        final Map<String, Integer> resultado = new HashMap<>();
        for (int i = 0; i < cabeceras.size(); i++) {
            final String nombre = cabeceras.get(i).trim().toLowerCase(Locale.ROOT);
            if (!nombre.isEmpty()) {
                resultado.put(nombre, i);
            }
        }

        final Set<String> obligatorias = new HashSet<>(Arrays.asList(
                "tipo_documento", "numero_documento", "primer_nombre", "primer_apellido", "correo", "clave_inicial"
        ));
        final Set<String> permitidas = new HashSet<>(Arrays.asList(
                "tipo_documento", "numero_documento", "primer_nombre", "segundo_nombre",
                "primer_apellido", "segundo_apellido", "correo", "clave_inicial"
        ));

        switch (perfilDestino) {
            case DOCENTE -> {
                obligatorias.add("codigo_tipo_vinculacion");
                permitidas.add("codigo_tipo_vinculacion");
                permitidas.add("codigo_programa");
            }
            case COORDINADOR -> {
                obligatorias.add("codigo_programa");
                permitidas.add("codigo_programa");
            }
            case DECANO -> {
                permitidas.add("codigo_facultad");
            }
        }

        for (final String obligatoria : obligatorias) {
            if (!resultado.containsKey(obligatoria)) {
                throw new ValidationException(CargaMasivaErrorCode.CMA_004);
            }
        }

        for (final String presente : resultado.keySet()) {
            if (!permitidas.contains(presente)) {
                throw new ValidationException(CargaMasivaErrorCode.CMA_004);
            }
        }

        return resultado;
    }

    private static List<List<String>> parsearLineasRfc4180(final String texto) {
        final char delimitador = detectarDelimitador(texto);
        final List<List<String>> lineas = new ArrayList<>();
        List<String> filaActual = new ArrayList<>();
        final StringBuilder celdaActual = new StringBuilder();
        boolean dentroComillas = false;

        for (int i = 0; i < texto.length(); i++) {
            final char c = texto.charAt(i);

            if (dentroComillas) {
                if (c == '"') {
                    if (i + 1 < texto.length() && texto.charAt(i + 1) == '"') {
                        celdaActual.append('"');
                        i++; // saltar comilla escapada
                    } else {
                        dentroComillas = false;
                    }
                } else {
                    celdaActual.append(c);
                }
            } else {
                if (c == '"') {
                    dentroComillas = true;
                } else if (c == delimitador) {
                    filaActual.add(celdaActual.toString());
                    celdaActual.setLength(0);
                } else if (c == '\r') {
                    if (i + 1 < texto.length() && texto.charAt(i + 1) == '\n') {
                        i++; // saltar \n de CRLF
                    }
                    filaActual.add(celdaActual.toString());
                    celdaActual.setLength(0);
                    lineas.add(filaActual);
                    filaActual = new ArrayList<>();
                } else if (c == '\n') {
                    filaActual.add(celdaActual.toString());
                    celdaActual.setLength(0);
                    lineas.add(filaActual);
                    filaActual = new ArrayList<>();
                } else {
                    celdaActual.append(c);
                }
            }
        }

        if (celdaActual.length() > 0 || !filaActual.isEmpty()) {
            filaActual.add(celdaActual.toString());
            lineas.add(filaActual);
        }

        return lineas;
    }

    private static char detectarDelimitador(final String texto) {
        int finPrimeraLinea = texto.indexOf('\n');
        if (finPrimeraLinea == -1) {
            finPrimeraLinea = texto.length();
        }
        final String primeraLinea = texto.substring(0, finPrimeraLinea);
        int puntosYComa = 0;
        int comas = 0;
        boolean enComillas = false;
        for (int i = 0; i < primeraLinea.length(); i++) {
            final char c = primeraLinea.charAt(i);
            if (c == '"') {
                enComillas = !enComillas;
            } else if (!enComillas) {
                if (c == ';') puntosYComa++;
                else if (c == ',') comas++;
            }
        }
        return puntosYComa >= comas ? ';' : ',';
    }

    private record LineaConNumero(int numeroLinea, List<String> columnas) {}
}
