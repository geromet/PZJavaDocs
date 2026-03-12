/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.BodyDamage;

import java.util.ArrayList;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.debug.DebugLog;

public final class BodyPartContacts {
    private static final ContactNode root;
    private static final ContactNode[] nodes;

    public static BodyPartType[] getAllContacts(BodyPartType bodyPartType) {
        for (int i = 0; i < nodes.length; ++i) {
            ContactNode node = nodes[i];
            if (node.bodyPart != bodyPartType) continue;
            return node.bodyPartAllContacts;
        }
        return null;
    }

    public static BodyPartType[] getChildren(BodyPartType bodyPartType) {
        for (int i = 0; i < nodes.length; ++i) {
            ContactNode node = nodes[i];
            if (node.bodyPart != bodyPartType) continue;
            return node.bodyPartChildren;
        }
        return null;
    }

    public static BodyPartType getParent(BodyPartType bodyPartType) {
        for (int i = 0; i < nodes.length; ++i) {
            ContactNode node = nodes[i];
            if (node.bodyPart != bodyPartType) continue;
            return node.bodyPartParent;
        }
        return null;
    }

    public static int getNodeDepth(BodyPartType bodyPartType) {
        for (int i = 0; i < nodes.length; ++i) {
            ContactNode node = nodes[i];
            if (node.bodyPart != bodyPartType) continue;
            if (!node.initialised) {
                DebugLog.log("Warning: attempting to get depth for non initialised node '" + node.bodyPart.toString() + "'.");
            }
            return node.depth;
        }
        return -1;
    }

    private static ContactNode getNodeForBodyPart(BodyPartType bodyPartType) {
        for (int i = 0; i < nodes.length; ++i) {
            if (BodyPartContacts.nodes[i].bodyPart != bodyPartType) continue;
            return nodes[i];
        }
        return null;
    }

    private static void initNodes(ContactNode current, int depth, ContactNode up) {
        current.parent = up;
        current.depth = depth;
        ArrayList<ContactNode> allContactsList = new ArrayList<ContactNode>();
        if (current.parent != null) {
            allContactsList.add(current.parent);
        }
        if (current.children != null) {
            for (ContactNode node : current.children) {
                allContactsList.add(node);
                BodyPartContacts.initNodes(node, depth + 1, current);
            }
        }
        current.allContacts = new ContactNode[allContactsList.size()];
        allContactsList.toArray(current.allContacts);
        current.initialised = true;
    }

    private static void postInit() {
        for (ContactNode node : nodes) {
            int i;
            if (node.parent != null) {
                node.bodyPartParent = node.parent.bodyPart;
            }
            if (node.children != null && node.children.length > 0) {
                node.bodyPartChildren = new BodyPartType[node.children.length];
                for (i = 0; i < node.children.length; ++i) {
                    node.bodyPartChildren[i] = node.children[i].bodyPart;
                }
            } else {
                node.bodyPartChildren = new BodyPartType[0];
            }
            if (node.allContacts != null && node.allContacts.length > 0) {
                node.bodyPartAllContacts = new BodyPartType[node.allContacts.length];
                for (i = 0; i < node.allContacts.length; ++i) {
                    node.bodyPartAllContacts[i] = node.allContacts[i].bodyPart;
                }
            } else {
                node.bodyPartAllContacts = new BodyPartType[0];
            }
            if (node.initialised) continue;
            DebugLog.log("Warning: node for '" + node.bodyPart.toString() + "' is not initialised!");
        }
    }

    static {
        int max = BodyPartType.ToIndex(BodyPartType.MAX);
        nodes = new ContactNode[max];
        for (int i = 0; i < max; ++i) {
            BodyPartContacts.nodes[i] = new ContactNode(BodyPartType.FromIndex(i));
        }
        root = BodyPartContacts.getNodeForBodyPart(BodyPartType.Torso_Upper);
        BodyPartContacts.root.children = new ContactNode[]{BodyPartContacts.getNodeForBodyPart(BodyPartType.Neck), BodyPartContacts.getNodeForBodyPart(BodyPartType.Torso_Lower), BodyPartContacts.getNodeForBodyPart(BodyPartType.UpperArm_L), BodyPartContacts.getNodeForBodyPart(BodyPartType.UpperArm_R)};
        ContactNode node = BodyPartContacts.getNodeForBodyPart(BodyPartType.Neck);
        node.children = new ContactNode[]{BodyPartContacts.getNodeForBodyPart(BodyPartType.Head)};
        node = BodyPartContacts.getNodeForBodyPart(BodyPartType.UpperArm_L);
        node.children = new ContactNode[]{BodyPartContacts.getNodeForBodyPart(BodyPartType.ForeArm_L)};
        node = BodyPartContacts.getNodeForBodyPart(BodyPartType.ForeArm_L);
        node.children = new ContactNode[]{BodyPartContacts.getNodeForBodyPart(BodyPartType.Hand_L)};
        node = BodyPartContacts.getNodeForBodyPart(BodyPartType.UpperArm_R);
        node.children = new ContactNode[]{BodyPartContacts.getNodeForBodyPart(BodyPartType.ForeArm_R)};
        node = BodyPartContacts.getNodeForBodyPart(BodyPartType.ForeArm_R);
        node.children = new ContactNode[]{BodyPartContacts.getNodeForBodyPart(BodyPartType.Hand_R)};
        node = BodyPartContacts.getNodeForBodyPart(BodyPartType.Torso_Lower);
        node.children = new ContactNode[]{BodyPartContacts.getNodeForBodyPart(BodyPartType.Groin)};
        node = BodyPartContacts.getNodeForBodyPart(BodyPartType.Groin);
        node.children = new ContactNode[]{BodyPartContacts.getNodeForBodyPart(BodyPartType.UpperLeg_L), BodyPartContacts.getNodeForBodyPart(BodyPartType.UpperLeg_R)};
        node = BodyPartContacts.getNodeForBodyPart(BodyPartType.UpperLeg_L);
        node.children = new ContactNode[]{BodyPartContacts.getNodeForBodyPart(BodyPartType.LowerLeg_L)};
        node = BodyPartContacts.getNodeForBodyPart(BodyPartType.LowerLeg_L);
        node.children = new ContactNode[]{BodyPartContacts.getNodeForBodyPart(BodyPartType.Foot_L)};
        node = BodyPartContacts.getNodeForBodyPart(BodyPartType.UpperLeg_R);
        node.children = new ContactNode[]{BodyPartContacts.getNodeForBodyPart(BodyPartType.LowerLeg_R)};
        node = BodyPartContacts.getNodeForBodyPart(BodyPartType.LowerLeg_R);
        node.children = new ContactNode[]{BodyPartContacts.getNodeForBodyPart(BodyPartType.Foot_R)};
        BodyPartContacts.initNodes(root, 0, null);
        BodyPartContacts.postInit();
    }

    private static class ContactNode {
        BodyPartType bodyPart;
        int depth = -1;
        ContactNode parent;
        ContactNode[] children;
        ContactNode[] allContacts;
        BodyPartType bodyPartParent;
        BodyPartType[] bodyPartChildren;
        BodyPartType[] bodyPartAllContacts;
        boolean initialised;

        public ContactNode(BodyPartType bodyPart) {
            this.bodyPart = bodyPart;
        }
    }
}

