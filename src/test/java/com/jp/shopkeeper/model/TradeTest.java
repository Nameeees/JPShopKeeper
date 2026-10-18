package com.jp.shopkeeper.model;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Un tradeo nunca puede guardar cantidades imposibles: si aceptara 64 de un
 * item que se apila de uno en uno, cada tradeo entregaria 64 objetos.
 */
public class TradeTest {

    private static ItemStack stack(Material material, int amount) {
        ItemStack item = new ItemStack(material, 1);
        item.setAmount(amount);
        return item;
    }

    @Test
    public void limitaLaCantidadALaPilaDelMaterial() {
        Trade trade = new Trade();
        trade.setResult(stack(Material.DIAMOND_SWORD, 64));
        assertEquals(1, trade.getResult().getAmount());

        trade.setResult(stack(Material.DIAMOND, 200));
        assertEquals(64, trade.getResult().getAmount());

        trade.setCost(0, stack(Material.DIAMOND_SWORD, 64));
        assertEquals(1, trade.getCost(0).getAmount());
    }

    @Test
    public void nuncaGuardaCantidadCeroONegativa() {
        Trade trade = new Trade();
        trade.setCost(0, stack(Material.STONE, 0));
        assertEquals(1, trade.getCost(0).getAmount());
        trade.setCost(1, stack(Material.STONE, -5));
        assertEquals(1, trade.getCost(1).getAmount());
    }

    @Test
    public void guardaCopiasYNoLaReferenciaOriginal() {
        Trade trade = new Trade();
        ItemStack original = new ItemStack(Material.STONE, 10);
        trade.setResult(original);
        original.setAmount(3);
        assertEquals(10, trade.getResult().getAmount());
    }

    @Test
    public void aceptaNullParaVaciarSlots() {
        Trade trade = new Trade();
        trade.setResult(new ItemStack(Material.STONE, 1));
        trade.setResult(null);
        assertNull(trade.getResult());
        trade.setCost(0, null);
        assertNull(trade.getCost(0));
    }

    @Test
    public void dineroYEnfriamientoNuncaSonNegativos() {
        Trade trade = new Trade();
        trade.setMoney(-100);
        trade.setCooldownSeconds(-30);
        assertEquals(0.0, trade.getMoney(), 0.0001);
        assertEquals(0, trade.getCooldownSeconds());
    }

    @Test
    public void elDineroNoAceptaValoresNoFinitos() {
        // Un "money: .NaN" editado a mano dejaria el tradeo gratis sin cobrar.
        Trade trade = new Trade();
        trade.setMoney(Double.NaN);
        assertEquals(0.0, trade.getMoney(), 0.0001);
        trade.setMoney(Double.POSITIVE_INFINITY);
        assertEquals(0.0, trade.getMoney(), 0.0001);
        trade.setMoney(Double.NEGATIVE_INFINITY);
        assertEquals(0.0, trade.getMoney(), 0.0001);
    }

    @Test
    public void estadoDelTradeo() {
        Trade trade = new Trade();
        assertFalse(trade.isComplete());
        assertTrue(trade.isEmpty());
        trade.setResult(new ItemStack(Material.STONE, 1));
        assertTrue(trade.isComplete());
        assertFalse(trade.isEmpty());
    }
}
