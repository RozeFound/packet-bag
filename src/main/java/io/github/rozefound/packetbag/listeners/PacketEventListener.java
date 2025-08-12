package io.github.rozefound.packetbag.listeners;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.world.chunk.LightData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChunkData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMultiBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateLight;
import io.github.rozefound.packetbag.utils.Chunk;
import io.github.rozefound.packetbag.utils.LightPacket;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PacketEventListener  implements PacketListener {

  @Override
  public void onPacketSend(@NotNull PacketSendEvent event) {

    switch (event.getPacketType()) {
      case PacketType.Play.Server.BLOCK_CHANGE -> onBlockChangeEvent(event);
      case PacketType.Play.Server.UPDATE_LIGHT, PacketType.Play.Server.CHUNK_DATA -> onLightUpdateEvent(event);
      case PacketType.Play.Server.MULTI_BLOCK_CHANGE -> onMultiBlockChangeEvent(event);
      default -> {}
    }

  }

  @Override
  public void onPacketReceive(@NotNull PacketReceiveEvent event) {



  }

  public void onLightUpdateEvent(@NotNull PacketSendEvent event) {

    LightData lightData = null;

    if (event.getPacketType() == PacketType.Play.Server.UPDATE_LIGHT)
      lightData = new WrapperPlayServerUpdateLight(event).getLightData();
    else if (event.getPacketType() == PacketType.Play.Server.CHUNK_DATA)
      lightData = new WrapperPlayServerChunkData(event).getLightData();

    if (lightData == null) return;

    var skyLightArray = lightData.getSkyLightArray();

    for (var sector : skyLightArray)
      Arrays.fill(sector, (byte) 0);

    lightData.setSkyLightArray(skyLightArray);

    event.markForReEncode(true);

  }

  public void onBlockChangeEvent(PacketSendEvent event) {

    var packet = new WrapperPlayServerBlockChange(event);

    var player = (Player)event.getPlayer();

    var blockPosition = packet.getBlockPosition();
    var location = new Location(player.getWorld(), blockPosition.getX(), blockPosition.getY(), blockPosition.getZ());
    LightPacket.sendDarkLight(player, location, true, false);

  }

  public void onMultiBlockChangeEvent(PacketSendEvent event) {

    var packet = new WrapperPlayServerMultiBlockChange(event);

    var player = (Player)event.getPlayer();

    List<Location> locations = new ArrayList<>();

    for (var encodedBlock : packet.getBlocks()) {

      var location = new Location(player.getWorld(), encodedBlock.getX(), encodedBlock.getY(), encodedBlock.getZ());
      locations.add(location);

    }

    LightPacket.sendDarkLight(player, Chunk.getChunkSectors(locations), true, false);

  }

}
