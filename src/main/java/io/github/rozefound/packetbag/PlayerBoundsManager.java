package io.github.rozefound.packetbag;

import io.github.rozefound.packetbag.utils.FakeBlock;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.joml.Vector2i;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerBoundsManager implements Listener {

  private final Main plugin;

  private final Map<UUID, Map<Location, BlockData>> originalBlockData = new ConcurrentHashMap<>();

  private final Map<UUID, Map<Vector2i, Set<Location>>> playerChunkBlockIndex = new ConcurrentHashMap<>();

  private final BlockData bordersMaterial = Material.BEDROCK.createBlockData();

  public PlayerBoundsManager(Main plugin) {
    this.plugin = plugin;
  }

  public void updateChunks(Player player, Set<Vector2i> addedChunks, Set<Vector2i> removedChunks, int heightHalf) {
    if (player == null) return;
    UUID uid = player.getUniqueId();

    originalBlockData.computeIfAbsent(uid, k -> new ConcurrentHashMap<>());
    playerChunkBlockIndex.computeIfAbsent(uid, k -> new ConcurrentHashMap<>());

    Map<Location, BlockData> originalMap = originalBlockData.get(uid);
    Map<Vector2i, Set<Location>> chunkIndex = playerChunkBlockIndex.get(uid);

    if (removedChunks != null && !removedChunks.isEmpty()) {
      Map<Location, BlockData> toRestore = new HashMap<>();
      for (Vector2i chunk : removedChunks) {
        Set<Location> locs = chunkIndex.get(chunk);
        if (locs == null || locs.isEmpty()) continue;
        for (Location loc : locs) {
          BlockData orig = originalMap.get(loc);
          if (orig != null) toRestore.put(loc, orig);
          originalMap.remove(loc);
        }
        chunkIndex.remove(chunk);
      }
      if (!toRestore.isEmpty()) {
        sendFakeBlocks(player, toRestore); 
      }
    }

    if (addedChunks != null && !addedChunks.isEmpty()) {
      Map<Location, BlockData> toFake = new HashMap<>();
      for (Vector2i chunk : addedChunks) {
        if (chunkIndex.containsKey(chunk)) continue;

        Set<Location> positionsInChunk = computeBorderLocationsInChunk(player, chunk, plugin.getBordersRadius(), heightHalf);
        if (positionsInChunk.isEmpty()) {
          chunkIndex.put(chunk, Collections.emptySet());
          continue;
        }

        Set<Location> stored = new HashSet<>(positionsInChunk.size());
        for (Location loc : positionsInChunk) {
          originalMap.put(loc, loc.getBlock().getBlockData());
          stored.add(loc);

          toFake.put(loc, bordersMaterial);
        }

        chunkIndex.put(chunk, Collections.unmodifiableSet(stored));
      }

      if (!toFake.isEmpty()) {
        sendFakeBlocks(player, toFake);
      }
    }
  }

  private Set<Location> computeBorderLocationsInChunk(Player player, Vector2i chunk, int radius, int heightHalf) {
    Set<Location> out = new HashSet<>();
    Location playerLoc = player.getLocation();
    int centerX = playerLoc.getBlockX();
    int centerZ = playerLoc.getBlockZ();
    int playerY = playerLoc.getBlockY();
    int minY = Math.max(player.getWorld().getMinHeight(), playerY - heightHalf);
    int maxY = Math.min(player.getWorld().getMaxHeight() - 1, playerY + heightHalf);

    int baseX = chunk.x * 16;
    int baseZ = chunk.y * 16;

    for (int x = baseX; x < baseX + 16; x++) {
      for (int z = baseZ; z < baseZ + 16; z++) {
        double dx = x - centerX;
        double dz = z - centerZ;
        double dist = Math.hypot(dx, dz);
        if (Math.round(dist) == radius) {
          for (int y = minY; y <= maxY; y++) {
            Location loc = new Location(player.getWorld(), x, y, z);
            out.add(loc);
          }
        }
      }
    }

    return out;
  }

  private void sendFakeBlocks(Player player, Map<Location, BlockData> blocks) {
    if (player == null || blocks == null || blocks.isEmpty()) return;
    try {
      if (plugin.isVerboseLogging()) plugin.getLogger().info("Sending %d fake blocks to %s".formatted(blocks.size(), player.getName()));
      FakeBlock.sendFakeBlocks(player, blocks);
    } catch (Exception ex) {
      plugin.getLogger().warning("Failed to send fake blocks to " + player.getName() + ": " + ex);
    }
  }

  public boolean isEnabled(Player player) {
    UUID uid = player.getUniqueId();
    return originalBlockData.containsKey(uid);
  }

  public void onEnable(Player player) {
    if (player == null) return;
    UUID uid = player.getUniqueId();
    originalBlockData.putIfAbsent(uid, new ConcurrentHashMap<>());
    playerChunkBlockIndex.putIfAbsent(uid, new ConcurrentHashMap<>());

    var chunks = plugin.getPlayerLoadedChunks(player);
    if (chunks != null && !chunks.isEmpty()) {
      Set<Vector2i> adds = new HashSet<>();
      for (Chunk c : chunks) adds.add(new Vector2i(c.getX(), c.getZ()));
      plugin.queueChunkDelta(uid, adds, Collections.emptySet());
    }
  }

  public void onDisable(Player player) {
    if (player == null) return;
    UUID uid = player.getUniqueId();
    Map<Location, BlockData> original = originalBlockData.getOrDefault(uid, Collections.emptyMap());
    if (!original.isEmpty()) {
      sendFakeBlocks(player, original);
    }
    originalBlockData.remove(uid);
    playerChunkBlockIndex.remove(uid);
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    if (player == null) return;
    UUID uid = player.getUniqueId();
    originalBlockData.remove(uid);
    playerChunkBlockIndex.remove(uid);
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onPlayerMove(PlayerMoveEvent event) {
  }
}
