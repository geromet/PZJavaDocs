/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.itemConfig.script;

import zombie.debug.DebugLog;
import zombie.scripting.ScriptParser;
import zombie.scripting.itemConfig.ItemConfig;
import zombie.scripting.itemConfig.enums.RootType;
import zombie.scripting.itemConfig.enums.SelectorType;
import zombie.scripting.itemConfig.enums.SituatedType;
import zombie.scripting.itemConfig.script.SelectorBucketScript;
import zombie.util.StringUtils;

public class BucketRootScript {
    private RootType type;
    private String id;
    private SelectorBucketScript defaultBucket;
    private SelectorBucketScript onCreateBucket;

    public RootType getType() {
        return this.type;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean idIsVariable() {
        return this.id != null && this.id.startsWith("$");
    }

    public SelectorBucketScript getDefaultBucket() {
        return this.defaultBucket;
    }

    public SelectorBucketScript getOnCreateBucket() {
        return this.onCreateBucket;
    }

    public BucketRootScript copy() {
        BucketRootScript other = new BucketRootScript();
        other.type = this.type;
        other.id = this.id;
        other.defaultBucket = this.defaultBucket.copy();
        other.onCreateBucket = this.onCreateBucket != null ? this.onCreateBucket.copy() : null;
        return other;
    }

    private void load(ScriptParser.Block block) throws ItemConfig.ItemConfigException {
        ItemConfig.errorRoot = block.id;
        this.type = RootType.valueOf(block.type.trim());
        String string = this.id = StringUtils.isNullOrWhitespace(block.id) ? block.id.trim() : null;
        if (this.type.isRequiresId() && this.id == null) {
            throw new ItemConfig.ItemConfigException("Root node with type '" + String.valueOf((Object)this.type) + "' requires id.");
        }
        for (ScriptParser.Block child : block.children) {
            if (child.type.equalsIgnoreCase("default")) {
                this.defaultBucket = this.loadSelectorBucket(child, null);
                continue;
            }
            if (!child.type.equalsIgnoreCase("oncreate")) continue;
            this.onCreateBucket = this.loadSelectorBucket(child, null);
        }
        if (this.defaultBucket == null) {
            this.defaultBucket = new SelectorBucketScript(SelectorType.Default);
        }
        for (ScriptParser.Block child : block.children) {
            if (child.type.equalsIgnoreCase("default") || child.type.equalsIgnoreCase("oncreate")) continue;
            SelectorBucketScript bucket = this.loadSelectorBucket(child, this.defaultBucket);
            this.defaultBucket.children.add(bucket);
        }
        ItemConfig.errorRoot = null;
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    private SelectorBucketScript loadSelectorBucket(ScriptParser.Block block, SelectorBucketScript parent) throws ItemConfig.ItemConfigException {
        SelectorType selectorType;
        ItemConfig.errorBucket = (block.type != null ? block.type : "null") + " " + (block.id != null ? block.id : "");
        SituatedType situatedType = SituatedType.None;
        int worldAgeDays = 0;
        String selectorString = null;
        if (block.type.equalsIgnoreCase("default")) {
            selectorType = SelectorType.Default;
            if (parent != null) {
                throw new ItemConfig.ItemConfigException("Default block may not be nested or defined twice.");
            }
        } else if (block.type.equalsIgnoreCase("oncreate")) {
            selectorType = SelectorType.OnCreate;
            if (parent != null) {
                throw new ItemConfig.ItemConfigException("OnCreate block may not be nested or defined twice.");
            }
        } else if (block.type.equalsIgnoreCase("situated")) {
            selectorType = SelectorType.Situated;
            if (StringUtils.isNullOrWhitespace(block.id)) {
                throw new ItemConfig.ItemConfigException("Block 'Situated' requires a parameter.");
            }
            if (block.id.equalsIgnoreCase("interior")) {
                situatedType = SituatedType.Interior;
            } else if (block.id.equalsIgnoreCase("exterior")) {
                situatedType = SituatedType.Exterior;
            } else if (block.id.equalsIgnoreCase("shop")) {
                situatedType = SituatedType.Shop;
            } else {
                if (!block.id.equalsIgnoreCase("junk")) throw new ItemConfig.ItemConfigException("Block 'Situated' requires a valid parameter.");
                situatedType = SituatedType.Junk;
            }
        } else if (block.type.equalsIgnoreCase("zone")) {
            selectorType = SelectorType.Zone;
            selectorString = block.id;
            if (StringUtils.isNullOrWhitespace(selectorString)) {
                throw new ItemConfig.ItemConfigException("Block 'Zone' requires a parameter.");
            }
        } else if (block.type.equalsIgnoreCase("vehicle")) {
            selectorType = SelectorType.Vehicle;
            selectorString = block.id;
            if (StringUtils.isNullOrWhitespace(selectorString)) {
                throw new ItemConfig.ItemConfigException("Block 'Vehicle' requires a parameter.");
            }
        } else if (block.type.equalsIgnoreCase("room")) {
            selectorType = SelectorType.Room;
            selectorString = block.id;
            if (StringUtils.isNullOrWhitespace(selectorString)) {
                throw new ItemConfig.ItemConfigException("Block 'Room' requires a parameter.");
            }
        } else if (block.type.equalsIgnoreCase("container")) {
            selectorType = SelectorType.Container;
            selectorString = block.id;
            if (StringUtils.isNullOrWhitespace(selectorString)) {
                throw new ItemConfig.ItemConfigException("Block 'Container' requires a parameter.");
            }
        } else if (block.type.equalsIgnoreCase("tile")) {
            selectorType = SelectorType.Tile;
            selectorString = block.id;
            if (StringUtils.isNullOrWhitespace(selectorString)) {
                throw new ItemConfig.ItemConfigException("Block 'Tile' requires a parameter.");
            }
        } else if (block.type.equalsIgnoreCase("worldagedays")) {
            selectorType = SelectorType.WorldAge;
            if (StringUtils.isNullOrWhitespace(block.id)) {
                throw new ItemConfig.ItemConfigException("Block 'WorldAgeDays' requires a parameter.");
            }
            worldAgeDays = Integer.parseInt(block.id);
            if (worldAgeDays < 0) {
                throw new ItemConfig.ItemConfigException("Block 'WorldAgeDays' requires a value greater than zero.");
            }
        } else {
            selectorType = SelectorType.None;
            if (!StringUtils.isNullOrWhitespace(block.id)) {
                DebugLog.General.warn("A custom block should not have a parameter, typo in block identifier? Block: " + block.type + " " + block.id);
            }
        }
        if (selectorType == SelectorType.Default && !block.children.isEmpty()) {
            throw new ItemConfig.ItemConfigException("Default block may not have any nested children.");
        }
        if (selectorType == SelectorType.OnCreate && !block.children.isEmpty()) {
            throw new ItemConfig.ItemConfigException("OnCreate block may not have any nested children.");
        }
        SelectorBucketScript bucket = new SelectorBucketScript(selectorType);
        bucket.selectorSituated = situatedType;
        bucket.selectorWorldAge = worldAgeDays;
        bucket.selectorString = selectorString;
        for (ScriptParser.Value value : block.values) {
            if (StringUtils.isNullOrWhitespace(value.string)) continue;
            ItemConfig.errorLine = value.string;
            bucket.randomizers.add(value.string.trim());
            ItemConfig.errorLine = null;
        }
        if (selectorType == SelectorType.OnCreate && bucket.randomizers.isEmpty()) {
            throw new ItemConfig.ItemConfigException("OnCreate block needs at least one randomizer parameter defined.");
        }
        if (selectorType == SelectorType.None && !bucket.randomizers.isEmpty()) {
            throw new ItemConfig.ItemConfigException("A custom container bucket may not have any randomizer parameters.");
        }
        for (ScriptParser.Block child : block.children) {
            SelectorBucketScript childBucket = this.loadSelectorBucket(child, bucket);
            bucket.children.add(childBucket);
        }
        return bucket;
    }

    public static BucketRootScript TryLoad(ScriptParser.Block block) throws ItemConfig.ItemConfigException {
        BucketRootScript script = new BucketRootScript();
        script.load(block);
        return script;
    }
}

