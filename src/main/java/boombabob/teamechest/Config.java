package boombabob.teamechest;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

public class Config {
    public static String path_string = "team-echest-config";
    // Initialize config.
    public static ConfigClassHandler<Config> HANDLER = ConfigClassHandler.createBuilder(Config.class)
        .id(Identifier.of(Main.MOD_ID, path_string))
        .serializer(config -> GsonConfigSerializerBuilder.create(config)
            .setPath(FabricLoader.getInstance().getConfigDir().resolve(path_string.concat(".json5")))
            .setJson5(true)
            .build())
        .build();

    @SerialEntry(comment = "Whether personal ender chests are enabled (vanilla implementation).")
    public boolean personalEnderChest = true;
    @SerialEntry(comment = "Whether each team has a team ender chest.")
    public boolean teamEnderChest = true;
    @SerialEntry(comment = "Whether teamless players have a team ender chest.")
    public boolean teamlessEnderChest = false;
    @SerialEntry(comment = "Whether there is a global ender chest for everyone.")
    public boolean globalEnderChest = false;

    @SerialEntry(comment = """
        Team Ender Chest number of rows (rows are 9 slots wide), must be from 1 to 6.
        *Will delete the items in any row that no longer exists when decreased.*
        """)
    public int teamEnderChestRows = 3;

    @SerialEntry(comment = """
        Teamless Ender Chest number of rows (rows are 9 slots wide), must be from 1 to 6.
        *Will delete the items in any row that no longer exists when decreased.*
        """)
    public int teamlessEnderChestRows = 3;

    @SerialEntry(comment = """
        Global Ender Chest number of rows (rows are 9 slots wide), must be from 1 to 6.
        *Will delete the items in any row that no longer exists when decreased.*
        """)
    public int globalEnderChestRows = 3;
}
