package boombabob.teamechest;


import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.scoreboard.Team;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Hashtable;

public class TeamEChests {
    public static Hashtable<Team, EChestInventory> eChestInventories = new Hashtable<>();
    public static EChestInventory defaultInventory;
    // Dictionary of access locations so sounds can be played from the correct position
    public static Hashtable<PlayerEntity, BlockPos> accessPosDict = new Hashtable<>();
    public static Path saveFolderPath;
    public static String saveFileExtension = ".sav";
    public static String specialFolder = "special";
    public static ScreenHandlerType<GenericContainerScreenHandler> screenSize;

    public static void open(PlayerEntity player, BlockPos blockPos) {
        accessPosDict.put(player, blockPos);
        Team team = player.getScoreboardTeam();
        Inventory inventory;
        String inventoryName;
        if (Main.CONFIG.enderChestForTeamless & team == null) {
            inventoryName = "Global Ender Chest";
            inventory = defaultInventory;
        } else {
            inventoryName = "Team Ender Chest";
            inventory = eChestInventories.get(team);
        }
        // Pretty confusey, but it manages to make the inventory show up
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, playerInventory, playerEntity) ->
            new GenericContainerScreenHandler(screenSize, syncId, playerInventory, inventory, Main.CONFIG.enderChestRows), Text.of(inventoryName))
        );
    }

    public static void newInventory(Team team) {
        if (team == null) {
            defaultInventory = new EChestInventory();
        } else {
            eChestInventories.put(team, new EChestInventory());
        }
    }

    public static void loadChest(@Nullable Team team) {
        newInventory(team);
        DefaultedList<ItemStack> items;
        String fileName;
        // Use default name and items for teamless chest (null team)
        if (team == null) {
            fileName = specialFolder.concat("\\default");
            items = defaultInventory.items;
        } else {
            fileName = team.getName();
            items = eChestInventories.get(team).items;
        }
        Path savePath = saveFolderPath.resolve(fileName.concat(saveFileExtension));
        try {
            NbtCompound nbt = NbtIo.read(savePath);
            if (nbt != null) {
                // Transfer inventory from nbt to ender chest
                Inventories.readNbt(nbt, items, Main.server.getRegistryManager());
            }
        } catch (IOException ioException) {
            Main.LOGGER.error("Failure loading team ender chests.");
        }
    }

    public static void saveChest(@Nullable Team team) {
        DefaultedList<ItemStack> items;
        String fileName;
        // Use default name and items for teamless chest (null team)
        if (team == null) {
            fileName = specialFolder.concat("\\default");
            items = defaultInventory.items;
        } else {
            fileName = team.getName();
            items = eChestInventories.get(team).items;
        }
        Path savePath = saveFolderPath.resolve(fileName.concat(saveFileExtension));
        NbtCompound nbt = new NbtCompound();
        // Transfer inventory from ender chest to nbt
        Inventories.writeNbt(nbt, items, Main.server.getRegistryManager());
        try {
            NbtIo.write(nbt, savePath);
        } catch (IOException ioException) {
            Main.LOGGER.error("Error saving ender chest inventories.");
        }
    }

    public static void load() {
        // Make container size correct
        switch (Main.CONFIG.enderChestRows) {
            case 1:
                screenSize = ScreenHandlerType.GENERIC_9X1;
                break;
            case 2:
                screenSize = ScreenHandlerType.GENERIC_9X2;
                break;
            case 3:
                screenSize = ScreenHandlerType.GENERIC_9X3;
                break;
            case 4:
                screenSize = ScreenHandlerType.GENERIC_9X4;
                break;
            case 5:
                screenSize = ScreenHandlerType.GENERIC_9X5;
                break;
            case 6:
                screenSize = ScreenHandlerType.GENERIC_9X6;
                break;
            default:
                screenSize = ScreenHandlerType.GENERIC_9X3;
                Main.CONFIG.enderChestRows = 3;
                break;
        }

        // Get and/or make the directory where the ender chest inventories are saved
        String fullSaveFolderString = Main.server.getSavePath(WorldSavePath.ROOT).resolve(Main.MOD_ID).resolve(specialFolder).toString();
        File fullSaveFolderFile = new File(fullSaveFolderString);
        if (!fullSaveFolderFile.exists()) {
            fullSaveFolderFile.mkdirs();
        }
        saveFolderPath = fullSaveFolderFile.toPath().getParent();

        // Load the inventory of each team's ender chest
        Main.server.getScoreboard().getTeams().forEach(TeamEChests::loadChest);
        if (Main.CONFIG.enderChestForTeamless) {
            loadChest(null);
        }
    }

    public static void save() {
        Main.server.getScoreboard().getTeams().forEach(TeamEChests::saveChest);
        if (Main.CONFIG.enderChestForTeamless) {
            saveChest(null);
        }
    }
}
