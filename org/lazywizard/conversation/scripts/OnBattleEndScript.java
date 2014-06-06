package org.lazywizard.conversation.scripts;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import org.lazywizard.conversation.ConversationDialog;

public interface OnBattleEndScript
{
    public void onBattleEnd(SectorEntityToken talkingTo,
            EngagementResultAPI battleResult, ConversationDialog dialog);
}
