package com.jp.shopkeeper.manager;

import com.jp.shopkeeper.JPShopKeeper;
import com.jp.shopkeeper.model.TradePage;
import com.jp.shopkeeper.model.Trader;
import com.jp.shopkeeper.util.Entities;
import com.jp.shopkeeper.util.Nms;
import com.jp.shopkeeper.util.Ph;
import com.jp.shopkeeper.util.Reflect;
import com.jp.shopkeeper.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Nucleo de los NPCs: registro de traders, generacion, adopcion tras
 * reinicios, proteccion pasiva (anclaje y mirada) y tareas de manteni-
 * miento. Los NPCs son mobs nativos: funcionan igual en 1.8 y en la
 * ultima version, sin NMS ni dependencias.
 */
public final class TraderManager {

    private static final long SPAWN_BACKOFF_MS = 30000L;

    private final JPShopKeeper plugin;
    /** id (minusculas) -> trader */
    private final Map<String, Trader> traders = new LinkedHashMap<String, Trader>();
    /** uuid de entidad -> trader */
    private final Map<UUID, Trader> byEntity = new HashMap<UUID, Trader>();
    /** id -> referencia directa viva de la entidad (la via mas fiable) */
    private final Map<String, Entity> liveEntities = new HashMap<String, Entity>();
    /** id -> momento hasta el que no se reintenta generar el NPC */
    private final Map<String, Long> spawnBackoffUntil = new HashMap<String, Long>();

    /** Desviacion (en bloques) a partir de la cual el anclaje corrige al NPC. */
    private static final double ANCHOR_EPSILON = 0.02;

    private int taskId = -1;
    private int anchorTaskId = -1;

    public TraderManager(JPShopKeeper plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------
    //  Ciclo de vida
    // ------------------------------------------------------------------

    public void loadAll() {
        cancelTask();
        Set<UUID> previousOwned = new HashSet<UUID>(byEntity.keySet());
        traders.clear();
        byEntity.clear();
        liveEntities.clear();
        spawnBackoffUntil.clear();
        for (Trader trader : plugin.storage().loadAll()) {
            traders.put(key(trader.getId()), trader);
        }
        plugin.configManager().debug("Traders cargados: " + traders.size());
        startTask();
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                adoptAll(previousOwned);
            }
        });
    }

    public void shutdown() {
        cancelTask();
        saveAll();
    }

    public void reload() {
        loadAll();
    }

    public void saveAll() {
        for (Trader trader : traders.values()) {
            plugin.storage().save(trader);
        }
    }

    public void save(Trader trader) {
        if (trader == null) {
            return;
        }
        Trader live = byId(trader.getId());
        if (live != null && live != trader) {
            // Un editor abierto guardaba la copia anterior (tipico tras
            // /trader reload): el clic se aplica al trader vivo para no
            // perder la edicion ni resucitar datos viejos.
            copyInto(live, trader);
            plugin.storage().save(live);
            return;
        }
        plugin.storage().save(trader);
    }

    /** Vuelca lo editado sobre la instancia viva, conservando su NPC. */
    private void copyInto(Trader live, Trader edited) {
        live.setDisplayName(edited.getDisplayName());
        live.setEntityTypeName(edited.getEntityTypeName());
        live.setProfessionName(edited.getProfessionName());
        live.setLookAtPlayers(edited.isLookAtPlayers());
        if (edited.getHome() != null) {
            live.setHome(edited.getHome());
        }
        while (live.pageCount() > 0) {
            live.removePage(0);
        }
        for (TradePage page : edited.getPages()) {
            live.addPage(page);
        }
    }

    // ------------------------------------------------------------------
    //  Consultas
    // ------------------------------------------------------------------

    public Trader byId(String id) {
        return id == null ? null : traders.get(key(id));
    }

    public Trader byEntity(Entity entity) {
        return entity == null ? null : byEntity.get(entity.getUniqueId());
    }

    public Collection<Trader> all() {
        return Collections.unmodifiableCollection(traders.values());
    }

    // ------------------------------------------------------------------
    //  Crear / eliminar / editar NPCs
    // ------------------------------------------------------------------

    /** Crea un trader y su NPC. Devuelve null si el id ya existe. */
    public Trader create(String id, Location location, String typeName) {
        if (byId(id) != null || plugin.storage().exists(id)) {
            return null;
        }
        String displayName = plugin.messages().text("editor.default-trader-name", Ph.of("id", id));
        Trader trader = new Trader(id, displayName, typeName, location, plugin.configManager().npcLookAtPlayers());
        traders.put(key(id), trader);
        spawn(trader);
        save(trader);
        return trader;
    }

    public boolean remove(String id) {
        Trader trader = byId(id);
        if (trader == null) {
            return false;
        }
        despawn(trader);
        traders.remove(key(id));
        plugin.storage().delete(trader);
        return true;
    }

    public void rename(Trader trader, String displayName) {
        trader.setDisplayName(displayName);
        Entity entity = findEntity(trader);
        if (entity != null) {
            try {
                entity.setCustomName(Text.color(displayName));
                entity.setCustomNameVisible(true);
            } catch (Throwable ignored) {
                // sin nombre visible, se corrige en el siguiente mantenimiento
            }
        }
        save(trader);
    }

    public void changeType(Trader trader, String typeName) {
        despawn(trader);
        trader.setEntityTypeName(typeName);
        spawn(trader);
        save(trader);
    }

    public void move(Trader trader, Location location) {
        trader.setHome(location);
        Location home = resolvedHome(trader);
        Entity entity = findEntity(trader);
        if (entity != null && home != null) {
            entity.teleport(home);
        }
        save(trader);
    }

    /** El NPC murio pese a las protecciones: se regenera al tick siguiente. */
    public void handleDeath(final Trader trader) {
        Entity entity = findEntity(trader);
        if (entity != null) {
            byEntity.remove(entity.getUniqueId());
        }
        // Aunque la entidad ya no se localice (murio, el chunk esta descargado
        // o la borro otro plugin), su UUID no puede quedarse en el mapa: haria
        // que el NPC desaparecido siguiera contando como protegido.
        if (trader.getEntityId() != null) {
            byEntity.remove(trader.getEntityId());
        }
        liveEntities.remove(trader.getId());
        trader.setEntityId(null);
        save(trader);
        Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                adopt(trader);
            }
        }, 1L);
    }

    // ------------------------------------------------------------------
    //  NPCs: generacion y mantenimiento
    // ------------------------------------------------------------------

    /** Ubicacion del trader con el mundo resuelto por nombre (mundo puede cargar tarde). */
    public Location resolvedHome(Trader trader) {
        Location home = trader.getHome();
        if (home == null) {
            return null;
        }
        if (home.getWorld() == null) {
            World world = Bukkit.getWorld(trader.getHomeWorldName());
            if (world == null) {
                return null;
            }
            home = home.clone();
            home.setWorld(world);
        }
        return home;
    }

    public Entity findEntity(Trader trader) {
        if (trader == null) {
            return null;
        }
        // 1) Referencia directa de la entidad (no depende de busquedas por UUID)
        Entity live = liveEntities.get(trader.getId());
        if (live != null) {
            if (live.isValid()) {
                return live;
            }
            liveEntities.remove(trader.getId());
        }
        // 2) Busqueda por UUID (Bukkit.getEntity o escaneo del mundo cargado)
        if (trader.getEntityId() == null) {
            return null;
        }
        Location home = resolvedHome(trader);
        Entity found = lookupByUuid(trader.getEntityId(), home);
        if (found != null) {
            liveEntities.put(trader.getId(), found);
        }
        return found;
    }

    /** Busca una entidad por UUID: API moderna o escaneo del mundo si el chunk esta cargado. */
    private Entity lookupByUuid(UUID id, Location near) {
        if (id == null) {
            return null;
        }
        Entity direct = bukkitGetEntity(id);
        if (direct != null) {
            return direct;
        }
        if (near == null || near.getWorld() == null) {
            return null;
        }
        if (!near.getWorld().isChunkLoaded(near.getBlockX() >> 4, near.getBlockZ() >> 4)) {
            return null;
        }
        for (Entity entity : near.getWorld().getEntities()) {
            if (entity.getUniqueId().equals(id)) {
                return entity;
            }
        }
        return null;
    }

    private void adoptAll(Set<UUID> previousOwned) {
        if (previousOwned != null && !previousOwned.isEmpty()) {
            Set<UUID> current = new HashSet<UUID>();
            for (Trader trader : traders.values()) {
                if (trader.getEntityId() != null) {
                    current.add(trader.getEntityId());
                }
            }
            for (UUID old : previousOwned) {
                if (current.contains(old)) {
                    continue;
                }
                Entity orphan = bukkitGetEntity(old);
                if (orphan != null) {
                    orphan.remove();
                    plugin.configManager().debug("NPC huerfano eliminado tras la recarga: " + old);
                }
            }
        }
        // Se adopta como maximo un trader por tick: si hay muchos NPCs sin
        // entidad, cargar sus chunks de golpe bloquearia el arranque del
        // servidor. El mantenimiento sigue adoptando los que falten.
        List<Trader> pending = new ArrayList<Trader>(traders.values());
        for (int i = 0; i < pending.size(); i++) {
            final Trader trader = pending.get(i);
            if (i == 0) {
                adopt(trader);
                continue;
            }
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    adopt(trader);
                }
            }, i);
        }
    }

    void adopt(Trader trader) {
        if (trader == null) {
            return;
        }
        if (byId(trader.getId()) != trader) {
            // Se elimino o se recargo mientras la adopcion estaba en cola:
            // adoptarlo resucitaria el NPC y su archivo.
            return;
        }
        Long retryAt = spawnBackoffUntil.get(trader.getId());
        if (retryAt != null) {
            if (System.currentTimeMillis() < retryAt) {
                return;
            }
            spawnBackoffUntil.remove(trader.getId());
        }
        Entity existing = findEntity(trader);
        if (existing != null) {
            applyNpcSettings(existing, trader);
            byEntity.put(existing.getUniqueId(), trader);
            return;
        }
        Location home = resolvedHome(trader);
        if (home == null || home.getWorld() == null) {
            plugin.configManager().debug("El mundo del trader '" + trader.getId() + "' no esta disponible todavia.");
            return;
        }
        boolean chunkLoaded = isChunkLoaded(home);

        if (trader.getEntityId() == null) {
            // Nunca se genero (o murio): cargar el chunk y crear el NPC.
            ensureChunk(home);
            Entity afterLoad = findEntity(trader);
            if (afterLoad != null) {
                applyNpcSettings(afterLoad, trader);
                byEntity.put(afterLoad.getUniqueId(), trader);
                return;
            }
            if (spawn(trader)) {
                save(trader);
                if (plugin.configManager().logNpcRespawn()) {
                    plugin.getLogger().info("NPC generado para el trader '" + trader.getId() + "'.");
                }
            }
            return;
        }

        if (chunkLoaded) {
            // El chunk esta cargado y la entidad no aparece: desaparecio de verdad.
            if (spawn(trader)) {
                save(trader);
                if (plugin.configManager().logNpcRespawn()) {
                    plugin.getLogger().info("NPC regenerado para el trader '" + trader.getId() + "'.");
                }
            }
            return;
        }

        // Chunk descargado: el NPC sigue guardado con el mundo y
        // aparecera cuando el chunk vuelva a cargarse.
        plugin.configManager().debug("NPC del trader '" + trader.getId() + "' en chunk descargado: se adopta cuando cargue.");
    }

    private boolean isChunkLoaded(Location home) {
        World world = home.getWorld();
        if (world == null) {
            return false;
        }
        try {
            return world.isChunkLoaded(home.getBlockX() >> 4, home.getBlockZ() >> 4);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Carga (o genera) el chunk del NPC para poder generarlo. */
    private void ensureChunk(Location home) {
        if (isChunkLoaded(home)) {
            return;
        }
        World world = home.getWorld();
        if (world == null) {
            return;
        }
        try {
            int chunkX = home.getBlockX() >> 4;
            int chunkZ = home.getBlockZ() >> 4;
            world.getChunkAt(chunkX, chunkZ);
            plugin.configManager().debug("Chunk cargado para el NPC en " + world.getName()
                    + " (" + chunkX + ", " + chunkZ + ").");
        } catch (Throwable throwable) {
            plugin.configManager().debug("No se pudo cargar el chunk del NPC: " + throwable);
        }
    }

    private boolean spawn(Trader trader) {
        Location home = resolvedHome(trader);
        if (home == null || home.getWorld() == null) {
            plugin.getLogger().warning("No se pudo generar el NPC del trader '" + trader.getId() + "': mundo no disponible.");
            return false;
        }
        EntityType type = Entities.type(trader.getEntityTypeName());
        if (type == null) {
            type = Entities.type(plugin.configManager().defaultType());
        }
        if (type == null) {
            plugin.getLogger().warning("Tipo de mob invalido para el trader '" + trader.getId() + "': " + trader.getEntityTypeName());
            return false;
        }
        try {
            Entity entity = home.getWorld().spawnEntity(home, type);
            // Verificacion real: en varias versiones, si otro plugin cancela
            // CreatureSpawnEvent, spawnEntity devuelve la entidad pero NO la
            // agrega al mundo. Hay que comprobar que existe de verdad.
            if (entity == null || lookupByUuid(entity.getUniqueId(), home) == null) {
                spawnFailed(trader, home,
                        "otro plugin o una proteccion (regiones con mob-spawning denegado, limitadores de mobs...) "
                                + "cancelo la aparicion del NPC (CreatureSpawnEvent)");
                return false;
            }
            trader.setEntityId(entity.getUniqueId());
            applyNpcSettings(entity, trader);
            byEntity.put(entity.getUniqueId(), trader);
            liveEntities.put(trader.getId(), entity);
            spawnBackoffUntil.remove(trader.getId());
            return true;
        } catch (Throwable throwable) {
            spawnFailed(trader, home, String.valueOf(throwable));
            return false;
        }
    }

    /** Registra el fallo con un aviso claro y aplica espera antes del siguiente intento. */
    private void spawnFailed(Trader trader, Location home, String cause) {
        spawnBackoffUntil.put(trader.getId(), System.currentTimeMillis() + SPAWN_BACKOFF_MS);
        String reason = blockedLocation(home)
                ? "la ubicacion del trader no admite un mob (bloque solido o fuera del mundo), muevelo con /trader edit"
                : cause;
        plugin.getLogger().warning("No se pudo agregar el NPC del trader '" + trader.getId() + "' en "
                + home.getWorld().getName() + " " + (int) home.getX() + ", " + (int) home.getY() + ", " + (int) home.getZ()
                + ": " + reason + ". Se reintentara en " + (SPAWN_BACKOFF_MS / 1000) + "s.");
    }

    /** La ubicacion guardada no admite un mob: bloque solido o fuera del mundo. */
    private boolean blockedLocation(Location home) {
        try {
            World world = home == null ? null : home.getWorld();
            if (world == null) {
                return true;
            }
            Material block = world.getBlockAt(home).getType();
            return block != null && block.isSolid();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void despawn(Trader trader) {
        Entity entity = findEntity(trader);
        if (entity != null) {
            byEntity.remove(entity.getUniqueId());
            entity.remove();
        }
        liveEntities.remove(trader.getId());
        spawnBackoffUntil.remove(trader.getId());
        trader.setEntityId(null);
    }

    private void applyNpcSettings(Entity entity, Trader trader) {
        try {
            entity.setCustomName(Text.color(trader.getDisplayName()));
            entity.setCustomNameVisible(true);
        } catch (Throwable ignored) {
            // propiedades cosmeticas: no son criticas
        }
        // Silencio y sin IA: primero el API (1.9+); en 1.8, donde el API y el
        // NMS no tienen esos metodos, se escriben las etiquetas NBT del mob.
        boolean silent = Reflect.call(entity, "setSilent", Boolean.TRUE) != null;
        boolean noAi = Reflect.call(entity, "setAI", Boolean.FALSE) != null;
        if (!silent || !noAi) {
            Nms.applyPeaceful(entity, !noAi, !silent);
        }
        Reflect.call(entity, "setInvulnerable", Boolean.TRUE);
        Reflect.call(entity, "setCanPickupItems", Boolean.FALSE);
        Reflect.call(entity, "setCollidable", Boolean.FALSE);
        Reflect.call(entity, "setGravity", Boolean.FALSE);
        if (plugin.configManager().npcForcePersistent()) {
            Reflect.call(entity, "setRemoveWhenFarAway", Boolean.FALSE);
            Reflect.call(entity, "setPersistent", Boolean.TRUE);
        }
        Entities.applyProfession(entity, trader.getProfessionName());
        Location home = resolvedHome(trader);
        if (home != null) {
            rotate(entity, home.getYaw());
        }
    }

    /**
     * Gira la entidad: setRotation donde exista (1.10+); en versiones
     * antiguas se teletransporta sobre su propia posicion con el yaw nuevo.
     */
    private void rotate(Entity entity, float yaw) {
        if (Reflect.call(entity, "setRotation", Float.valueOf(yaw), Float.valueOf(0.0f)) != null) {
            return;
        }
        try {
            Location current = entity.getLocation();
            current.setYaw(yaw);
            current.setPitch(0.0f);
            entity.teleport(current);
        } catch (Throwable ignored) {
            // rotacion no critica
        }
    }

    private void startTask() {
        int interval = plugin.configManager().maintenanceInterval();
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
                maintenance();
            }
        }, interval, interval);

        // Anclaje anti-empuje: en 1.8 no existe setCollidable, asi que el
        // NPC se re-fija a su punto antes de que el cliente note el empuje
        // de la hitbox (tambien cubre knockback de otras fuentes).
        int anchorInterval = plugin.configManager().anchorInterval();
        anchorTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
                anchorAll();
            }
        }, anchorInterval, anchorInterval);
    }

    /**
     * Devuelve al punto de origen a cualquier NPC que se haya desplazado
     * (empujones de jugadores, mobs, knockback...) conservando su mirada.
     */
    private void anchorAll() {
        for (Trader trader : traders.values()) {
            Entity entity = findEntity(trader);
            if (entity == null) {
                continue;
            }
            Location home = resolvedHome(trader);
            if (home == null || home.getWorld() == null) {
                continue;
            }
            Location current = entity.getLocation();
            if (current.getWorld() == null || !current.getWorld().equals(home.getWorld())) {
                moveTo(entity, home, current);
                continue;
            }
            double dx = current.getX() - home.getX();
            double dy = current.getY() - home.getY();
            double dz = current.getZ() - home.getZ();
            if (dx * dx + dy * dy + dz * dz > ANCHOR_EPSILON * ANCHOR_EPSILON) {
                moveTo(entity, home, current);
            }
        }
    }

    private void moveTo(Entity entity, Location home, Location current) {
        try {
            Location fix = home.clone();
            if (current != null) {
                fix.setYaw(current.getYaw());
                fix.setPitch(current.getPitch());
            }
            entity.teleport(fix);
            entity.setVelocity(new Vector(0, 0, 0));
        } catch (Throwable ignored) {
            // si la entidad acaba de desaparecer, el mantenimiento la regenerara
        }
    }

    private void cancelTask() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        if (anchorTaskId != -1) {
            Bukkit.getScheduler().cancelTask(anchorTaskId);
            anchorTaskId = -1;
        }
    }

    /**
     * Cada pocos ticks: adopta NPCs desaparecidos (con el chunk cargado),
     * los devuelve a su punto si algo los empujo y gira la mirada hacia
     * el jugador mas cercano si esta activado.
     */
    private void maintenance() {
        // Como maximo se adopta un trader por pasada: adoptar de golpe todos
        // los que no tienen NPC cargaria sus chunks en el mismo tick.
        boolean adopted = false;
        for (Trader trader : new ArrayList<Trader>(traders.values())) {
            Entity entity = findEntity(trader);
            if (entity == null) {
                if (!adopted) {
                    adopted = true;
                    adopt(trader);
                }
                continue;
            }
            byEntity.put(entity.getUniqueId(), trader);
            Location home = resolvedHome(trader);
            if (home == null) {
                continue;
            }
            Location current = entity.getLocation();
            double dx = current.getX() - home.getX();
            double dy = current.getY() - home.getY();
            double dz = current.getZ() - home.getZ();
            double distanceSquared = dx * dx + dy * dy + dz * dz;
            double snap = plugin.configManager().snapDistance();
            if (current.getWorld() == null || !current.getWorld().equals(home.getWorld()) || distanceSquared > snap * snap) {
                entity.teleport(home);
                current = home;
            }
            float yaw = home.getYaw();
            if (trader.isLookAtPlayers() && plugin.configManager().npcLookAtPlayers()) {
                Player nearest = nearestPlayer(home, plugin.configManager().lookRadius());
                if (nearest != null) {
                    yaw = yawToward(home, nearest.getLocation());
                }
            }
            rotate(entity, yaw);
        }
    }

    private Player nearestPlayer(Location center, double radius) {
        World world = center.getWorld();
        if (world == null) {
            return null;
        }
        Player best = null;
        double bestSquared = radius * radius;
        for (Player player : world.getPlayers()) {
            if (player.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }
            double distance = player.getLocation().distanceSquared(center);
            if (distance <= bestSquared) {
                bestSquared = distance;
                best = player;
            }
        }
        return best;
    }

    private float yawToward(Location from, Location to) {
        Location look = from.clone();
        look.setDirection(to.toVector().subtract(from.toVector()));
        return look.getYaw();
    }

    private Entity bukkitGetEntity(UUID id) {
        try {
            Method method = Bukkit.class.getMethod("getEntity", UUID.class);
            Object result = method.invoke(null, id);
            if (result instanceof Entity) {
                return (Entity) result;
            }
        } catch (Throwable ignored) {
            // metodo no disponible antes de 1.11: se usa el escaneo
        }
        return null;
    }

    private static String key(String id) {
        return id.toLowerCase(Locale.ROOT);
    }
}
