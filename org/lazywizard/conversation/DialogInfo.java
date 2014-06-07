package org.lazywizard.conversation;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;

public final class DialogInfo
{
    private final SectorEntityToken talkingTo;
    private final ConversationDialog dialog;
    private final CampaignFleetAPI player;

    DialogInfo(SectorEntityToken talkingTo, ConversationDialog dialog)
    {
        this.talkingTo = talkingTo;
        this.dialog = dialog;
        this.player = Global.getSector().getPlayerFleet();
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
