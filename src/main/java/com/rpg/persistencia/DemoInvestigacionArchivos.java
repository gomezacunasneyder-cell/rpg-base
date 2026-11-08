package com.rpg.persistencia;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 * INVESTIGACION 1 - FileWriter vs Files.writeString
 * FileWriter (con BufferedWriter) es un flujo: lo abres, escribes poco a poco con write()/newLine() y lo cierras tu
 * (try-with-resources); sirve para archivos grandes o cuando generas el texto por partes, como el log de combate.
 * Files.writeString(Path, texto, opciones...) es una sola llamada de NIO que abre, escribe todo el String y cierra;
 * es mas corta y usa UTF-8 por defecto, pero necesita todo el texto en memoria. La usaria para textos pequenos
 * que ya tengo completos (por ejemplo un archivo de configuracion o el autoguardado).
 *
 * INVESTIGACION 2 - Seguridad de Serializable
 * No se debe llamar a readObject() con datos que no son de confianza: al reconstruir los objetos Java ejecuta codigo
 * de las clases que trae el archivo (sus metodos readObject, constructores de colecciones, etc.) ANTES de que tu
 * cast compruebe el tipo, asi que un archivo manipulado puede ejecutar codigo (cadenas de "gadgets" de las librerias
 * del classpath) o agotar memoria y pila con estructuras enormes o muy anidadas.
 * Defensas: un filtro ObjectInputFilter (lista blanca de clases y limites de profundidad, referencias y bytes),
 * como el de GestorPartida; y, mejor aun, no usar serializacion nativa para datos externos y guardar en JSON o en un
 * formato de texto propio, que solo se lee como datos y se valida campo por campo.
 */
public class DemoInvestigacionArchivos {

    private static class Peligroso implements Serializable {
        private static final long serialVersionUID = 1L;

        private void readObject(ObjectInputStream entrada) throws IOException, ClassNotFoundException {
            entrada.defaultReadObject();
            System.out.println("   >>> Se ejecuto codigo de la clase durante readObject() (aqui podria ir cualquier cosa)");
        }
    }

    public static void main(String[] args) {
        compararEscritura();
        mostrarRiesgoDeSerializable();
    }

    private static void compararEscritura() {
        System.out.println("=== Investigacion 1: FileWriter vs Files.writeString ===");
        GestorArchivos archivos = new GestorArchivos();
        if (!archivos.asegurarCarpeta()) {
            System.out.println("No se pudo crear la carpeta " + GestorArchivos.CARPETA_PARTIDAS);
            return;
        }
        File conWriter = new File(archivos.carpetaPartidas(), "demo_filewriter.txt");
        File conString = new File(archivos.carpetaPartidas(), "demo_writestring.txt");
        String salto = System.lineSeparator();
        String texto = "Linea 1" + salto + "Linea 2" + salto;
        System.out.println("Charset por defecto de FileWriter(File): " + Charset.defaultCharset());

        try (BufferedWriter escritor = new BufferedWriter(new FileWriter(conWriter))) {
            escritor.write("Linea 1");
            escritor.newLine();
            escritor.write("Linea 2");
            escritor.newLine();
        } catch (IOException e) {
            System.out.println("Error con FileWriter: " + e.getMessage());
            return;
        }
        try {
            Files.writeString(conString.toPath(), texto);
            System.out.println("FileWriter: " + conWriter.length() + " bytes | Files.writeString: "
                    + conString.length() + " bytes | mismo contenido: "
                    + Arrays.equals(Files.readAllBytes(conWriter.toPath()), Files.readAllBytes(conString.toPath())));

            Files.writeString(conString.toPath(), "Linea 3" + salto, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            System.out.println("Con StandardOpenOption.APPEND el archivo pasa a " + conString.length() + " bytes (agrega, no pisa)");
        } catch (IOException e) {
            System.out.println("Error con Files.writeString: " + e.getMessage());
        }
        archivos.borrarSiExiste(conWriter);
        archivos.borrarSiExiste(conString);
    }

    private static void mostrarRiesgoDeSerializable() {
        System.out.println("\n=== Investigacion 2: seguridad de Serializable ===");
        try {
            byte[] datos = serializar(new Peligroso());

            System.out.println("1) Sin filtro: lee un objeto que NO es una partida y luego hace el cast:");
            try (ObjectInputStream entrada = new ObjectInputStream(new ByteArrayInputStream(datos))) {
                PartidaGuardada partida = (PartidaGuardada) entrada.readObject();
                System.out.println("   Partida leida: " + partida);
            } catch (ClassCastException e) {
                System.out.println("   El cast fallo (ClassCastException), pero el codigo anterior ya se habia ejecutado");
            }

            System.out.println("2) Con filtro (lista blanca de GestorPartida):");
            try (ObjectInputStream entrada = new ObjectInputStream(new ByteArrayInputStream(datos))) {
                entrada.setObjectInputFilter(ObjectInputFilter.Config.createFilter(GestorPartida.FILTRO_DESERIALIZACION));
                entrada.readObject();
            } catch (InvalidClassException e) {
                System.out.println("   Rechazado antes de ejecutar nada -> InvalidClassException: " + e.getMessage());
            }

            byte[] anidado = serializar(listaAnidada(300));
            System.out.println("3) Estructura muy anidada (300 niveles):");
            try (ObjectInputStream entrada = new ObjectInputStream(new ByteArrayInputStream(anidado))) {
                entrada.readObject();
                System.out.println("   Sin filtro: se reconstruye entera (consume pila y memoria)");
            }
            try (ObjectInputStream entrada = new ObjectInputStream(new ByteArrayInputStream(anidado))) {
                entrada.setObjectInputFilter(ObjectInputFilter.Config.createFilter(GestorPartida.FILTRO_DESERIALIZACION));
                entrada.readObject();
            } catch (InvalidClassException e) {
                System.out.println("   Con filtro (maxdepth): rechazada -> " + e.getMessage());
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error inesperado en la demo: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static byte[] serializar(Object objeto) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream salida = new ObjectOutputStream(bytes)) {
            salida.writeObject(objeto);
        }
        return bytes.toByteArray();
    }

    private static List<Object> listaAnidada(int niveles) {
        List<Object> raiz = new ArrayList<>();
        List<Object> actual = raiz;
        for (int i = 0; i < niveles; i++) {
            List<Object> siguiente = new ArrayList<>();
            actual.add(siguiente);
            actual = siguiente;
        }
        return raiz;
    }
}
