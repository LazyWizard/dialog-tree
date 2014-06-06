package org.lazywizard.conversation.scripts;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import org.lazywizard.conversation.Conversation.Response.Visibility;
import org.lazywizard.conversation.ConversationDialog;

public interface VisibilityScript
{
    public Visibility getVisibility(SectorEntityToken talkingTo, ConversationDialog dialog);
}
