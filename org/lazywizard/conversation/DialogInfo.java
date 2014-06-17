package org.lazywizard.conversation;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;

public final class DialogInfo
{
    private final SectorEntityToken talkingTo;
    private final ConversationDialog dialog;
    private final CampaignFleetAPI player;

    DialogInfo(SectorEntityToken talkingTo, CampaignFleetAPI player,
            ConversationDialog dialog)
    {
        this.talkingTo = talkingTo;
        this.player = player;
        this.dialog = dialog;
    }

    public CampaignFleetAPI getPlayer()
    {
        return player;
    }

    public SectorEntityToken getConversationPartner()
    {
        return talkingTo;
    }

    public ConversationDialog getDialog()
    {
        return dialog;
    }
}
