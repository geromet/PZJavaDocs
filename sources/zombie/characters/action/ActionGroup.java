/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.action;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import zombie.ZomboidFileSystem;
import zombie.characters.action.ActionState;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;

public final class ActionGroup {
    private String name;
    private String initialStateName;
    private final List<ActionState> states = new ArrayList<ActionState>();
    private final Map<String, ActionState> stateLookup = new HashMap<String, ActionState>();
    private final Map<Integer, String> stateNameLookup = new HashMap<Integer, String>();
    private static final Map<String, ActionGroup> s_actionGroupMap = new HashMap<String, ActionGroup>();

    private void load() {
        File folder;
        File[] listOfFiles;
        String name = this.name;
        DebugType.ActionSystem.debugln("Loading ActionGroup: %s", name);
        File actionGroupFile = ZomboidFileSystem.instance.getMediaFile("actiongroups/" + name + "/actionGroup.xml");
        if (actionGroupFile.exists() && actionGroupFile.canRead()) {
            this.loadGroupData(actionGroupFile);
        }
        if ((listOfFiles = (folder = ZomboidFileSystem.instance.getMediaFile("actiongroups/" + name)).listFiles()) != null) {
            for (File stateFolder : listOfFiles) {
                if (!stateFolder.isDirectory()) continue;
                ActionState state = this.getOrCreate(stateFolder.getName());
                String statepath = stateFolder.getPath();
                state.load(statepath);
            }
        }
    }

    private void loadGroupData(File groupDataFile) {
        Document doc;
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(groupDataFile);
        }
        catch (IOException | ParserConfigurationException | SAXException e) {
            DebugType.ActionSystem.printException(e, "Error loading: " + groupDataFile.getPath(), LogSeverity.Error);
            return;
        }
        doc.getDocumentElement().normalize();
        Element elem = doc.getDocumentElement();
        if (!elem.getNodeName().equals("actiongroup")) {
            DebugType.ActionSystem.error("Error loading: " + groupDataFile.getPath() + ", expected root element '<actiongroup>', received '<" + elem.getNodeName() + ">'");
            return;
        }
        for (Node child = elem.getFirstChild(); child != null; child = child.getNextSibling()) {
            Element childElem;
            if (!(child instanceof Element) || !(childElem = (Element)child).getNodeName().equals("initial")) continue;
            this.initialStateName = childElem.getTextContent().trim();
        }
    }

    public ActionState addState(ActionState state) {
        if (this.states.contains(state)) {
            DebugType.ActionSystem.trace("State already added.");
            return state;
        }
        state.setParentActionGroup(this);
        this.states.add(state);
        this.stateLookup.put(state.getName().toLowerCase(), state);
        this.stateNameLookup.put(state.getName().hashCode(), state.getName());
        return state;
    }

    public ActionState findState(String stateName) {
        return this.stateLookup.get(stateName.toLowerCase());
    }

    public ActionState getOrCreate(String stateName) {
        ActionState state = this.findState(stateName = stateName.toLowerCase());
        if (state == null) {
            state = this.addState(new ActionState(stateName));
        }
        return state;
    }

    public ActionState getInitialState() {
        ActionState state = null;
        if (this.initialStateName != null) {
            state = this.findState(this.initialStateName);
        }
        if (state == null && !this.states.isEmpty()) {
            state = this.states.get(0);
        }
        return state;
    }

    public ActionState getDefaultState() {
        return this.getInitialState();
    }

    public String getName() {
        return this.name;
    }

    public static ActionGroup getActionGroup(String groupName) {
        ActionGroup grp = s_actionGroupMap.get(groupName = groupName.toLowerCase());
        if (grp != null || s_actionGroupMap.containsKey(groupName)) {
            return grp;
        }
        grp = new ActionGroup();
        grp.name = groupName;
        s_actionGroupMap.put(groupName, grp);
        try {
            grp.load();
        }
        catch (Exception e) {
            DebugType.ActionSystem.printException(e, "Error loading action group: " + groupName, LogSeverity.Error);
        }
        return grp;
    }

    public static void reloadAll() {
        for (Map.Entry<String, ActionGroup> entry : s_actionGroupMap.entrySet()) {
            ActionGroup actionGroup = entry.getValue();
            for (ActionState state : actionGroup.states) {
                state.resetForReload();
            }
            actionGroup.load();
        }
    }
}

