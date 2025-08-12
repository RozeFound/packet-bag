package io.github.rozefound.packetbag.utils;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange.EncodedBlock;

public class FakeBlock {

  public static void sendFakeBlock(Player player, Location location, BlockData blockData) {

    var blockPosition = SpigotConversionUtil.fromBukkitLocation(location).getPosition().toVector3i();
    var blockId = SpigotConversionUtil.fromBukkitBlockData(blockData).getGlobalId();

    var packet = new WrapperPlayServerBlockChange(blockPosition, blockId);
    PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);

  }

  public static void sendFakeBlocks(Player player, Map<Location, BlockData> blockChanges) {

    if (blockChanges.isEmpty()) return;

    // Group block changes by the chunk they belong to
    Map<Chunk, Map<Location, BlockData>> perChunk = blockChanges.entrySet().stream()
      .collect(Collectors.groupingBy(
        entry -> entry.getKey().getChunk(),
        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)
      ));

    for (var entry : perChunk.entrySet()) {

      Chunk chunk = entry.getKey();
      Map<Location, BlockData> chunkChanges = entry.getValue();

      // Further group by chunk section (Y-level)
      Map<Integer, Map<Location, BlockData>> perSection = chunkChanges.entrySet().stream()
        .collect(Collectors.groupingBy(
          locEntry -> locEntry.getKey().getBlockY() >> 4, // Y-level / 16
          Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)
        ));


      for (var sectionEntry : perSection.entrySet()) {

        List<EncodedBlock> encodedBlocks = new ArrayList<>();

        for (var blockEntry : sectionEntry.getValue().entrySet()) {

          Location loc = blockEntry.getKey();
          var blockId = SpigotConversionUtil.fromBukkitBlockData(blockEntry.getValue()).getGlobalId();

          var encodedBlock = new EncodedBlock(blockId, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
          encodedBlocks.add(encodedBlock);

        }

        var sectionPos = new Vector3i(chunk.getX(), sectionEntry.getKey(), chunk.getZ());
        var blocksData = encodedBlocks.toArray(EncodedBlock[]::new);

        var packet = new WrapperPlayServerMultiBlockChange(sectionPos, true, blocksData);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);

      }

    }

  }

}
