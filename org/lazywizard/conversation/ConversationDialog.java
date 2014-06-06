package org.lazywizard.conversation;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import org.lazywizard.conversation.Conversation.Node;
import org.lazywizard.conversation.scripts.OnBattleEndScript;

public interface ConversationDialog
{
    public InteractionDialogAPI getInteractionDialog();

    public void goToNode(Node node);

    public void reloadCurrentNode();

    public void startBattle(BattleCreationContext context, OnBattleEndScript onFinish);

    public void endConversation();
}
