/*
 * Decompiled with CFR 0.152.
 */
package zombie.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import zombie.core.properties.IsoPropertyType;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.GameEntityScript;

public final class TilePropertyAliasMap {
    public static final TilePropertyAliasMap instance = new TilePropertyAliasMap();
    public final HashMap<String, Integer> propertyToId = new HashMap();
    public final ArrayList<TileProperty> properties = new ArrayList();

    public void Generate(HashMap<String, ArrayList<String>> propertyValueMap) {
        this.properties.clear();
        this.propertyToId.clear();
        for (Map.Entry<String, ArrayList<String>> stringArrayListEntry : propertyValueMap.entrySet()) {
            this.register(stringArrayListEntry.getKey(), stringArrayListEntry.getValue());
        }
        this.generateEntityProperties();
    }

    private void generateEntityProperties() {
        ArrayList<String> value = new ArrayList<String>();
        for (GameEntityScript script : ScriptManager.instance.getAllGameEntities()) {
            value.add(script.getName());
        }
        this.register(IsoPropertyType.ENTITY_SCRIPT_NAME, value);
    }

    private void register(IsoPropertyType property, ArrayList<String> possiblePropValues) {
        this.register(property.getName(), possiblePropValues);
    }

    private void register(String propKey, ArrayList<String> possiblePropValues) {
        if (this.properties.size() >= Short.MAX_VALUE) {
            throw new RuntimeException("too many properties defined");
        }
        if (possiblePropValues.size() > Short.MAX_VALUE) {
            throw new RuntimeException("too many property values defined for " + propKey);
        }
        String property = IsoPropertyType.lookupOrDefaultStr(propKey);
        this.propertyToId.put(property, this.properties.size());
        TileProperty newProp = new TileProperty();
        this.properties.add(newProp);
        newProp.propertyName = property;
        newProp.possibleValues.addAll(possiblePropValues);
        ArrayList<String> possibleValues = newProp.possibleValues;
        for (int i = 0; i < possibleValues.size(); ++i) {
            String possibleValue = possibleValues.get(i);
            newProp.idMap.put(possibleValue, i);
        }
    }

    public int getIDFromPropertyName(String name) {
        return this.propertyToId.getOrDefault(name, -1);
    }

    public int getIDFromPropertyValue(int property, String value) {
        TileProperty tileProperty = this.properties.get(property);
        if (tileProperty.possibleValues.isEmpty()) {
            return 0;
        }
        return tileProperty.idMap.getOrDefault(value, 0);
    }

    public String getPropertyValueString(int property, int value) {
        TileProperty tileProperty = this.properties.get(property);
        if (tileProperty.possibleValues.isEmpty()) {
            return "";
        }
        return tileProperty.possibleValues.get(value);
    }

    public static final class TileProperty {
        public String propertyName;
        public final ArrayList<String> possibleValues = new ArrayList();
        public final HashMap<String, Integer> idMap = new HashMap();
    }
}

