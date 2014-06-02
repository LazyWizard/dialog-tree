package org.lazywizard.conversation;

import com.fs.starfarer.api.Global;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.conversation.Conversation.Node;
import org.lazywizard.conversation.Conversation.Response;

class JSONParser
{
    static Conversation parseConversation(JSONObject data) throws JSONException
    {
        Conversation conv = new Conversation();
        String startingNode = data.getString("startingNode");
        JSONObject nodes = data.getJSONObject("nodes");
        for (Iterator keys = nodes.keys(); keys.hasNext();)
        {
            String nodeId = (String) keys.next();
            JSONObject nodeData = nodes.getJSONObject(nodeId);

            // Create the node (and by extension, the response list)
            Node node = parseNode(nodeData);

            // Prevent trapping the player in an unfinished node
            if (node.getResponses().isEmpty())
            {
                node.addResponse(new Response("(no responses found, leave)", null));
            }

            // Register the node with this conversation
            conv.addNode(nodeId, node);

            // Check if this is the node that we should open the conversation with
            if (startingNode.equals(nodeId))
            {
                conv.setStartingNode(node);
            }
        }

        // No starting node? Crash and burn!
        if (conv.getStartingNode() == null)
        {
            throw new RuntimeException("No startingNode found!");
        }

        return conv;
    }

    static Node parseNode(JSONObject data) throws JSONException
    {
        String text = data.getString("text");
        List<Response> responses = new ArrayList<>();

        JSONArray rData = data.getJSONArray("responses");
        for (int x = 0; x < rData.length(); x++)
        {
            Response response = parseResponse(rData.getJSONObject(x));
            responses.add(response);
        }

        return new Node(text, responses);
    }

    static Response parseResponse(JSONObject data) throws JSONException
    {
        String text = data.getString("text");
        String leadsTo = data.optString("leadsTo", null);
        String tooltip = data.optString("tooltip", null);

        // Try to create the 'on chosen' effect script if an entry for it is present
        OnChosenScript onChosen = null;
        String scriptPath = data.optString("onChosenScript", null);
        if (scriptPath != null)
        {
            OnChosenScript tmp = null;

            try
            {
                tmp = (OnChosenScript) Global.getSettings()
                        .getScriptClassLoader().loadClass(scriptPath).newInstance();
            }
            catch (ClassNotFoundException | ClassCastException |
                    IllegalAccessException | InstantiationException ex)
            {
                throw new RuntimeException("Failed to create OnChosenScript: "
                        + scriptPath, ex);
            }

            onChosen = tmp;
        }

        // Try to create the visibility script if an entry for it is present
        VisibilityScript visibility = null;
        scriptPath = data.optString("visibilityScript", null);
        if (scriptPath != null)
        {
            VisibilityScript tmp = null;

            try
            {
                tmp = (VisibilityScript) Global.getSettings()
                        .getScriptClassLoader().loadClass(scriptPath).newInstance();
            }
            catch (ClassNotFoundException | ClassCastException |
                    IllegalAccessException | InstantiationException ex)
            {
                throw new RuntimeException("Failed to create VisibilityScript: "
                        + scriptPath, ex);
            }

            visibility = tmp;
        }

        return new Response(text, leadsTo, tooltip, onChosen, visibility);
    }

    private JSONParser()
    {
    }
}
