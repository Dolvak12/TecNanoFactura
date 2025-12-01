// =====================================================
// src/main/java/com/tecnano/factura/service/BackupService.java
// =====================================================
package com.tecnano.factura.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);

    private final Path dataDir = Paths.get("data");
    private final Path backupsRoot = Paths.get("backups");

    public Path crearBackupLocal(LocalDate fecha) throws IOException {
        if (fecha == null) fecha = LocalDate.now();

        if (!Files.exists(dataDir) || !Files.isDirectory(dataDir)) {
            throw new IOException("La carpeta 'data' no existe o no es un directorio: "
                    + dataDir.toAbsolutePath());
        }

        if (!Files.exists(backupsRoot)) {
            Files.createDirectories(backupsRoot);
        }

        String baseName = fecha.toString();
        Path destino = backupsRoot.resolve(baseName);

        if (Files.exists(destino)) {
            String timeSuffix = LocalTime.now()
                    .format(DateTimeFormatter.ofPattern("HHmmss"));
            destino = backupsRoot.resolve(baseName + "-" + timeSuffix);
        }

        log.info("Creando backup local desde {} hacia {}",
                dataDir.toAbsolutePath(), destino.toAbsolutePath());
        copyDirectoryRecursively(dataDir, destino);

        return destino;
    }

    public Path crearBackupEnUsb(Path usbRoot, LocalDate fecha) throws IOException {
        if (usbRoot == null) {
            throw new IllegalArgumentException("usbRoot no puede ser null");
        }
        if (!Files.exists(usbRoot) || !Files.isDirectory(usbRoot)) {
            throw new IOException("La ruta USB no existe o no es un directorio: "
                    + usbRoot.toAbsolutePath());
        }
        if (!Files.exists(dataDir) || !Files.isDirectory(dataDir)) {
            throw new IOException("La carpeta 'data' no existe o no es un directorio: "
                    + dataDir.toAbsolutePath());
        }

        if (fecha == null) fecha = LocalDate.now();

        String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
        String folderName = "TecNanoFactura-backup-" + fecha + "-" + timestamp;

        Path destino = usbRoot.resolve(folderName);

        log.info("Creando backup en USB desde {} hacia {}",
                dataDir.toAbsolutePath(), destino.toAbsolutePath());
        copyDirectoryRecursively(dataDir, destino);

        return destino;
    }

    private void copyDirectoryRecursively(Path origen, Path destino) throws IOException {
        if (!Files.exists(destino)) {
            Files.createDirectories(destino);
        }

        try (var stream = Files.walk(origen)) {
            stream.forEach(sourcePath -> {
                Path relative = origen.relativize(sourcePath);
                Path targetPath = destino.resolve(relative);

                try {
                    if (Files.isDirectory(sourcePath)) {
                        if (!Files.exists(targetPath)) {
                            Files.createDirectories(targetPath);
                        }
                    } else {
                        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Error copiando " + sourcePath + " a " + targetPath, e);
                }
            });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException io) {
                throw io;
            }
            throw e;
        }
    }
}
