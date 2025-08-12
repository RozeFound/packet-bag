package io.github.rozefound.packetbag;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.rozefound.packetbag.utils.FakeBlock;
import io.github.rozefound.packetbag.utils.Light;
import io.github.rozefound.packetbag.utils.Shape;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver;
import io.papermc.paper.math.BlockPosition;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;

import java.util.Map;

public class TestCommand {

  Main plugin;

  public TestCommand(Main plugin) {

    this.plugin = plugin;

  }

  public LiteralCommandNode<CommandSourceStack> buildCommand() {

    var getLightValueCommand = Commands.literal("getLightValue")
      .then(Commands.argument("pos", ArgumentTypes.blockPosition())
        .executes(TestCommand::getLightValue));

    var spawnBlockCommand = Commands.literal("spawnFakeBlock")
      .then(Commands.argument("pos",  ArgumentTypes.blockPosition())
        .then(Commands.argument("block", ArgumentTypes.blockState())
          .executes(TestCommand::spawnBlock)));

    var spawnPlatformCommand = Commands.literal("spawnFakePlatform")
      .then(Commands.argument("position",  ArgumentTypes.blockPosition())
        .then(Commands.argument("size", IntegerArgumentType.integer(1, 50))
          .then(Commands.argument("block", ArgumentTypes.blockState())
            .executes(ctx -> spawnShape(ctx, Shape.ShapeEnum.PLATFORM)))));

    var spawnCubeCommand = Commands.literal("spawnFakeCube")
      .then(Commands.argument("position",  ArgumentTypes.blockPosition())
        .then(Commands.argument("size", IntegerArgumentType.integer(1, 50))
          .then(Commands.argument("block", ArgumentTypes.blockState())
            .executes(ctx -> spawnShape(ctx, Shape.ShapeEnum.CUBE)))));

    var spawnSphereCommand = Commands.literal("spawnFakeSphere")
      .then(Commands.argument("position",  ArgumentTypes.blockPosition())
        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 50))
          .then(Commands.argument("block", ArgumentTypes.blockState())
            .executes(ctx -> spawnShape(ctx, Shape.ShapeEnum.SPHERE)))));

    var spawnCylinderCommand = Commands.literal("spawnFakeCylinder")
      .then(Commands.argument("position",  ArgumentTypes.blockPosition())
        .then(Commands.argument("size", IntegerArgumentType.integer(1, 50))
          .then(Commands.argument("radius", IntegerArgumentType.integer(1, 50))
            .then(Commands.argument("block", ArgumentTypes.blockState())
              .executes(ctx -> spawnShape(ctx, Shape.ShapeEnum.CYLINDER))))));

    var spawnDomeCommand = Commands.literal("spawnFakeDome")
      .then(Commands.argument("position",  ArgumentTypes.blockPosition())
        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 50))
          .then(Commands.argument("block", ArgumentTypes.blockState())
            .executes(ctx -> spawnShape(ctx, Shape.ShapeEnum.DOME)))));

    var toggleBordersCommand = Commands.literal("toggleBorders")
      .executes(ctx -> {

        if (ctx.getSource().getExecutor() instanceof Player player) {

          if (!plugin.getPlayerBoundsManager().isEnabled(player))
            plugin.getPlayerBoundsManager().onEnable(player);
          else plugin.getPlayerBoundsManager().onDisable(player);

        }

        return Command.SINGLE_SUCCESS;

      });

    var root = Commands.literal("testplugin")
      .then(getLightValueCommand)
      .then(spawnBlockCommand)
      .then(spawnPlatformCommand)
      .then(spawnCubeCommand)
      .then(spawnSphereCommand)
      .then(spawnCylinderCommand)
      .then(spawnDomeCommand)
      .then(toggleBordersCommand);

    return root.build();

  }

  public static int getLightValue(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {

    final BlockPositionResolver resolver = ctx.getArgument("pos", BlockPositionResolver.class);
    final BlockPosition blockPosition = resolver.resolve(ctx.getSource());

    World bukkitWorld = ctx.getSource().getLocation().getWorld();
    ServerLevel serverLevel = ((CraftWorld) bukkitWorld).getHandle();

    var lightValue = Light.getLightValueAtPosition(blockPosition, serverLevel);

    ctx.getSource().getSender().sendMessage("Light level at specified position is: %d".formatted(lightValue));

    return Command.SINGLE_SUCCESS;

  }

  public static int spawnBlock(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {

    final BlockPositionResolver resolver = ctx.getArgument("pos", BlockPositionResolver.class);
    final BlockPosition blockPosition = resolver.resolve(ctx.getSource());

    var blockState = ctx.getArgument("block", BlockState.class);

    var executor = ctx.getSource().getExecutor();

    FakeBlock.sendFakeBlock((Player)executor, blockPosition.toLocation(executor.getWorld()), blockState.getBlockData());

    return Command.SINGLE_SUCCESS;

  }

  public int spawnShape(CommandContext<CommandSourceStack> ctx, Shape.ShapeEnum shapeEnum) throws CommandSyntaxException {

    plugin.getLogger().info("Calling spawnShape with %s".formatted(shapeEnum.name()));

    final BlockPositionResolver resolver = ctx.getArgument("position", BlockPositionResolver.class);
    final BlockPosition blockPosition = resolver.resolve(ctx.getSource());

    var size = 0;
    var blockState = ctx.getArgument("block", BlockState.class);
    var radius = 0;

    if (shapeEnum == Shape.ShapeEnum.SPHERE || shapeEnum == Shape.ShapeEnum.CYLINDER || shapeEnum == Shape.ShapeEnum.DOME)
      radius = ctx.getArgument("radius", Integer.class);

    if (shapeEnum == Shape.ShapeEnum.PLATFORM || shapeEnum == Shape.ShapeEnum.CYLINDER || shapeEnum == Shape.ShapeEnum.CUBE)
      size = ctx.getArgument("size", Integer.class);

    var executor = ctx.getSource().getExecutor();
    var location = blockPosition.toLocation(executor.getWorld());

    Map<Location, BlockData> blocks;

    switch (shapeEnum) {
      case PLATFORM -> blocks = Shape.drawPlatform(location, size, blockState.getBlockData());
      case CUBE -> blocks = Shape.drawCube(location, size, blockState.getBlockData());
      case SPHERE -> blocks = Shape.drawSphere(location, radius, blockState.getBlockData());
      case CYLINDER -> blocks = Shape.drawCylinder(location, size, radius, blockState.getBlockData());
      case DOME -> blocks = Shape.drawDome(location, radius, blockState.getBlockData());
      case null, default -> throw new RuntimeException("Get the fuck out");
    }

    FakeBlock.sendFakeBlocks((Player)executor, blocks);

    return Command.SINGLE_SUCCESS;

  }



}
