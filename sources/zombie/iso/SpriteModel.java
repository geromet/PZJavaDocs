/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import org.joml.Vector3f;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.core.math.PZMath;
import zombie.scripting.ScriptManager;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.scripting.objects.BaseScriptObject;
import zombie.scripting.objects.ModelAttachment;
import zombie.scripting.objects.ModelScript;
import zombie.util.StringUtils;

@UsedFromLua
public final class SpriteModel
extends BaseScriptObject {
    public String modelScriptName;
    public String textureName;
    public final Vector3f translate = new Vector3f();
    public final Vector3f rotate = new Vector3f();
    public float scale = 1.0f;
    public String animationName;
    public float animationTime = -1.0f;
    public String runtimeString;

    public SpriteModel() {
        super(ScriptType.SpriteModel);
    }

    protected SpriteModel(ScriptType type) {
        super(type);
    }

    @Override
    public void Load(String name, String totalFile) throws Exception {
        ScriptParser.Block block = ScriptParser.parse(totalFile);
        block = block.children.get(0);
        this.LoadCommonBlock(block);
        this.modelScriptName = block.getValue("modelScript").getValue().trim();
        ScriptParser.Value value = block.getValue("texture");
        if (value != null) {
            this.textureName = StringUtils.discardNullOrWhitespace(value.getValue().trim());
        }
        this.parseVector3f(block.getValue("translate").getValue().trim(), this.translate);
        this.parseVector3f(block.getValue("rotate").getValue().trim(), this.rotate);
        this.scale = PZMath.tryParseFloat(block.getValue("scale").getValue().trim(), 1.0f);
        value = block.getValue("animation");
        if (value != null) {
            String animationName = value.getValue().trim();
            this.animationName = StringUtils.discardNullOrWhitespace(animationName);
        }
        if ((value = block.getValue("animationTime")) != null) {
            String animationTime = value.getValue();
            this.animationTime = PZMath.tryParseFloat(animationTime, -1.0f);
        }
        for (ScriptParser.Block child : block.children) {
            if (!"xxx".equals(child.type)) continue;
        }
    }

    void parseVector3f(String str, Vector3f v) {
        String[] ss = str.split(" ");
        v.setComponent(0, PZMath.tryParseFloat(ss[0], 0.0f));
        v.setComponent(1, PZMath.tryParseFloat(ss[1], 0.0f));
        v.setComponent(2, PZMath.tryParseFloat(ss[2], 0.0f));
    }

    public SpriteModel set(SpriteModel other) {
        this.modelScriptName = other.modelScriptName;
        this.textureName = other.textureName;
        this.translate.set(other.translate);
        this.rotate.set(other.rotate);
        this.scale = other.scale;
        this.animationName = other.animationName;
        this.animationTime = other.animationTime;
        return this;
    }

    public String getModelScriptName() {
        return this.modelScriptName;
    }

    public void setModelScriptName(String modelScriptName) {
        this.modelScriptName = modelScriptName;
    }

    public String getTextureName() {
        return this.textureName;
    }

    public void setTextureName(String textureName) {
        this.textureName = StringUtils.discardNullOrWhitespace(textureName);
    }

    public Vector3f getTranslate() {
        return this.translate;
    }

    public Vector3f getRotate() {
        return this.rotate;
    }

    public float getScale() {
        return this.scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public String getAnimationName() {
        return this.animationName;
    }

    public void setAnimationName(String animationName) {
        this.animationName = animationName;
    }

    public float getAnimationTime() {
        return this.animationTime;
    }

    public void setAnimationTime(float animationTime) {
        this.animationTime = animationTime;
    }

    public String getRuntimeString() {
        return this.runtimeString;
    }

    public void setRuntimeString(String runtimeString) {
        this.runtimeString = StringUtils.discardNullOrWhitespace(runtimeString);
    }

    public void parseRuntimeString(String tilesetName, int tileColumn, int tileRow, String runtimeString) throws RuntimeException {
        if (runtimeString == null) {
            return;
        }
        if (runtimeString.contains("standard_door")) {
            this.parseStandardDoor(tilesetName, tileColumn, tileRow, runtimeString);
        } else if (runtimeString.contains("pair_door")) {
            this.parsePairDoor(tilesetName, tileColumn, tileRow, runtimeString);
        }
    }

    void parseStandardDoor(String tilesetName, int tileColumn, int tileRow, String runtimeString) throws RuntimeException {
        int textureIndex;
        String[] ss = runtimeString.trim().split(" ");
        if (!"standard_door".equalsIgnoreCase(ss[0])) {
            throw new RuntimeException("expected \"standard_door\" but got \"%s\"".formatted(runtimeString));
        }
        String edge = ss[1];
        String state = ss[2];
        int tileIndex = tileColumn + tileRow * 8;
        int firstDoor = tileColumn / 4 * 4;
        String meshName = switch (edge) {
            case "w" -> {
                switch (state) {
                    case "closed": {
                        textureIndex = firstDoor + 2 + tileRow * 8;
                        this.modelScriptName = String.format("Base.%s_%d", tilesetName, tileIndex);
                        this.translate.set(-0.469f, 0.0f, -0.375f);
                        this.rotate.set(0.0f, 90.0f, 0.0f);
                        this.scale = 0.6666667f;
                        this.animationName = "Open";
                        this.animationTime = 0.0f;
                        yield "IsoObject/door_w_se.glb";
                    }
                    case "open": {
                        textureIndex = firstDoor + 2 + tileRow * 8;
                        this.modelScriptName = String.format("Base.%s_%d", tilesetName, tileIndex);
                        this.translate.set(-0.469f, 0.0f, -0.375f);
                        this.rotate.set(0.0f, 90.0f, 0.0f);
                        this.scale = 0.6666667f;
                        this.animationName = "Close";
                        this.animationTime = 0.0f;
                        yield "IsoObject/door_w_se.glb";
                    }
                }
                throw new RuntimeException("invalid SpriteModel runtime string \"%s\"".formatted(runtimeString));
            }
            case "n" -> {
                switch (state) {
                    case "closed": {
                        textureIndex = firstDoor + 2 + tileRow * 8;
                        this.modelScriptName = String.format("Base.%s_%d", tilesetName, tileIndex);
                        this.translate.set(-0.383f, 0.0f, -0.445f);
                        this.rotate.set(0.0f, 0.0f, 0.0f);
                        this.scale = 0.6666667f;
                        this.animationName = "Open";
                        this.animationTime = 0.0f;
                        yield "IsoObject/door_n_sw.glb";
                    }
                    case "open": {
                        textureIndex = firstDoor + 2 + tileRow * 8;
                        this.modelScriptName = String.format("Base.%s_%d", tilesetName, tileIndex);
                        this.translate.set(-0.383f, 0.0f, -0.445f);
                        this.rotate.set(0.0f, 0.0f, 0.0f);
                        this.scale = 0.6666667f;
                        this.animationName = "Close";
                        this.animationTime = 0.0f;
                        yield "IsoObject/door_n_sw.glb";
                    }
                }
                throw new RuntimeException("invalid SpriteModel runtime string \"%s\"".formatted(runtimeString));
            }
            default -> throw new RuntimeException("invalid SpriteModel runtime string \"%s\"".formatted(runtimeString));
        };
        this.runtimeString = runtimeString.trim();
        ModelScript modelScript = this.createDoorModelScriptIfNeeded(tilesetName, textureIndex, meshName);
        if (modelScript == null) {
            return;
        }
        ModelAttachment modelAttachment = modelScript.getAttachmentById("curtain1");
        if (modelAttachment == null) {
            modelAttachment = new ModelAttachment("curtain1");
            modelScript.addAttachment(modelAttachment);
        }
        modelAttachment.setBone("DoorBone");
        if ("n".equalsIgnoreCase(edge)) {
            modelAttachment.getOffset().set(-0.3997f, 1.8943f, 0.0f);
            modelAttachment.getRotate().set(180.0f, 0.0f, 180.0f);
        }
        if ("w".equalsIgnoreCase(edge)) {
            modelAttachment.getOffset().set(0.3997f, 1.8943f, 0.0f);
            modelAttachment.getRotate().set(180.0f, 0.0f, 180.0f);
        }
        if ((modelAttachment = modelScript.getAttachmentById("curtain2")) == null) {
            modelAttachment = new ModelAttachment("curtain2");
            modelScript.addAttachment(modelAttachment);
        }
        modelAttachment.setBone("DoorBone");
        if ("n".equalsIgnoreCase(edge)) {
            modelAttachment.getOffset().set(-0.3997f, 1.8943f, -0.0581f);
            modelAttachment.getRotate().set(0.0f, 0.0f, 0.0f);
        }
        if ("w".equalsIgnoreCase(edge)) {
            modelAttachment.getOffset().set(0.3997f, 1.8943f, -0.0585f);
            modelAttachment.getRotate().set(0.0f, 0.0f, 0.0f);
        }
    }

    void parsePairDoor(String tilesetName, int tileColumn, int tileRow, String runtimeString) throws RuntimeException {
        int textureIndex;
        String[] ss = runtimeString.trim().split(" ");
        if (!"pair_door".equalsIgnoreCase(ss[0])) {
            throw new RuntimeException("expected \"pair_door\" but got \"%s\"".formatted(ss[0]));
        }
        String leftRight = ss[1];
        String edge = ss[2];
        String state = ss[3];
        int tileIndex = tileColumn + tileRow * 8;
        String meshName = switch (edge) {
            case "w" -> {
                if ("left".equalsIgnoreCase(leftRight)) {
                    switch (state) {
                        case "closed": {
                            textureIndex = 4 + tileRow * 8;
                            this.modelScriptName = String.format("Base.%s_%d", tilesetName, tileIndex);
                            this.translate.set(-0.492f, 0.0f, -0.1f);
                            this.rotate.set(0.0f, 90.0f, 0.0f);
                            this.scale = 0.6666667f;
                            this.animationName = "Open";
                            this.animationTime = 0.0f;
                            yield "IsoObject/door_pair_left_w.glb";
                        }
                        case "open": {
                            textureIndex = 2 + tileRow * 8;
                            this.modelScriptName = String.format("Base.%s_%d", tilesetName, tileIndex);
                            this.translate.set(-0.492f, 0.0f, -0.1f);
                            this.rotate.set(0.0f, 90.0f, 0.0f);
                            this.scale = 0.6666667f;
                            this.animationName = "Close";
                            this.animationTime = 0.0f;
                            yield "IsoObject/door_pair_left_w_open.glb";
                        }
                    }
                    throw new RuntimeException("invalid SpriteModel runtime string \"%s\"".formatted(runtimeString));
                }
                if ("right".equalsIgnoreCase(leftRight)) {
                    switch (state) {
                        case "closed": {
                            textureIndex = 7 + tileRow * 8;
                            this.modelScriptName = String.format("Base.%s_%d", tilesetName, tileIndex);
                            this.translate.set(-0.49f, 0.0f, 0.1f);
                            this.rotate.set(0.0f, 90.0f, 0.0f);
                            this.scale = 0.6666667f;
                            this.animationName = "Open";
                            this.animationTime = 0.0f;
                            yield "IsoObject/door_pair_right_w.glb";
                        }
                        case "open": {
                            textureIndex = 7 + tileRow * 8;
                            this.modelScriptName = String.format("Base.%s_%d", tilesetName, tileIndex);
                            this.translate.set(-0.49f, 0.0f, 0.1f);
                            this.rotate.set(0.0f, 90.0f, 0.0f);
                            this.scale = 0.6666667f;
                            this.animationName = "Close";
                            this.animationTime = 0.0f;
                            yield "IsoObject/door_pair_right_w.glb";
                        }
                    }
                    throw new RuntimeException("invalid SpriteModel runtime string \"%s\"".formatted(runtimeString));
                }
                throw new RuntimeException("invalid SpriteModel runtime string \"%s\"".formatted(runtimeString));
            }
            case "n" -> {
                if ("left".equalsIgnoreCase(leftRight)) {
                    switch (state) {
                        case "closed": {
                            textureIndex = tileColumn + 1 + tileRow * 8;
                            this.modelScriptName = String.format("Base.%s_%d", tilesetName, tileIndex);
                            this.translate.set(0.1f, 0.0f, -0.47f);
                            this.rotate.set(0.0f, 0.0f, 0.0f);
                            this.scale = 0.6666667f;
                            this.animationName = "Open";
                            this.animationTime = 0.0f;
                            yield "IsoObject/door_pair_left_n.glb";
                        }
                        case "open": {
                            textureIndex = tileColumn - 1 + tileRow * 8;
                            this.modelScriptName = String.format("Base.%s_%d", tilesetName, tileIndex);
                            this.translate.set(0.1f, 0.0f, -0.47f);
                            this.rotate.set(0.0f, 0.0f, 0.0f);
                            this.scale = 0.6666667f;
                            this.animationName = "Close";
                            this.animationTime = 0.0f;
                            yield "IsoObject/door_pair_left_n.glb";
                        }
                    }
                    throw new RuntimeException("invalid SpriteModel runtime string \"%s\"".formatted(runtimeString));
                }
                if ("right".equalsIgnoreCase(leftRight)) {
                    switch (state) {
                        case "closed": {
                            textureIndex = 5 + tileRow * 8;
                            this.modelScriptName = String.format("Base.%s_%d", tilesetName, tileIndex);
                            this.translate.set(-0.117f, 0.0f, -0.47f);
                            this.rotate.set(0.0f, 0.0f, 0.0f);
                            this.scale = 0.6666667f;
                            this.animationName = "Open";
                            this.animationTime = 0.0f;
                            yield "IsoObject/door_pair_right_n.glb";
                        }
                        case "open": {
                            textureIndex = 7 + tileRow * 8;
                            this.modelScriptName = String.format("Base.%s_%d", tilesetName, tileIndex);
                            this.translate.set(-0.117f, 0.0f, -0.47f);
                            this.rotate.set(0.0f, 0.0f, 0.0f);
                            this.scale = 0.6666667f;
                            this.animationName = "Close";
                            this.animationTime = 0.0f;
                            yield "IsoObject/door_pair_right_n_open.glb";
                        }
                    }
                    throw new RuntimeException("invalid SpriteModel runtime string \"%s\"".formatted(runtimeString));
                }
                throw new RuntimeException("invalid SpriteModel runtime string \"%s\"".formatted(runtimeString));
            }
            default -> throw new RuntimeException("invalid SpriteModel runtime string \"%s\"".formatted(runtimeString));
        };
        this.runtimeString = runtimeString.trim();
        ModelScript modelScript = this.createDoorModelScriptIfNeeded(tilesetName, textureIndex, meshName);
        if (modelScript == null) {
            throw new RuntimeException("invalid SpriteModel runtime string \"%s\"".formatted(runtimeString));
        }
        ModelAttachment modelAttachment = modelScript.getAttachmentById("curtain1");
        if (modelAttachment == null) {
            modelAttachment = new ModelAttachment("curtain1");
            modelScript.addAttachment(modelAttachment);
        }
        modelAttachment.setBone("DoorBone");
        if ("n".equalsIgnoreCase(edge) && "left".equalsIgnoreCase(leftRight)) {
            modelAttachment.getOffset().set(0.0f, 1.8943f, 0.027928859f);
            modelAttachment.getRotate().set(180.0f, 0.0f, 180.0f);
        } else if ("n".equalsIgnoreCase(edge) && "right".equalsIgnoreCase(leftRight)) {
            modelAttachment.getOffset().set(-0.0, (double)1.8943f, 0.028792206);
            modelAttachment.getRotate().set(180.0f, 0.0f, 180.0f);
        } else if ("w".equalsIgnoreCase(edge) && "left".equalsIgnoreCase(leftRight)) {
            modelAttachment.getOffset().set(0.0f, 1.8943f, 0.02863244f);
            modelAttachment.getRotate().set(180.0f, 0.0f, 180.0f);
        } else if ("w".equalsIgnoreCase(edge) && "right".equalsIgnoreCase(leftRight)) {
            modelAttachment.getOffset().set(0.0f, 1.8943f, 0.02863244f);
            modelAttachment.getRotate().set(180.0f, 0.0f, 180.0f);
        }
        modelAttachment = modelScript.getAttachmentById("curtain2");
        if (modelAttachment == null) {
            modelAttachment = new ModelAttachment("curtain2");
            modelScript.addAttachment(modelAttachment);
        }
        modelAttachment.setBone("DoorBone");
        if ("n".equalsIgnoreCase(edge) && "left".equalsIgnoreCase(leftRight)) {
            modelAttachment.getOffset().set(0.0f, 1.8943f, -0.027928859f);
            modelAttachment.getRotate().set(0.0f, 0.0f, 0.0f);
        } else if ("n".equalsIgnoreCase(edge) && "right".equalsIgnoreCase(leftRight)) {
            modelAttachment.getOffset().set(0.0f, 1.8943f, -0.027928859f);
            modelAttachment.getRotate().set(0.0f, 0.0f, 0.0f);
        } else if ("w".equalsIgnoreCase(edge) && "left".equalsIgnoreCase(leftRight)) {
            modelAttachment.getOffset().set(0.0f, 1.8943f, -0.029316932f);
            modelAttachment.getRotate().set(0.0f, 0.0f, 0.0f);
        } else if ("w".equalsIgnoreCase(edge) && "right".equalsIgnoreCase(leftRight)) {
            modelAttachment.getOffset().set(0.0f, 1.8943f, -0.029316932f);
            modelAttachment.getRotate().set(0.0f, 0.0f, 0.0f);
        }
    }

    ModelScript createDoorModelScriptIfNeeded(String tilesetName, int textureIndex, String meshName) {
        ModelScript modelScript = ScriptManager.instance.getModelScript(this.getModelScriptName());
        if (modelScript == null) {
            modelScript = new ModelScript();
            modelScript.setModule(ScriptManager.instance.getModule("Base"));
            modelScript.InitLoadPP(this.getModelScriptName().substring("Base.".length()));
            ScriptManager.instance.addModelScript(modelScript);
        }
        modelScript.fileName = ZomboidFileSystem.instance.getMediaPath("scripts/models_runtime.txt");
        modelScript.name = this.getModelScriptName().substring("Base.".length());
        modelScript.meshName = meshName;
        modelScript.textureName = String.format("%s_%d", tilesetName, textureIndex);
        modelScript.scale = meshName.endsWith(".glb") ? 1.0f : 0.01f;
        modelScript.shaderName = "door";
        modelScript.isStatic = false;
        int p1 = meshName.lastIndexOf(47) + 1;
        int p2 = meshName.lastIndexOf(46);
        if (p2 == -1) {
            p2 = meshName.length();
        }
        modelScript.animationsMesh = meshName.substring(p1, p2);
        modelScript.loadedModel = null;
        return modelScript;
    }
}

