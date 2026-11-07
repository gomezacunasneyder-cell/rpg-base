package com.rpg.persistencia;

import com.rpg.entities.EstadoEfecto;
import com.rpg.entities.Gremio;
import com.rpg.entities.Guerrero;
import com.rpg.entities.Mago;
import com.rpg.entities.PerfilJugador;
import com.rpg.entities.Personaje;
import com.rpg.entities.enums.RaridadItem;
import com.rpg.entities.enums.TipoEfecto;
import com.rpg.exceptions.CapacidadExcedidaException;
import com.rpg.items.Pocion;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;

public class DemoSerializacion {

    private static class Mochila {
        private final String contenido = "tres pociones";
    }

    private static class MochilaSerializable implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String contenido = "tres pociones";
    }

    private static class HeroeConMochila implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String nombre = "Lyra";
        private final Mochila mochila = new Mochila();
    }

    private static class HeroeCorregido implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String nombre = "Lyra";
        private final MochilaSerializable mochila = new MochilaSerializable();
    }

    public static void main(String[] args) throws CapacidadExcedidaException {
        GestorArchivos archivos = new GestorArchivos();
        GestorPartida gestor = new GestorPartida();
        if (!archivos.asegurarCarpeta()) {
            System.out.println("No se pudo crear la carpeta " + GestorArchivos.CARPETA_PARTIDAS);
            return;
        }
        File archivo = new File(archivos.carpetaPartidas(), "demo_serializacion.dat");

        Gremio gremio = new Gremio("Gremio de Prueba");
        Guerrero guerrero = new Guerrero("Maximo");
        Mago mago = new Mago("Circe");
        gremio.agregarMiembro(guerrero);
        gremio.agregarMiembro(mago);
        guerrero.getInventario().agregar(new Pocion("Elixir de Regeneracion", 0, RaridadItem.RARO,
                new EstadoEfecto("Regeneracion", TipoEfecto.CURACION_CONTINUA, 6, 3), false));
        mago.recibirDanio(30);
        PerfilJugador perfil = new PerfilJugador();
        perfil.registrarVictoria();
        perfil.desbloquearLogro("Primera Victoria");

        System.out.println("1) Guardar y cargar el grafo completo (Gremio - Personaje - Inventario - Pocion - Arma):");
        System.out.println("Antes de guardar:");
        mostrar(gremio);
        PartidaGuardada original = new PartidaGuardada(gremio, perfil, Map.of("Goblin", 2));
        if (!gestor.guardar(archivo, original)) {
            return;
        }
        System.out.println("Guardado en " + archivo.getPath() + " (" + archivo.length() + " bytes)");
        PartidaGuardada cargada = gestor.cargar(archivo);
        if (cargada == null) {
            return;
        }
        System.out.println("Despues de cargar:");
        mostrar(cargada.getGremio());
        System.out.println("Perfil: " + cargada.getPerfil().getVictorias() + " victoria(s), logros "
                + cargada.getPerfil().getLogros() + ", derrotados " + cargada.getDerrotados());
        Personaje primero = cargada.getGremio().getMiembros().get(0);
        System.out.println("Es una copia, no el mismo objeto: " + (cargada.getGremio() != gremio));
        System.out.println("El ciclo se conserva (miembro.getGremio() == gremio cargado): "
                + (primero.getGremio() == cargada.getGremio()));

        System.out.println("\n2) transient: los efectos activos (con su hilo) no se guardan");
        mago.aplicarEfecto(new EstadoEfecto("Veneno", TipoEfecto.DANIO_CONTINUO, 4, 3));
        System.out.println("Antes de guardar: efectos = " + mago.getEfectos().size() + ", estado = " + mago.getEstado());
        gestor.guardar(archivo, new PartidaGuardada(gremio, perfil, Map.of()));
        mago.cancelarEfectos();
        PartidaGuardada conEfecto = gestor.cargar(archivo);
        if (conEfecto != null) {
            Personaje magoCargado = conEfecto.getGremio().getMiembros().get(1);
            System.out.println("Despues de cargar: efectos = " + magoCargado.getEfectos().size()
                    + ", estado = " + magoCargado.getEstado() + " (valor por defecto: sin efectos)");
        }

        System.out.println("\n3) NotSerializableException por un objeto anidado:");
        File archivoHeroe = new File(archivos.carpetaPartidas(), "demo_heroe.dat");
        try (ObjectOutputStream salida = new ObjectOutputStream(new FileOutputStream(archivoHeroe))) {
            salida.writeObject(new HeroeConMochila());
        } catch (NotSerializableException e) {
            System.out.println("Provocada -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Error de E/S: " + e.getMessage());
        }
        try (ObjectOutputStream salida = new ObjectOutputStream(new FileOutputStream(archivoHeroe))) {
            salida.writeObject(new HeroeCorregido());
            System.out.println("Resuelta (la Mochila ahora implements Serializable) -> guardado: "
                    + archivoHeroe.length() + " bytes");
        } catch (IOException e) {
            System.out.println("Error de E/S: " + e.getMessage());
        }
        try (ObjectInputStream entrada = new ObjectInputStream(new FileInputStream(archivoHeroe))) {
            HeroeCorregido leido = (HeroeCorregido) entrada.readObject();
            System.out.println("Cargado -> " + leido.nombre + " con " + leido.mochila.contenido);
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error al leer: " + e.getMessage());
        }

        archivos.borrarSiExiste(archivo);
        archivos.borrarSiExiste(archivoHeroe);
    }

    private static void mostrar(Gremio gremio) {
        System.out.println("  Gremio " + gremio.getNombre() + " (" + gremio.getMiembros().size() + " miembros)");
        for (Personaje miembro : gremio.getMiembros()) {
            String extra = miembro instanceof Mago mago ? " | mana " + mago.getMana() : "";
            System.out.println("  - " + miembro.getNombre() + " " + miembro.getVida() + "/" + miembro.getVidaMaxima()
                    + " | arma " + miembro.getArma().getNombre() + " | pociones " + miembro.getInventario().tamanio()
                    + extra);
        }
    }
}
