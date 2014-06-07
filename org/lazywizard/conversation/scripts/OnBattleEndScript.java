package org.lazywizard.conversation.scripts;

import com.fs.starfarer.api.combat.EngagementResultAPI;
import org.lazywizard.conversation.Conversation.ConversationInfo;

public interface OnBattleEndScript
{
    public void onBattleEnd(EngagementResultAPI battleResult, ConversationInfo info);
}
