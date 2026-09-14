# Limpieza pendiente del repositorio

## `uploads/`

`uploads/` esta ignorado para evitar versionar documentos cargados por usuarios o archivos de runtime. Si existen archivos historicos bajo `uploads/soportes/` que ya fueron incorporados al indice Git, la limpieza debe hacerse manualmente y revisarse antes del commit:

```bash
git rm --cached -r uploads/
```

Este comando quita los archivos del indice Git; no debe utilizarse para borrar documentos externos, productivos o que deban conservarse fuera del repositorio.

Despues de ejecutar la limpieza, revisar el diff antes de confirmar cambios. Si esos PDF contienen informacion sensible y ya fueron publicados en un remoto, debe evaluarse una limpieza de historial por separado.
