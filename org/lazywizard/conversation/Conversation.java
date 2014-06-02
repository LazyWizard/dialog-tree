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

// TODO: Much more commenting, better logging
public final class Conversation
{
    private final Map<String, Node> nodes;
    private Node startingNode;

    public Conversation()
    {
        nodes = new HashMap<>();
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
        return new HashMap<>(nodes);
    }

    public Node getStartingNode()
    {
        return startingNode;
    }

    public void setStartingNode(Node startingNode)
    {
        this.startingNode = startingNode;
    }

    public static final class Node
    {
        private final String text;
        private final Set<Response> responses;
        private Conversation parentConv;

        public Node(String text, List<Response> responses)
        {
            this.text = text;
            this.responses = new LinkedHashSet<>();

            for (Response tmp : responses)
            {
                addResponse(tmp);
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

        public List<Response> getResponses()
        {
            return new ArrayList<>(responses);
        }

        public Conversation getConversation()
        {
            return parentConv;
        }
    }

    public static final class Response
    {
        private final String text, tooltip;
        private final String leadsTo;
        private final OnChosenScript onChosen;
        private final VisibilityScript visibility;
        private Node parentNode = null;

        public enum Visibility
        {
            HIDDEN,
            DISABLED,
            VISIBLE
        }

        public Response(String text, String leadsTo, String tooltip,
                OnChosenScript onChosen, VisibilityScript visibility)
        {
            this.text = text;
            this.leadsTo = leadsTo;
            this.tooltip = tooltip;
            this.onChosen = onChosen;
            this.visibility = visibility;
        }

        public Response(String text, String leadsTo)
        {
            this(text, leadsTo, null, null, null);
        }

        private void setParentNode(Node parentNode)
        {
            this.parentNode = parentNode;
        }

        void onChosen(SectorEntityToken talkingTo, ConversationDialog dialog)
        {
            Global.getLogger(Response.class).log(Level.DEBUG,
                    "Chose response: \"" + text + "\"\nLeads to: " + leadsTo);

            if (onChosen != null)
            {
                onChosen.onChosen(talkingTo, dialog);
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

        public Visibility getVisibility()
        {
            if (visibility != null)
            {
                return visibility.getVisibility();
            }

            return Visibility.VISIBLE;
        }

        public Node getParentNode()
        {
            return parentNode;
        }

        public String getDestination()
        {
            return leadsTo;
        }
    }
}
