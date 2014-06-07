package org.lazywizard.conversation.scripts;

import com.fs.starfarer.api.combat.EngagementResultAPI;
import org.lazywizard.conversation.DialogInfo;

public interface OnBattleEndScript
{
    public void onBattleEnd(EngagementResultAPI battleResult, DialogInfo info);
}
