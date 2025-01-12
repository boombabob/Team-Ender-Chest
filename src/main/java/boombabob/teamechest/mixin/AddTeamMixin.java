package boombabob.teamechest.mixin;

import boombabob.teamechest.Main;
import boombabob.teamechest.TeamEChests;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Scoreboard.class)
public class AddTeamMixin {
	@Inject(at = @At("RETURN"), method = "addTeam")
	private void init(String name, CallbackInfoReturnable<Team> cir) {
		if (Main.CONFIG.modifyEnderChests & Main.CONFIG.teamEnderChests) {
			TeamEChests.newInventory(cir.getReturnValue());
		}
	}
}