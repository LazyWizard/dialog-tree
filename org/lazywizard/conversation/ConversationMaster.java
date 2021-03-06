package org.lazywizard.conversation;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.conversation.Conversation.Node;
import org.lazywizard.conversation.Conversation.Response;

public class ConversationMaster
{
    private static final Map<String, Conversation> masterConvs = new HashMap<>();
    private static Conversation currentConv = null;

    public static void registerConversation(String convId, Conversation conv)
    {
        if (conv == null)
        {
            masterConvs.remove(convId);
            return;
        }

        // Validate unscripted conversations now, scripted ones after initiation
        if (conv.getConversationScript() == null && !conv.isValid())
        {
            throw new RuntimeException("Conversation '" + convId
                    + "' is broken or malformed!");
        }

        masterConvs.put(convId, conv);
    }

    public static void reloadConversations()
    {
        masterConvs.clear();

        try
        {
            JSONArray csv = Global.getSettings().getMergedSpreadsheetDataForMod(
                    Constants.CSV_COLUMN_ID, Constants.CSV_PATH, Constants.MOD_ID);
            for (int x = 0; x < csv.length(); x++)
            {
                JSONObject row = csv.getJSONObject(x);
                String id = row.getString(Constants.CSV_COLUMN_ID);
                String filePath = row.getString(Constants.CSV_COLUMN_PATH);
                try
                {
                    JSONObject rawData = Global.getSettings().loadJSON(filePath);
                    Conversation conv = JSONParser.parseConversation(rawData);
                    registerConversation(id, conv);
                }
                catch (IOException ex)
                {
                    throw new RuntimeException("Unable to find conversation '"
                            + id + "' at " + filePath, ex);
                }
                catch (JSONException ex)
                {
                    throw new RuntimeException("Unable to create conversation '"
                            + id + "' at " + filePath, ex);
                }
            }
        }
        catch (IOException ex)
        {
            throw new RuntimeException("Failed to find conversation list!", ex);
        }
        catch (JSONException ex)
        {
            throw new RuntimeException("Malformed conversations.csv!", ex);
        }
    }

    public static Conversation copyConversation(Conversation conv)
    {
        if (conv == null)
        {
            return null;
        }

        Conversation copy = new Conversation();

        if (conv.getConversationScriptClass() != null)
        {
            copy.setConversationScriptClass(conv.getConversationScriptClass());
        }

        for (Map.Entry<String, Node> nodeData : conv.getNodes().entrySet())
        {
            String nodeId = nodeData.getKey();
            Node oldNode = nodeData.getValue();

            List<Response> responses = new ArrayList<>();
            for (Response response : nodeData.getValue().getResponsesCopy())
            {
                responses.add(new Response(response.getText(),
                        response.getDestination(), response.getTooltip(),
                        response.getResponseScript(),
                        response.getOnChosenArgs(),
                        response.getVisibilityScript(),
                        response.getVisibilityArgs()));
            }

            Node node = new Node(oldNode.getText(), responses, oldNode.getNodeScriptClass());
            copy.addNode(nodeId, node);

            if (oldNode == conv.getStartingNode())
            {
                copy.setStartingNode(node);
            }
        }

        return copy;
    }

    public static List<String> getLoadedConversations()
    {
        return new ArrayList<>(masterConvs.keySet());
    }

    public static boolean hasConversation(String convId)
    {
        return masterConvs.containsKey(convId);
    }

    // Any changes made to this conversation will affect all new copies of it
    public static Conversation getMasterConversation(String convId)
    {
        return masterConvs.get(convId);
    }

    public static Conversation getConversation(String convId)
    {
        return copyConversation(masterConvs.get(convId));
    }

    public static InteractionDialogPlugin createDialogPlugin(Conversation conv,
            SectorEntityToken talkingTo)
    {
        return new ConversationDialogPlugin(conv, talkingTo);
    }

    public static void showConversation(Conversation conv, SectorEntityToken talkingTo)
    {
        // DEBUG
        System.out.println(conv.toJSONString());

        Global.getSector().getCampaignUI().showInteractionDialog(
                createDialogPlugin(conv, talkingTo), talkingTo);
    }

    public static boolean isPlayerInConversation()
    {
        return (currentConv != null);
    }

    public static Conversation getCurrentConversation()
    {
        return currentConv;
    }

    static void setCurrentConversation(Conversation conv)
    {
        currentConv = conv;
    }

    private ConversationMaster()
    {
    }
}
