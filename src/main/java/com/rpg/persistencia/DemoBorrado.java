package com.rpg.persistencia;

import java.io.File;
import java.io.IOException;

public class DemoBorrado {

    public static void main(String[] args) {
        GestorArchivos archivos = new GestorArchivos();
        EscritorLog escritor = new EscritorLog();
        if (!archivos.asegurarCarpeta()) {
            System.out.println("No se pudo crear la carpeta " + GestorArchivos.CARPETA_PARTIDAS);
            return;
        }

        File conFile = new File(archivos.carpetaPartidas(), "demo_borrado_file.txt");
        File conFiles = new File(archivos.carpetaPartidas(), "demo_borrado_files.txt");

        System.out.println("1) File.delete() sobre un archivo que existe:");
        escritor.escribir(conFile, "Archivo de prueba", "linea 1", false);
        archivos.borrarConFile(conFile);

        System.out.println("\n2) File.delete() sobre un archivo que ya no existe:");
        archivos.borrarConFile(conFile);

        System.out.println("\n3) Files.deleteIfExists() sobre un archivo que existe:");
        escritor.escribir(conFiles, "Archivo de prueba", "linea 1", false);
        archivos.borrarSiExiste(conFiles);

        System.out.println("\n4) Files.deleteIfExists() sobre un archivo que ya no existe:");
        archivos.borrarSiExiste(conFiles);

        System.out.println("\n5) Diferencia clave: carpeta con contenido");
        File carpeta = new File(archivos.carpetaPartidas(), "demo_borrado_carpeta");
        File dentro = new File(carpeta, "dentro.txt");
        carpeta.mkdirs();
        archivos.crearSiNoExiste(dentro);
        System.out.println("- Con File.delete():");
        archivos.borrarConFile(carpeta);
        System.out.println("- Con Files.deleteIfExists():");
        archivos.borrarSiExiste(carpeta);

        archivos.borrarSiExiste(dentro);
        archivos.borrarSiExiste(carpeta);
    }
}
