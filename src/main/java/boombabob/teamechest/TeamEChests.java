package boombabob.teamechest;


import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.scoreboard.Team;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Hashtable;

public class TeamEChests {
    public static Hashtable<Team, EChestInventory> eChestInventories = new Hashtable<>();
    // Dictionary of access locations so sounds can be played from the correct position
    public static Hashtable<PlayerEntity, BlockPos> accessPosDict = new Hashtable<>();
    public static Path saveFolderPath;
    public static String saveFileSuffix = ".sav";

    public static boolean open(PlayerEntity player, BlockPos blockPos) {
        if (!eChestInventories.isEmpty()) {
            accessPosDict.put(player, blockPos);
            // Pretty confusey, but it manages to make the inventory show up
            player.openHandledScreen(new SimpleNamedScreenHandlerFactory((sync_id, playerInventory, playerEntity) ->
                new GenericContainerScreenHandler(ScreenHandlerType.GENERIC_9X3, sync_id, playerInventory, eChestInventories.get(player.getScoreboardTeam()), 3), Text.of("Team Ender Chest"))
            );
            return true;
        }
        return false;
    }

    public static void newInventory(Team team) {
        eChestInventories.put(team, new EChestInventory());
    }

    public static void load() {
        // Get and/or make the directory where the ender chest inventories are saved
        String saveFolderString = Main.server.getSavePath(WorldSavePath.ROOT).resolve(Main.MOD_ID).toString();
        File saveFolderFile = new File(saveFolderString);
        if (!saveFolderFile.exists()) {
            saveFolderFile.mkdirs();
        }
        saveFolderPath = saveFolderFile.toPath();

        // Load the inventory of each team's ender chest
        Main.server.getScoreboard().getTeams().forEach((team) -> {
            Path savePath = saveFolderPath.resolve(team.getName().concat(saveFileSuffix));
            newInventory(team);
            try {
                NbtCompound nbt = NbtIo.read(savePath);
                if (nbt != null) {
                    // Transfer inventory from nbt to ender chest
                    Inventories.readNbt(nbt, eChestInventories.get(team).items, Main.server.getRegistryManager());
                }
            } catch (IOException ioException) {
                Main.LOGGER.error("Failure loading team ender chests.");
            }
        });
    }

    public static void save() {
        Main.server.getScoreboard().getTeams().forEach((team) -> {
            Path savePath = saveFolderPath.resolve(team.getName().concat(saveFileSuffix));
            NbtCompound nbt = new NbtCompound();
            // Transfer inventory from ender chest to nbt
            Inventories.writeNbt(nbt, eChestInventories.get(team).items, Main.server.getRegistryManager());
            try {
                NbtIo.write(nbt, savePath);
            } catch (IOException ioException) {
                Main.LOGGER.error("Error saving ender chest inventories.");
            }
        });
    }
}
