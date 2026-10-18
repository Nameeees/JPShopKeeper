package com.jp.shopkeeper.model;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Un trader: NPC con nombre, tipo de mob, posicion fija y paginas de tradeos.
 * Es la unidad que se guarda en plugins/JPShopKeeper/traders/<id>.yml.
 */
public class Trader {

    private final String id;
    private String displayName;
    private String entityTypeName;
    private String professionName = "";
    private UUID entityId;
    private Location home;
    private String homeWorldName = "";
    private boolean lookAtPlayers = true;
    private final List<TradePage> pages = new ArrayList<TradePage>();

    public Trader(String id, String displayName, String entityTypeName, Location home, boolean lookAtPlayers) {
        this.id = id;
        this.displayName = displayName;
        this.entityTypeName = entityTypeName;
        setHome(home);
        this.lookAtPlayers = lookAtPlayers;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName == null ? id : displayName;
    }

    public String getEntityTypeName() {
        return entityTypeName;
    }

    public void setEntityTypeName(String entityTypeName) {
        this.entityTypeName = entityTypeName;
    }

    public String getProfessionName() {
        return professionName;
    }

    public void setProfessionName(String professionName) {
        this.professionName = professionName == null ? "" : professionName;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public Location getHome() {
        return home == null ? null : home.clone();
    }

    public void setHome(Location home) {
        if (home == null) {
            this.home = null;
            return;
        }
        this.home = home.clone();
        if (home.getWorld() != null) {
            this.homeWorldName = home.getWorld().getName();
        }
    }

    public String getHomeWorldName() {
        if (homeWorldName == null || homeWorldName.isEmpty()) {
            return home != null && home.getWorld() != null ? home.getWorld().getName() : "";
        }
        return homeWorldName;
    }

    public void setHomeWorldName(String worldName) {
        this.homeWorldName = worldName == null ? "" : worldName;
    }

    public boolean isLookAtPlayers() {
        return lookAtPlayers;
    }

    public void setLookAtPlayers(boolean lookAtPlayers) {
        this.lookAtPlayers = lookAtPlayers;
    }

    public void addPage(TradePage page) {
        if (page != null) {
            pages.add(page);
        }
    }

    public void removePage(int index) {
        if (index >= 0 && index < pages.size()) {
            pages.remove(index);
        }
    }

    public TradePage getPage(int index) {
        if (index < 0 || index >= pages.size()) {
            return null;
        }
        return pages.get(index);
    }

    public List<TradePage> getPages() {
        return Collections.unmodifiableList(pages);
    }

    public int pageCount() {
        return pages.size();
    }

    public int totalTrades() {
        int total = 0;
        for (TradePage page : pages) {
            total += page.count();
        }
        return total;
    }

    /** Coordenadas legibles para mensajes: "x, y, z". */
    public String homeString() {
        if (home == null) {
            return "?";
        }
        return (int) Math.floor(home.getX()) + ", " + (int) Math.floor(home.getY()) + ", " + (int) Math.floor(home.getZ());
    }
}
