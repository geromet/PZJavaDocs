/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import java.util.ArrayList;
import java.util.Collections;
import org.apache.commons.lang3.tuple.Pair;
import zombie.characters.professions.CharacterProfessionDefinition;
import zombie.characters.skills.PerkFactory;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.debug.DebugLog;
import zombie.scripting.ScriptParser;
import zombie.scripting.ScriptType;
import zombie.scripting.objects.BaseScriptObject;
import zombie.scripting.objects.CharacterProfession;
import zombie.scripting.objects.CharacterTrait;
import zombie.scripting.objects.Registries;
import zombie.scripting.objects.ResourceLocation;

public final class CharacterProfessionDefinitionScript
extends BaseScriptObject {
    public CharacterProfessionDefinitionScript() {
        super(ScriptType.CharacterProfessionDefinition);
    }

    @Override
    public void Load(String name, String totalFile) throws Exception {
        ScriptParser.Block block = ScriptParser.parse(totalFile);
        block = block.children.get(0);
        super.LoadCommonBlock(block);
        this.loadCharacterProfessionDefinition(block);
    }

    private void loadCharacterProfessionDefinition(ScriptParser.Block block) throws Exception {
        CharacterProfession characterProfessionType = null;
        String uiName = null;
        int cost = 0;
        String uiDescription = null;
        String iconPathName = null;
        ArrayList<CharacterTrait> grantedTraits = new ArrayList<CharacterTrait>();
        ArrayList grantedRecipes = new ArrayList();
        ArrayList<Pair<PerkFactory.Perk, Integer>> xpBoosts = new ArrayList<Pair<PerkFactory.Perk, Integer>>();
        for (ScriptParser.BlockElement element : block.elements) {
            String[] stringArray;
            if (element.asValue() == null || (stringArray = element.asValue().string.split("=", 2)).length < 2) continue;
            String k = stringArray[0].trim();
            String v = stringArray[1].trim();
            if (k.isEmpty() || v.isEmpty()) continue;
            if (k.equalsIgnoreCase("CharacterProfession")) {
                characterProfessionType = Registries.CHARACTER_PROFESSION.get(ResourceLocation.of(v));
                continue;
            }
            if (k.equalsIgnoreCase("UIName")) {
                uiName = v;
                continue;
            }
            if (k.equalsIgnoreCase("Cost")) {
                cost = Integer.parseInt(v);
                continue;
            }
            if (k.equalsIgnoreCase("UIDescription")) {
                uiDescription = v;
                continue;
            }
            if (k.equalsIgnoreCase("GrantedRecipes")) {
                Collections.addAll(grantedRecipes, v.split(";"));
                continue;
            }
            if (k.equalsIgnoreCase("GrantedTraits")) {
                for (String s : v.split(";")) {
                    grantedTraits.add(Registries.CHARACTER_TRAIT.get(ResourceLocation.of(s.trim())));
                }
                continue;
            }
            if (k.equalsIgnoreCase("XPBoosts")) {
                for (String s : v.split(";")) {
                    PerkFactory.Perk perk;
                    String[] xpPair = s.split("=", 2);
                    if (xpPair.length < 2 || (perk = PerkFactory.Perks.FromString(xpPair[0].trim())) == PerkFactory.Perks.MAX) continue;
                    int boost = PZMath.tryParseInt(xpPair[1], 1);
                    xpBoosts.add(Pair.of(perk, boost));
                }
                continue;
            }
            if (k.equalsIgnoreCase("IconPathName")) {
                iconPathName = v;
                continue;
            }
            DebugLog.Script.error("Unknown key '%s' in CharacterProfessionDefinitionScript '%s'", k, block.id);
            if (!Core.debug) continue;
            throw new Exception("CharacterProfessionDefinitionScript error in " + block.id);
        }
        CharacterProfessionDefinition definition = CharacterProfessionDefinition.addCharacterProfessionDefinition(characterProfessionType, uiName, cost, uiDescription, iconPathName);
        for (Pair pair : xpBoosts) {
            definition.addXPBoost((PerkFactory.Perk)pair.getLeft(), (Integer)pair.getRight());
        }
        for (String string : grantedRecipes) {
            definition.addGrantedRecipe(string);
        }
        for (CharacterTrait characterTrait : grantedTraits) {
            definition.addGrantedTrait(characterTrait);
        }
    }
}

