package boombabob.teamechest;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main implements ModInitializer {
	public static final String MOD_ID = "teamechest";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static MinecraftServer server = null;

	@Override
	public void onInitialize() {
		// Get server instance
		ServerLifecycleEvents.SERVER_STARTING.register(mcServer -> server = mcServer);
		// Load team ender chests only once the server, and scoreboard, has loaded
		ServerLifecycleEvents.SERVER_STARTED.register(mcServer -> TeamEChests.load());
		// Save team ender chest inventory data
		ServerLifecycleEvents.SERVER_STOPPING.register(a -> TeamEChests.save());
		// Check for the ender chest being accessed whenever a block is used
		UseBlockCallback.EVENT.register((player, world, hand, blockHitResult) -> {
			BlockPos blockPos = blockHitResult.getBlockPos();
			if (world.getBlockEntity(blockPos) instanceof EnderChestBlockEntity && TeamEChests.open(player, blockPos)) {
				return ActionResult.SUCCESS;
			} else {
				return ActionResult.PASS;
			}
		});
	}
}