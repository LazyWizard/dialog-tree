package org.lazywizard.conversation;

import com.fs.starfarer.api.campaign.SectorEntityToken;

public interface OnChosenScript
{
    public void onChosen(SectorEntityToken talkingTo, ConversationDialog dialog);
}
