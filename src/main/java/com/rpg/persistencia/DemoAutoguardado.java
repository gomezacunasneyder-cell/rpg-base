package com.rpg.persistencia;

import com.rpg.concurrency.AutoGuardadoDaemon;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;

public class DemoAutoguardado {

    private static final int LINEAS = 3000;
    private static final long DURACION_MS = 1500;
    private static final long INTERVALO_MS = 2;

    public static void main(String[] args) {
        GestorArchivos archivos = new GestorArchivos();
        if (!archivos.asegurarCarpeta()) {
            System.out.println("No se pudo crear la carpeta " + GestorArchivos.CARPETA_PARTIDAS);
            return;
        }
        File archivo = new File(archivos.carpetaPartidas(), "demo_autoguardado.txt");
        System.out.println("Un daemon reescribe el archivo cada " + INTERVALO_MS + " ms ("
                + LINEAS + " lineas por version) mientras el hilo principal lo lee sin parar durante "
                + DURACION_MS + " ms.\n");

        System.out.println("1) SIN candado (EscritorLog directo, sin synchronized):");
        EscritorLog escritor = new EscritorLog();
        AtomicInteger version = new AtomicInteger();
        Thread sinCandado = iniciarDaemon(() -> {
            escritor.escribir(archivo, "version " + version.incrementAndGet(), cuerpo(version.get()), false);
            return "";
        });
        int[] sin = leerDurante(() -> leerDirecto(archivo));
        detener(sinCandado);
        System.out.println("   lecturas incompletas o mezcladas: " + sin[1] + " de " + sin[0]);

        System.out.println("\n2) CON candado (AutoguardadoEnDisco: metodos synchronized + archivo temporal):");
        archivos.borrarSiExiste(archivo);
        AutoguardadoEnDisco autoguardado = new AutoguardadoEnDisco(archivo);
        autoguardado.escribir(cuerpo(0));
        AtomicInteger versionSegura = new AtomicInteger();
        Thread conCandado = iniciarDaemon(() -> {
            autoguardado.escribir(cuerpo(versionSegura.incrementAndGet()));
            return "";
        });
        int[] con = leerDurante(autoguardado::leerTexto);
        detener(conCandado);
        System.out.println("   lecturas incompletas o mezcladas: " + con[1] + " de " + con[0]);

        archivos.borrarSiExiste(archivo);
        archivos.borrarSiExiste(new File(archivo.getPath() + ".tmp"));
    }

    private static String cuerpo(int version) {
        StringBuilder texto = new StringBuilder();
        for (int i = 1; i <= LINEAS; i++) {
            texto.append("v").append(version).append(" linea ").append(i).append(System.lineSeparator());
        }
        texto.append("FIN v").append(version);
        return texto.toString();
    }

    private static Thread iniciarDaemon(java.util.function.Supplier<String> accion) {
        Thread hilo = new Thread(new AutoGuardadoDaemon(INTERVALO_MS, accion), "Demo-Autoguardado");
        hilo.setDaemon(true);
        hilo.start();
        return hilo;
    }

    private static void detener(Thread hilo) {
        hilo.interrupt();
        try {
            hilo.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String leerDirecto(File archivo) {
        try {
            return Files.readString(archivo.toPath());
        } catch (IOException e) {
            return null;
        }
    }

    /** @return {total de lecturas, lecturas incompletas o mezcladas} */
    private static int[] leerDurante(java.util.function.Supplier<String> lector) {
        int total = 0;
        int malas = 0;
        long fin = System.currentTimeMillis() + DURACION_MS;
        while (System.currentTimeMillis() < fin) {
            String texto = lector.get();
            if (texto == null) {
                continue;
            }
            total++;
            if (!estaCompleto(texto)) {
                malas++;
            }
        }
        return new int[] {total, malas};
    }

    private static boolean estaCompleto(String texto) {
        String[] lineas = texto.split("\\R");
        String ultima = lineas[lineas.length - 1];
        if (!ultima.startsWith("FIN v")) {
            return false;
        }
        String version = "v" + ultima.substring("FIN v".length()) + " linea ";
        return lineas.length >= LINEAS && lineas[lineas.length - 2].startsWith(version);
    }
}
