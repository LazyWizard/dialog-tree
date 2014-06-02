package org.lazywizard.conversation;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ConversationMaster
{
    private static final String MOD_ID = "lw_dialog";
    private static final String CSV_PATH = "data/conv/conversations.csv";
    private static final Map<String, Conversation> conversations = new HashMap<>();
    static Conversation currentConv = null;

    public static void reloadConversations()
    {
        conversations.clear();

        try
        {
            JSONArray csv = Global.getSettings().getMergedSpreadsheetDataForMod(
                    "id", CSV_PATH, MOD_ID);
            for (int x = 0; x < csv.length(); x++)
            {
                JSONObject row = csv.getJSONObject(x);
                String id = row.getString("id");
                String filePath = row.getString("filePath");
                try
                {
                    JSONObject rawData = Global.getSettings().loadJSON(filePath);
                    Conversation conv = JSONParser.parseConversation(rawData);
                    conversations.put(id, conv);
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

    public static List<String> getLoadedConversations()
    {
        return new ArrayList<>(conversations.keySet());
    }

    public static boolean hasConversation(String id)
    {
        return conversations.containsKey(id);
    }

    public static Conversation getConversation(String id)
    {
        return conversations.get(id);
    }

    public static void showConversation(Conversation conv, SectorEntityToken talkingTo)
    {
        if (conv.getStartingNode() == null)
        {
            throw new RuntimeException("No startingNode found!");
        }

        Global.getSector().getCampaignUI().showInteractionDialog(
                new ConversationDialogPlugin(conv, talkingTo), talkingTo);
    }

    public static boolean isInConversation()
    {
        return (currentConv != null);
    }

    private ConversationMaster()
    {
    }
}
