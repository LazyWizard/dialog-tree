package org.lazywizard.conversation;

class Constants
{
    static final String MOD_ID = "lw_dialog";

    static final String CSV_PATH = "data/conv/conversations.csv";
    static final String CSV_COLUMN_ID = "id";
    static final String CSV_COLUMN_PATH = "filePath";

    static final String CONV_STARTING_NODE = "startingNode";
    static final String CONV_NODES = "nodes";
    static final String CONV_SCRIPT = "convScript";

    static final String NODE_TEXT = "text";
    static final String NODE_RESPONSES = "responses";
    static final String NODE_SCRIPT = "nodeScript";

    static final String RESPONSE_TEXT = "text";
    static final String RESPONSE_TOOLTIP = "tooltip";
    static final String RESPONSE_LEADS_TO = "leadsTo";
    static final String RESPONSE_VISIBILITY = "visibilityScript";
    static final String RESPONSE_VISIBILITY_ARGS = "visibilityArgs";
    static final String RESPONSE_SCRIPT = "responseScript";
    static final String RESPONSE_ON_CHOSEN_ARGS = "onChosenArgs";

    private Constants()
    {
    }
}
