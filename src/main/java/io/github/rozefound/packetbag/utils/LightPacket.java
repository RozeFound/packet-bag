package io.github.rozefound.packetbag.utils;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.world.chunk.LightData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateLight;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.joml.Vector2i;

import java.util.*;
import java.util.stream.Collectors;

public class LightPacket {

  private static final byte[] EMPTY_LIGHT_SECTION = new byte[2048];

  public static void sendDarkLight(Player player, Location location, boolean skyUpdates, boolean blockUpdates) {

    List<Location> locations = new ArrayList<>();
    locations.add(location);

    sendDarkLight(player, Chunk.getChunkSectors(locations), skyUpdates, blockUpdates);

  }

  public static void sendDarkLight(Player player, Map<Vector2i, Set<Integer>> sectorMap, boolean skyUpdates, boolean blockUpdates) {

    for (var entry : sectorMap.entrySet()) {

      var chunkCoords = entry.getKey();
      var sectors = entry.getValue();

      var lightSectors = sectors.stream().map(sector -> sector - (player.getWorld().getMinHeight() >> 4) + 1).collect(Collectors.toSet());

      BitSet skyLightMask = new BitSet();
      BitSet blockLightMask = new BitSet();

      List<byte[]> skyLightArray = new ArrayList<>();
      List<byte[]> blockLightArray = new ArrayList<>();

      for (var sector : lightSectors) {

        if (skyUpdates) {
          skyLightMask.set(sector);
          skyLightArray.add(EMPTY_LIGHT_SECTION);
        }

        if (blockUpdates) {
          blockLightMask.set(sector);
          blockLightArray.add(EMPTY_LIGHT_SECTION);
        }

      }

      var lightData = new LightData(
        true, // deprecated since 1.19.2
        blockLightMask, skyLightMask,
        new BitSet(), new BitSet(),
        skyLightArray.size(), blockLightArray.size(),
        skyLightArray.toArray(new byte[0][]),
        blockLightArray.toArray(new byte[0][])
      );

      var packet = new WrapperPlayServerUpdateLight(chunkCoords.x(), chunkCoords.y(), lightData);
      PacketEvents.getAPI().getPlayerManager().sendPacketSilently(player, packet);

    }

  }

}
