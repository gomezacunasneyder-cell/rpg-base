package com.rpg.persistencia;

import java.io.File;

public class DemoEscritura {

    public static void main(String[] args) {
        GestorArchivos archivos = new GestorArchivos();
        EscritorLog escritor = new EscritorLog();
        if (!archivos.asegurarCarpeta()) {
            System.out.println("No se pudo crear la carpeta " + GestorArchivos.CARPETA_PARTIDAS);
            return;
        }
        File demo = new File(archivos.carpetaPartidas(), "demo_escritura.txt");

        escritor.escribir(demo, "Combate 1", "Linea A", false);
        System.out.println("1) Modo sobrescribir (false), primer combate: " + demo.length() + " bytes");

        escritor.escribir(demo, "Combate 2", "Linea B", true);
        System.out.println("2) Modo agregar (true), segundo combate:      " + demo.length() + " bytes (crece: conserva el anterior)");

        escritor.escribir(demo, "Combate 3", "Linea C", true);
        System.out.println("3) Modo agregar (true), tercer combate:       " + demo.length() + " bytes (sigue creciendo)");

        escritor.escribir(demo, "Combate 4", "Linea D", false);
        System.out.println("4) Modo sobrescribir (false), cuarto combate: " + demo.length() + " bytes (vuelve a empezar: borro lo anterior)");

        System.out.println("5) Carpeta inexistente: ");
        escritor.escribir(new File("no_existe/log.txt"), "x", "y", true);
    }
}
