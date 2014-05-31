package org.lazywizard.conversation;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Level;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lazywizard.conversation.VisibilityScript.Visibility;

// TODO: Move all JSON parsing into single method for easier debugging
// TODO: Much more commenting, better logging
class Conversation
{
    private final Map<String, Node> nodes;
    private Node startingNode;

    Conversation(String filePath) throws JSONException, IOException
    {
        JSONObject rawData = Global.getSettings().loadJSON(filePath);
        String startNode = rawData.getString("startingNode");

        nodes = new HashMap<>();
        JSONObject nodeData = rawData.getJSONObject("nodes");
        for (Iterator keys = nodeData.keys(); keys.hasNext();)
        {
            String id = (String) keys.next();
            JSONObject data = nodeData.getJSONObject(id);
            Node node = new Node(id, data);

            if (node.getResponses().isEmpty())
            {
                continue;
            }

            nodes.put(id, node);

            if (startNode.equals(id))
            {
                startingNode = node;
            }
        }

        if (startingNode == null)
        {
            throw new RuntimeException("No startingNode found in " + filePath);
        }
    }

    Map<String, Node> getNodes()
    {
        return nodes;
    }

    Node getStartingNode()
    {
        return startingNode;
    }

    class Node
    {
        private final String text;
        private final List<Response> responses;

        Node(String nodeId, JSONObject data) throws JSONException
        {
            text = data.getString("text");
            responses = new ArrayList<>();

            JSONArray rData = data.getJSONArray("responses");
            for (int x = 0; x < rData.length(); x++)
            {
                responses.add(new Response(rData.getJSONObject(x)));
            }
        }

        String getText()
        {
            return text;
        }

        List<Response> getResponses()
        {
            return responses;
        }

        Conversation getConversation()
        {
            return Conversation.this;
        }

        class Response
        {
            private final String text, tooltip;
            private final String leadsTo;
            private final OnChosenScript onChosen;
            private final VisibilityScript visibility;

            Response(JSONObject data) throws JSONException
            {
                text = data.getString("text");
                leadsTo = data.optString("leadsTo", null);
                tooltip = data.optString("tooltip", null);

                String scriptPath = data.optString("onChosenScript", null);
                if (scriptPath == null)
                {
                    onChosen = null;
                }
                else
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
                        Global.getLogger(Response.class).log(Level.ERROR,
                                "Failed to create OnChosenScript: " + scriptPath, ex);
                    }

                    onChosen = tmp;
                }

                scriptPath = data.optString("visibilityScript", null);
                if (scriptPath == null)
                {
                    visibility = null;
                }
                else
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
                        Global.getLogger(Response.class).log(Level.ERROR,
                                "Failed to create VisibilityScript: " + scriptPath, ex);
                    }

                    visibility = tmp;
                }
            }

            void onChosen(SectorEntityToken talkingTo, InteractionDialogAPI dialog)
            {
                Global.getLogger(Response.class).log(Level.DEBUG,
                        "Chose response: \"" + text + "\"\nLeads to: " + leadsTo);

                if (onChosen != null)
                {
                    onChosen.onChosen(talkingTo, dialog);
                }
            }

            String getText()
            {
                return text;
            }

            String getTooltip()
            {
                return tooltip;
            }

            Visibility getVisibility()
            {
                if (visibility != null)
                {
                    return visibility.getVisibility();
                }

                return VisibilityScript.Visibility.VISIBLE;
            }

            Node getNode()
            {
                return Conversation.Node.this;
            }

            String getNodeLedTo()
            {
                return leadsTo;
            }
        }
    }
}
