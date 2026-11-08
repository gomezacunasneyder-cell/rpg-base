package com.rpg.model;

/**
 * Una fila del ranking: un usuario y su total de victorias. Es el objeto Java en que el DAO
 * convierte cada fila del SELECT con JOIN y GROUP BY (el ResultSet nunca sale del DAO).
 */
public class PuestoRanking {

    private final String usuario;
    private final int totalVictorias;

    public PuestoRanking(String usuario, int totalVictorias) {
        this.usuario = usuario;
        this.totalVictorias = totalVictorias;
    }

    public String getUsuario() {
        return usuario;
    }

    public int getTotalVictorias() {
        return totalVictorias;
    }

    @Override
    public String toString() {
        return "PuestoRanking{usuario='" + usuario + "', totalVictorias=" + totalVictorias + "}";
    }
}
