package com.rpg.model;

/**
 * Entidad (POJO) de la tabla jugadores. Atributos privados, constructor y getters/setters con
 * validacion. No guarda la clave: la clave (hash) solo viaja hacia la base de datos y nunca
 * vuelve a la aplicacion.
 *
 * No tiene nada de JDBC: eso vive solo en el paquete dao.
 */
public class Jugador {

    public static final String ROL_JUGADOR = "jugador";
    public static final String ROL_ADMIN = "admin";
    private static final int USUARIO_MAX = 30;

    private int id;
    private String usuario;
    private String rol;

    /** id = 0 significa "todavia no guardado en la base de datos". */
    public Jugador(int id, String usuario, String rol) {
        this.id = validarId(id);
        this.usuario = validarUsuario(usuario);
        this.rol = validarRol(rol);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = validarId(id);
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = validarUsuario(usuario);
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = validarRol(rol);
    }

    public boolean esAdmin() {
        return ROL_ADMIN.equals(rol);
    }

    public static boolean esRolValido(String rol) {
        return ROL_JUGADOR.equals(rol) || ROL_ADMIN.equals(rol);
    }

    private static int validarId(int id) {
        if (id < 0) {
            throw new IllegalArgumentException("El id no puede ser negativo.");
        }
        return id;
    }

    private static String validarUsuario(String usuario) {
        if (usuario == null || usuario.trim().isEmpty()) {
            throw new IllegalArgumentException("El usuario no puede estar vacio.");
        }
        if (usuario.length() > USUARIO_MAX) {
            throw new IllegalArgumentException("El usuario no puede pasar de " + USUARIO_MAX + " caracteres.");
        }
        return usuario;
    }

    private static String validarRol(String rol) {
        if (!esRolValido(rol)) {
            throw new IllegalArgumentException("El rol debe ser '" + ROL_JUGADOR + "' o '" + ROL_ADMIN + "'.");
        }
        return rol;
    }

    @Override
    public String toString() {
        return "Jugador{id=" + id + ", usuario='" + usuario + "', rol='" + rol + "'}";
    }
}
