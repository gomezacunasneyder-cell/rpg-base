package com.rpg.persistencia;

import java.io.File;

public class DemoLectura {

    public static void main(String[] args) {
        GestorArchivos archivos = new GestorArchivos();
        EscritorLog escritor = new EscritorLog();
        LectorLog lector = new LectorLog();
        if (!archivos.asegurarCarpeta()) {
            System.out.println("No se pudo crear la carpeta " + GestorArchivos.CARPETA_PARTIDAS);
            return;
        }

        File existente = new File(archivos.carpetaPartidas(), "demo_lectura.txt");
        escritor.escribir(existente, "Combate de prueba", "Maximo ataca a Circe\nCirce cae derrotada", false);
        System.out.println("1) Leer un archivo que existe:");
        lector.mostrar(existente);

        System.out.println("\n2) Leer un archivo que NO existe (FileNotFoundException):");
        lector.mostrar(new File(archivos.carpetaPartidas(), "no_existe.txt"));
    }
}
