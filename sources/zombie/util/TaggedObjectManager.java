/*
 * Decompiled with CFR 0.152.
 */
package zombie.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import zombie.debug.DebugLog;
import zombie.entity.util.BitSet;
import zombie.scripting.objects.BaseScriptObject;
import zombie.util.StringUtils;
import zombie.util.list.PZUnmodifiableList;

public class TaggedObjectManager<T extends TaggedObject> {
    private static final String defaultTag = "untagged".toLowerCase();
    private final HashMap<String, Integer> tagStringToIndexMap = new HashMap();
    private final HashMap<Integer, String> tagIndexToStringMap = new HashMap();
    private final HashMap<String, List<T>> tagToObjectListMap = new HashMap();
    private final List<String> registeredTags = new ArrayList<String>();
    private final List<String> registeredTagsView = PZUnmodifiableList.wrap(this.registeredTags);
    private final HashMap<String, TagGroup<T>> tagGroupMap = new HashMap();
    private final List<TagGroup<T>> tagGroups = new ArrayList<TagGroup<T>>();
    private final HashMap<String, String> tagsStringAliasMap = new HashMap();
    private final List<T> emptyTagObjects = new ArrayList<T>();
    private final List<String> tempStringList = new ArrayList<String>();
    private final BackingListProvider<T> backingListProvider;
    private boolean verbose;
    private boolean warnNonPreprocessedNewTag = true;

    public TaggedObjectManager(BackingListProvider<T> backingListProvider) {
        this.backingListProvider = Objects.requireNonNull(backingListProvider);
        this.registerTag(defaultTag, false);
    }

    public void setVerbose(boolean b) {
        this.verbose = b;
    }

    public boolean isVerbose() {
        return this.verbose;
    }

    public void setWarnNonPreprocessedNewTag(boolean b) {
        this.warnNonPreprocessedNewTag = b;
    }

    public boolean isWarnNonPreprocessedNewTag() {
        return this.warnNonPreprocessedNewTag;
    }

    public void clear() {
        this.tagStringToIndexMap.clear();
        this.tagIndexToStringMap.clear();
        this.registeredTags.clear();
        this.tagGroupMap.clear();
        this.tagGroups.clear();
        this.tagsStringAliasMap.clear();
    }

    public void setDirty() {
        for (int i = 0; i < this.tagGroups.size(); ++i) {
            this.tagGroups.get((int)i).dirty = true;
        }
    }

    public List<String> getRegisteredTags() {
        return this.registeredTagsView;
    }

    public void getRegisteredTagGroups(ArrayList<String> list) {
        for (Map.Entry<String, TagGroup<T>> entry : this.tagGroupMap.entrySet()) {
            list.add(entry.getKey());
        }
    }

    public void registerObjectsFromBackingList() {
        this.registerObjectsFromBackingList(false);
    }

    public void registerObjectsFromBackingList(boolean clear) {
        if (this.verbose) {
            DebugLog.General.println("Registering objects from backing list...");
        }
        if (clear) {
            this.clear();
            this.registerTag(defaultTag, false);
        }
        List<T> list = this.backingListProvider.getTaggedObjectList();
        for (int i = 0; i < list.size(); ++i) {
            TaggedObject taggedObject = (TaggedObject)list.get(i);
            this.registerObject(taggedObject, false);
        }
    }

    public void registerObject(T taggedObject, boolean bSetDirty) {
        if (this.verbose) {
            DebugLog.General.println("register tagged object: " + String.valueOf(taggedObject));
        }
        taggedObject.getTagBits().clear();
        List<String> tags = taggedObject.getTags();
        for (int i = 0; i < tags.size(); ++i) {
            this.registerTag(tags.get(i), bSetDirty);
        }
        if (tags.isEmpty()) {
            categoryIndex = this.tagStringToIndexMap.get(defaultTag);
            taggedObject.getTagBits().set(categoryIndex);
            this.tagToObjectListMap.get(defaultTag).add(taggedObject);
        } else {
            for (int i = 0; i < tags.size(); ++i) {
                String tag = this.sanitizeTag(tags.get(i));
                categoryIndex = this.tagStringToIndexMap.get(tag);
                taggedObject.getTagBits().set(categoryIndex);
                this.tagToObjectListMap.get(tag).add(taggedObject);
            }
        }
        if (bSetDirty) {
            this.setDirty();
        }
    }

    private String sanitizeTag(String tag) {
        if (tag != null) {
            return tag.trim().toLowerCase();
        }
        return tag;
    }

    private int registerTag(String tag, boolean bSetDirty) {
        if (!this.registeredTags.contains(tag = this.sanitizeTag(tag))) {
            if (this.verbose) {
                DebugLog.General.println("register new tag: " + tag);
            }
            int bitIndex = this.registeredTags.size() + 1;
            this.registeredTags.add(tag);
            this.tagStringToIndexMap.put(tag, bitIndex);
            this.tagIndexToStringMap.put(bitIndex, tag);
            this.tagToObjectListMap.put(tag, new ArrayList());
            if (bSetDirty) {
                this.setDirty();
            }
            return bitIndex;
        }
        return this.registeredTags.indexOf(tag) + 1;
    }

    public List<T> getListForTag(String tag) {
        List<T> taggedObjects = this.tagToObjectListMap.get(this.sanitizeTag(tag));
        if (taggedObjects != null) {
            return taggedObjects;
        }
        return this.emptyTagObjects;
    }

    public List<T> getListForTag(int tagBitIndex) {
        String tag = this.tagIndexToStringMap.get(tagBitIndex);
        if (tag != null) {
            return this.getListForTag(tag);
        }
        return this.emptyTagObjects;
    }

    public List<T> queryTaggedObjects(String tagQueryString) {
        if (StringUtils.isNullOrWhitespace(tagQueryString)) {
            DebugLog.General.warn("manager-> returning empty list for: " + tagQueryString);
            return this.emptyTagObjects;
        }
        TagGroup<T> tagGroup = this.tagGroupMap.get(tagQueryString);
        if (tagGroup != null) {
            if (this.verbose) {
                DebugLog.General.println("manager-> returning cached list for: " + tagQueryString);
            }
            return tagGroup.getUpdatedClientView();
        }
        String alias = this.tagsStringAliasMap.get(tagQueryString);
        if (alias != null && (tagGroup = this.tagGroupMap.get(alias)) != null) {
            if (this.verbose) {
                DebugLog.General.println("manager-> returning cached list for alias '" + alias + "', cache: " + tagQueryString);
            }
            return tagGroup.getUpdatedClientView();
        }
        String formattedQueryStr = this.formatQueryString(tagQueryString);
        tagGroup = this.tagGroupMap.get(formattedQueryStr);
        if (tagGroup != null) {
            if (this.verbose) {
                DebugLog.General.println("manager-> created new alias '" + tagQueryString + "' for: " + formattedQueryStr);
            }
            this.tagsStringAliasMap.put(tagQueryString, formattedQueryStr);
            return tagGroup.getUpdatedClientView();
        }
        String[] whitelist = this.readWhitelist(formattedQueryStr);
        String[] blacklist = this.readBlackList(formattedQueryStr);
        BitSet whitelistBits = this.createTagBits(whitelist);
        BitSet blacklistBits = this.createTagBits(blacklist);
        boolean hasWhiteList = whitelistBits.notEmpty();
        boolean hasBlackList = blacklistBits.notEmpty();
        if (!hasWhiteList && !hasBlackList) {
            DebugLog.General.warn("manager-> could not gather objects for key: " + tagQueryString);
            return this.emptyTagObjects;
        }
        tagGroup = new TagGroup(this, whitelistBits, blacklistBits);
        this.populateTagGroupList(tagGroup, false);
        this.tagGroupMap.put(formattedQueryStr, tagGroup);
        this.tagGroups.add(tagGroup);
        if (this.verbose) {
            DebugLog.General.println("manager-> created new set for: " + formattedQueryStr);
        }
        return tagGroup.getUpdatedClientView();
    }

    private BitSet createTagBits(String[] tags) {
        return this.createTagBits(tags, true);
    }

    private BitSet createTagBits(String[] tags, boolean registerNewTags) {
        BitSet categoryBits = new BitSet();
        if (tags != null) {
            for (int i = 0; i < tags.length; ++i) {
                String tag = tags[i];
                Integer bitIndex = this.tagStringToIndexMap.get(tag);
                if (bitIndex != null) {
                    categoryBits.set(bitIndex);
                    continue;
                }
                if (!registerNewTags) continue;
                if (this.warnNonPreprocessedNewTag) {
                    DebugLog.General.warn("manager-> new tag discovered that was not preprocessed, tag: " + tag);
                }
                int newBitIndex = this.registerTag(tag, true);
                categoryBits.set(newBitIndex);
            }
        }
        return categoryBits;
    }

    private List<T> populateTagGroupList(TagGroup<T> tagGroup, boolean clear) {
        return this.populateTaggedObjectList(tagGroup.whitelist, tagGroup.blacklist, tagGroup.list, clear);
    }

    private List<T> populateTaggedObjectList(BitSet whitelistBits, BitSet blacklistBits, List<T> listToPopulate, boolean clear) {
        return this.populateTaggedObjectList(whitelistBits, blacklistBits, listToPopulate, null, clear);
    }

    private List<T> populateTaggedObjectList(BitSet whitelistBits, BitSet blacklistBits, List<T> listToPopulate, List<T> sources, boolean clear) {
        if (clear && !listToPopulate.isEmpty()) {
            listToPopulate.clear();
        }
        List<T> taggedObjects = sources != null ? sources : this.backingListProvider.getTaggedObjectList();
        boolean hasWhiteList = whitelistBits.notEmpty();
        boolean hasBlackList = blacklistBits.notEmpty();
        for (int i = 0; i < taggedObjects.size(); ++i) {
            TaggedObject taggedObject = (TaggedObject)taggedObjects.get(i);
            if (hasWhiteList && !taggedObject.getTagBits().intersects(whitelistBits) || hasBlackList && taggedObject.getTagBits().intersects(blacklistBits)) continue;
            listToPopulate.add(taggedObject);
        }
        return listToPopulate;
    }

    public List<T> filterList(String tagQueryString, List<T> listToPopulate, List<T> sourceList, boolean clearList) {
        if (clearList) {
            listToPopulate.clear();
        }
        if (StringUtils.isNullOrWhitespace(tagQueryString)) {
            if (this.verbose) {
                DebugLog.General.warn("manager-> query string empty, returning input list for: " + tagQueryString);
            }
            return listToPopulate;
        }
        tagQueryString = this.formatQueryString(tagQueryString);
        String[] whitelist = this.readWhitelist(tagQueryString);
        String[] blacklist = this.readBlackList(tagQueryString);
        BitSet whitelistBits = this.createTagBits(whitelist, false);
        BitSet blacklistBits = this.createTagBits(blacklist, false);
        boolean hasWhiteList = whitelistBits.notEmpty();
        boolean hasBlackList = blacklistBits.notEmpty();
        if (!hasWhiteList && !hasBlackList) {
            if (this.verbose) {
                DebugLog.General.warn("manager-> could not gather objects for key: " + tagQueryString);
            }
            return listToPopulate;
        }
        List<T> taggedObjects = sourceList != null ? sourceList : this.backingListProvider.getTaggedObjectList();
        for (int i = 0; i < taggedObjects.size(); ++i) {
            TaggedObject taggedObject = (TaggedObject)taggedObjects.get(i);
            if (hasWhiteList && !taggedObject.getTagBits().intersects(whitelistBits) || hasBlackList && taggedObject.getTagBits().intersects(blacklistBits)) continue;
            listToPopulate.add(taggedObject);
        }
        return listToPopulate;
    }

    public List<T> populateList(String tagQueryString, List<T> listToPopulate, List<T> sourceList, boolean clearList) {
        if (clearList) {
            listToPopulate.clear();
        }
        if (StringUtils.isNullOrWhitespace(tagQueryString)) {
            if (this.verbose) {
                DebugLog.General.warn("manager-> query string empty, returning input list for: " + tagQueryString);
            }
            return listToPopulate;
        }
        tagQueryString = this.formatQueryString(tagQueryString);
        String[] whitelist = this.readWhitelist(tagQueryString);
        String[] blacklist = this.readBlackList(tagQueryString);
        BitSet whitelistBits = this.createTagBits(whitelist);
        BitSet blacklistBits = this.createTagBits(blacklist);
        boolean hasWhiteList = whitelistBits.notEmpty();
        boolean hasBlackList = blacklistBits.notEmpty();
        if (!hasWhiteList && !hasBlackList) {
            DebugLog.General.warn("manager-> could not gather objects for key: " + tagQueryString);
            return listToPopulate;
        }
        return this.populateTaggedObjectList(whitelistBits, blacklistBits, listToPopulate, sourceList, clearList);
    }

    public String formatAndRegisterQueryString(String tagQueryString) {
        if (StringUtils.isNullOrWhitespace(tagQueryString)) {
            throw new IllegalArgumentException("Key is null or whitespace.");
        }
        if (this.tagGroupMap.containsKey(tagQueryString)) {
            return tagQueryString;
        }
        if (!this.tagGroupMap.containsKey(tagQueryString = this.formatQueryString(tagQueryString))) {
            this.queryTaggedObjects(tagQueryString);
        }
        return tagQueryString;
    }

    public String formatQueryString(String tagQueryString) {
        int i;
        if (StringUtils.isNullOrWhitespace(tagQueryString)) {
            return tagQueryString;
        }
        String[] whitelist = this.readWhitelist(tagQueryString);
        String[] blacklist = this.readBlackList(tagQueryString);
        StringBuilder sb = new StringBuilder();
        this.tempStringList.clear();
        if (whitelist != null) {
            for (i = 0; i < whitelist.length; ++i) {
                this.tempStringList.add(this.sanitizeTag(whitelist[i]));
            }
            Collections.sort(this.tempStringList);
            for (i = 0; i < this.tempStringList.size(); ++i) {
                if (i == 0) {
                    sb.append(this.tempStringList.get(i));
                    continue;
                }
                sb.append(";").append(this.tempStringList.get(i));
            }
        }
        this.tempStringList.clear();
        if (blacklist != null) {
            sb.append("-");
            for (i = 0; i < blacklist.length; ++i) {
                this.tempStringList.add(this.sanitizeTag(blacklist[i]));
            }
            Collections.sort(this.tempStringList);
            for (i = 0; i < this.tempStringList.size(); ++i) {
                if (i == 0) {
                    sb.append(this.tempStringList.get(i));
                    continue;
                }
                sb.append(";").append(this.tempStringList.get(i));
            }
        }
        return sb.toString();
    }

    private String[] readWhitelist(String tagQueryString) {
        if (!tagQueryString.contains("-")) {
            return tagQueryString.split(";");
        }
        if (tagQueryString.startsWith("-")) {
            return null;
        }
        String[] sp = tagQueryString.split("-");
        return sp[0].split(";");
    }

    private String[] readBlackList(String tagQueryString) {
        if (!tagQueryString.contains("-")) {
            return null;
        }
        if (tagQueryString.startsWith("-")) {
            String s = tagQueryString.substring(1);
            return s.split(";");
        }
        String[] sp = tagQueryString.split("-");
        return sp[1].split(";");
    }

    public void debugPrint() {
        this.debugPrint(null);
    }

    public void debugPrint(ArrayList<String> lines) {
        BaseScriptObject baseScriptObject;
        this.debugLog("[TaggedObjectManager]", lines);
        this.debugLog("{", lines);
        this.debugLog("[registeredTags]", lines);
        this.debugLog("{", lines);
        for (int i = 0; i < this.registeredTags.size(); ++i) {
            String string = this.registeredTags.get(i);
            this.debugLog("  " + i + " = " + string, lines);
        }
        this.debugLog("}", lines);
        this.debugLog("", lines);
        this.debugLog("[tagStringToIndexMap]", lines);
        this.debugLog("{", lines);
        for (Map.Entry<String, Integer> entry : this.tagStringToIndexMap.entrySet()) {
            this.debugLog("  " + entry.getKey() + " = " + String.valueOf(entry.getValue()), lines);
        }
        this.debugLog("}", lines);
        this.debugLog("", lines);
        this.debugLog("[tagIndexToStringMap]", lines);
        this.debugLog("{", lines);
        for (Map.Entry<Object, Object> entry : this.tagIndexToStringMap.entrySet()) {
            this.debugLog("  " + String.valueOf(entry.getKey()) + " = " + (String)entry.getValue(), lines);
        }
        this.debugLog("}", lines);
        this.debugLog("", lines);
        this.debugLog("[tagToObjectListMap]", lines);
        this.debugLog("{", lines);
        for (Map.Entry<Object, Object> entry : this.tagToObjectListMap.entrySet()) {
            this.debugLog("  " + (String)entry.getKey(), lines);
            this.debugLog("  {", lines);
            for (TaggedObject obj : (List)entry.getValue()) {
                if (obj instanceof BaseScriptObject) {
                    baseScriptObject = (BaseScriptObject)((Object)obj);
                    this.debugLog("    " + baseScriptObject.getScriptObjectFullType(), lines);
                    continue;
                }
                this.debugLog("    " + String.valueOf(obj), lines);
            }
            this.debugLog("  }", lines);
        }
        this.debugLog("}", lines);
        this.debugLog("", lines);
        this.debugLog("[tagStringAliasMap]", lines);
        this.debugLog("{", lines);
        for (Map.Entry<Object, Object> entry : this.tagsStringAliasMap.entrySet()) {
            this.debugLog("  " + (String)entry.getKey() + " = " + (String)entry.getValue(), lines);
        }
        this.debugLog("}", lines);
        this.debugLog("", lines);
        this.debugLog("[tagGroupMap]", lines);
        this.debugLog("{", lines);
        for (Map.Entry<Object, Object> entry : this.tagGroupMap.entrySet()) {
            this.debugLog("  [" + (String)entry.getKey() + "]", lines);
            this.debugLog("  {", lines);
            this.debugLog("    whitelist = " + this.getBitSetString(((TagGroup)entry.getValue()).whitelist), lines);
            this.debugLog("    blacklist = " + this.getBitSetString(((TagGroup)entry.getValue()).blacklist), lines);
            this.debugLog("    [objects]", lines);
            this.debugLog("    {", lines);
            for (TaggedObject obj : ((TagGroup)entry.getValue()).list) {
                if (obj instanceof BaseScriptObject) {
                    baseScriptObject = (BaseScriptObject)((Object)obj);
                    this.debugLog("      " + baseScriptObject.getScriptObjectFullType(), lines);
                    continue;
                }
                this.debugLog("      " + String.valueOf(obj), lines);
            }
            this.debugLog("    }", lines);
            this.debugLog("  }", lines);
        }
        this.debugLog("}", lines);
        this.debugLog("}", lines);
    }

    private String getBitSetString(BitSet bitSet) {
        StringBuilder sb = new StringBuilder();
        sb.append("{ ");
        boolean prepend = false;
        for (int i = 0; i < bitSet.length(); ++i) {
            if (!bitSet.get(i)) continue;
            if (prepend) {
                sb.append(", ");
            }
            sb.append(this.tagIndexToStringMap.get(i));
            sb.append("(");
            sb.append(i);
            sb.append(")");
            prepend = true;
        }
        sb.append(" }");
        return sb.toString();
    }

    private void debugLog(String s, ArrayList<String> lines) {
        if (lines != null) {
            lines.add(s);
        } else {
            DebugLog.log(s);
        }
    }

    public static interface BackingListProvider<T extends TaggedObject> {
        public List<T> getTaggedObjectList();
    }

    private static class TagGroup<T extends TaggedObject> {
        private final TaggedObjectManager<T> manager;
        private final BitSet whitelist;
        private final BitSet blacklist;
        private final List<T> list = new ArrayList<T>();
        private final List<T> clientView;
        private boolean dirty;

        private TagGroup(TaggedObjectManager<T> manager, BitSet whitelist, BitSet blacklist) {
            this.manager = manager;
            this.whitelist = whitelist;
            this.blacklist = blacklist;
            this.clientView = PZUnmodifiableList.wrap(this.list);
        }

        private List<T> getUpdatedClientView() {
            if (this.dirty) {
                this.manager.populateTaggedObjectList(this.whitelist, this.blacklist, this.list, true);
                this.dirty = false;
            }
            return this.clientView;
        }
    }

    public static interface TaggedObject {
        public List<String> getTags();

        public BitSet getTagBits();
    }
}

