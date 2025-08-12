package io.github.rozefound.packetbag.listeners;

import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import io.papermc.paper.event.packet.PlayerChunkUnloadEvent;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.*;

public class PlayerChunkLoadListener implements Listener {

  Map<UUID, Set<Chunk>> loadedChunks = new HashMap<>();

  @EventHandler(priority = EventPriority.LOW)
  public void onPlayerChunkLoad(PlayerChunkLoadEvent event) {

    var player = event.getPlayer();

    if (!loadedChunks.containsKey(player.getUniqueId()))
      loadedChunks.put(player.getUniqueId(), new HashSet<>());

    loadedChunks.get(player.getUniqueId()).add(event.getChunk());

  }

  @EventHandler(priority = EventPriority.LOW)
  public void onPlayerChunkUnload(PlayerChunkUnloadEvent event) {

    var player = event.getPlayer();

    if (loadedChunks.containsKey(player.getUniqueId()))
      loadedChunks.get(player.getUniqueId()).remove(event.getChunk());

  }

  public Set<Chunk> getPlayerLoadedChunks(Player player) {

    if (loadedChunks.containsKey(player.getUniqueId()))
      return loadedChunks.get(player.getUniqueId());

    return null;

  }

}
