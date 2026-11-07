package com.rpg.persistencia;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

public class EscritorLog {

    public boolean escribir(File archivo, String encabezado, String texto, boolean agregar) {
        try (BufferedWriter escritor = new BufferedWriter(new FileWriter(archivo, agregar))) {
            escritor.write(encabezado);
            escritor.newLine();
            for (String linea : texto.split("\\R")) {
                escritor.write(linea);
                escritor.newLine();
            }
            escritor.flush();
            return true;
        } catch (FileNotFoundException e) {
            System.out.println("No se pudo abrir " + archivo.getPath() + " para escribir (la carpeta no existe, es un directorio o no hay permisos): " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Error al escribir " + archivo.getPath() + ": " + e.getMessage());
        }
        return false;
    }
}
