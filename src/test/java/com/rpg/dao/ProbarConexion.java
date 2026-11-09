package com.rpg.dao;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Comprobacion rapida de la Parte A: abre una conexion con ConexionDB y muestra a que base
 * y servidor se llego. Ejecutar su main (Run) despues de crear config.txt y la base de datos.
 * Vive en el paquete dao porque usa java.sql.
 */
public class ProbarConexion {

    public static void main(String[] args) {
        try (Connection conexion = ConexionDB.obtener()) {
            System.out.println("Conexion exitosa a la base: " + conexion.getCatalog());
            System.out.println("Servidor: " + conexion.getMetaData().getDatabaseProductName()
                    + " " + conexion.getMetaData().getDatabaseProductVersion());
            System.out.println("Conexion valida: " + conexion.isValid(2));
        } catch (DaoException e) {
            System.out.println("No se pudo conectar: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("Error al leer los datos de la conexion: " + e.getMessage());
        }
    }
}
