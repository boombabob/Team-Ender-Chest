package boombabob.teamechest;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import static net.minecraft.server.command.CommandManager.literal;

public class InteractMethodCommand {
    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        final LiteralCommandNode<ServerCommandSource> interactMethodCommandNode =
            dispatcher.register(literal("teamEChest")
                // Has to be a player, player might be null errors can be ignored.
                .requires(ServerCommandSource::isExecutedByPlayer)
                    // Option for each ender chest type
                    .then(eChestTypeLiteral(TeamEChests.EChestType.PERSONAL))
                    .then(eChestTypeLiteral(TeamEChests.EChestType.TEAM))
                    .then(eChestTypeLiteral(TeamEChests.EChestType.TEAMLESS))
                    .then(eChestTypeLiteral(TeamEChests.EChestType.GLOBAL))
                    // Lists players interact methods for each ender chest type. Only includes activated ender chests.
                    .then(literal("list").executes((context) -> {
                        ServerCommandSource source = context.getSource();
                        Hashtable<TeamEChests.EChestType, TeamEChests.InteractMethod> playerInteractMethods = TeamEChests.playersInteractMethods.get(source.getPlayer().getUuid());
                        List<String> playerFeedback = new ArrayList<>();
                        playerInteractMethods.forEach((eChestType, interactMethod) -> {
                                // Skip any ender chest type that isn't enabled.
                                if (TeamEChests.isEchestInvalid(eChestType)) {
                                    return;
                                }
                                playerFeedback.add("%s: %s".formatted(eChestType.displayName, interactMethod.displayName));
                            }
                        );
                        source.sendFeedback(() -> Text.literal(String.join("\n", playerFeedback)), false);
                        return Command.SINGLE_SUCCESS;
                    }))
            );
        // Alias for /teamEChest, so they can type /eChest instead.
        dispatcher.register(literal("eChest")
            .requires(ServerCommandSource::isExecutedByPlayer)
            .redirect(interactMethodCommandNode));
    }

    private LiteralArgumentBuilder<ServerCommandSource> eChestTypeLiteral(TeamEChests.EChestType eChestType) {
        // Don't add options to change any ender chest type that isn't enabled.
        if (TeamEChests.isEchestInvalid(eChestType)) {
            return literal("");
        }
        // Return the literal (i.e. the thing the player has to type in to access the option) along with sub options.
        return literal(eChestType.displayName)
                .then(interactMethod(eChestType, TeamEChests.InteractMethod.ALWAYS))
                .then(interactMethod(eChestType, TeamEChests.InteractMethod.NEVER))
                .then(interactMethod(eChestType, TeamEChests.InteractMethod.ON_SNEAK))
                .then(interactMethod(eChestType, TeamEChests.InteractMethod.ON_UNSNEAK));
    }

    private LiteralArgumentBuilder<ServerCommandSource> interactMethod(TeamEChests.EChestType eChestType, TeamEChests.InteractMethod interactMethod) {
        // Return the literal (i.e. the thing the player has to type in to access the option) along with the action.
        return literal(interactMethod.displayName)
                .executes(context -> {
                    TeamEChests.changePlayerInteractMethod(context.getSource().getPlayer(), interactMethod, eChestType);
                    context.getSource().sendFeedback(() -> Text.literal(eChestType.displayName.concat(" ender chest set to open ").concat(interactMethod.displayName)), false);
                    return Command.SINGLE_SUCCESS;
                });
    }
}
