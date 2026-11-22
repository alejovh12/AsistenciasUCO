package co.edu.uco.asistenciasuco.application.exception.catalog;

import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorDefinition;
import co.edu.uco.asistenciasuco.crosscutting.exception.ErrorKind;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.CommonErrorCode;
import co.edu.uco.asistenciasuco.crosscutting.exception.catalog.SecurityErrorCode;

/**
 * VAL-oleada3 MSG-05: mensajes de usuario de las validaciones, accesos y recursos inexistentes del backend.
 *
 * <p>Cada constante conserva el codigo publico de la respuesta ({@code VALIDATION_ERROR}, {@code FORBIDDEN},
 * {@code RESOURCE_NOT_FOUND} o el codigo propio que ya tenia, como {@code ERR_CAMPO_OBLIGATORIO}) y el estado HTTP, pero su
 * texto sale del catalogo de mensajes por {@link #messageCode()}, que es el nombre de la constante: {@code VAL_APP_###},
 * {@code ACC_APP_###}, {@code NOF_APP_###}, {@code DIS_APP_###} o un codigo que la BD ya tenia ({@code GEN_001},
 * {@code PER_001}, {@code PAG_001}, {@code KPI_001}). El texto de aqui es solo el respaldo cuando el catalogo no responde.
 * Las filas del catalogo estan en {@code schema/seed/62_val_mensajes_aplicacion.sql} (migracion 41) de la BD; la prueba
 * {@code MensajeUsuarioErrorCodeCatalogoTest} exige que cada codigo este en el catalogo de usuario.</p>
 */
public enum MensajeUsuarioErrorCode implements ErrorDefinition {
    VAL_APP_001(CommonErrorCode.VALIDATION_ERROR, "Los datos de la facultad son obligatorios."),
    VAL_APP_002(CommonErrorCode.VALIDATION_ERROR, "El identificador de la facultad es obligatorio."),
    VAL_APP_003(CommonErrorCode.VALIDATION_ERROR, "Debe indicar al menos el nombre o el decano de la facultad."),
    VAL_APP_004(CommonErrorCode.VALIDATION_ERROR, "El nombre de la facultad debe tener entre 3 y 50 caracteres."),
    VAL_APP_005(CommonErrorCode.VALIDATION_ERROR, "Los datos para actualizar el parametro no son validos."),
    VAL_APP_006(CommonErrorCode.VALIDATION_ERROR, "Los datos del cambio de estado del decano son obligatorios."),
    VAL_APP_007(CommonErrorCode.VALIDATION_ERROR, "El decano es obligatorio."),
    VAL_APP_008(CommonErrorCode.VALIDATION_ERROR, "El usuario autenticado es obligatorio."),
    VAL_APP_009(CommonErrorCode.VALIDATION_ERROR, "El estado deseado de la facultad es obligatorio."),
    VAL_APP_010(CommonErrorCode.VALIDATION_ERROR, "El parametro es obligatorio."),
    VAL_APP_011(CommonErrorCode.VALIDATION_ERROR, "Los datos de la consulta son obligatorios."),
    VAL_APP_012(CommonErrorCode.VALIDATION_ERROR, "La granularidad debe ser SEMANA o MES."),
    VAL_APP_013(CommonErrorCode.VALIDATION_ERROR, "Indique una facultad existente o una facultad nueva, no ambas."),
    VAL_APP_014(CommonErrorCode.VALIDATION_ERROR, "El nombre de la facultad es obligatorio y debe tener entre 3 y 50 caracteres."),
    VAL_APP_015(CommonErrorCode.VALIDATION_ERROR, "El decano titular de la facultad es obligatorio."),
    VAL_APP_016(CommonErrorCode.VALIDATION_ERROR, "El usuario ejecutor es obligatorio."),
    VAL_APP_017(CommonErrorCode.VALIDATION_ERROR, "El periodo academico a modificar es obligatorio."),
    VAL_APP_018(CommonErrorCode.VALIDATION_ERROR, "El periodo academico a eliminar es obligatorio."),
    VAL_APP_019(CommonErrorCode.VALIDATION_ERROR, "El nombre del periodo academico es obligatorio."),
    VAL_APP_020(CommonErrorCode.VALIDATION_ERROR, "La fecha de inicio del periodo debe ser anterior a la fecha de fin."),
    VAL_APP_021(CommonErrorCode.VALIDATION_ERROR, "El archivo adjunto no puede estar vacio."),
    VAL_APP_022(CommonErrorCode.VALIDATION_ERROR, "El archivo excede el tamano maximo permitido ({})."),
    VAL_APP_023(CommonErrorCode.VALIDATION_ERROR, "Formato de archivo no permitido. Solo se aceptan archivos PDF, PNG o JPG."),
    VAL_APP_024(CommonErrorCode.VALIDATION_ERROR, "El Content-Type declarado no coincide con la extension del archivo."),
    VAL_APP_025(CommonErrorCode.VALIDATION_ERROR, "El contenido del archivo no coincide con su extension declarada."),
    VAL_APP_026(CommonErrorCode.VALIDATION_ERROR, "Nombre de archivo no valido."),
    VAL_APP_027(CommonErrorCode.VALIDATION_ERROR, "El nombre del archivo excede la longitud maxima permitida (250 caracteres)."),
    VAL_APP_028(CommonErrorCode.VALIDATION_ERROR, "El archivo fue rechazado: se detecto una firma de malware conocida."),
    VAL_APP_029(CommonErrorCode.VALIDATION_ERROR, "El estado de vinculo debe ser ACTIVO, SIN_VINCULO o TODOS."),
    VAL_APP_030(CommonErrorCode.VALIDATION_ERROR, "El estudiante es obligatorio."),
    VAL_APP_031(CommonErrorCode.VALIDATION_ERROR, "La fecha inicial debe ser anterior a la fecha final."),
    VAL_APP_032(CommonErrorCode.VALIDATION_ERROR, "El rango de fechas no puede superar {} dias."),
    VAL_APP_033(CommonErrorCode.VALIDATION_ERROR, "El resultado debe ser SUCCESS o FAILURE."),
    VAL_APP_034(CommonErrorCode.VALIDATION_ERROR, "El filtro {} excede la longitud permitida."),
    VAL_APP_035(CommonErrorCode.VALIDATION_ERROR, "Los filtros de consulta son obligatorios."),
    VAL_APP_036(CommonErrorCode.VALIDATION_ERROR, "El semaforo debe ser VERDE, AMBAR o ROJO."),
    VAL_APP_037(CommonErrorCode.VALIDATION_ERROR, "La fecha desde no puede ser posterior a la fecha hasta."),
    VAL_APP_038(CommonErrorCode.VALIDATION_ERROR, "La pagina debe ser mayor o igual a 1."),
    VAL_APP_039(CommonErrorCode.VALIDATION_ERROR, "El tamano de pagina debe estar entre 1 y {}."),
    VAL_APP_040(CommonErrorCode.VALIDATION_ERROR, "El numero de semestre debe ser un entero positivo."),
    VAL_APP_041(CommonErrorCode.VALIDATION_ERROR, "El numero de pagina debe ser mayor o igual a 1."),
    VAL_APP_042(CommonErrorCode.VALIDATION_ERROR, "El tamano de pagina debe ser mayor o igual a 1."),
    VAL_APP_043(CommonErrorCode.VALIDATION_ERROR, "Los datos del programa son obligatorios."),
    VAL_APP_044(CommonErrorCode.VALIDATION_ERROR, "La busqueda excede la longitud permitida."),
    VAL_APP_045(CommonErrorCode.VALIDATION_ERROR, "El nombre del programa academico es obligatorio."),
    VAL_APP_046(CommonErrorCode.VALIDATION_ERROR, "El nombre del programa academico no puede superar {} caracteres."),
    VAL_APP_047(CommonErrorCode.VALIDATION_ERROR, "Los parametros de paginacion deben ser mayores o iguales a 1."),
    VAL_APP_048(CommonErrorCode.VALIDATION_ERROR, "Indique un programa existente o un programa nuevo, no ambos."),
    VAL_APP_049(CommonErrorCode.VALIDATION_ERROR, "El tipo del programa nuevo es obligatorio."),
    VAL_APP_050(CommonErrorCode.VALIDATION_ERROR, "La contrasena inicial del coordinador es obligatoria."),
    VAL_APP_051(CommonErrorCode.VALIDATION_ERROR, "La solicitud de revision es obligatoria."),
    VAL_APP_052(CommonErrorCode.VALIDATION_ERROR, "El estado de la solicitud no es valido."),
    VAL_APP_053(CommonErrorCode.VALIDATION_ERROR, "El grupo y el estudiante son obligatorios."),
    VAL_APP_054(CommonErrorCode.VALIDATION_ERROR, "El motivo del retiro es obligatorio y no puede superar 500 caracteres."),
    VAL_APP_055(CommonErrorCode.VALIDATION_ERROR, "Los datos del retiro del reclamo son obligatorios."),
    VAL_APP_056(CommonErrorCode.VALIDATION_ERROR, "El reclamo a retirar es obligatorio."),
    VAL_APP_057(CommonErrorCode.VALIDATION_ERROR, "Los datos de la busqueda son obligatorios."),
    VAL_APP_058(CommonErrorCode.VALIDATION_ERROR, "El grupo es obligatorio."),
    VAL_APP_059(CommonErrorCode.VALIDATION_ERROR, "El tipo de identificacion es obligatorio."),
    VAL_APP_060(CommonErrorCode.VALIDATION_ERROR, "El numero de documento debe ser un entero positivo."),
    VAL_APP_061(CommonErrorCode.VALIDATION_ERROR, "El tipo de usuario a corregir no es valido."),
    VAL_APP_062(CommonErrorCode.VALIDATION_ERROR, "Los datos del registro son obligatorios."),
    VAL_APP_063(CommonErrorCode.VALIDATION_ERROR, "El cuerpo de la peticion es obligatorio."),
    VAL_APP_064(CommonErrorCode.VALIDATION_ERROR, "El tipo de programa es obligatorio."),
    VAL_APP_065(CommonErrorCode.VALIDATION_ERROR, "El coordinador del programa es obligatorio."),
    VAL_APP_066(CommonErrorCode.VALIDATION_ERROR, "El estado deseado del programa es obligatorio."),
    VAL_APP_067(Codigos.ERR_CAMPO_OBLIGATORIO, ErrorKind.VALIDATION, "Cada franja del horario es obligatoria."),
    VAL_APP_068(Codigos.ERR_CAMPO_OBLIGATORIO, ErrorKind.VALIDATION, "El campo dias es obligatorio cuando se envia el horario."),
    VAL_APP_069(Codigos.ERR_CAMPO_OBLIGATORIO, ErrorKind.VALIDATION, "El campo {} es obligatorio."),
    VAL_APP_070("ERR_FECHA_HORA_INVALIDA", ErrorKind.VALIDATION, "El campo {} debe usar formato ISO local {}."),
    PAG_001(CommonErrorCode.VALIDATION_ERROR, "Los parametros de paginacion no son validos."),
    VAL_APP_071(CommonErrorCode.VALIDATION_ERROR, "Los datos de la consulta no son validos."),
    VAL_APP_072(CommonErrorCode.VALIDATION_ERROR, "La sesion es obligatoria."),
    VAL_APP_073(CommonErrorCode.VALIDATION_ERROR, "El identificador del estudiante es obligatorio."),
    VAL_APP_074(CommonErrorCode.VALIDATION_ERROR, "El identificador del docente es obligatorio."),
    VAL_APP_075(CommonErrorCode.VALIDATION_ERROR, "El identificador del programa es obligatorio."),
    VAL_APP_076(Codigos.ERR_CAMPO_OBLIGATORIO, ErrorKind.VALIDATION, "El horario del grupo es obligatorio: envie al menos una franja."),
    ACC_APP_001(SecurityErrorCode.FORBIDDEN, "El docente autenticado no tiene titularidad sobre el grupo consultado."),
    ACC_APP_002(SecurityErrorCode.FORBIDDEN, "El docente autenticado no tiene titularidad sobre el grupo solicitado."),
    ACC_APP_003(SecurityErrorCode.FORBIDDEN, "No fue posible resolver el docente autenticado."),
    ACC_APP_004(SecurityErrorCode.FORBIDDEN, "No fue posible resolver el estudiante autenticado."),
    ACC_APP_005(SecurityErrorCode.FORBIDDEN, "No fue posible resolver el usuario autenticado."),
    ACC_APP_006(SecurityErrorCode.FORBIDDEN, "No fue posible resolver el programa del coordinador autenticado."),
    ACC_APP_007(SecurityErrorCode.FORBIDDEN, "No fue posible resolver el programa ni la facultad del usuario autenticado."),
    ACC_APP_008(SecurityErrorCode.FORBIDDEN, "El plan de estudio solicitado no pertenece al ambito del usuario autenticado."),
    ACC_APP_009(SecurityErrorCode.FORBIDDEN, "La asignatura no pertenece al programa del coordinador autenticado."),
    ACC_APP_010(SecurityErrorCode.FORBIDDEN, "El plan de estudio no pertenece al programa del coordinador autenticado."),
    // Los KPI_00x conservan su codigo publico: el frontend decide por el codigo si el usuario no tiene ambito (HU178).
    KPI_001("KPI_001", ErrorKind.FORBIDDEN, "No tiene un programa académico a su cargo para consultar estos indicadores."),
    SEC_001("SEC_001", ErrorKind.FORBIDDEN, "No tiene permisos suficientes para realizar esta operación."),
    KPI_003("KPI_003", ErrorKind.FORBIDDEN, "No tiene una facultad a su cargo para consultar estos indicadores."),
    KPI_004("KPI_004", ErrorKind.FORBIDDEN, "No tiene una institución a su cargo para consultar estos indicadores."),
    // IDENT-001/IDENT-002: reenvio del enlace de verificacion o de activacion.
    VCE_003("VCE_003", ErrorKind.CONFLICT, "Debe esperar unos minutos antes de volver a enviar el enlace de verificación."),
    VCE_004("VCE_004", ErrorKind.NOT_FOUND, "El estudiante no pertenece a un grupo a su cargo."),
    ACT_002("ACT_002", ErrorKind.CONFLICT, "La cuenta ya está activa; no es necesario enviar el enlace de activación."),
    ACC_APP_011(SecurityErrorCode.FORBIDDEN, "No fue posible resolver la facultad del decano autenticado."),
    ACC_APP_012(SecurityErrorCode.FORBIDDEN, "El usuario autenticado no puede gestionar cuentas de acceso."),
    ACC_APP_013(SecurityErrorCode.FORBIDDEN, "La cuenta indicada esta fuera del alcance del usuario autenticado."),
    ACC_APP_014(SecurityErrorCode.FORBIDDEN, "El usuario autenticado no tiene autorizacion institucional para consultar el reporte del grupo."),
    ACC_APP_015(SecurityErrorCode.FORBIDDEN, "El usuario autenticado no tiene relacion institucional con la sesion consultada."),
    ACC_APP_016(SecurityErrorCode.FORBIDDEN, "El docente autenticado no tiene titularidad sobre la sesion consultada."),
    ACC_APP_017(SecurityErrorCode.FORBIDDEN, "El usuario autenticado no tiene acceso al grupo solicitado."),
    NOF_APP_001(CommonErrorCode.RESOURCE_NOT_FOUND, "El decano especificado no existe en el sistema."),
    NOF_APP_002(CommonErrorCode.RESOURCE_NOT_FOUND, "La facultad indicada para crear decano no existe."),
    PER_001(CommonErrorCode.RESOURCE_NOT_FOUND, "El periodo academico indicado no existe."),
    // FIX-CIERRE-002: la base lo publica con su propio codigo y con la fecha de fin del periodo en el texto.
    PER_007("PER_007", ErrorKind.CONFLICT,
            "El período académico aún no ha finalizado; el cierre masivo solo puede ejecutarse después de su fecha de fin ({})."),
    // VAL-pendientes-john PRU-15: el cierre masivo ya cerro el periodo; sus sesiones no admiten tomas, lotes ni reclamos (con el
    // nombre del periodo en el texto) y el cierre no se repite (con la fecha del primer cierre).
    PER_008("PER_008", ErrorKind.CONFLICT,
            "El período académico {} ya fue cerrado; no se pueden registrar ni modificar tomas, asistencias ni reclamos de sus sesiones."),
    PER_009("PER_009", ErrorKind.CONFLICT,
            "El período académico ya fue cerrado el {}; el cierre masivo no se puede repetir."),
    // Migracion 63 de la BD: semestre actual del estudiante en el programa (EST_008 sin vinculo; EST_009 con el rango del catalogo).
    EST_008("EST_008", ErrorKind.NOT_FOUND, "El estudiante no está vinculado a este programa académico."),
    EST_009("EST_009", ErrorKind.VALIDATION, "El semestre debe estar entre {} y {}."),
    // SES-GEN-01: reglas del horario del grupo, de las horas de la materia y de la duracion del periodo. La base publica estos
    // codigos con su propio texto (los marcadores {} los sustituye la DB); aqui solo viven el codigo, la clase y un respaldo.
    HOR_006("HOR_006", ErrorKind.CONFLICT,
            "Las horas semanales del horario ({} h) no coinciden con las de la materia ({} h)."),
    HOR_007("HOR_007", ErrorKind.VALIDATION, "La franja {} debe durar entre {} y {} minutos."),
    HOR_008("HOR_008", ErrorKind.VALIDATION, "La franja {} debe estar dentro del horario permitido ({})."),
    HOR_009("HOR_009", ErrorKind.CONFLICT,
            "El nuevo horario se cruza con otros grupos de {} estudiante(s) inscrito(s) en la franja {}."),
    // Revision final BE-1: la asignatura no tiene vinculo activo con un plan activo de los programas del coordinador (usp_crear_grupo y
    // usp_previsualizar_horario_grupo); antes llegaba sin clasificar como un 500.
    // Revision final BE-4 (BD-3): fechas del periodo que se cruzan con otro periodo de la institucion (con su nombre en el texto).
    PER_012("PER_012", ErrorKind.CONFLICT,
            "Las fechas del período se cruzan con el período {}; los períodos de la institución no pueden solaparse."),
    // Revision final BE-4 (BD-4): matricula manual solo con un programa comun entre el estudiante y el grupo (misma familia que EST_006: 400).
    EST_010("EST_010", ErrorKind.VALIDATION,
            "El estudiante no pertenece a ningún programa de este grupo; solo puedes matricular estudiantes de los programas del grupo."),
    EST_011("EST_011", ErrorKind.VALIDATION,
            "El programa indicado no es uno de los programas del estudiante; elige uno de sus programas del grupo."),
    ASI_014("ASI_014", ErrorKind.CONFLICT,
            "La asignatura no tiene un vínculo activo con un plan activo de los programas que coordinas."),
    // A-02 (auditoria backend 2026-11-22): codigos de negocio de la BD que no tenian clasificacion y salian como 500.
    GRUP_001("GRUP_001", ErrorKind.NOT_FOUND, "El grupo seleccionado no existe o no se encuentra habilitado."),
    GRUP_003("GRUP_003", ErrorKind.CONFLICT, "No se encontró un programa académico asociado a este grupo."),
    SES_006("SES_006", ErrorKind.CONFLICT,
            "La sesión no coincide con el manifiesto de clasificación aprobado; no se modificó."),
    EST_003("EST_003", ErrorKind.CONFLICT, "El estudiante ya pertenece a este programa académico."),
    INST_001("INST_001", ErrorKind.VALIDATION, "El estudiante, programa y facultad deben pertenecer a la misma institución."),
    ASI_013("ASI_013", ErrorKind.VALIDATION, "Las horas semanales de la materia deben ser un entero entre 1 y {}."),
    // DEC-076 + SES-GEN-01: periodo no vigente (terminado, cerrado o de otra institucion); un solo codigo, con el nombre del periodo.
    PER_010("PER_010", ErrorKind.CONFLICT,
            "El período académico {} no está vigente: ya terminó, está cerrado o no es de tu institución."),
    PER_011("PER_011", ErrorKind.VALIDATION, "El periodo debe durar {} semanas: la fecha de fin debe ser {}."),
    SES_005("SES_005", ErrorKind.CONFLICT,
            "El horario incluye una hora local inexistente por cambio de horario; no se generaron sesiones."),
    // DEC-081 (migracion 68 de BD): receso academico del periodo. La base publica estos codigos con su propio texto (los marcadores {}
    // los sustituye la DB); aqui solo viven el codigo, la clase HTTP y un respaldo. REC_001/REC_002 (exito) y REC_011 (aviso de la
    // previsualizacion) no son errores y no viven aqui.
    REC_003("REC_003", ErrorKind.VALIDATION, "La fecha del receso debe estar dentro del período académico, del {} al {}."),
    REC_004("REC_004", ErrorKind.VALIDATION, "La fecha del receso debe ser posterior a hoy; elige una fecha desde mañana."),
    REC_005("REC_005", ErrorKind.CONFLICT, "Ese día ya es festivo ({}); no necesita receso académico."),
    REC_006("REC_006", ErrorKind.CONFLICT, "Ya hay un receso académico registrado para esa fecha en este período."),
    REC_007("REC_007", ErrorKind.CONFLICT, "El período académico {} está cerrado; no se pueden registrar ni retirar días de receso."),
    REC_008("REC_008", ErrorKind.VALIDATION, "El motivo del receso es obligatorio y puede tener hasta {} caracteres."),
    REC_009("REC_009", ErrorKind.NOT_FOUND, "El día de receso académico no existe o ya fue retirado."),
    REC_010("REC_010", ErrorKind.CONFLICT, "Este día de receso ya canceló {} clases y no se puede retirar ni cambiar."),
    NOF_APP_003(CommonErrorCode.RESOURCE_NOT_FOUND, "El estudiante no existe en su ambito."),
    NOF_APP_004(CommonErrorCode.RESOURCE_NOT_FOUND, "El docente no existe o no esta vinculado a su programa."),
    NOF_APP_005(CommonErrorCode.RESOURCE_NOT_FOUND, "El plan de estudio indicado no existe en el programa del coordinador."),
    NOF_APP_006(CommonErrorCode.RESOURCE_NOT_FOUND, "El programa academico especificado no existe en el sistema."),
    NOF_APP_007(CommonErrorCode.RESOURCE_NOT_FOUND, "El estudiante no existe en la facultad del decano."),
    NOF_APP_008(CommonErrorCode.RESOURCE_NOT_FOUND, "La solicitud de revision no existe."),
    NOF_APP_009(CommonErrorCode.RESOURCE_NOT_FOUND, "El estudiante especificado no se encuentra registrado."),
    NOF_APP_010(CommonErrorCode.RESOURCE_NOT_FOUND, "La cuenta indicada no existe."),
    NOF_APP_011(CommonErrorCode.RESOURCE_NOT_FOUND, "La matrícula por código no está disponible en este momento."),
    NOF_APP_012(CommonErrorCode.RESOURCE_NOT_FOUND, "No se encontro el perfil del usuario autenticado."),
    NOF_APP_013(CommonErrorCode.RESOURCE_NOT_FOUND, "La facultad especificada no existe en el sistema."),
    NOF_APP_014(CommonErrorCode.RESOURCE_NOT_FOUND, "El archivo solicitado no existe."),
    GEN_001(CommonErrorCode.RESOURCE_NOT_FOUND, "El {} especificado no existe en el sistema."),
    DIS_APP_001("FEATURE_UNAVAILABLE", ErrorKind.BUSINESS, "La generacion automatica de sesiones al crear el grupo aun no esta disponible (fase F2: precarga de sesiones).");

    private final String code;
    private final String defaultMessage;
    private final ErrorKind kind;

    MensajeUsuarioErrorCode(final ErrorDefinition respuesta, final String defaultMessage) {
        this(respuesta.code(), respuesta.kind(), defaultMessage);
    }

    MensajeUsuarioErrorCode(final String code, final ErrorKind kind, final String defaultMessage) {
        this.code = code;
        this.kind = kind;
        this.defaultMessage = defaultMessage;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String defaultMessage() {
        return defaultMessage;
    }

    @Override
    public ErrorKind kind() {
        return kind;
    }

    @Override
    public String messageCode() {
        return name();
    }

    private static final class Codigos {
        static final String ERR_CAMPO_OBLIGATORIO = "ERR_CAMPO_OBLIGATORIO";
    }
}
