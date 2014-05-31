package org.lazywizard.conversation;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.VisualPanelAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import java.awt.Color;
import org.apache.log4j.Level;
import org.lazywizard.conversation.Conversation.Node;
import org.lazywizard.conversation.Conversation.Node.Response;

class ConversationDialogPlugin implements InteractionDialogPlugin
{
    private final Conversation conv;
    private final SectorEntityToken talkingTo;
    private final boolean devMode;
    private InteractionDialogAPI dialog;
    private TextPanelAPI text;
    private OptionPanelAPI options;
    private VisualPanelAPI visual;
    private Node currentNode;

    ConversationDialogPlugin(Conversation conv, SectorEntityToken talkingTo)
    {
        this.conv = conv;
        this.talkingTo = talkingTo;
        devMode = Global.getSettings().isDevMode();
    }

    @Override
    public void init(InteractionDialogAPI dialog)
    {
        this.dialog = dialog;
        this.text = dialog.getTextPanel();
        this.options = dialog.getOptionPanel();
        this.visual = dialog.getVisualPanel();

        if (talkingTo instanceof CampaignFleetAPI)
        {
            visual.showPersonInfo(((CampaignFleetAPI) talkingTo).getCommander());
        }

        goToNode(conv.getStartingNode());
    }

    private void endConversation()
    {
        dialog.dismiss();
    }

    private void checkAddResponse(Response response)
    {
        // Dev mode = on, allow player to choose even disabled/hidden options
        if (devMode)
        {
            switch (response.getVisibility())
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
                            "Unsupported status: " + response.getVisibility().name());
            }

            return;
        }

        // Dev mode = off, respect visibility status
        switch (response.getVisibility())
        {
            case VISIBLE:
                options.addOption(response.getText(), response, response.getTooltip());
                break;
            case DISABLED:
                options.addOption(response.getText(), response, response.getTooltip());
                options.setEnabled(response, false);
                break;
            default:
                Global.getLogger(ConversationDialogPlugin.class).log(Level.ERROR,
                        "Unsupported status: " + response.getVisibility().name());
        }
    }

    private void goToNode(Node node)
    {
        // Conversation ends when the response chosen doesn't lead to another node
        if (node == null)
        {
            endConversation();
            return;
        }

        currentNode = node;
        text.addParagraph(node.getText());
        options.clearOptions();

        for (Response response : node.getResponses())
        {
            checkAddResponse(response);
        }
    }

    @Override
    public void optionSelected(String optionText, Object optionData)
    {
        Response response = (Response) optionData;
        text.addParagraph(response.getText(), Color.CYAN);
        response.onChosen(talkingTo, dialog);
        goToNode(conv.getNodes().get(response.getNodeLedTo()));
    }

    @Override
    public void optionMousedOver(String optionText, Object optionData)
    {
    }

    @Override
    public void advance(float amount)
    {
    }

    @Override
    public void backFromEngagement(EngagementResultAPI battleResult)
    {
    }

    @Override
    public Object getContext()
    {
        return null;
    }
}
