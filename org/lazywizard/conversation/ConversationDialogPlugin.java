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
import org.lazywizard.conversation.Conversation.Node;
import org.lazywizard.conversation.Conversation.Response;
import org.lazywizard.conversation.scripts.OnBattleEndScript;

class ConversationDialogPlugin implements InteractionDialogPlugin, ConversationDialog
{
    private final Conversation conv;
    private final SectorEntityToken talkingTo;
    private final boolean devMode;
    private InteractionDialogAPI dialog;
    private TextPanelAPI text;
    private OptionPanelAPI options;
    private VisualPanelAPI visual;
    private Node currentNode;
    private OnBattleEndScript endBattleScript;
    private BattleCreationContext context;

    ConversationDialogPlugin(Conversation conv, SectorEntityToken talkingTo)
    {
        this.conv = conv;
        this.talkingTo = talkingTo;
        devMode = Global.getSettings().isDevMode();
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

        ConversationMaster.currentConv = conv;
        goToNode(conv.getStartingNode());
    }

    @Override
    public void endConversation()
    {
        ConversationMaster.currentConv = null;
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
        text.addParagraph(node.getText());
        reloadCurrentNode();
    }

    @Override
    public void reloadCurrentNode()
    {
        options.clearOptions();
        for (Response response : currentNode.getResponses())
        {
            checkAddResponse(response);
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
    public SectorEntityToken getConversationPartner()
    {
        return talkingTo;
    }

    private void checkAddResponse(Response response)
    {
        Response.Visibility visibility = response.getVisibility(talkingTo);

        // Dev mode = on, allow player to choose even disabled/hidden options
        if (devMode)
        {
            switch (visibility)
            {
                case VISIBLE:
                    options.addOption(response.getText(), response, response.getTooltip());
                    break;
                case DISABLED:
                    options.addOption("[DISABLED] " + response.getText(),
                            response, Color.YELLOW, response.getTooltip());
                    break;
                case HIDDEN:
                    options.addOption("[HIDDEN] " + response.getText(),
                            response, Color.RED, response.getTooltip());
                    break;
                default:
                    Global.getLogger(ConversationDialogPlugin.class).log(Level.ERROR,
                            "Unsupported status: " + visibility.name());
            }

            return;
        }

        // Dev mode = off, respect visibility status
        switch (visibility)
        {
            case VISIBLE:
                options.addOption(response.getText(), response, response.getTooltip());
                break;
            case DISABLED:
                options.addOption(response.getText(), response, response.getTooltip());
                options.setEnabled(response, false);
                break;
            case HIDDEN:
                break;
            default:
                Global.getLogger(ConversationDialogPlugin.class).log(Level.ERROR,
                        "Unsupported status: " + visibility.name());
        }
    }

    @Override
    public void optionSelected(String optionText, Object optionData)
    {
        Response response = (Response) optionData;
        text.addParagraph(response.getText(), Color.CYAN);
        response.onChosen(talkingTo, this);
        goToNode(conv.getNodes().get(response.getDestination()));
    }

    @Override
    public void optionMousedOver(String optionText, Object optionData)
    {
        Response response = (Response) optionData;

        if (response != null)
        {
            response.onMousedOver(talkingTo, this);
        }
    }

    @Override
    public void advance(float amount)
    {
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
            endBattleScript.onBattleEnd(talkingTo, battleResult, this);
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
