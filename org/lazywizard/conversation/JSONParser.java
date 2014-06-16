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

// All JSON-related code should go here to make JSON file structure changes easier
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
                json.put(Constants.CONV_STARTING_NODE, tmp.getKey());
            }
        }

        if (conv.getConversationScriptClass() != null)
        {
            json.put(Constants.CONV_SCRIPT, conv.getConversationScriptClass()
                    .getCanonicalName());
        }

        json.put(Constants.CONV_NODES, nodes);
        return json;
    }

    static Conversation parseConversation(JSONObject data) throws JSONException
    {
        String scriptPath = data.optString(Constants.CONV_SCRIPT, null);
        Class<? extends ConversationScript> scriptClass = null;
        if (scriptPath != null)
        {
            try
            {
                scriptClass = (Class<? extends ConversationScript>) Global
                        .getSettings().getScriptClassLoader().loadClass(scriptPath);
            }
            catch (ClassNotFoundException | ClassCastException ex)
            {
                throw new RuntimeException("Failed to load ConversationScript: "
                        + scriptPath, ex);
            }
        }

        Conversation conv = new Conversation();
        conv.setConversationScriptClass(scriptClass);

        String startingNode = data.getString(Constants.CONV_STARTING_NODE);
        JSONObject nodes = data.getJSONObject(Constants.CONV_NODES);
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
            json.put(Constants.NODE_TEXT, node.getText());
        }

        for (Response tmp : node.getResponsesCopy())
        {
            json.append(Constants.NODE_RESPONSES, toJSON(tmp));
        }

        if (node.getNodeScript() != null)
        {
            json.put(Constants.NODE_SCRIPT, node.getNodeScript().getClass().getCanonicalName());
        }

        return json;
    }

    static Node parseNode(JSONObject data) throws JSONException
    {
        String text = data.optString(Constants.NODE_TEXT, "");
        List<Response> responses = new ArrayList<>();

        JSONArray rData = data.optJSONArray(Constants.NODE_RESPONSES);
        if (rData != null)
        {
            for (int x = 0; x < rData.length(); x++)
            {
                Response response = parseResponse(rData.getJSONObject(x));
                responses.add(response);
            }
        }

        String scriptPath = data.optString(Constants.NODE_SCRIPT, null);
        Class<? extends NodeScript> scriptClass = null;
        if (scriptPath != null)
        {
            try
            {
                scriptClass = (Class<? extends NodeScript>) Global
                        .getSettings()
                        .getScriptClassLoader().loadClass(scriptPath);
            }
            catch (ClassNotFoundException | ClassCastException ex)
            {
                throw new RuntimeException("Failed to load NodeScript: "
                        + scriptPath, ex);
            }
        }

        return new Node(text, responses, scriptClass);
    }

    static JSONObject toJSON(Response response) throws JSONException
    {
        JSONObject json = new JSONObject();
        json.put(Constants.RESPONSE_TEXT, response.getText());
        json.putOpt(Constants.RESPONSE_TOOLTIP, response.getTooltip());
        json.putOpt(Constants.RESPONSE_LEADS_TO, response.getDestination());

        if (response.getVisibilityScript() != null)
        {
            json.put(Constants.RESPONSE_VISIBILITY, response.getVisibilityScript()
                    .getClass().getCanonicalName());

            List visibilityArgs = response.getVisibilityArgs();
            if (visibilityArgs != null && visibilityArgs.size() > 0)
            {
                for (Object tmp : visibilityArgs)
                {
                    json.append(Constants.RESPONSE_VISIBILITY_ARGS, tmp);
                }
            }
        }

        if (response.getResponseScript() != null)
        {
            json.put(Constants.RESPONSE_SCRIPT, response.getResponseScript()
                    .getClass().getCanonicalName());

            List onChosenArgs = response.getOnChosenArgs();
            if (onChosenArgs != null && onChosenArgs.size() > 0)
            {
                for (Object tmp : onChosenArgs)
                {
                    json.append(Constants.RESPONSE_ON_CHOSEN_ARGS, tmp);
                }
            }
        }

        return json;
    }

    static Response parseResponse(JSONObject data) throws JSONException
    {
        String text = data.getString(Constants.RESPONSE_TEXT);
        String leadsTo = data.optString(Constants.RESPONSE_LEADS_TO, null);
        String tooltip = data.optString(Constants.RESPONSE_TOOLTIP, null);

        // Try to create the 'on chosen' effect script if an entry for it is present
        ResponseScript responseScript = null;
        List onChosenArgs = null;
        String scriptPath = data.optString(Constants.RESPONSE_SCRIPT, null);
        if (scriptPath != null)
        {
            ResponseScript tmp = null;

            try
            {
                tmp = (ResponseScript) Global.getSettings()
                        .getScriptClassLoader().loadClass(scriptPath).newInstance();
                JSONArray args = data.optJSONArray(Constants.RESPONSE_ON_CHOSEN_ARGS);

                if (args != null)
                {
                    onChosenArgs = new ArrayList();
                    for (int x = 0; x < args.length(); x++)
                    {
                        onChosenArgs.add(args.get(x));
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
        scriptPath = data.optString(Constants.RESPONSE_VISIBILITY, null);
        List visibilityArgs = null;
        if (scriptPath != null)
        {
            VisibilityScript tmp = null;

            try
            {
                tmp = (VisibilityScript) Global.getSettings()
                        .getScriptClassLoader().loadClass(scriptPath).newInstance();
                JSONArray args = data.optJSONArray(Constants.RESPONSE_VISIBILITY_ARGS);

                if (args != null)
                {
                    visibilityArgs = new ArrayList();
                    for (int x = 0; x < args.length(); x++)
                    {
                        visibilityArgs.add(args.get(x));
                    }
                }
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
                onChosenArgs, visibility, visibilityArgs);
    }

    private JSONParser()
    {
    }
}
