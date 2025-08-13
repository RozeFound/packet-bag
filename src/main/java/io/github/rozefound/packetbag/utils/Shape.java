package io.github.rozefound.packetbag.utils;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

import java.util.HashMap;
import java.util.Map;

/**
 * A utility class to generate blueprints for geometric shapes.
 * Each method returns a Map of Locations to BlockData, perfect for sending
 * fake blocks to players without modifying the world.
 */
public final class Shape {

  public enum ShapeEnum {
    PLATFORM,
    CUBE,
    SPHERE,
    CYLINDER,
    DOME
  }

  public static Map<Location, BlockData> drawPlatform(Location center, int size, BlockData blockData) {
    Map<Location, BlockData> blocks = new HashMap<>();

    for (int x = -size; x <= size; x++) {
      for (int z = -size; z <= size; z++) {
        Location loc = center.clone().add(x, 0, z);
        blocks.put(loc, blockData);
      }
    }
    return blocks;
  }

  public static Map<Location, BlockData> drawCube(Location center, int size, BlockData blockData) {
    Map<Location, BlockData> blocks = new HashMap<>();
    World world = center.getWorld();

    int centerX = center.getBlockX();
    int centerY = center.getBlockY();
    int centerZ = center.getBlockZ();

    for (int x = -size; x <= size; x++) {
      for (int y = -size; y <= size; y++) {
        for (int z = -size; z <= size; z++) {
          if (Math.abs(x) == size || Math.abs(y) == size || Math.abs(z) == size) {
            Location loc = new Location(world, centerX + x, centerY + y, centerZ + z);
            blocks.put(loc, blockData);
          }
        }
      }
    }
    return blocks;
  }

  public static Map<Location, BlockData> drawSphere(Location center, int radius, BlockData blockData) {
    Map<Location, BlockData> blocks = new HashMap<>();
    int radiusSquared = radius * radius;

    for (int x = -radius; x <= radius; x++) {
      for (int y = -radius; y <= radius; y++) {
        for (int z = -radius; z <= radius; z++) {
          int distanceSquared = (x * x) + (y * y) + (z * z);

          if (distanceSquared > (radius - 1) * (radius - 1) && distanceSquared <= radiusSquared) {
            Location loc = center.clone().add(x, y, z);
            blocks.put(loc, blockData);
          }
        }
      }
    }
    return blocks;
  }

  public static Map<Location, BlockData> drawCylinder(Location center, int radius, int height, BlockData blockData) {
    Map<Location, BlockData> blocks = new HashMap<>();
    int radiusSquared = radius * radius;

    for (int y = 0; y < height; y++) {
      for (int x = -radius; x <= radius; x++) {
        for (int z = -radius; z <= radius; z++) {
          int distanceSquaredXZ = (x * x) + (z * z);

          if (y == 0 || y == height - 1) {
            if (distanceSquaredXZ <= radiusSquared) {
              blocks.put(center.clone().add(x, y, z), blockData);
            }
          }
          else {
            if (distanceSquaredXZ > (radius - 1) * (radius - 1) && distanceSquaredXZ <= radiusSquared) {
              blocks.put(center.clone().add(x, y, z), blockData);
            }
          }
        }
      }
    }
    return blocks;
  }

  public static Map<Location, BlockData> drawWorldCylinder(Location center, int radius, BlockData blockData) {
    Map<Location, BlockData> blocks = new HashMap<>();
    World world = center.getWorld();
    int radiusSquared = radius * radius;

    int centerX = center.getBlockX();
    int centerZ = center.getBlockZ();

    int minY = world.getMinHeight();
    int maxY = center.getBlockY() + 100;

    for (int x = -radius; x <= radius; x++) {
      for (int z = -radius; z <= radius; z++) {

        int distanceSquaredXZ = (x * x) + (z * z);

        if (distanceSquaredXZ <= radiusSquared) {
          Location topCapLoc = new Location(world, centerX + x, maxY, centerZ + z);
          Location bottomCapLoc = new Location(world, centerX + x, minY, centerZ + z);
          blocks.put(topCapLoc, blockData);
          blocks.put(bottomCapLoc, blockData);
        }

        if (distanceSquaredXZ > (radius - 1) * (radius - 1) && distanceSquaredXZ <= radiusSquared) {
          for (int y = minY + 1; y < maxY; y++) {
            Location wallLoc = new Location(world, centerX + x, y, centerZ + z);
            blocks.put(wallLoc, blockData);
          }
        }
      }
    }
    return blocks;
  }

  public static Map<Location, BlockData> drawDome(Location center, int radius, BlockData blockData) {
    Map<Location, BlockData> blocks = new HashMap<>();
    int radiusSquared = radius * radius;

    for (int x = -radius; x <= radius; x++) {
      for (int y = 0; y <= radius; y++) {
        for (int z = -radius; z <= radius; z++) {
          int distanceSquared = (x * x) + (y * y) + (z * z);

          if (distanceSquared > (radius - 1) * (radius - 1) && distanceSquared <= radiusSquared) {
            Location loc = center.clone().add(x, y, z);
            blocks.put(loc, blockData);
          }
        }
      }
    }
    return blocks;
  }
}
