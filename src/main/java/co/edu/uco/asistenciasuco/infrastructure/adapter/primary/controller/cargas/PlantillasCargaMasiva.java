package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.cargas;

import java.nio.charset.StandardCharsets;

public final class PlantillasCargaMasiva {

    private static final byte[] BOM_UTF8 = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private static final String PLANTILLA_DOCENTES_TEXTO =
            "tipo_documento;numero_documento;primer_nombre;segundo_nombre;primer_apellido;segundo_apellido;correo;clave_inicial;codigo_tipo_vinculacion;codigo_programa\r\n"
                    + "CC;1017000001;Docente;Ejemplo;Perez;Gomez;docente.ejemplo@uco.edu.co;ClaveTemporal123!;TC;ING-SIS\r\n";

    private static final String PLANTILLA_COORDINADORES_TEXTO =
            "tipo_documento;numero_documento;primer_nombre;segundo_nombre;primer_apellido;segundo_apellido;correo;clave_inicial;codigo_programa\r\n"
                    + "CC;1017000002;Coordinador;Ejemplo;Lopez;Diaz;coordinador.ejemplo@uco.edu.co;ClaveTemporal123!;ING-SIS\r\n";

    private static final String PLANTILLA_DECANOS_TEXTO =
            "tipo_documento;numero_documento;primer_nombre;segundo_nombre;primer_apellido;segundo_apellido;correo;clave_inicial;codigo_facultad\r\n"
                    + "CC;1017000003;Decano;Ejemplo;Ramirez;Castro;decano.ejemplo@uco.edu.co;ClaveTemporal123!;FAC-ING\r\n";

    private PlantillasCargaMasiva() {
    }

    public static byte[] plantillaDocentes() {
        return combinarBom(PLANTILLA_DOCENTES_TEXTO);
    }

    public static byte[] plantillaCoordinadores() {
        return combinarBom(PLANTILLA_COORDINADORES_TEXTO);
    }

    public static byte[] plantillaDecanos() {
        return combinarBom(PLANTILLA_DECANOS_TEXTO);
    }

    private static byte[] combinarBom(final String contenido) {
        final byte[] textoBytes = contenido.getBytes(StandardCharsets.UTF_8);
        final byte[] resultado = new byte[BOM_UTF8.length + textoBytes.length];
        System.arraycopy(BOM_UTF8, 0, resultado, 0, BOM_UTF8.length);
        System.arraycopy(textoBytes, 0, resultado, BOM_UTF8.length, textoBytes.length);
        return resultado;
    }
}
