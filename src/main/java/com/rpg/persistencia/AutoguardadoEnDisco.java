package com.rpg.persistencia;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Archivo de autoguardado compartido entre el hilo daemon (escribe) y el hilo principal (lee).
 * Todos los metodos son synchronized sobre la misma instancia, que hace de candado unico:
 * mientras uno escribe o lee, el otro espera. Ademas se escribe en un archivo temporal que luego
 * reemplaza al definitivo, para que un cierre brusco nunca deje el archivo a medias.
 */
public class AutoguardadoEnDisco {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final File archivo;
    private final File temporal;
    private final EscritorLog escritor = new EscritorLog();
    private final LectorLog lector = new LectorLog();

    public AutoguardadoEnDisco(File archivo) {
        this.archivo = archivo;
        this.temporal = new File(archivo.getPath() + ".tmp");
    }

    public File getArchivo() {
        return archivo;
    }

    public synchronized boolean escribir(String texto) {
        File carpeta = archivo.getAbsoluteFile().getParentFile();
        if (!carpeta.isDirectory() && !carpeta.mkdirs()) {
            System.out.println("No se pudo crear la carpeta " + carpeta.getPath());
            return false;
        }
        String encabezado = "=== Autoguardado el " + LocalDateTime.now().format(FORMATO_FECHA) + " ===";
        if (!escritor.escribir(temporal, encabezado, texto, false)) {
            return false;
        }
        try {
            reemplazarArchivo();
            return true;
        } catch (IOException e) {
            System.out.println("No se pudo completar el autoguardado en " + archivo.getPath() + ": "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
            return false;
        }
    }

    private void reemplazarArchivo() throws IOException {
        try {
            Files.move(temporal.toPath(), archivo.toPath(),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporal.toPath(), archivo.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public synchronized boolean mostrar() {
        return lector.mostrar(archivo);
    }

    /** @return el contenido completo, o null si el archivo no existe o no se pudo leer */
    public synchronized String leerTexto() {
        try {
            return Files.readString(archivo.toPath());
        } catch (IOException e) {
            return null;
        }
    }
}
