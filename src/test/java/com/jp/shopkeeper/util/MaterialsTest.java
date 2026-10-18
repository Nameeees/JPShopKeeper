package com.jp.shopkeeper.util;

import org.bukkit.Material;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Los materiales se resuelven por nombre porque el enum cambio en 1.13:
 * estas pruebas fijan las traducciones entre nombres modernos y antiguos
 * (se ejecutan contra la API 1.8.8, donde los nombres modernos no existen).
 */
public class MaterialsTest {

    private static String describe(String raw) {
        Materials.Resolved resolved = Materials.resolve(raw);
        return resolved == null ? null : resolved.material.name() + ":" + resolved.data;
    }

    @Test
    public void traduceNombresModernosALa18() {
        assertEquals("STAINED_GLASS_PANE:7", describe("GRAY_STAINED_GLASS_PANE"));
        assertEquals("STAINED_GLASS_PANE:8", describe("LIGHT_GRAY_STAINED_GLASS_PANE"));
        assertEquals("INK_SACK:10", describe("LIME_DYE"));
    }

    @Test
    public void respetaLasVariantesDeLaMismaFamilia() {
        // STAINED_GLASS_PANE tiene cuatro alias: cada data debe ser distinta.
        assertEquals("STAINED_GLASS_PANE:7", describe("GRAY_STAINED_GLASS_PANE"));
        assertEquals("STAINED_GLASS_PANE:8", describe("LIGHT_GRAY_STAINED_GLASS_PANE"));
        assertEquals("STAINED_GLASS_PANE:15", describe("BLACK_STAINED_GLASS_PANE"));
        assertEquals("STAINED_GLASS_PANE:0", describe("WHITE_STAINED_GLASS_PANE"));
    }

    @Test
    public void aceptaElFormatoAntiguoConData() {
        assertEquals("STAINED_GLASS_PANE:7", describe("STAINED_GLASS_PANE:7"));
        assertEquals("STAINED_GLASS_PANE:0", describe("STAINED_GLASS_PANE"));
    }

    @Test
    public void aceptaMinusculasYPrefijos() {
        assertEquals("STONE:0", describe("stone"));
        assertEquals("WATCH:0", describe("minecraft:clock"));
        assertEquals("WATCH:0", describe("CLOCK"));
        assertEquals("WATCH:0", describe("LEGACY_WATCH"));
    }

    @Test
    public void devuelveNullSiNoExiste() {
        assertNull(Materials.resolve("NO_EXISTE_ESTO"));
        assertNull(Materials.resolve(""));
        assertNull(Materials.resolve(null));
    }

    @Test
    public void stackAceptaCantidadesRaras() {
        assertEquals(1, Materials.stack("STONE", 0).getAmount());
        assertEquals(64, Materials.stack("STONE", 999).getAmount());
        assertEquals(7, Materials.stack("STAINED_GLASS_PANE:7", 1).getDurability());
        assertNull(Materials.stack("NO_EXISTE_ESTO", 1));
    }

    @Test
    public void colorHexSeAproximaAlLegacySinServidor() {
        // Sin servidor el plugin cae al modo legacy: el hex se aproxima al
        // color de la paleta mas parecido en vez de desaparecer.
        // El rojo puro queda mas cerca del rojo oscuro legacy que del rojo claro.
        assertEquals("\u00A74Rojo", Text.color("&#ff0000Rojo"));
        assertEquals("\u00A7fBlanco", Text.color("&#ffffffBlanco"));
        assertEquals("\u00A70Negro", Text.color("&#000000Negro"));
    }

    @Test
    public void colorLegacySigueFuncionando() {
        assertEquals("\u00A7aVerde", Text.color("&aVerde"));
        assertEquals("Verde", Text.strip("&aVerde"));
        assertEquals("Rojo", Text.strip("&#ff0000Rojo"));
    }

    @Test
    public void miniMessageSeIgnoraSinAdventure() {
        // Sin Paper, las etiquetas MiniMessage no se interpretan y el texto se
        // queda como estaba en vez de perderse.
        MiniText.setEnabled(true);
        try {
            assertEquals("<red>Hola", Text.color("<red>Hola"));
        } finally {
            MiniText.setEnabled(false);
        }
    }

    @Test
    public void nameOfDevuelveUnNombreUtilizable() {
        // Material#name() no es fiable en las ultimas versiones del servidor:
        // nameOf lo intenta por la clave del registro y cae a name() si no hay.
        assertEquals("STONE", Materials.nameOf(Material.STONE));
        assertEquals("STAINED_GLASS_PANE", Materials.nameOf(Material.STAINED_GLASS_PANE));
        assertEquals("", Materials.nameOf(null));
    }

    @Test
    public void elMaterialDeLosMenusPorDefectoExiste() {
        Materials.Resolved resolved = Materials.resolve("GRAY_STAINED_GLASS_PANE");
        assertEquals(Material.STAINED_GLASS_PANE, resolved.material);
    }
}
