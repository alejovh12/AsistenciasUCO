package co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.archivo;

import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.controller.response.ApiDataResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Controlador REST para carga y descarga física de soportes de inasistencia (HU014).
 */
@RestController
@RequestMapping("/api/v1/archivos")
public class ArchivoController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArchivoController.class);

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "png", "jpg", "jpeg");
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024; // 5 MB

    private final Path storageDirectory;

    public ArchivoController(@Value("${app.providers.local-storage.upload-directory:uploads/soportes}") final String uploadDir) {
        this.storageDirectory = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageDirectory);
        } catch (IOException e) {
            LOGGER.error("No fue posible inicializar el directorio de almacenamiento de archivos: {}", this.storageDirectory, e);
        }
    }

    @PostMapping("/subir")
    public ResponseEntity<ApiDataResponse<Map<String, Object>>> subirArchivo(
            @RequestParam("archivo") final MultipartFile file
    ) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo adjunto no puede estar vacío.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo excede el tamaño máximo permitido (5 MB).");
        }

        final String originalFilename = Objects.requireNonNullElse(file.getOriginalFilename(), "archivo");
        if (!isSimpleFilename(originalFilename)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre de archivo no válido.");
        }
        final String extension = getExtension(originalFilename).toLowerCase();

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Formato de archivo no permitido. Solo se aceptan archivos PDF, PNG o JPG.");
        }

        final String savedFilename = UUID.randomUUID() + "_" + originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_");

        try {
            final Path targetLocation = this.storageDirectory.resolve(savedFilename).normalize();
            if (!targetLocation.startsWith(this.storageDirectory)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ruta de archivo no válida.");
            }

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            final Map<String, Object> data = new HashMap<>();
            data.put("nombre", originalFilename);
            data.put("nombreGuardado", savedFilename);
            data.put("url", "/api/v1/archivos/" + savedFilename);
            data.put("tamanio", file.getSize());

            return ResponseEntity.status(HttpStatus.CREATED).body(new ApiDataResponse<>(true, data));
        } catch (IOException ex) {
            LOGGER.error("Error al almacenar archivo en disco: {}", savedFilename, ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno al guardar el soporte.");
        }
    }

    @GetMapping("/{nombreArchivo:.+}")
    public ResponseEntity<Resource> descargarArchivo(@PathVariable final String nombreArchivo) {
        if (!isSimpleFilename(nombreArchivo)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El archivo solicitado no existe.");
        }
        try {
            final Path filePath = this.storageDirectory.resolve(nombreArchivo).normalize();
            if (!filePath.startsWith(this.storageDirectory) || !Files.exists(filePath)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El archivo solicitado no existe.");
            }
            if (!filePath.toRealPath().startsWith(this.storageDirectory.toRealPath()) || !Files.isRegularFile(filePath)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El archivo solicitado no existe.");
            }

            final Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El archivo no se encuentra disponible.");
            }

            final String contentType = determineContentType(nombreArchivo);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (IOException ex) {
            LOGGER.error("Error resolviendo URL de archivo: {}", nombreArchivo, ex);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Archivo no encontrado.");
        }
    }

    private static boolean isSimpleFilename(final String filename) {
        if (filename == null || filename.isBlank() || filename.equals(".") || filename.contains("..")
                || filename.indexOf('/') >= 0 || filename.indexOf('\\') >= 0 || filename.indexOf(':') >= 0) {
            return false;
        }
        try {
            final Path path = Paths.get(filename);
            return !path.isAbsolute() && path.getNameCount() == 1;
        } catch (InvalidPathException exception) {
            return false;
        }
    }

    private String getExtension(final String filename) {
        final int lastDot = filename.lastIndexOf('.');
        return (lastDot == -1) ? "" : filename.substring(lastDot + 1);
    }

    private String determineContentType(final String filename) {
        final String ext = getExtension(filename).toLowerCase();
        return switch (ext) {
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            default -> "application/octet-stream";
        };
    }
}
