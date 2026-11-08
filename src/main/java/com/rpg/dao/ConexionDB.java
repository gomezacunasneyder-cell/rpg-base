package com.rpg.dao;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * ConexionDB: la unica clase de la aplicacion que sabe como llegar a la base de datos.
 * Lee url, usuario y clave de config.txt y entrega una Connection. Ninguna otra clase lee
 * ese archivo ni conoce la clave (los datos sensibles no estan escritos en el codigo fuente).
 *
 * ---------------------------------------------------------------------------------------
 * QUE ES JDBC
 * JDBC (Java Database Connectivity) no es una base de datos ni algo que se instale: es una
 * API, es decir, un conjunto de interfaces y clases del paquete java.sql (Connection,
 * PreparedStatement, ResultSet...) que fija las reglas para que un programa Java hable con
 * una base de datos relacional. Gracias a eso mi codigo usa siempre las mismas clases, sin
 * importar el motor. Cada fabricante entrega el "controlador" (driver) que cumple esas reglas
 * para su motor; el de MySQL es mysql-connector-j, que agregue en el pom.xml.
 *
 * QUE HACE DriverManager
 * Es el administrador de drivers de java.sql. Cuando llamo
 * DriverManager.getConnection(url, usuario, clave), mira el comienzo de la url ("jdbc:mysql:"),
 * recorre los drivers que hay en el classpath, le pregunta a cada uno si acepta esa url y le
 * pide la conexion al primero que la acepta. Desde JDBC 4 el jar del driver se registra solo
 * (trae un archivo META-INF/services/java.sql.Driver), por eso no hace falta Class.forName(...).
 *
 * QUE PASA SI FALTA EL DRIVER
 * Reproduje el error sin el driver en el classpath (equivale a quitar la dependencia del
 * pom.xml): DriverManager no encuentra ningun driver que acepte "jdbc:mysql:" y lanza
 *
 *     java.sql.SQLException: No suitable driver found for jdbc:mysql://localhost:3306/rpg_arena_db
 *
 * con SQLState 08001. No es un problema de la base de datos ni de la clave: el codigo no sabe
 * "hablar" MySQL. La solucion es agregar mysql-connector-j al pom.xml y pulsar "Load Maven
 * Changes" en IntelliJ. Este mismo caso esta controlado en describirErrorConexion().
 *
 * PARTES DE LA URL  jdbc:mysql://localhost:3306/rpg_arena_db
 *   jdbc:mysql  protocolo (jdbc) y subprotocolo (mysql): indica a DriverManager que driver usar.
 *   localhost   host: la direccion del servidor donde corre MySQL (mi propio equipo).
 *   3306        puerto: la "puerta de entrada" por defecto de MySQL.
 *   rpg_arena_db esquema: el nombre exacto de la base de datos a la que se entra.
 * ---------------------------------------------------------------------------------------
 *
 * FORMATO DE config.txt (raiz del proyecto, junto al pom.xml): una clave=valor por linea.
 * Se ignoran las lineas vacias y las que empiezan con #. La clave (password) puede quedar
 * vacia si el usuario de MySQL no tiene contrasena, pero la linea "clave=" debe existir.
 */
public final class ConexionDB {

    private static final String ARCHIVO_CONFIG = "config.txt";
    private static final String CLAVE_URL = "url";
    private static final String CLAVE_USUARIO = "usuario";
    private static final String CLAVE_CLAVE = "clave";

    // Codigos de error de MySQL usados para dar mensajes mas claros.
    private static final int MYSQL_ACCESO_DENEGADO = 1045;
    private static final int MYSQL_BASE_DESCONOCIDA = 1049;

    private static Map<String, String> configuracion;

    private ConexionDB() {
    }

    /**
     * Abre una conexion nueva. Quien la pide es responsable de cerrarla (try-with-resources).
     * Si config.txt no existe o le falta una clave, lanza DaoException y NO intenta conectar.
     */
    public static Connection obtener() throws DaoException {
        Map<String, String> datos = configuracion();
        try {
            return DriverManager.getConnection(
                    datos.get(CLAVE_URL), datos.get(CLAVE_USUARIO), datos.get(CLAVE_CLAVE));
        } catch (SQLException e) {
            throw new DaoException(describirErrorConexion(e), e);
        }
    }

    private static synchronized Map<String, String> configuracion() throws DaoException {
        if (configuracion == null) {
            configuracion = leerConfiguracion();
        }
        return configuracion;
    }

    private static Map<String, String> leerConfiguracion() throws DaoException {
        File archivo = new File(ARCHIVO_CONFIG);
        if (!archivo.exists()) {
            throw new DaoException("No se encontro " + ARCHIVO_CONFIG + " en " + archivo.getAbsolutePath()
                    + ". Copia config.example.txt como " + ARCHIVO_CONFIG
                    + " y escribe tus datos. No se intento conectar.");
        }

        Map<String, String> valores = new HashMap<>();
        try (BufferedReader lector = new BufferedReader(new FileReader(archivo, StandardCharsets.UTF_8))) {
            String linea;
            int numeroLinea = 0;
            while ((linea = lector.readLine()) != null) {
                numeroLinea++;
                if (linea.trim().isEmpty() || linea.trim().startsWith("#")) {
                    continue;
                }
                String[] partes = linea.split("=", 2);
                if (partes.length < 2) {
                    // No se muestra el contenido de la linea: podria ser parte de la clave.
                    throw new DaoException("La linea " + numeroLinea + " de " + ARCHIVO_CONFIG
                            + " no tiene el formato clave=valor. No se intento conectar.");
                }
                valores.put(partes[0].trim(), partes[1].trim());
            }
        } catch (FileNotFoundException e) {
            throw new DaoException("No se pudo abrir " + ARCHIVO_CONFIG + " ("
                    + "es una carpeta o no hay permisos de lectura): " + archivo.getAbsolutePath(), e);
        } catch (IOException e) {
            throw new DaoException("Error al leer " + ARCHIVO_CONFIG + ": " + e.getMessage(), e);
        }

        exigirValor(valores, CLAVE_URL);
        exigirValor(valores, CLAVE_USUARIO);
        if (!valores.containsKey(CLAVE_CLAVE)) {
            throw faltaClave(CLAVE_CLAVE);
        }
        return valores;
    }

    private static void exigirValor(Map<String, String> valores, String clave) throws DaoException {
        String valor = valores.get(clave);
        if (valor == null || valor.isEmpty()) {
            throw faltaClave(clave);
        }
    }

    private static DaoException faltaClave(String clave) {
        return new DaoException("Falta la clave '" + clave + "' en " + ARCHIVO_CONFIG
                + " (revisa config.example.txt). No se intento conectar.");
    }

    private static String describirErrorConexion(SQLException e) {
        String detalle = " (detalle tecnico: " + e.getMessage() + ")";
        String mensaje = e.getMessage() == null ? "" : e.getMessage();
        if (mensaje.startsWith("No suitable driver")) {
            return "Falta el driver de MySQL: agrega mysql-connector-j al pom.xml y pulsa Load Maven Changes."
                    + detalle;
        }
        if (e.getErrorCode() == MYSQL_ACCESO_DENEGADO) {
            return "MySQL rechazo el usuario o la clave de " + ARCHIVO_CONFIG + "." + detalle;
        }
        if (e.getErrorCode() == MYSQL_BASE_DESCONOCIDA) {
            return "La base de datos de la url no existe. Ejecuta schema.sql en DBeaver primero." + detalle;
        }
        return "No se pudo conectar a la base de datos. Revisa que MySQL este encendido y que la url de "
                + ARCHIVO_CONFIG + " sea correcta." + detalle;
    }
}
