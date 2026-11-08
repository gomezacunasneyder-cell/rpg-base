package com.rpg.model;

/**
 * Entidad (POJO) de la tabla monstruos. Atributos privados, constructor y getters/setters
 * validados. Sin nada de JDBC (eso vive solo en el paquete dao).
 */
public class Monstruo {

    private static final int NOMBRE_MAX = 40;

    private int id;
    private String nombre;
    private int vida;
    private int ataque;

    /** id = 0 significa "todavia no guardado en la base de datos". */
    public Monstruo(int id, String nombre, int vida, int ataque) {
        this.id = validarId(id);
        this.nombre = validarNombre(nombre);
        this.vida = validarPositivo(vida, "La vida");
        this.ataque = validarPositivo(ataque, "El ataque");
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = validarId(id);
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = validarNombre(nombre);
    }

    public int getVida() {
        return vida;
    }

    public void setVida(int vida) {
        this.vida = validarPositivo(vida, "La vida");
    }

    public int getAtaque() {
        return ataque;
    }

    public void setAtaque(int ataque) {
        this.ataque = validarPositivo(ataque, "El ataque");
    }

    private static int validarId(int id) {
        if (id < 0) {
            throw new IllegalArgumentException("El id no puede ser negativo.");
        }
        return id;
    }

    private static String validarNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede estar vacio.");
        }
        if (nombre.length() > NOMBRE_MAX) {
            throw new IllegalArgumentException("El nombre no puede pasar de " + NOMBRE_MAX + " caracteres.");
        }
        return nombre;
    }

    private static int validarPositivo(int valor, String campo) {
        if (valor <= 0) {
            throw new IllegalArgumentException(campo + " debe ser mayor que cero.");
        }
        return valor;
    }

    @Override
    public String toString() {
        return "Monstruo{id=" + id + ", nombre='" + nombre + "', vida=" + vida + ", ataque=" + ataque + "}";
    }
}
