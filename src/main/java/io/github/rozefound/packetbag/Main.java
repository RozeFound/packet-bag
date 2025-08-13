package io.github.rozefound.packetbag;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.rozefound.packetbag.listeners.PacketEventListener;
import io.github.rozefound.packetbag.listeners.PlayerChunkLoadListener;
import io.github.rozefound.packetbag.utils.LightPacket;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.joml.Vector2i;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@DefaultQualifier(NonNull.class)
public final class Main extends JavaPlugin {

    private PlayerBoundsManager playerBoundsManager;
    private PlayerChunkLoadListener playerChunkLoadListener;
    private TestCommand testCommand;

    private int bordersRadius = 128;
    private int lightUpdateIntervalTicks = 20;
    private boolean verboseLogging = false;
    private int boundsProcessIntervalTicks = 10;
    private int boundsUpdateDebounceTicks = 20;
    private int bordersHeightHalf = 3;

    private final Map<UUID, Integer> playerChunkHash = new ConcurrentHashMap<>();
    private final Map<UUID, Vector2i> playerLastChunkPos = new ConcurrentHashMap<>();
    private static final Set<Integer> FULL_SECTORS =
            IntStream.range(0, 20).boxed().collect(Collectors.toUnmodifiableSet());

    private final ConcurrentHashMap<UUID, Set<Vector2i>> pendingAddedChunks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Set<Vector2i>> pendingRemovedChunks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> lastBoundsProcessedAt = new ConcurrentHashMap<>();

    private BukkitRunnable lightTask;
    private BukkitRunnable boundsProcessorTask;

    @Override
    public void onLoad() {
        PacketEvents.getAPI().getEventManager().registerListener(
                new PacketEventListener(), PacketListenerPriority.NORMAL);
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();

        getLogger().info("PacketBag starting — server version: " + Bukkit.getVersion());
        if (verboseLogging) getLogger().info("Verbose logging enabled");

        testCommand = new TestCommand(this);
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(testCommand.buildCommand());
        });

        playerBoundsManager = new PlayerBoundsManager(this);
        playerChunkLoadListener = new PlayerChunkLoadListener(this);

        getServer().getPluginManager().registerEvents(playerChunkLoadListener, this);
        getServer().getPluginManager().registerEvents(playerBoundsManager, this);

        startLightUpdateTask();
        startBoundsProcessorTask();
    }

    @Override
    public void onDisable() {
        if (lightTask != null) {
            lightTask.cancel();
            lightTask = null;
        }
        if (boundsProcessorTask != null) {
            boundsProcessorTask.cancel();
            boundsProcessorTask = null;
        }

        pendingAddedChunks.clear();
        pendingRemovedChunks.clear();
        lastBoundsProcessedAt.clear();
        playerChunkHash.clear();
        playerLastChunkPos.clear();

        if (playerBoundsManager != null) {
            for (Player p : getServer().getOnlinePlayers()) {
                try {
                    if (playerBoundsManager.isEnabled(p)) playerBoundsManager.onDisable(p);
                } catch (Exception ex) {
                    getLogger().warning("Error while disabling bounds for " + p.getName() + ": " + ex);
                }
            }
        }

        getLogger().info("PacketBag disabled and cleaned up");
    }

    private void loadConfigValues() {
        try {
            reloadConfig();
            bordersRadius = getConfig().getInt("bordersRadius", bordersRadius);
            lightUpdateIntervalTicks = getConfig().getInt("lightUpdateIntervalTicks", lightUpdateIntervalTicks);
            verboseLogging = getConfig().getBoolean("verboseLogging", verboseLogging);
            boundsProcessIntervalTicks = getConfig().getInt("boundsProcessIntervalTicks", boundsProcessIntervalTicks);
            boundsUpdateDebounceTicks = getConfig().getInt("boundsUpdateDebounceTicks", boundsUpdateDebounceTicks);
            bordersHeightHalf = getConfig().getInt("bordersHeightHalf", bordersHeightHalf);

            getLogger().info("Config loaded: radius=%d lightTicks=%d verbose=%b boundsProc=%d debounce=%d heightHalf=%d"
                    .formatted(bordersRadius, lightUpdateIntervalTicks, verboseLogging, boundsProcessIntervalTicks, boundsUpdateDebounceTicks, bordersHeightHalf));
        } catch (Exception ex) {
            getLogger().warning("Failed to load config.yml: " + ex.getMessage());
        }
    }

    public void startLightUpdateTask() {
        if (lightTask != null) {
            lightTask.cancel();
        }

        final int interval = Math.max(1, lightUpdateIntervalTicks);
        lightTask = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (Player player : getServer().getOnlinePlayers()) {
                        Set<Chunk> chunks = playerChunkLoadListener.getPlayerLoadedChunks(player);
                        if (chunks.isEmpty()) continue;

                        Set<Vector2i> chunkCoords = new HashSet<>();
                        for (Chunk c : chunks) chunkCoords.add(new Vector2i(c.getX(), c.getZ()));

                        Vector2i currentChunkPos = new Vector2i(player.getLocation().getChunk().getX(), player.getLocation().getChunk().getZ());
                        int coordsHash = chunkCoords.hashCode();

                        Integer lastHash = playerChunkHash.get(player.getUniqueId());
                        Vector2i lastPos = playerLastChunkPos.get(player.getUniqueId());

                        if (Objects.equals(currentChunkPos, lastPos) && lastHash != null && lastHash == coordsHash) continue;

                        playerLastChunkPos.put(player.getUniqueId(), currentChunkPos);
                        playerChunkHash.put(player.getUniqueId(), coordsHash);

                        Map<Vector2i, Set<Integer>> chunkSectorsMap = new HashMap<>();
                        for (Vector2i coord : chunkCoords) {
                            chunkSectorsMap.put(coord, FULL_SECTORS);
                        }

                        Bukkit.getScheduler().runTaskAsynchronously(Main.this, () -> {
                            Bukkit.getScheduler().runTask(Main.this, () -> {
                                try {
                                    LightPacket.sendDarkLight(player, chunkSectorsMap, true, false);
                                    if (verboseLogging) getLogger().info("Sent light update to " + player.getName() + " chunks=" + chunkCoords.size());
                                } catch (Exception ex) {
                                    getLogger().warning("Failed to send light packet to " + player.getName() + ": " + ex);
                                }
                            });
                        });
                    }
                } catch (Exception ex) {
                    getLogger().warning("Exception in light update task: " + ex);
                }
            }
        };
        lightTask.runTaskTimer(this, 1L, interval);
        getLogger().info("Light update task started, interval ticks=" + interval);
    }

    private void startBoundsProcessorTask() {
        if (boundsProcessorTask != null) boundsProcessorTask.cancel();

        boundsProcessorTask = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    processPendingChunkDeltas();
                } catch (Exception ex) {
                    getLogger().warning("Exception in bounds processor: " + ex);
                }
            }
        };
        boundsProcessorTask.runTaskTimer(this, 1L, Math.max(1, boundsProcessIntervalTicks));
        getLogger().info("Bounds processor task started, interval ticks=" + boundsProcessIntervalTicks);
    }

    public void queueChunkDelta(UUID playerUuid, Set<Vector2i> added, Set<Vector2i> removed) {
        if (playerUuid == null) return;
        if ((added == null || added.isEmpty()) && (removed == null || removed.isEmpty())) return;

        pendingAddedChunks.compute(playerUuid, (k, v) -> {
            Set<Vector2i> set = v == null ? new HashSet<>() : v;
            if (added != null) set.addAll(added);
            return set;
        });

        pendingRemovedChunks.compute(playerUuid, (k, v) -> {
            Set<Vector2i> set = v == null ? new HashSet<>() : v;
            if (removed != null) set.addAll(removed);
            return set;
        });

        if (verboseLogging) getLogger().info("Queued chunk delta for " + playerUuid + " add=" + (added == null ? 0 : added.size()) + " rem=" + (removed == null ? 0 : removed.size()));
    }

    private void processPendingChunkDeltas() {
        if (pendingAddedChunks.isEmpty() && pendingRemovedChunks.isEmpty()) return;

        Set<UUID> players = new HashSet<>();
        players.addAll(pendingAddedChunks.keySet());
        players.addAll(pendingRemovedChunks.keySet());

        for (UUID uid : players) {
            try {
                long nowTickApprox = System.currentTimeMillis() / 50L;
                Long lastTick = lastBoundsProcessedAt.get(uid);
                if (lastTick != null && (nowTickApprox - lastTick < boundsUpdateDebounceTicks)) continue;

                Set<Vector2i> adds = pendingAddedChunks.remove(uid);
                Set<Vector2i> rems = pendingRemovedChunks.remove(uid);

                if ((adds == null || adds.isEmpty()) && (rems == null || rems.isEmpty())) continue;

                Player player = getServer().getPlayer(uid);
                if (player == null) continue;

                if (adds != null && rems != null) {
                    for (Vector2i v : new HashSet<>(adds)) {
                        if (rems.contains(v)) {
                            adds.remove(v);
                            rems.remove(v);
                        }
                    }
                }

                lastBoundsProcessedAt.put(uid, nowTickApprox);
                Set<Vector2i> finalAdds = adds == null ? Collections.emptySet() : adds;
                Set<Vector2i> finalRems = rems == null ? Collections.emptySet() : rems;

                Bukkit.getScheduler().runTask(this, () -> playerBoundsManager.updateChunks(player, finalAdds, finalRems, bordersHeightHalf));
            } catch (Exception ex) {
                getLogger().warning("Error processing pending chunk deltas for uid " + uid + ": " + ex);
            }
        }
    }

    public int getBordersRadius() { return bordersRadius; }
    public boolean isVerboseLogging() { return verboseLogging; }
    public int getBoundsUpdateDebounceTicks() { return boundsUpdateDebounceTicks; }
    public int getBordersHeightHalf() { return bordersHeightHalf; }

    public Set<Chunk> getPlayerLoadedChunks(Player player) {
        if (player == null || playerChunkLoadListener == null) return Collections.emptySet();
        return playerChunkLoadListener.getPlayerLoadedChunks(player);
    }

    public PlayerBoundsManager getPlayerBoundsManager() { return playerBoundsManager; }
}
