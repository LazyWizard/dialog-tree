package org.lazywizard.conversation;

import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.BattleCreationContext;
import org.lazywizard.conversation.Conversation.Node;
import org.lazywizard.conversation.scripts.OnBattleEndScript;

public interface ConversationDialog
{
    // Do NOT call the dialog's dismiss() method, use endConversation() instead!
    // Failure to do so will result in script bugs and potential memory leaks
    public InteractionDialogAPI getInteractionDialog();

    public SectorEntityToken getConversationPartner();

    public Conversation getConversation();

    public void addKeyword(String keyword, String replaceWith);

    public void removeKeyword(String keyword);

    public String replaceKeywords(String rawText);

    public Node getCurrentNode();

    public void goToNode(Node node);

    public void reloadCurrentNode();

    public void startBattle(BattleCreationContext context, OnBattleEndScript onFinish);

    public void endConversation();
}
