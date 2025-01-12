package boombabob.teamechest;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

public class Config {
    public static String path_string ="team-echest-config";
    public static ConfigClassHandler<Config> HANDLER = ConfigClassHandler.createBuilder(Config.class)
        .id(Identifier.of(Main.MOD_ID, path_string))
        .serializer(config -> GsonConfigSerializerBuilder.create(config)
            .setPath(FabricLoader.getInstance().getConfigDir().resolve(path_string.concat(".json5")))
            .setJson5(true)
            .build())
        .build();

    @SerialEntry(comment = "Global toggle to disable mod.")
    public boolean modifyEnderChests = true;
    @SerialEntry(comment = "Whether ender chests are per team (true) or for everyone (false).")
    public boolean teamEnderChests = true;
    @SerialEntry(comment = "Ender chest for teamless players (true).")
    public boolean enderChestForTeamless = false;
    @SerialEntry(comment = """
        Team/Global Ender Chest number of rows (rows are 9 slots wide), must be from 1 to 6.
        *Will delete the items in any row that no longer exists when decreased.*
        """)
    public int enderChestRows = 3;
    @SerialEntry(comment = """
        require player to be sneaking to open team/global ender chest.
        When not sneaking they will still open their personal ender chest.
        If false team chest will always be opened, never personal chest.
        """)
    public boolean sneakToOpen = true;
}
