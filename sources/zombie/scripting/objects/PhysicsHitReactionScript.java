/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import zombie.core.Translator;
import zombie.core.physics.RagdollBodyPart;
import zombie.debug.DebugLog;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.scripting.objects.AmmoType;
import zombie.scripting.objects.BaseScriptObject;
import zombie.scripting.objects.PhysicsHitReaction;
import zombie.scripting.objects.Registries;
import zombie.scripting.objects.ResourceLocation;
import zombie.util.StringUtils;

public final class PhysicsHitReactionScript
extends BaseScriptObject {
    private static String originalFilename;
    public static final ArrayList<PhysicsHitReaction> physicsHitReactionList;

    public PhysicsHitReactionScript() {
        super(ScriptType.PhysicsHitReaction);
    }

    @Override
    public void InitLoadPP(String name) {
        super.InitLoadPP(name);
        ScriptManager scriptMgr = ScriptManager.instance;
        originalFilename = scriptMgr.currentFileName;
    }

    @Override
    public void Load(String name, String totalFile) throws Exception {
        ScriptParser.Block block = ScriptParser.parse(totalFile);
        block = block.children.get(0);
        this.LoadCommonBlock(block);
        this.loadPhysicsHitReaction(block);
    }

    private void loadPhysicsHitReaction(ScriptParser.Block block) {
        int impulseIndex = 0;
        int upImpulseIndex = 0;
        PhysicsHitReaction physicsHitReaction = new PhysicsHitReaction();
        physicsHitReaction.ammoType = AmmoType.get(ResourceLocation.of(block.id));
        if (physicsHitReaction.ammoType == null) {
            physicsHitReaction.physicsObject = ScriptManager.instance.resolveItemType(this.getModule(), block.id);
        }
        for (ScriptParser.BlockElement element : block.elements) {
            if (element.asValue() == null) continue;
            String[] ss = element.asValue().string.split("=");
            String k = ss[0].trim();
            String v = ss[1].trim();
            if (k.contains("impulse_")) {
                physicsHitReaction.impulse[impulseIndex++] = Float.parseFloat(v);
                continue;
            }
            if (!k.contains("upwardImpulse_")) continue;
            physicsHitReaction.upwardImpulse[upImpulseIndex++] = Float.parseFloat(v);
        }
        physicsHitReactionList.add(physicsHitReaction);
    }

    public static void writeToFile() {
        Object filename = originalFilename;
        String userDirectory = System.getProperty("user.dir");
        if (userDirectory != null) {
            filename = userDirectory + "\\" + originalFilename;
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter((String)filename));){
            writer.write("module Base \n{\n");
            for (PhysicsHitReaction physicsHitReaction : physicsHitReactionList) {
                int i;
                String id = physicsHitReaction.ammoType != null ? Registries.AMMO_TYPE.getLocation(physicsHitReaction.ammoType).getPath() : StringUtils.stripModule(physicsHitReaction.physicsObject);
                writer.write("    physicsHitReaction " + id + "\n    {\n");
                for (i = 0; i < physicsHitReaction.impulse.length; ++i) {
                    writer.write("        impulse_" + Translator.getText(RagdollBodyPart.values()[i].name()).replace(" ", "_").toLowerCase() + " = " + physicsHitReaction.impulse[i] + ",\n");
                }
                for (i = 0; i < physicsHitReaction.upwardImpulse.length; ++i) {
                    writer.write("        upwardImpulse_" + Translator.getText(RagdollBodyPart.values()[i].name()).replace(" ", "_").toLowerCase() + " = " + physicsHitReaction.upwardImpulse[i] + ",\n");
                }
                writer.write("    }\n\n");
            }
            writer.write("}\n");
        }
        catch (Exception e) {
            DebugLog.Script.error(e);
        }
    }

    public static float getImpulse(RagdollBodyPart bodyPart, String physicsObject) {
        for (PhysicsHitReaction reaction : physicsHitReactionList) {
            if (!StringUtils.equals(physicsObject, reaction.physicsObject)) continue;
            if (reaction.useImpulseOverride) {
                return reaction.overrideForwardImpulse;
            }
            return reaction.impulse[bodyPart.ordinal()];
        }
        return 80.0f;
    }

    public static float getImpulse(RagdollBodyPart bodyPart, AmmoType ammoType) {
        for (PhysicsHitReaction reaction : physicsHitReactionList) {
            if (ammoType != reaction.ammoType) continue;
            if (reaction.useImpulseOverride) {
                return reaction.overrideForwardImpulse;
            }
            return reaction.impulse[bodyPart.ordinal()];
        }
        return 80.0f;
    }

    public static float getUpwardImpulse(RagdollBodyPart bodyPart, AmmoType ammoType) {
        for (PhysicsHitReaction reaction : physicsHitReactionList) {
            if (ammoType != reaction.ammoType) continue;
            if (reaction.useImpulseOverride) {
                return reaction.overrideUpwardImpulse;
            }
            return reaction.upwardImpulse[bodyPart.ordinal()];
        }
        return 40.0f;
    }

    public static float getUpwardImpulse(RagdollBodyPart bodyPart, String physicsObject) {
        for (PhysicsHitReaction reaction : physicsHitReactionList) {
            if (!StringUtils.equals(physicsObject, reaction.physicsObject)) continue;
            if (reaction.useImpulseOverride) {
                return reaction.overrideUpwardImpulse;
            }
            return reaction.upwardImpulse[bodyPart.ordinal()];
        }
        return 40.0f;
    }

    public static void Reset() {
        physicsHitReactionList.clear();
    }

    static {
        physicsHitReactionList = new ArrayList();
    }
}

