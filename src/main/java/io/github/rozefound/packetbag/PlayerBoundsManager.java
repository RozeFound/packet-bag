package io.github.rozefound.packetbag;

import io.github.rozefound.packetbag.utils.FakeBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.joml.Vector3i;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class PlayerBoundsManager implements Listener {

  private final Main plugin;

  private final Map<UUID, Map<Location, BlockData>> originalBlockData = new HashMap<>();

  private final Map<UUID, Vector3i> playerLastSector = new HashMap<>();

  private final BlockData bordersMaterial = Material.BLACK_CONCRETE.createBlockData();

  public PlayerBoundsManager(Main plugin) {
    this.plugin = plugin;
  }

  public Map<Location, BlockData> getBorderBlocks(Player player) {

    var viewDistance = plugin.getPlayerViewDistance(player);
    int bordersRadius = ((viewDistance - 1) * 16);
    int verticalDistance  = (int) ((viewDistance * 16) * 0.75);

    Map<Location, BlockData> blocks = new HashMap<>();

    var center = player.getLocation();
    World world = center.getWorld();

    int radiusSquared = bordersRadius * bordersRadius;

    int centerX = center.getBlockX();
    int centerZ = center.getBlockZ();

    int minY = world.getMinHeight();
    int maxY = Math.min(center.getBlockY() + verticalDistance, world.getMaxHeight() - 1);

    for (int x = -bordersRadius; x <= bordersRadius; x++) {
      for (int z = -bordersRadius; z <= bordersRadius; z++) {

        int distanceSquaredXZ = (x * x) + (z * z);

        if (distanceSquaredXZ <= radiusSquared) {
          Location topCapLoc = new Location(world, centerX + x, maxY, centerZ + z);
          blocks.put(topCapLoc, bordersMaterial);
        }

        if (distanceSquaredXZ > (bordersRadius - 1) * (bordersRadius - 1) && distanceSquaredXZ <= radiusSquared) {
          for (int y = minY + 1; y < maxY; y++) {
            Location wallLoc = new Location(world, centerX + x, y, centerZ + z);

            if (!world.getBlockAt(wallLoc).getBlockData().isOccluding() || !world.getBlockAt(wallLoc).isSolid())
              blocks.put(wallLoc, bordersMaterial);
          }
        }
      }
    }

    return blocks;

  }

  public void sendFakeBlocks(Player player, Map<Location, BlockData> blocks) {

    plugin.getLogger().info("Sending %d block updates to the player %s".formatted(blocks.size(), player.getName()));
    FakeBlock.sendFakeBlocks(player, blocks);

  }

  public void onEnable(Player player) {

    var borderBlocks = getBorderBlocks(player);

    Map<Location, BlockData> originalBlocks = new HashMap<>();

    for (var location : borderBlocks.keySet())
      originalBlocks.put(location, location.getBlock().getBlockData());

    originalBlockData.put(player.getUniqueId(), originalBlocks);

    sendFakeBlocks(player, borderBlocks);

  }

  public void onDisable(Player player) {

    var originalBlocks = originalBlockData.get(player.getUniqueId());

    if (!originalBlocks.isEmpty())
      sendFakeBlocks(player, originalBlocks);

    originalBlockData.remove(player.getUniqueId());
    playerLastSector.remove(player.getUniqueId());

  }

  public void onUpdate(Player player) {

    if (!isEnabled(player)) return;

    var originalBlocks = originalBlockData.get(player.getUniqueId());
    var newBorders = getBorderBlocks(player);

    var diffAdd = newBorders.keySet().stream()
      .filter(loc -> !originalBlocks.containsKey(loc))
      .collect(Collectors.toMap(loc -> loc, loc -> bordersMaterial));

    var diffRemove = originalBlocks.entrySet().stream()
      .filter(entry -> !newBorders.containsKey(entry.getKey()))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    for (var location : diffAdd.keySet())
      originalBlocks.put(location, location.getBlock().getBlockData());

    for (var location : diffRemove.keySet())
      originalBlocks.remove(location);

    originalBlockData.put(player.getUniqueId(), originalBlocks);

    if (!diffAdd.isEmpty())
      sendFakeBlocks(player, diffAdd);

    if (!diffRemove.isEmpty())
      sendFakeBlocks(player, diffRemove);

  }

  public boolean isEnabled(Player player) {
    return originalBlockData.containsKey(player.getUniqueId());
  }

  @EventHandler(priority = EventPriority.LOW)
  public void onPLayerMove(PlayerMoveEvent event) {

    var player = event.getPlayer();
    var currentSector = new Vector3i(player.getChunk().getX(), player.getLocation().getBlockY() >> 4, player.getChunk().getZ());

    if (!playerLastSector.containsKey(player.getUniqueId()))
      playerLastSector.put(player.getUniqueId(), currentSector);

    if (!playerLastSector.get(player.getUniqueId()).equals(currentSector)) {
      playerLastSector.put(player.getUniqueId(), currentSector);
      this.onUpdate(player);
    }

  }

}
