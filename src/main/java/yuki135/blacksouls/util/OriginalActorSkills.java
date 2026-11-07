package yuki135.blacksouls.util;

import yuki135.blacksouls.capability.BSPlayerStats;
import yuki135.blacksouls.handler.StatEventHandler;
import yuki135.blacksouls.story.OriginalSkillAcquisitionService;
import yuki135.blacksouls.util.skill.SkillDodoRun;
import yuki135.blacksouls.util.skill.SkillRegistry;
import net.minecraft.server.level.ServerPlayer;

public final class OriginalActorSkills {
    private OriginalActorSkills() {
    }

    public static String mapped(int sourceSkill) {
        if (sourceSkill != 340 || !(SkillRegistry.SKILLS.get(SkillDodoRun.ID) instanceof SkillDodoRun)) {
            throw new UnsupportedOperationException("unmapped_original_actor_skill:" + sourceSkill);
        }
        return SkillDodoRun.ID;
    }

    public static void change(ServerPlayer player, int actorId, int sourceSkill, boolean learn) {
        change(player, actorId, sourceSkill, learn, actorId);
    }

    public static void change(ServerPlayer player, int actorId, int sourceSkill, boolean learn, int sourceActorOperand) {
        String skill = mapped(sourceSkill);
        if (actorId != 0 && !OriginalActorBridge.isPlayerActor(player, actorId) || player == null) {
            throw new UnsupportedOperationException("unmapped_actor_skill:" + actorId);
        }
        BSPlayerStats stats = player.getCapability(BSPlayerStats.CAPABILITY)
                .orElseThrow(() -> new IllegalStateException("Missing original actor skills"));
        var presentation = OriginalSkillAcquisitionService.prepare(player, sourceSkill, sourceActorOperand, learn);
        if (learn) {
            if (!stats.unlockedSkills.contains(skill)) stats.unlockedSkills.add(skill);
        } else {
            stats.unlockedSkills.removeIf(skill::equals);
            if (skill.equals(stats.skillZ)) stats.skillZ = "";
            if (skill.equals(stats.skillX)) stats.skillX = "";
            if (skill.equals(stats.skillC)) stats.skillC = "";
            if (skill.equals(stats.skillV)) stats.skillV = "";
        }
        StatEventHandler.syncToClient(player);
        OriginalSkillAcquisitionService.publish(player, presentation);
    }
}
