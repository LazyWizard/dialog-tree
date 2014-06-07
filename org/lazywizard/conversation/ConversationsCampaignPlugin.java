package org.lazywizard.conversation;

import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.BaseCampaignPlugin;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;

class ConversationsCampaignPlugin extends BaseCampaignPlugin
{
    @Override
    public PluginPick<InteractionDialogPlugin> pickInteractionDialogPlugin(
            SectorEntityToken interactionTarget)
    {
        if (interactionTarget instanceof JumpPointAPI)
        {
            Conversation conv = ConversationMaster.getConversation("jumpPoint");
            InteractionDialogPlugin dialog = ConversationMaster.createDialogPlugin(conv, interactionTarget);
            return new PluginPick<>(dialog, PickPriority.MOD_GENERAL);
        }

        return null;
    }
}
