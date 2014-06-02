package org.lazywizard.conversation;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import org.lazywizard.conversation.Conversation.Node;

public interface ConversationDialog
{
    public InteractionDialogAPI getInteractionDialog();

    public void goToNode(Node node);

    public void endConversation();
}
