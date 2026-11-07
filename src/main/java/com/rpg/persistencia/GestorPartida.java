package com.rpg.persistencia;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;

/** Guarda y carga la partida en un archivo .dat con serializacion de objetos. */
public class GestorPartida {

    public boolean guardar(File archivo, PartidaGuardada partida) {
        try (ObjectOutputStream salida = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(archivo)))) {
            salida.writeObject(partida);
            return true;
        } catch (NotSerializableException e) {
            System.out.println("No se pudo guardar: la clase " + e.getMessage() + " no es Serializable");
            return false;
        } catch (FileNotFoundException e) {
            System.out.println("No se pudo abrir " + archivo.getPath()
                    + " para guardar (es un directorio, esta bloqueado o no hay permisos): " + e.getMessage());
            return false;
        } catch (IOException e) {
            System.out.println("Error al guardar la partida en " + archivo.getPath() + ": " + e.getMessage());
            return false;
        }
    }

    /** @return la partida leida, o null si no se pudo cargar (ya se explico el motivo en pantalla) */
    public PartidaGuardada cargar(File archivo) {
        try (ObjectInputStream entrada = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(archivo)))) {
            return (PartidaGuardada) entrada.readObject();
        } catch (FileNotFoundException e) {
            System.out.println("No hay partida guardada: " + archivo.getPath()
                    + " no existe o no se puede abrir. Juega un combate para crearla.");
        } catch (InvalidClassException e) {
            System.out.println("La partida guardada es de otra version del juego y no se puede cargar: "
                    + e.getMessage());
        } catch (EOFException | StreamCorruptedException e) {
            System.out.println("El archivo de partida esta corrupto o incompleto y no se puede cargar ("
                    + e.getClass().getSimpleName() + ")");
        } catch (ClassNotFoundException | ClassCastException e) {
            System.out.println("El archivo no contiene una partida valida: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Error al leer la partida de " + archivo.getPath() + ": "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return null;
    }
}
