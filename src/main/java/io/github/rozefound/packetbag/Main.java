package io.github.rozefound.packetbag;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.rozefound.packetbag.listeners.PacketEventListener;
import io.github.rozefound.packetbag.listeners.PlayerChunkLoadListener;
import io.github.rozefound.packetbag.utils.LightPacket;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.minecraft.SharedConstants;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.joml.Vector2i;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

@DefaultQualifier(NonNull.class)
public final class Main extends JavaPlugin implements Listener {

  private PlayerBoundsManager playerBoundsManager;
  private PlayerChunkLoadListener playerChunkLoadListener;
  private TestCommand testCommand;

  private final Map<Player, Integer> playerViewDistance = new HashMap<>();

  @Override
  public void onLoad() {
    PacketEvents.getAPI().getEventManager().registerListener(
      new PacketEventListener(this), PacketListenerPriority.NORMAL);
  }
  @Override
  public void onEnable() {

    testCommand = new TestCommand(this);

    this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
      commands.registrar().register(testCommand.buildCommand());
    });


    this.getServer().getPluginManager().registerEvents(this, this);
    this.getLogger().info(SharedConstants.getCurrentVersion().getId());

    playerBoundsManager = new PlayerBoundsManager(this);
    playerChunkLoadListener = new PlayerChunkLoadListener();

    getServer().getPluginManager().registerEvents(playerBoundsManager, this);
    getServer().getPluginManager().registerEvents(playerChunkLoadListener, this);

    startLightUpdateTask();

  }

  public PlayerBoundsManager getPlayerBoundsManager() {
    return playerBoundsManager;
  }

  public void startLightUpdateTask() {

    new BukkitRunnable() {
      @Override
      public void run() {

        for (var player : getServer().getOnlinePlayers()) {

          var chunks = playerChunkLoadListener.getPlayerLoadedChunks(player);

          Map<Vector2i, Set<Integer>> chunkSectorsMap = new HashMap<>();

          if (chunks.isEmpty()) return;

          for (var chunk : chunks) {

            Set<Integer> sectors = new HashSet<>();
            IntStream.range(0, 20).forEach(sectors::add);
            chunkSectorsMap.put(new Vector2i(chunk.getX(), chunk.getZ()), sectors);

          }

          LightPacket.sendDarkLight(player, chunkSectorsMap, true, false);

        }

      }
    }.runTaskTimer(this, 1L, 20L);
  }

  public int getPlayerViewDistance(Player player) {
    return playerViewDistance.getOrDefault(player, getServer().getViewDistance());
  }

  public void setPlayerViewDistance(Player player, int viewDistance) {

    var oldviewDistance = getPlayerViewDistance(player);
    playerViewDistance.put(player, Math.min(viewDistance, getServer().getViewDistance()));

    if (oldviewDistance != viewDistance) {
      playerBoundsManager.onUpdate(player);
    }

  }
}
