package org.lazywizard.conversation.scripts;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import org.lazywizard.conversation.ConversationDialog;

public interface OnChosenScript
{
    public void onChosen(SectorEntityToken talkingTo, ConversationDialog dialog);
}
