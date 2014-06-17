package org.lazywizard.conversation;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.VisualPanelAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.FullName.Gender;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import java.awt.Color;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.lazywizard.conversation.Conversation.Node;
import org.lazywizard.conversation.Conversation.Response;
import org.lazywizard.conversation.scripts.OnBattleEndScript;

class ConversationDialogPlugin implements InteractionDialogPlugin, ConversationDialog
{
    private final Conversation conv;
    private final CampaignFleetAPI player;
    private final SectorEntityToken talkingTo;
    private final boolean devMode;
    private final Map<Pattern, String> keywords;
    private final DialogInfo info;
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
        player = Global.getSector().getPlayerFleet();
        keywords = new LinkedHashMap<>();
        devMode = Global.getSettings().isDevMode();
        info = new DialogInfo(talkingTo, player, this);
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

        ConversationMaster.setCurrentConversation(conv);
        generateKeywords();
        conv.init(info);

        if (!conv.isValid())
        {
            throw new RuntimeException("Conversation is broken or malformed!");
        }

        goToNode(conv.getStartingNode());
    }

    @Override
    public void endConversation()
    {
        conv.end(info);
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
        text.addParagraph(replaceKeywords(node.getText()));
        reloadCurrentNode();
    }

    @Override
    public void reloadCurrentNode()
    {
        options.clearOptions();
        int numSelectable = 0;
        for (Response response : currentNode.getResponsesCopy())
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

    // TODO: Test duplicate handling (unlikely to work since no Pattern.equals())
    @Override
    public void addKeyword(String keyword, String replaceWith)
    {
        keywords.put(Pattern.compile(keyword, Pattern.LITERAL), replaceWith);
    }

    @Override
    public void removeKeyword(String keyword)
    {
        for (Iterator<Pattern> iter = keywords.keySet().iterator(); iter.hasNext();)
        {
            Pattern pattern = iter.next();
            if (keyword.equals(pattern.pattern()))
            {
                iter.remove();
            }
        }
    }

    private void generateKeywords()
    {
        FullName playerName = player.getCommander().getName();

        // TODO
        keywords.clear();
        addKeyword("$PLAYERFIRSTNAME", playerName.getFirst());
        addKeyword("$PLAYERLASTNAME", playerName.getLast());
        addKeyword("$PLAYERFULLNAME", playerName.getFullName());

        switch (playerName.getGender())
        {
            case MALE:
                addKeyword("$PLAYERHIMHER", "him");
                break;
            case FEMALE:
                addKeyword("$PLAYERHIMHER", "her");
                break;
            default:
                addKeyword("$PLAYERHIMHER", "them");
        }

        if (talkingTo instanceof CampaignFleetAPI)
        {
            FullName targetName = ((CampaignFleetAPI) talkingTo).getCommander().getName();
            addKeyword("$TARGETFIRSTNAME", targetName.getFirst());
            addKeyword("$TARGETLASTNAME", targetName.getLast());
            addKeyword("$TARGETFULLNAME", targetName.getFullName());

            switch (targetName.getGender())
            {
                case MALE:
                    addKeyword("$PLAYERHIMHER", "him");
                    break;
                case FEMALE:
                    addKeyword("$PLAYERHIMHER", "her");
                    break;
                default:
                    addKeyword("$PLAYERHIMHER", "them");
            }
        }
    }

    @Override
    public String replaceKeywords(String text)
    {
        // Don't bother with text that doesn't include keywords
        if (text == null || text.isEmpty() || !text.contains("$"))
        {
            return text;
        }

        // Replace all keywords in the string
        for (Map.Entry<Pattern, String> entry : keywords.entrySet())
        {
            text = entry.getKey().matcher(text).replaceAll(entry.getValue());
        }

        return text;
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
                    options.addOption(replaceKeywords(response.getText()),
                            response, replaceKeywords(response.getTooltip()));
                    return true;
                case DISABLED:
                    options.addOption("[DISABLED] " + replaceKeywords(response.getText()),
                            response, Color.YELLOW, replaceKeywords(response.getTooltip()));
                    return false;
                case HIDDEN:
                    options.addOption("[HIDDEN] " + replaceKeywords(response.getText()),
                            response, Color.RED, replaceKeywords(response.getTooltip()));
                    return false;
                default:
                    throw new RuntimeException("Unsupported status: " + visibility.name());
            }
        }

        // Dev mode = off, respect visibility status
        switch (visibility)
        {
            case VISIBLE:
                options.addOption(replaceKeywords(response.getText()),
                        response, replaceKeywords(response.getTooltip()));
                return true;
            case DISABLED:
                options.addOption(replaceKeywords(response.getText()),
                        response, replaceKeywords(response.getTooltip()));
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
        text.addParagraph(replaceKeywords(response.getText()), Color.CYAN);
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
        conv.advance(amount, currentNode);
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
