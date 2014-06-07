package org.lazywizard.conversation;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Level;
import org.json.JSONException;
import org.json.JSONString;
import org.lazywizard.conversation.scripts.ConversationScript;
import org.lazywizard.conversation.scripts.NodeScript;
import org.lazywizard.conversation.scripts.ResponseScript;
import org.lazywizard.conversation.scripts.VisibilityScript;

// TODO: Much more commenting, better logging
// TODO: Clean up constructors
public final class Conversation implements JSONString
{
    private final Map<String, Node> nodes;
    private final ConversationScript convScript;
    private Node startingNode;

    public Conversation(Map<String, Node> nodes)
    {
        this(nodes, null);
    }

    public Conversation(ConversationScript script)
    {
        this(new HashMap<String, Node>(), script);
    }

    public Conversation(Map<String, Node> nodes, ConversationScript script)
    {
        this.nodes = nodes;
        this.convScript = script;
    }

    void init(DialogInfo info)
    {
        if (convScript != null)
        {
            convScript.init(this, info);
        }
    }

    void advance(float amount, Node node)
    {
        if (convScript != null)
        {
            convScript.advance(amount, node);
        }
    }

    public void addNode(String id, Node node)
    {
        if (nodes.containsKey(id))
        {
            nodes.put(id, node).setParentConversation(null);
        }
        else
        {
            nodes.put(id, node);
        }

        node.setParentConversation(this);
    }

    public void removeNode(String id)
    {
        if (nodes.containsKey(id))
        {
            nodes.remove(id).setParentConversation(null);
        }
    }

    public boolean hasNode(String id)
    {
        return nodes.containsKey(id);
    }

    public Node getNode(String id)
    {
        return nodes.get(id);
    }

    public Map<String, Node> getNodes()
    {
        return nodes;
    }

    ConversationScript getConversationScript()
    {
        return convScript;
    }

    public Node getStartingNode()
    {
        return startingNode;
    }

    public void setStartingNode(Node startingNode)
    {
        this.startingNode = startingNode;
    }

    public boolean isValid()
    {
        boolean isValid = true;

        if (startingNode == null || !nodes.containsValue(startingNode))
        {
            Global.getLogger(Conversation.class).log(Level.ERROR,
                    "Starting node not found!");
            isValid = false;
        }

        for (Node node : nodes.values())
        {
            for (Response response : node.responses)
            {
                String destination = response.getDestination();

                if (destination == null)
                {
                    continue;
                }

                if (!nodes.containsKey(destination))
                {
                    Global.getLogger(Conversation.class).log(Level.ERROR,
                            "Invalid response destination: " + destination);
                    isValid = false;
                }
            }
        }

        return isValid;
    }

    @Override
    public String toJSONString()
    {
        try
        {
            return JSONParser.toJSON(this).toString(3);
        }
        catch (JSONException ex)
        {
            throw new RuntimeException("Failed to JSONify conversation!", ex);
        }
    }

    public static final class Node implements JSONString
    {
        private String text;
        private final Set<Response> responses;
        private final NodeScript nodeScript;
        private Conversation parentConv;
        private boolean hasInitiated = false;

        public Node(String text, List<Response> responses)
        {
            this(text, responses, null);
        }

        public Node(String text, List<Response> responses, NodeScript script)
        {
            this.text = text;
            this.responses = new LinkedHashSet<>();
            this.nodeScript = script;

            for (Response tmp : responses)
            {
                addResponse(tmp);
            }
        }

        void init(DialogInfo info)
        {
            if (!hasInitiated && nodeScript != null)
            {
                nodeScript.init(this, info);
            }

            hasInitiated = true;
        }

        void advance(float amount)
        {
            if (nodeScript != null)
            {
                nodeScript.advance(amount);
            }
        }

        private void setParentConversation(Conversation parentConv)
        {
            this.parentConv = parentConv;
        }

        public String getText()
        {
            return text;
        }

        public void setText(String text)
        {
            this.text = text;
        }

        public void appendText(String text)
        {
            this.text += " " + text;
        }

        public void addResponse(Response response)
        {
            responses.add(response);
            response.setParentNode(this);
        }

        public void removeResponse(Response response)
        {
            responses.remove(response);
            response.setParentNode(null);
        }

        public List<Response> getResponsesCopy()
        {
            return new ArrayList<>(responses);
        }

        NodeScript getNodeScript()
        {
            return nodeScript;
        }

        public Conversation getConversation()
        {
            return parentConv;
        }

        @Override
        public String toJSONString()
        {
            try
            {
                return JSONParser.toJSON(this).toString(3);
            }
            catch (JSONException ex)
            {
                throw new RuntimeException("Failed to JSONify node!", ex);
            }
        }
    }

    public static final class Response implements JSONString
    {
        private final String text, tooltip, leadsTo;
        private final ResponseScript responseScript;
        private final Object[] onChosenArgs;
        private final VisibilityScript visibility;
        private Node parentNode = null;

        public enum Visibility
        {
            HIDDEN,
            DISABLED,
            VISIBLE
        }

        public Response(String text, String leadsTo, String tooltip,
                ResponseScript responseScript, Object[] onChosenArgs,
                VisibilityScript visibility)
        {
            this.text = text;
            this.leadsTo = leadsTo;
            this.tooltip = tooltip;
            this.responseScript = responseScript;
            this.onChosenArgs = onChosenArgs;
            this.visibility = visibility;
        }

        public Response(String text, String leadsTo)
        {
            this(text, leadsTo, null, null, null, null);
        }

        private void setParentNode(Node parentNode)
        {
            this.parentNode = parentNode;
        }

        void onChosen(DialogInfo info)
        {
            Global.getLogger(Response.class).log(Level.DEBUG,
                    "Chose response: \"" + text + "\"\nLeads to: " + leadsTo);

            if (responseScript != null)
            {
                responseScript.onChosen(info, onChosenArgs);
            }
        }

        void onMousedOver(DialogInfo info, boolean wasLastMousedOver)
        {
            Global.getLogger(Response.class).log(Level.DEBUG,
                    "Moused over response: \"" + text + "\"\nLeads to: " + leadsTo);

            if (responseScript != null)
            {
                responseScript.onMousedOver(info, wasLastMousedOver);
            }
        }

        public String getText()
        {
            return text;
        }

        public String getTooltip()
        {
            return tooltip;
        }

        public Visibility getVisibility(SectorEntityToken talkingTo)
        {
            if (visibility != null)
            {
                return visibility.getVisibility(talkingTo);
            }

            return Visibility.VISIBLE;
        }

        ResponseScript getResponseScript()
        {
            return responseScript;
        }

        Object[] getOnChosenArgs()
        {
            return onChosenArgs;
        }

        VisibilityScript getVisibilityScript()
        {
            return visibility;
        }

        public Node getParentNode()
        {
            return parentNode;
        }

        public String getDestination()
        {
            return leadsTo;
        }

        @Override
        public String toJSONString()
        {
            try
            {
                return JSONParser.toJSON(this).toString(3);
            }
            catch (JSONException ex)
            {
                throw new RuntimeException("Failed to JSONify response!", ex);
            }
        }
    }
}
