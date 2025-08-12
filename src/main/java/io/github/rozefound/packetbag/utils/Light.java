package io.github.rozefound.packetbag.utils;

import io.papermc.paper.math.BlockPosition;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class Light {

  /**
   * Calculates the index within the 4096-value conceptual array for a given coordinate.
   * Minecraft typically orders by Y, then Z, then X.
   * @param x Block X within the section (0-15)
   * @param y Block Y within the section (0-15)
   * @param z Block Z within the section (0-15)
   * @return The index from 0 to 4095.
   */
  public static int getLightArrayIndex(int x, int y, int z) {
    return y * 256 + z * 16 + x;
  }

  /**
   * Extracts the x, y, z coordinates from an index within the 4096-value conceptual array.
   * @param index The index from 0 to 4095
   * @return An array containing [x, y, z] where each value is 0-15
   */
  public static int[] getCoordinatesFromIndex(int index) {
    int y = index / 256;
    int remainder = index % 256;
    int z = remainder / 16;
    int x = remainder % 16;

    return new int[]{x, y, z};
  }

  /**
   * Gets the light value (0-15) for a specific block from the raw light data array.
   */
  public static int getLightValue(byte[] data, int x, int y, int z) {
    int index = getLightArrayIndex(x, y, z);
    int byteIndex = index / 2; // Each byte contains two light values

    // Ensure we don't go out of bounds
    if (byteIndex >= data.length) {
      return 0; // Or throw an exception
    }

    byte value = data[byteIndex];
    boolean isHighNibble = (index % 2 != 0); // Is it the second value in the byte?

    if (isHighNibble) {
      return (value >> 4) & 0x0F; // Get the upper 4 bits
    } else {
      return value & 0x0F;        // Get the lower 4 bits
    }
  }

  /**
   * Sets the light value (0-15) for a specific block in the raw light data array.
   * This performs an in-place modification of the byte array.
   */
  public static void setLightValue(byte[] data, int x, int y, int z, int lightLevel) {
    int index = getLightArrayIndex(x, y, z);
    int byteIndex = index / 2;

    if (byteIndex >= data.length) {
      return; // Or throw an exception
    }

    // Clamp light level to valid range
    lightLevel = Math.max(0, Math.min(15, lightLevel));

    byte oldValue = data[byteIndex];
    boolean isHighNibble = (index % 2 != 0);

    if (isHighNibble) {
      // Clear the high nibble, then OR the new value into place
      data[byteIndex] = (byte) ((oldValue & 0x0F) | (lightLevel << 4));
    } else {
      // Clear the low nibble, then OR the new value into place
      data[byteIndex] = (byte) ((oldValue & 0xF0) | lightLevel);
    }
  }

  public static int getLightValueAtPosition(final BlockPosition position, ServerLevel level) {

    var lightEngine = level.getLightEngine().starlight$getLightEngine();
    BlockPos blockPosition = new BlockPos(position.blockX(), position.blockY(), position.blockZ());
    return lightEngine.getBlockLightValue(blockPosition, level.getChunk(blockPosition));

  }

}
