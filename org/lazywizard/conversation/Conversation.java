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

// TODO: Add Javadoc, more commentary, better logging
// TODO: Make Response more customizable (currently read-only once created)
// TODO: Change Response's leadsTo to a Node, set in the Conversation's init()
// TODO: Add promptText field to Node
// TODO: Add Selector support
// TODO: Add shortcut support
// TODO: Add color support (tentative)
// TODO: This needs some clean-up
public final class Conversation implements JSONString
{
    private final Map<String, Node> nodes;
    private Class<? extends ConversationScript> convScriptClass;
    private transient ConversationScript convScript;
    private Node startingNode;

    public Conversation()
    {
        this(new HashMap<String, Node>(), null);
    }

    public Conversation(Map<String, Node> nodes,
            Class<? extends ConversationScript> scriptClass)
    {
        this.nodes = nodes;
        this.convScriptClass = scriptClass;
    }

    void init(DialogInfo info)
    {
        if (convScriptClass != null)
        {
            try
            {
                convScript = convScriptClass.newInstance();
            }
            catch (InstantiationException | IllegalAccessException ex)
            {
                throw new RuntimeException("Failed to instantiate ConversationScript!", ex);
            }

            convScript.init(this, info);
        }
    }

    void end(DialogInfo info)
    {
        if (convScript != null)
        {
            convScript.end(this, info);
            convScript = null;
        }

        for (Node node : nodes.values())
        {
            node.nodeScript = null;
            node.isInitiated = false;
        }
    }

    void advance(float amount, Node node)
    {
        if (convScript != null)
        {
            convScript.advance(amount, node);
        }
    }

    public void addNode(String nodeId, Node node)
    {
        if (nodes.containsKey(nodeId))
        {
            nodes.put(nodeId, node).setParentConversation(null);
        }
        else
        {
            nodes.put(nodeId, node);
        }

        node.setParentConversation(this);
    }

    public void removeNode(String nodeId)
    {
        if (nodes.containsKey(nodeId))
        {
            nodes.remove(nodeId).setParentConversation(null);
        }
    }

    public boolean hasNode(String nodeId)
    {
        return nodes.containsKey(nodeId);
    }

    public Node getNode(String nodeId)
    {
        return nodes.get(nodeId);
    }

    public Map<String, Node> getNodes()
    {
        return nodes;
    }

    public Class<? extends ConversationScript> getConversationScriptClass()
    {
        return convScriptClass;
    }

    public void setConversationScriptClass(Class<? extends ConversationScript> scriptClass)
    {
        convScriptClass = scriptClass;
        convScript = null;
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
        private final Set<Response> responses;
        private String text;
        private Class<? extends NodeScript> nodeScriptClass;
        private transient NodeScript nodeScript;
        private Conversation parentConv;
        private boolean isInitiated = false;

        public Node(String text)
        {
            this(text, new ArrayList<Response>(), null);
        }

        public Node(String text, List<Response> responses,
                Class<? extends NodeScript> scriptClass)
        {
            this.text = text;
            this.responses = new LinkedHashSet<>();
            this.nodeScriptClass = scriptClass;

            for (Response tmp : responses)
            {
                addResponse(tmp);
            }
        }

        void init(DialogInfo info)
        {
            if (isInitiated)
            {
                return;
            }

            if (nodeScriptClass != null)
            {
                try
                {
                    nodeScript = nodeScriptClass.newInstance();
                }
                catch (InstantiationException | IllegalAccessException ex)
                {
                    throw new RuntimeException("Failed to instantiate NodeScript!", ex);
                }

                nodeScript.init(this, info);
            }

            isInitiated = true;
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

        // Only works if this has a parent conversation!
        public String getNodeId()
        {
            if (parentConv != null)
            {
                for (Map.Entry<String, Node> entry : parentConv.nodes.entrySet())
                {
                    if (entry.getValue() == this)
                    {
                        return entry.getKey();
                    }
                }
            }

            return null;
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

        public Class<? extends NodeScript> getNodeScriptClass()
        {
            return nodeScriptClass;
        }

        public void setNodeScriptClass(Class<? extends NodeScript> scriptClass)
        {
            nodeScriptClass = scriptClass;
            nodeScript = null;
            isInitiated = false;
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
        private final List onChosenArgs;
        private final VisibilityScript visibility;
        private final List visibilityArgs;
        private Node parentNode = null;

        public enum Visibility
        {
            HIDDEN,
            DISABLED,
            VISIBLE
        }

        public Response(String text, String leadsTo, String tooltip,
                ResponseScript responseScript, List onChosenArgs,
                VisibilityScript visibility, List visibilityArgs)
        {
            this.text = text;
            this.leadsTo = leadsTo;
            this.tooltip = tooltip;
            this.responseScript = responseScript;
            this.onChosenArgs = onChosenArgs;
            this.visibility = visibility;
            this.visibilityArgs = visibilityArgs;
        }

        public Response(String text, String leadsTo)
        {
            this(text, leadsTo, null, null, null, null, null);
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
                return visibility.getVisibility(talkingTo, visibilityArgs);
            }

            return Visibility.VISIBLE;
        }

        List getVisibilityArgs()
        {
            return visibilityArgs;
        }

        ResponseScript getResponseScript()
        {
            return responseScript;
        }

        List getOnChosenArgs()
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
