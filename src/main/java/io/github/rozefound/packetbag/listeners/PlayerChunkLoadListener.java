package io.github.rozefound.packetbag.listeners;

import io.github.rozefound.packetbag.Main;
import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import io.papermc.paper.event.packet.PlayerChunkUnloadEvent;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.joml.Vector2i;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerChunkLoadListener implements Listener {

  private final Main plugin;
  private final ConcurrentHashMap<UUID, Set<Chunk>> loadedChunks = new ConcurrentHashMap<>();

  public PlayerChunkLoadListener(Main plugin) {
    this.plugin = plugin;
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onPlayerChunkLoad(PlayerChunkLoadEvent event) {
    Player player = event.getPlayer();
    UUID uid = player.getUniqueId();

    Set<Chunk> set = loadedChunks.computeIfAbsent(uid, k -> ConcurrentHashMap.newKeySet());
    boolean added = set.add(event.getChunk()); // true если реально добавлен

    if (added) {
      plugin.queueChunkDelta(uid,
        Collections.singleton(new Vector2i(event.getChunk().getX(), event.getChunk().getZ())),
        Collections.emptySet());
      if (plugin.isVerboseLogging()) plugin.getLogger().info("Chunk load queued for " + player.getName() + " " + event.getChunk().getX() + "," + event.getChunk().getZ());
    }
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onPlayerChunkUnload(PlayerChunkUnloadEvent event) {
    Player player = event.getPlayer();
    UUID uid = player.getUniqueId();

    Set<Chunk> set = loadedChunks.get(uid);
    if (set != null) {
      boolean removed = set.remove(event.getChunk());
      if (removed) {
        plugin.queueChunkDelta(uid,
          Collections.emptySet(),
          Collections.singleton(new Vector2i(event.getChunk().getX(), event.getChunk().getZ())));
        if (plugin.isVerboseLogging()) plugin.getLogger().info("Chunk unload queued for " + player.getName() + " " + event.getChunk().getX() + "," + event.getChunk().getZ());
      }
      if (set.isEmpty()) loadedChunks.remove(uid);
    }
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    loadedChunks.remove(event.getPlayer().getUniqueId());
  }

  public Set<Chunk> getPlayerLoadedChunks(Player player) {
    Set<Chunk> set = loadedChunks.get(player.getUniqueId());
    return set == null ? Collections.emptySet() : Collections.unmodifiableSet(set);
  }
}
