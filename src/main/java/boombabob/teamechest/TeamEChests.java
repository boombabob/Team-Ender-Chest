package boombabob.teamechest;


import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.scoreboard.Team;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class TeamEChests {
    // List of ender chest inventories for each team.
    public static Hashtable<Team, EChestInventory> eChestInventories = new Hashtable<>();
    // Other ender chest inventories.
    public static EChestInventory teamlessInventory;
    public static EChestInventory globalInventory;
    // Saved hash table of interact methods for each ender chest type per player.
    public static Hashtable<UUID, Hashtable<EChestType, InteractMethod>> playersInteractMethods = new Hashtable<>();
    // Minecraft row width, only as a constant up here to follow best practice.
    public static final int ROW_WIDTH = 9;
    // Paths and strings of sub paths to be used in saving nbt data such as ender chest inventories
    public static Path saveFolderPath;
    public static final String SAVE_FILE_EXTENSION = ".sav";
    public static final String SPECIAL_FOLDER = "special";
    public static final String TEAMLESS_ECHEST_SUB_PATH = SPECIAL_FOLDER.concat("\\teamless").concat(SAVE_FILE_EXTENSION);
    public static final String GLOBAL_ECHEST_SUB_PATH = SPECIAL_FOLDER.concat("\\global").concat(SAVE_FILE_EXTENSION);
    public static final String PLAYER_INTERACT_METHODS_FOLDER = SPECIAL_FOLDER.concat("\\playerInteractMethods");

    public enum InteractMethod {
        ALWAYS("always"),
        NEVER("never"),
        ON_SNEAK("onSneak"),
        ON_UNSNEAK("onUnSneak");

        final String displayName;

        InteractMethod(String displayName) {
            this.displayName = displayName;
        }
    }

    public enum EChestType {
        TEAM(Main.CONFIG.teamEnderChestRows, "Team"),
        TEAMLESS(Main.CONFIG.teamlessEnderChestRows, "Teamless"),
        GLOBAL(Main.CONFIG.globalEnderChestRows, "Global"),
        PERSONAL(3, "Personal");

        public final int rows;
        public final String displayName;

        EChestType(int rows, String displayName) {
            this.rows = rows;
            this.displayName = displayName;
        }
    }

    public static boolean isEchestInvalid(EChestType eChestType) {
        switch (eChestType) {
            case TeamEChests.EChestType.PERSONAL:
                if (!Main.CONFIG.personalEnderChest) {
                    return true;
                }
                break;
            case TeamEChests.EChestType.TEAM:
                if (!Main.CONFIG.teamEnderChest) {
                    return true;
                }
                break;
            case TeamEChests.EChestType.TEAMLESS:
                if (!Main.CONFIG.teamlessEnderChest) {
                    return true;
                }
                break;
            case TeamEChests.EChestType.GLOBAL:
                if (!Main.CONFIG.globalEnderChest) {
                    return true;
                }
                break;
        }
        return false;
    }

    public static Hashtable<EChestType, InteractMethod> getPlayerInteractMethods(PlayerEntity player) {
        UUID uuid = player.getUuid();
        if (playersInteractMethods.containsKey(uuid)) {
            return playersInteractMethods.get(uuid);
        } else {
            // Lots of logic to figure out which interact methods should be enabled for what by default.
            Hashtable<EChestType, InteractMethod> playerInteractMethods = new Hashtable<>();
            if (Main.CONFIG.teamEnderChest || Main.CONFIG.teamlessEnderChest) {
                playerInteractMethods.put(EChestType.TEAM, InteractMethod.ON_SNEAK);
                playerInteractMethods.put(EChestType.TEAMLESS, InteractMethod.ON_SNEAK);
                if (!Main.CONFIG.personalEnderChest & Main.CONFIG.globalEnderChest) {
                    playerInteractMethods.put(EChestType.GLOBAL, InteractMethod.ON_UNSNEAK);
                }
            } else {
                playerInteractMethods.put(EChestType.TEAM, InteractMethod.NEVER);
                playerInteractMethods.put(EChestType.TEAMLESS, InteractMethod.NEVER);
                if (Main.CONFIG.globalEnderChest) {
                    playerInteractMethods.put(EChestType.GLOBAL, InteractMethod.ON_SNEAK);
                }
            }
            if (Main.CONFIG.personalEnderChest) {
                playerInteractMethods.put(EChestType.PERSONAL, InteractMethod.ON_UNSNEAK);
            } else {
                playerInteractMethods.put(EChestType.PERSONAL, InteractMethod.NEVER);
                if (playerInteractMethods.get(EChestType.GLOBAL) == InteractMethod.ON_SNEAK) {
                    playerInteractMethods.put(EChestType.GLOBAL, InteractMethod.ALWAYS);
                } else {
                    playerInteractMethods.put(EChestType.GLOBAL, InteractMethod.ON_UNSNEAK);
                }
            }
            playersInteractMethods.put(uuid, playerInteractMethods);
            return playerInteractMethods;
        }
    }
    public static void changePlayerInteractMethod(@NotNull PlayerEntity player, @NotNull InteractMethod interactMethod, @NotNull EChestType changingEChestType) {
        Hashtable<EChestType, InteractMethod> playerInteractMethods = getPlayerInteractMethods(player);
        playerInteractMethods.forEach((eChestType, currentInteractMethod) -> {
            // Prevents conflicts such as two different methods opening on crouch.
            if (changingEChestType != eChestType) {
                if (interactMethod == currentInteractMethod || interactMethod == InteractMethod.ALWAYS) {
                    playerInteractMethods.put(eChestType, InteractMethod.NEVER);
                } else if (interactMethod == InteractMethod.ON_UNSNEAK & currentInteractMethod == InteractMethod.ALWAYS) {
                    playerInteractMethods.put(eChestType, InteractMethod.ON_SNEAK);
                } else if (interactMethod == InteractMethod.ON_SNEAK & currentInteractMethod == InteractMethod.ALWAYS) {
                    playerInteractMethods.put(eChestType, InteractMethod.ON_UNSNEAK);
                }
            }
        });
        playerInteractMethods.put(changingEChestType, interactMethod);
        playersInteractMethods.put(player.getUuid(), playerInteractMethods);
    }

    private static @Nullable EChestType getOpenType(@NotNull PlayerEntity player) {
        Hashtable<EChestType, InteractMethod> playerInteractMethods = getPlayerInteractMethods(player);
        // Figures out which ender chest type should open based on the players interaction method with each.
        for (EChestType eChestType : playerInteractMethods.keySet()) {
            if (isEchestInvalid(eChestType)) {
                continue;
            }
            InteractMethod playerInteractMethod = playerInteractMethods.get(eChestType);
            if (playerInteractMethod == InteractMethod.ALWAYS
                    || (playerInteractMethod == InteractMethod.ON_SNEAK & player.isSneaking())
                    || (playerInteractMethod == InteractMethod.ON_UNSNEAK & !player.isSneaking())) {
                return eChestType;
            }
        }
        return null;
    }

    public static @NotNull ActionResult open(@NotNull PlayerEntity player, @NotNull BlockPos blockPos) {
        EChestType eChestType = getOpenType(player);
        // if null, no ender chest type should open and so the action is consumed.
        if (eChestType == null) {
            return ActionResult.CONSUME;
        }
        EChestInventory inventory;
        // Default inventory name for all inventories except the team inventory.
        Text inventoryName = Text.literal(eChestType.displayName.concat(" Ender Chest"));
        int rows = eChestType.rows;
        // Getting the correct inventory name, team and inventory for each ender chest type.
        switch (eChestType) {
            case EChestType.PERSONAL:
                return ActionResult.PASS;
            case EChestType.TEAM:
                Team team = player.getScoreboardTeam();
                inventoryName = Text.literal("Team ").append(team.getDisplayName()).append(Text.literal("'s Ender Chest"));
                inventory = eChestInventories.get(team);
                break;
            case EChestType.TEAMLESS:
                inventory = teamlessInventory;
                break;
            case EChestType.GLOBAL:

                inventory = globalInventory;
                break;
            default:
                return ActionResult.FAIL;
        }
        // put the player access position in so the noise can be played from there.
        inventory.playerAccessPositions.put(player, blockPos);
        ScreenHandlerType<GenericContainerScreenHandler> screenSize;
        // get the correct screen size.
        switch (rows) {
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
                Main.LOGGER.error("All Ender Chest row amounts must be from 1 to 6.");
                return ActionResult.FAIL;
        }
        // Pretty confusey, but it manages to make the inventory show up.
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, playerInventory, playerEntity) ->
            new GenericContainerScreenHandler(screenSize, syncId, playerInventory, inventory, rows), inventoryName)
        );
        return ActionResult.SUCCESS;
    }

    public static void newInventory(@NotNull EChestType eChestType, @Nullable Team team) {
        EChestInventory eChestInventory = new EChestInventory(eChestType);
        // Put the inventory in the right variable based on the ender chest type.
        switch (eChestType) {
            case EChestType.TEAM:
                if (team != null) {
                    eChestInventories.put(team, eChestInventory);
                }
                break;
            case EChestType.TEAMLESS:
                teamlessInventory = eChestInventory;
                break;
            case EChestType.GLOBAL:
                globalInventory = eChestInventory;
                break;
        }
    }

    private static void loadChest(@NotNull EChestType eChestType, @Nullable Team team) {
        // Creates the inventory.
        newInventory(eChestType, team);
        DefaultedList<ItemStack> items;
        Path savePath;
        // Gets the correct save path and items for the ender chest type.
        switch (eChestType) {
            case EChestType.TEAM:
                if (team == null) {
                    return;
                } else {
                    savePath = saveFolderPath.resolve(team.getName().concat(SAVE_FILE_EXTENSION));
                    items = eChestInventories.get(team).items;
                }
                break;
            case EChestType.TEAMLESS:
                savePath = saveFolderPath.resolve(TEAMLESS_ECHEST_SUB_PATH);
                items = teamlessInventory.items;
                break;
            case EChestType.GLOBAL:
                savePath = saveFolderPath.resolve(GLOBAL_ECHEST_SUB_PATH);
                items = globalInventory.items;
                break;
            default:
                return;
        }

        try {
            // Get nbt from file.
            NbtCompound nbt = NbtIo.read(savePath);
            if (nbt != null) {
                // Transfer inventory from nbt to items.
                Inventories.readNbt(nbt, items, Main.server.getRegistryManager());
            }
        } catch (IOException ioException) {
            Main.LOGGER.error("Failure loading ender chest inventories.");
        }
    }

    private static void saveChest(@NotNull EChestType eChestType, @Nullable Team team) {
        DefaultedList<ItemStack> items;
        Path savePath;
        // Get correct save path and items based on the ender chest type.
        switch (eChestType) {
            case EChestType.TEAM:
                if (team == null) {
                    return;
                } else {
                    savePath = saveFolderPath.resolve(team.getName().concat(SAVE_FILE_EXTENSION));
                    items = eChestInventories.get(team).items;
                }
                break;
            case EChestType.TEAMLESS:
                savePath = saveFolderPath.resolve(TEAMLESS_ECHEST_SUB_PATH);
                items = teamlessInventory.items;
                break;
            case EChestType.GLOBAL:
                savePath = saveFolderPath.resolve(GLOBAL_ECHEST_SUB_PATH);
                items = globalInventory.items;
                break;
            default:
                return;
        }
        NbtCompound nbt = new NbtCompound();
        // Transfer inventory from items to nbt
        Inventories.writeNbt(nbt, items, Main.server.getRegistryManager());
        try {
            // Save the nbt to a file.
            NbtIo.write(nbt, savePath);
        } catch (IOException ioException) {
            Main.LOGGER.error("Failure saving ender chest inventories.");
        }
    }

    public static void load() {
        // Get and/or make the directory where the ender chest inventories are saved.
        saveFolderPath = Main.server.getSavePath(WorldSavePath.ROOT).resolve(Main.MOD_ID);
        File fullSaveFolderFile = saveFolderPath.resolve(PLAYER_INTERACT_METHODS_FOLDER).toFile();
        if (!fullSaveFolderFile.exists()) {
            fullSaveFolderFile.mkdirs();
        }

        // Load the inventory of each ender chest.
        if (Main.CONFIG.teamEnderChest) {
            Main.server.getScoreboard().getTeams().forEach((team) -> loadChest(EChestType.TEAM, team));
        }
        if (Main.CONFIG.teamlessEnderChest) {
            loadChest(EChestType.TEAMLESS, null);
        }
        if (Main.CONFIG.globalEnderChest) {
            loadChest(EChestType.GLOBAL, null);
        }

        // Load player interaction methods
        File[] playerInteractMethodsFiles = saveFolderPath.resolve(PLAYER_INTERACT_METHODS_FOLDER).toFile().listFiles((file) -> file.getName().endsWith(".sav"));
        if (playerInteractMethodsFiles != null) {
            for (File saveFile: playerInteractMethodsFiles) {
                try {
                    // Get the uuid from the file name.
                    UUID uuid = UUID.fromString(saveFile.getName().replace(".sav", ""));
                    // Get nbt of player interaction methods.
                    NbtCompound nbt = NbtIo.read(saveFile.toPath());
                    // Turn the nbt into the desired hash table.
                    if (!(nbt == null || nbt.isEmpty())) {
                        Hashtable<EChestType, InteractMethod> playerInteractMethods = new Hashtable<>();
                        for (String eChestType: nbt.getKeys()) {
                            playerInteractMethods.put(EChestType.valueOf(eChestType), InteractMethod.valueOf(nbt.getString(eChestType)));
                        }
                        // Store the player's interaction methods.
                        playersInteractMethods.put(uuid, playerInteractMethods);
                    }
                } catch (IOException ioException) {
                    Main.LOGGER.error("Error loading player preferred interact method, skipping player.");
                    Main.LOGGER.warn("player had uuid of ".concat(saveFile.getName().replace(".sav", "")));
                }
            }
        }
    }

    public static void save() {
        // Save each ender chest.
        if (Main.CONFIG.teamEnderChest) {
            Main.server.getScoreboard().getTeams().forEach((team -> saveChest(EChestType.TEAM, team)));
        }
        if (Main.CONFIG.teamlessEnderChest) {
            saveChest(EChestType.TEAMLESS, null);
        }
        if (Main.CONFIG.globalEnderChest) {
            saveChest(EChestType.GLOBAL, null);
        }

        // Save the player interaction methods
        Path playerInteractMethodsFolderPath = saveFolderPath.resolve(PLAYER_INTERACT_METHODS_FOLDER);
        playersInteractMethods.forEach((uuid, interactMethods) -> {
            NbtCompound nbt = new NbtCompound();
            // Put all the interaction methods for each ender chest type into the nbt.
            interactMethods.forEach((eChestType, interactMethod) -> nbt.putString(eChestType.toString(), interactMethod.name()));
            try {
                // Write the nbt to the save file of the player.
                NbtIo.write(nbt, playerInteractMethodsFolderPath.resolve(uuid.toString().concat(SAVE_FILE_EXTENSION)));
            } catch (IOException ioException) {
                Main.LOGGER.error("Error saving player preferred interact method, skipping player.");
                Main.LOGGER.warn("player had uuid of ".concat(uuid.toString()));
            }
        });
    }
}
