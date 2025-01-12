package boombabob.teamechest.mixin;

import boombabob.teamechest.Main;
import boombabob.teamechest.TeamEChests;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Scoreboard.class)
public class RemoveTeamMixin {
    @Inject(at = @At("TAIL"), method = "removeTeam")
    private void init(Team team, CallbackInfo ci) {
        if (Main.CONFIG.modifyEnderChests & Main.CONFIG.teamEnderChests) {
            TeamEChests.eChestInventories.remove(team);
        }
    }
}