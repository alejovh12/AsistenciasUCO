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
    VAL_APP_067("ERR_CAMPO_OBLIGATORIO", ErrorKind.VALIDATION, "Cada franja del horario es obligatoria."),
    VAL_APP_068("ERR_CAMPO_OBLIGATORIO", ErrorKind.VALIDATION, "El campo dias es obligatorio cuando se envia el horario."),
    VAL_APP_069("ERR_CAMPO_OBLIGATORIO", ErrorKind.VALIDATION, "El campo {} es obligatorio."),
    VAL_APP_070("ERR_FECHA_HORA_INVALIDA", ErrorKind.VALIDATION, "El campo {} debe usar formato ISO local {}."),
    PAG_001(CommonErrorCode.VALIDATION_ERROR, "Los parametros de paginacion no son validos."),
    VAL_APP_071(CommonErrorCode.VALIDATION_ERROR, "Los datos de la consulta no son validos."),
    VAL_APP_072(CommonErrorCode.VALIDATION_ERROR, "La sesion es obligatoria."),
    VAL_APP_073(CommonErrorCode.VALIDATION_ERROR, "El identificador del estudiante es obligatorio."),
    VAL_APP_074(CommonErrorCode.VALIDATION_ERROR, "El identificador del docente es obligatorio."),
    VAL_APP_075(CommonErrorCode.VALIDATION_ERROR, "El identificador del programa es obligatorio."),
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
    KPI_001(SecurityErrorCode.FORBIDDEN, "No tiene un programa academico a su cargo para consultar estos indicadores."),
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
}
