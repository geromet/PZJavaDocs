/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.animals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.Lua.LuaManager;
import zombie.SandboxOptions;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.iso.IsoDirections;

public class AnimalTracksDefinitions {
    public static HashMap<String, AnimalTracksDefinitions> tracksDefinitions;
    public String type;
    public HashMap<String, AnimalTracksType> tracks;
    public HashMap<String, Integer> trackChance;
    public int skillToIdentify;
    public String trackType;
    public int chanceToFindTrack;

    public static AnimalTracksType getRandomTrack(String animalType, String action) {
        if (SandboxOptions.getInstance().animalTrackChance.getValue() == 1) {
            return null;
        }
        AnimalTracksDefinitions def = AnimalTracksDefinitions.getTracksDefinition().get(animalType);
        if (def == null) {
            DebugLog.Animal.debugln("Couldn't find animal tracks definition for animal: " + animalType);
            return null;
        }
        int chance = def.trackChance.get(action);
        int chanceMod = 15;
        switch (SandboxOptions.getInstance().animalTrackChance.getValue()) {
            case 2: {
                chanceMod = 500;
                break;
            }
            case 3: {
                chanceMod = 300;
                break;
            }
            case 4: {
                chanceMod = 150;
                break;
            }
            case 5: {
                chanceMod = 50;
            }
        }
        if (!Rand.NextBool(chance * chanceMod)) {
            return null;
        }
        int totalChance = 0;
        ArrayList<AnimalTracksType> list = new ArrayList<AnimalTracksType>();
        for (String trackType : def.tracks.keySet()) {
            AnimalTracksType track = def.tracks.get(trackType);
            if (!track.actionType.equalsIgnoreCase(action)) continue;
            list.add(track);
            totalChance += track.chanceToSpawn;
        }
        AnimalTracksType choosenTrack = null;
        int rand = Rand.Next(totalChance);
        int chanceIndex = 0;
        for (int j = 0; j < list.size(); ++j) {
            choosenTrack = (AnimalTracksType)list.get(j);
            if (choosenTrack.chanceToSpawn + chanceIndex >= rand) break;
            chanceIndex += choosenTrack.chanceToSpawn;
            choosenTrack = null;
        }
        return choosenTrack;
    }

    public static AnimalTracksType getTrackType(String animal, String type) {
        AnimalTracksDefinitions def = AnimalTracksDefinitions.getTracksDefinition().get(animal);
        if (def == null) {
            return null;
        }
        return def.tracks.get(type);
    }

    public static HashMap<String, AnimalTracksDefinitions> getTracksDefinition() {
        if (tracksDefinitions == null) {
            AnimalTracksDefinitions.loadTracksDefinitions();
        }
        return tracksDefinitions;
    }

    public static void loadTracksDefinitions() {
        if (tracksDefinitions != null) {
            return;
        }
        tracksDefinitions = new HashMap();
        KahluaTableImpl definitions = (KahluaTableImpl)LuaManager.env.rawget("AnimalTracksDefinitions");
        if (definitions == null) {
            return;
        }
        KahluaTableImpl animallist = (KahluaTableImpl)definitions.rawget("animallist");
        KahluaTableIterator iterator2 = animallist.iterator();
        while (iterator2.advance()) {
            AnimalTracksDefinitions def = new AnimalTracksDefinitions();
            def.type = iterator2.getKey().toString();
            tracksDefinitions.put(def.type, def);
            KahluaTableIterator it2 = ((KahluaTableImpl)iterator2.getValue()).iterator();
            while (it2.advance()) {
                String key = it2.getKey().toString();
                Object value = it2.getValue();
                String valueStr = value.toString().trim();
                if ("skillToIdentify".equalsIgnoreCase(key)) {
                    def.skillToIdentify = Float.valueOf(valueStr).intValue();
                }
                if ("chanceToFindTrack".equalsIgnoreCase(key)) {
                    def.chanceToFindTrack = Float.valueOf(valueStr).intValue();
                }
                if ("trackType".equalsIgnoreCase(key)) {
                    def.trackType = valueStr;
                }
                if ("tracks".equalsIgnoreCase(key)) {
                    def.loadTracks((KahluaTableImpl)value);
                }
                if (!"trackChance".equalsIgnoreCase(key)) continue;
                def.loadTrackChance((KahluaTableImpl)value);
            }
        }
    }

    private void loadTrackChance(KahluaTableImpl trackDef) {
        this.trackChance = new HashMap();
        KahluaTableIterator iterator2 = trackDef.iterator();
        while (iterator2.advance()) {
            String key = iterator2.getKey().toString();
            Object value = iterator2.getValue();
            String valueStr = value.toString().trim();
            this.trackChance.put(key.trim(), Float.valueOf(valueStr).intValue());
        }
    }

    private void loadTracks(KahluaTableImpl trackDef) {
        this.tracks = new HashMap();
        KahluaTableIterator iterator2 = trackDef.iterator();
        while (iterator2.advance()) {
            AnimalTracksType def = new AnimalTracksType(this);
            def.type = iterator2.getKey().toString();
            this.tracks.put(def.type, def);
            KahluaTableIterator it2 = ((KahluaTableImpl)iterator2.getValue()).iterator();
            while (it2.advance()) {
                String key = it2.getKey().toString();
                Object value = it2.getValue();
                String valueStr = value.toString().trim();
                if ("name".equalsIgnoreCase(key)) {
                    def.name = valueStr;
                }
                if ("sprite".equalsIgnoreCase(key)) {
                    def.sprite = valueStr;
                }
                if ("actionType".equalsIgnoreCase(key)) {
                    def.actionType = valueStr;
                }
                if ("chanceToFindTrack".equalsIgnoreCase(key)) {
                    def.chanceToFindTrack = Float.valueOf(valueStr).intValue();
                }
                if ("chanceToSpawn".equalsIgnoreCase(key)) {
                    def.chanceToSpawn = Float.valueOf(valueStr).intValue();
                }
                if ("minSkill".equalsIgnoreCase(key)) {
                    def.minSkill = Float.valueOf(valueStr).intValue();
                }
                if ("needDir".equalsIgnoreCase(key)) {
                    def.needDir = Boolean.parseBoolean(valueStr);
                }
                if ("item".equalsIgnoreCase(key)) {
                    def.item = valueStr;
                }
                if (!"sprites".equalsIgnoreCase(key)) continue;
                def.loadSprites((KahluaTableImpl)value);
            }
        }
    }

    public class AnimalTracksType {
        public String type;
        public String name;
        public boolean needDir;
        public String sprite;
        public String actionType;
        public int chanceToFindTrack;
        public int minSkill;
        public int chanceToSpawn;
        public String item;
        public HashMap<IsoDirections, String> sprites;

        public AnimalTracksType(AnimalTracksDefinitions this$0) {
            Objects.requireNonNull(this$0);
        }

        private void loadSprites(KahluaTableImpl trackDef) {
            this.sprites = new HashMap();
            KahluaTableIterator iterator2 = trackDef.iterator();
            while (iterator2.advance()) {
                String key = iterator2.getKey().toString();
                Object value = iterator2.getValue();
                String valueStr = value.toString().trim();
                this.sprites.put(IsoDirections.fromString(key.trim()), valueStr);
            }
        }
    }
}

