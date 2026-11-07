package com.rpg.persistencia;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class GestorArchivos {

    public static final String CARPETA_PARTIDAS = "partidas";
    public static final String NOMBRE_LOG = "log_combate.txt";
    public static final String NOMBRE_PARTIDA = "partida.dat";
    public static final String NOMBRE_AUTOGUARDADO = "autoguardado.txt";

    public File carpetaPartidas() {
        return new File(CARPETA_PARTIDAS);
    }

    public File archivoLog() {
        return new File(carpetaPartidas(), NOMBRE_LOG);
    }

    public File archivoPartida() {
        return new File(carpetaPartidas(), NOMBRE_PARTIDA);
    }

    public File archivoAutoguardado() {
        return new File(carpetaPartidas(), NOMBRE_AUTOGUARDADO);
    }

    public boolean asegurarCarpeta() {
        File carpeta = carpetaPartidas();
        return carpeta.isDirectory() || carpeta.mkdirs();
    }

    public boolean crearSiNoExiste(File archivo) {
        try {
            if (archivo.createNewFile()) {
                System.out.println("Archivo creado: " + archivo.getName());
            }
            return true;
        } catch (IOException e) {
            System.out.println("No se pudo crear " + archivo.getName() + ": " + e.getMessage());
            return false;
        }
    }

    // TALLER ARCHIVOS - Seccion 4: borrado con File.delete() (devuelve boolean, sin motivo del fallo)
    public boolean borrarConFile(File archivo) {
        boolean borrado = archivo.delete();
        if (borrado) {
            System.out.println("File.delete() -> true: se borro " + archivo.getPath());
        } else {
            System.out.println("File.delete() -> false: no se borro " + archivo.getPath()
                    + " (no existe, esta en uso o no hay permisos; el metodo no dice cual)");
        }
        return borrado;
    }

    // TALLER ARCHIVOS - Seccion 4: borrado con Files.deleteIfExists (false solo si no existia; los fallos lanzan IOException)
    public boolean borrarSiExiste(File archivo) {
        try {
            boolean borrado = Files.deleteIfExists(archivo.toPath());
            if (borrado) {
                System.out.println("Files.deleteIfExists() -> true: se borro " + archivo.getPath());
            } else {
                System.out.println("Files.deleteIfExists() -> false: " + archivo.getPath() + " no existia");
            }
            return borrado;
        } catch (IOException e) {
            System.out.println("No se pudo borrar " + archivo.getPath() + ": "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
            return false;
        }
    }

    public String describir(File archivo) {
        StringBuilder texto = new StringBuilder();
        texto.append("Nombre: ").append(archivo.getName()).append(System.lineSeparator());
        texto.append("Ruta indicada: ").append(archivo.getPath())
                .append(archivo.isAbsolute() ? " (absoluta)" : " (relativa)").append(System.lineSeparator());
        texto.append("Ruta absoluta: ").append(archivo.getAbsolutePath()).append(System.lineSeparator());
        if (archivo.exists()) {
            texto.append("Existe: si | Tamano: ").append(archivo.length()).append(" bytes");
        } else {
            texto.append("Existe: no");
        }
        return texto.toString();
    }
}
