package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.report;

import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.report.ReporteAsistenciaRow;
import co.edu.uco.asistenciasuco.application.secondaryports.report.ReporteExcelPort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Adaptador de {@link ReporteExcelPort} con Apache POI. Los rotulos salen del catalogo de mensajes cuando el codigo
 * {@code RPT_ASISTENCIA_*} existe y, si no, del respaldo en espanol con tildes. El tope de filas evita armar en memoria un
 * libro desmedido (los hilos virtuales no limitan el consumo por solicitud).
 */
public final class PoiReporteExcelAdapter implements ReporteExcelPort {

    public static final int TOPE_FILAS_POR_DEFECTO = 50_000;
    /** Zona institucional por defecto (parametro TIEMPO/ZONA_HORARIA_IANA); la base guarda las sesiones en UTC. */
    public static final ZoneId ZONA_INSTITUCIONAL_POR_DEFECTO = ZoneId.of("America/Bogota");
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    static final String HOJA = "Asistencia";
    private static final String PREFIJO_CATALOGO = "RPT_ASISTENCIA_";
    private static final String[][] ENCABEZADOS = {
            {"CODIGO_GRUPO", "Código del grupo"}, {"GRUPO", "Grupo"}, {"NUMERO_SESION", "Número de sesión"},
            {"SESION", "Sesión"}, {"INICIO", "Inicio"}, {"FIN", "Fin"}, {"DOCUMENTO", "Documento"},
            {"ESTUDIANTE", "Estudiante"}, {"CORREO", "Correo"}, {"ESTADO", "Estado de asistencia"},
            {"OBSERVACION", "Observación"}
    };
    private static final String ESTADO_SIN_REGISTRO = "SIN REGISTRO";
    private static final String ESTADO_ASISTIO = "ASISTIÓ";
    private static final String ESTADO_NO_ASISTIO = "NO ASISTIÓ";
    private static final String ESTADO_CANCELADA = "CANCELADA";
    private static final int COLUMNA_INICIO = 4;
    private static final int COLUMNA_FIN = 5;

    private final Function<String, Optional<String>> catalogo;
    private final int topeFilas;
    private final ZoneId zona;

    public PoiReporteExcelAdapter(final MessageCatalogPort messageCatalogPort, final int topeFilas) {
        this(messageCatalogPort, topeFilas, ZONA_INSTITUCIONAL_POR_DEFECTO);
    }

    public PoiReporteExcelAdapter(final MessageCatalogPort messageCatalogPort, final int topeFilas, final ZoneId zona) {
        this(messageCatalogPort == null ? codigo -> Optional.empty() : messageCatalogPort::findUserMessage, topeFilas, zona);
    }

    PoiReporteExcelAdapter(final Function<String, Optional<String>> catalogo, final int topeFilas) {
        this(catalogo, topeFilas, ZONA_INSTITUCIONAL_POR_DEFECTO);
    }

    PoiReporteExcelAdapter(final Function<String, Optional<String>> catalogo, final int topeFilas, final ZoneId zona) {
        this.zona = Objects.requireNonNull(zona, "La zona horaria institucional es obligatoria.");
        this.catalogo = Objects.requireNonNull(catalogo, "El catalogo de rotulos es obligatorio.");
        if (topeFilas < 1) {
            throw new IllegalArgumentException("El tope de filas del reporte debe ser positivo.");
        }
        this.topeFilas = topeFilas;
    }

    @Override
    public byte[] generarReporteAsistencia(final List<ReporteAsistenciaRow> filas) {
        if (filas.size() > topeFilas) {
            throw ValidationException.conParametros(MensajeUsuarioErrorCode.VAL_APP_022, topeFilas + " filas");
        }
        try (var libro = new XSSFWorkbook(); var salida = new ByteArrayOutputStream()) {
            final var hoja = libro.createSheet(HOJA);
            escribirEncabezado(hoja.createRow(0));
            int indice = 1;
            for (final ReporteAsistenciaRow fila : filas) {
                escribirFila(hoja.createRow(indice++), fila);
            }
            for (int columna = 0; columna < ENCABEZADOS.length; columna++) {
                hoja.autoSizeColumn(columna);
            }
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException exception) {
            throw new CrosscuttingException("No fue posible generar el libro de Excel del reporte de asistencia.", exception);
        }
    }

    private void escribirEncabezado(final Row fila) {
        for (int i = 0; i < ENCABEZADOS.length; i++) {
            final String texto = rotulo(ENCABEZADOS[i][0], ENCABEZADOS[i][1]);
            // Inicio y Fin se escriben en hora institucional: el encabezado dice la zona.
            fila.createCell(i).setCellValue(i == COLUMNA_INICIO || i == COLUMNA_FIN ? texto + " (" + zona.getId() + ")" : texto);
        }
    }

    private String rotulo(final String clave, final String respaldo) {
        return catalogo.apply(PREFIJO_CATALOGO + clave).filter(texto -> !texto.isBlank()).orElse(respaldo);
    }

    private void escribirFila(final Row fila, final ReporteAsistenciaRow origen) {
        fila.createCell(0).setCellValue(vacioSiNulo(origen.codigoGrupo()));
        fila.createCell(1).setCellValue(vacioSiNulo(origen.nombreGrupo()));
        fila.createCell(2).setCellValue(origen.numeroSesion() == null ? "" : origen.numeroSesion().toString());
        fila.createCell(3).setCellValue(vacioSiNulo(origen.nombreSesion()));
        fila.createCell(4).setCellValue(local(origen.fechaHoraInicio()));
        fila.createCell(5).setCellValue(local(origen.fechaHoraFin()));
        fila.createCell(6).setCellValue(vacioSiNulo(origen.documentoEstudiante()));
        fila.createCell(7).setCellValue(vacioSiNulo(origen.nombreEstudiante()));
        fila.createCell(8).setCellValue(vacioSiNulo(origen.correoEstudiante()));
        fila.createCell(9).setCellValue(estado(origen));
        fila.createCell(10).setCellValue(vacioSiNulo(origen.razonCausa()));
    }

    /** El valor de la base es UTC; se escribe en la zona institucional como dd/MM/yyyy HH:mm. */
    private String local(final LocalDateTime utc) {
        return utc == null ? "" : utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(zona).format(FORMATO_FECHA_HORA);
    }

    private static String vacioSiNulo(final String valor) {
        return valor == null ? "" : valor;
    }

    private String estado(final ReporteAsistenciaRow origen) {
        if (origen.sesionCancelada()) {
            return rotulo("ESTADO_CANCELADA", ESTADO_CANCELADA);
        }
        if (origen.asistio() == null) {
            return rotulo("ESTADO_SIN_REGISTRO", ESTADO_SIN_REGISTRO);
        }
        if (Boolean.TRUE.equals(origen.asistio())) {
            return rotulo("ESTADO_ASISTIO", ESTADO_ASISTIO);
        }
        return origen.razonCausa() == null || origen.razonCausa().isBlank()
                ? rotulo("ESTADO_NO_ASISTIO", ESTADO_NO_ASISTIO)
                : origen.razonCausa();
    }
}
