package org.lazywizard.conversation.scripts;

import com.fs.starfarer.api.combat.EngagementResultAPI;
import org.lazywizard.conversation.Conversation.Info;

public interface OnBattleEndScript
{
    public void onBattleEnd(EngagementResultAPI battleResult, Info info);
}
