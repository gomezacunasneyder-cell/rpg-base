package com.rpg.persistencia;

import java.io.File;
import java.io.IOException;

public class GestorArchivos {

    public static final String CARPETA_PARTIDAS = "partidas";
    public static final String NOMBRE_LOG = "log_combate.txt";
    public static final String NOMBRE_PARTIDA = "partida.dat";

    public File carpetaPartidas() {
        return new File(CARPETA_PARTIDAS);
    }

    public File archivoLog() {
        return new File(carpetaPartidas(), NOMBRE_LOG);
    }

    public File archivoPartida() {
        return new File(carpetaPartidas(), NOMBRE_PARTIDA);
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
