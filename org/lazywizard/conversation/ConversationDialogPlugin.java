package org.lazywizard.conversation;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.VisualPanelAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import java.awt.Color;
import org.apache.log4j.Level;
import org.lazywizard.conversation.Conversation.ConversationInfo;
import org.lazywizard.conversation.Conversation.Node;
import org.lazywizard.conversation.Conversation.Response;
import org.lazywizard.conversation.scripts.OnBattleEndScript;

class ConversationDialogPlugin implements InteractionDialogPlugin, ConversationDialog
{
    private final Conversation conv;
    private final SectorEntityToken talkingTo;
    private final boolean devMode;
    private final ConversationInfo info;
    private InteractionDialogAPI dialog;
    private TextPanelAPI text;
    private OptionPanelAPI options;
    private VisualPanelAPI visual;
    private Node currentNode;
    private Response lastMousedOver;
    private OnBattleEndScript endBattleScript;
    private BattleCreationContext context;

    ConversationDialogPlugin(Conversation conv, SectorEntityToken talkingTo)
    {
        this.conv = conv;
        this.talkingTo = talkingTo;
        devMode = Global.getSettings().isDevMode();
        info = new ConversationInfo(talkingTo, this);
    }

    @Override
    public void init(InteractionDialogAPI dialog)
    {
        if (conv.getStartingNode() == null)
        {
            throw new RuntimeException("No startingNode found!");
        }

        this.dialog = dialog;
        this.text = dialog.getTextPanel();
        this.options = dialog.getOptionPanel();
        this.visual = dialog.getVisualPanel();

        if (talkingTo instanceof CampaignFleetAPI)
        {
            visual.showPersonInfo(((CampaignFleetAPI) talkingTo).getCommander());
        }

        ConversationMaster.setCurrentConversation(conv);
        goToNode(conv.getStartingNode());
    }

    @Override
    public void endConversation()
    {
        ConversationMaster.setCurrentConversation(null);
        dialog.dismiss();
    }

    @Override
    public void goToNode(Node node)
    {
        // Conversation ends when the response chosen doesn't lead to another node
        if (node == null)
        {
            endConversation();
            return;
        }

        currentNode = node;
        currentNode.init(info);
        text.addParagraph(node.getText());
        reloadCurrentNode();
    }

    @Override
    public void reloadCurrentNode()
    {
        options.clearOptions();
        int numSelectable = 0;
        for (Response response : currentNode.getResponses())
        {
            if (checkAddResponse(response))
            {
                numSelectable++;
            }
        }

        // Prevent trapping the player in an unfinished node
        if (numSelectable == 0)
        {
            checkAddResponse(new Response("(no responses found, leave)", null));
        }
    }

    @Override
    public InteractionDialogAPI getInteractionDialog()
    {
        return dialog;
    }

    @Override
    public Conversation getConversation()
    {
        return conv;
    }

    @Override
    public Node getCurrentNode()
    {
        return currentNode;
    }

    @Override
    public SectorEntityToken getConversationPartner()
    {
        return talkingTo;
    }

    private boolean checkAddResponse(Response response)
    {
        Response.Visibility visibility = response.getVisibility(talkingTo);

        // Dev mode = on, allow player to choose even disabled/hidden options
        if (devMode)
        {
            switch (visibility)
            {
                case VISIBLE:
                    options.addOption(response.getText(), response, response.getTooltip());
                    return true;
                case DISABLED:
                    options.addOption("[DISABLED] " + response.getText(),
                            response, Color.YELLOW, response.getTooltip());
                    return false;
                case HIDDEN:
                    options.addOption("[HIDDEN] " + response.getText(),
                            response, Color.RED, response.getTooltip());
                    return false;
                default:
                    throw new RuntimeException("Unsupported status: " + visibility.name());
            }
        }

        // Dev mode = off, respect visibility status
        switch (visibility)
        {
            case VISIBLE:
                options.addOption(response.getText(), response, response.getTooltip());
                return true;
            case DISABLED:
                options.addOption(response.getText(), response, response.getTooltip());
                options.setEnabled(response, false);
                return false;
            case HIDDEN:
                return false;
            default:
                throw new RuntimeException("Unsupported status: " + visibility.name());
        }
    }

    @Override
    public void optionSelected(String optionText, Object optionData)
    {
        Response response = (Response) optionData;
        text.addParagraph(response.getText(), Color.CYAN);
        response.onChosen(info);
        goToNode(conv.getNodes().get(response.getDestination()));
    }

    @Override
    public void optionMousedOver(String optionText, Object optionData)
    {
        Response response = (Response) optionData;
        lastMousedOver = response;

        if (response != null)
        {
            response.onMousedOver(info, (response == lastMousedOver));
        }
    }

    @Override
    public void advance(float amount)
    {
        // TODO: add scripts for conversation and node that are hooked into here
        currentNode.advance(amount);
    }

    @Override
    public void startBattle(BattleCreationContext context, OnBattleEndScript onFinish)
    {
        this.context = context;
        this.endBattleScript = onFinish;
        dialog.startBattle(context);
    }

    @Override
    public void backFromEngagement(EngagementResultAPI battleResult)
    {
        if (endBattleScript != null)
        {
            endBattleScript.onBattleEnd(battleResult, info);
            endBattleScript = null;
        }

        context = null;
    }

    @Override
    public Object getContext()
    {
        return context;
    }
}
