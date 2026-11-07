package com.rpg.persistencia;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class LectorLog {

    public boolean mostrar(File archivo) {
        try (BufferedReader lector = new BufferedReader(new FileReader(archivo))) {
            String linea;
            int total = 0;
            while ((linea = lector.readLine()) != null) {
                System.out.println(linea);
                total++;
            }
            if (total == 0) {
                System.out.println("El historial esta vacio: aun no hay combates guardados.");
            } else {
                System.out.println("-- " + total + " lineas leidas de " + archivo.getName() + " --");
            }
            return true;
        } catch (FileNotFoundException e) {
            System.out.println("Aun no hay historial: " + archivo.getPath()
                    + " no existe o no se puede abrir. Juega un combate para crearlo.");
        } catch (IOException e) {
            System.out.println("Error al leer " + archivo.getPath() + ": " + e.getMessage());
        }
        return false;
    }
}
