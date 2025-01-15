package boombabob.teamechest;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
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
	public static Config CONFIG;


	@Override
	public void onInitialize() {
		// Load config.
		Config.HANDLER.load();
		CONFIG = Config.HANDLER.instance();

		// only do this if a group ender chest is enabled.
		if (CONFIG.teamEnderChest || CONFIG.teamlessEnderChest || CONFIG.globalEnderChest || CONFIG.personalEnderChest) {
			// Get server instance.
			ServerLifecycleEvents.SERVER_STARTING.register(mcServer -> server = mcServer);
			// Load team ender chests only once the server, and scoreboard, has loaded.
			ServerLifecycleEvents.SERVER_STARTED.register(mcServer -> TeamEChests.load());
			// Save team ender chest inventory data.
			ServerLifecycleEvents.SERVER_STOPPING.register(mcServer -> TeamEChests.save());
			// Register command.
			CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> new InteractMethodCommand().register(dispatcher));
			// Check for the ender chest being accessed whenever a block is used.
			UseBlockCallback.EVENT.register((player, world, hand, blockHitResult) -> {
				BlockPos blockPos = blockHitResult.getBlockPos();
				if (world.getBlockEntity(blockPos) instanceof EnderChestBlockEntity) {
					return TeamEChests.open(player, blockPos);
				} else {
					return ActionResult.PASS;
				}
			});
		// Completely disable ender chests if chosen to do so in config.
		// This is in a separate if branch to prevent the loading of unnecessary things.
		} else {
			UseBlockCallback.EVENT.register((player, world, hand, blockHitResult) -> {
				if (world.getBlockEntity(blockHitResult.getBlockPos()) instanceof EnderChestBlockEntity) {
					return ActionResult.CONSUME;
				} else {
					return ActionResult.PASS;
				}
			});
		}
	}
}