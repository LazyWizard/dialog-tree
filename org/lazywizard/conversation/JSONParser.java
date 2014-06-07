package org.lazywizard.conversation;

import com.fs.starfarer.api.Global;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.conversation.Conversation.Node;
import org.lazywizard.conversation.Conversation.Response;
import org.lazywizard.conversation.scripts.ConversationScript;
import org.lazywizard.conversation.scripts.NodeScript;
import org.lazywizard.conversation.scripts.ResponseScript;
import org.lazywizard.conversation.scripts.VisibilityScript;

// TODO: Write raw JSON text ourselves if we want to preserve key-pair ordering
class JSONParser
{
    static JSONObject toJSON(Conversation conv) throws JSONException
    {
        JSONObject json = new JSONObject();

        JSONObject nodes = new JSONObject();
        for (Map.Entry<String, Node> tmp : conv.getNodes().entrySet())
        {
            nodes.put(tmp.getKey(), toJSON(tmp.getValue()));

            if (conv.getStartingNode() == tmp.getValue())
            {
                json.put("startingNode", tmp.getKey());
            }
        }

        if (conv.getConversationScript()!= null)
        {
            json.put("convScript", conv.getConversationScript()
                    .getClass().getCanonicalName());
        }

        json.put("nodes", nodes);
        return json;
    }

    static Conversation parseConversation(JSONObject data) throws JSONException
    {
        String scriptPath = data.optString("convScript", null);
        ConversationScript script = null;
        if (scriptPath != null)
        {
            try
            {
                script = (ConversationScript) Global.getSettings()
                        .getScriptClassLoader().loadClass(scriptPath).newInstance();
            }
            catch (ClassNotFoundException | ClassCastException |
                    IllegalAccessException | InstantiationException ex)
            {
                throw new RuntimeException("Failed to create NodeScript: "
                        + scriptPath, ex);
            }
        }

        Conversation conv = new Conversation(script);

        String startingNode = data.getString("startingNode");
        JSONObject nodes = data.getJSONObject("nodes");
        for (Iterator keys = nodes.keys(); keys.hasNext();)
        {
            String nodeId = (String) keys.next();
            JSONObject nodeData = nodes.getJSONObject(nodeId);

            // Create the node (and by extension, the response list)
            Node node = parseNode(nodeData);

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

    static JSONObject toJSON(Node node) throws JSONException
    {
        JSONObject json = new JSONObject();

        String text = node.getText();
        if (text != null && !text.isEmpty())
        {
            json.put("text", node.getText());
        }

        for (Response tmp : node.getResponsesCopy())
        {
            json.append("responses", toJSON(tmp));
        }

        if (node.getNodeScript() != null)
        {
            json.put("nodeScript", node.getNodeScript().getClass().getCanonicalName());
        }

        return json;
    }

    static Node parseNode(JSONObject data) throws JSONException
    {
        String text = data.optString("text", "");
        List<Response> responses = new ArrayList<>();

        JSONArray rData = data.optJSONArray("responses");
        if (rData != null)
        {
            for (int x = 0; x < rData.length(); x++)
            {
                Response response = parseResponse(rData.getJSONObject(x));
                responses.add(response);
            }
        }

        String scriptPath = data.optString("nodeScript", null);
        if (scriptPath != null)
        {
            try
            {
                NodeScript script = (NodeScript) Global.getSettings()
                        .getScriptClassLoader().loadClass(scriptPath).newInstance();
                return new Node(text, responses, script);
            }
            catch (ClassNotFoundException | ClassCastException |
                    IllegalAccessException | InstantiationException ex)
            {
                throw new RuntimeException("Failed to create NodeScript: "
                        + scriptPath, ex);
            }
        }

        return new Node(text, responses);
    }

    static JSONObject toJSON(Response response) throws JSONException
    {
        JSONObject json = new JSONObject();
        json.put("text", response.getText());
        json.putOpt("tooltip", response.getTooltip());
        json.putOpt("leadsTo", response.getDestination());

        if (response.getVisibilityScript() != null)
        {
            json.put("visibilityScript", response.getVisibilityScript()
                    .getClass().getCanonicalName());
        }

        if (response.getResponseScript() != null)
        {
            json.put("responseScript", response.getResponseScript()
                    .getClass().getCanonicalName());

            Object[] onChosenArgs = response.getOnChosenArgs();
            if (onChosenArgs != null && onChosenArgs.length > 0)
            {
                for (Object tmp : onChosenArgs)
                {
                    json.append("onChosenArgs", tmp);
                }
            }
        }

        return json;
    }

    static Response parseResponse(JSONObject data) throws JSONException
    {
        String text = data.getString("text");
        String leadsTo = data.optString("leadsTo", null);
        String tooltip = data.optString("tooltip", null);

        // Try to create the 'on chosen' effect script if an entry for it is present
        ResponseScript responseScript = null;
        Object[] onChosenArgs = null;
        String scriptPath = data.optString("responseScript", null);
        if (scriptPath != null)
        {
            ResponseScript tmp = null;

            try
            {
                tmp = (ResponseScript) Global.getSettings()
                        .getScriptClassLoader().loadClass(scriptPath).newInstance();
                JSONArray args = data.optJSONArray("onChosenArgs");

                if (args != null)
                {
                    onChosenArgs = new Object[args.length()];
                    for (int x = 0; x < args.length(); x++)
                    {
                        onChosenArgs[x] = args.get(x);
                    }
                }
            }
            catch (ClassNotFoundException | ClassCastException |
                    IllegalAccessException | InstantiationException ex)
            {
                throw new RuntimeException("Failed to create ResponseScript: "
                        + scriptPath, ex);
            }

            responseScript = tmp;
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

        return new Response(text, leadsTo, tooltip, responseScript,
                onChosenArgs, visibility);
    }

    private JSONParser()
    {
    }
}
