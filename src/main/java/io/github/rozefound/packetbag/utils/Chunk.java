package io.github.rozefound.packetbag.utils;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Chunk {

  public static Vector2i getChunkLocation(Location location) {

    int chunkX = location.getBlockX() >> 4;
    int chunkZ = location.getBlockZ() >> 4;

    return new Vector2i(chunkX ,chunkZ);

  }

  public static Map<Vector2i, Set<Integer>> getChunkSectors(@NotNull List<Location> locations) {

    return locations.stream()
      .collect(Collectors.groupingBy(
        Chunk::getChunkLocation,
        Collectors.mapping(location -> location.getBlockY() >> 4, Collectors.toSet())
      ));

  }

}
