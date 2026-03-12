/*
 * Decompiled with CFR 0.152.
 */
package zombie.scripting.objects;

import generation.builders.validation.TranslationKeyValidator;
import zombie.core.Core;
import zombie.scripting.objects.RecipeKey;
import zombie.scripting.objects.Registries;
import zombie.scripting.objects.RegistryReset;
import zombie.scripting.objects.ResourceLocation;

public class MetaRecipe
implements RecipeKey {
    public static final MetaRecipe ASSEMBLE_ADVANCED_FRAMEPACK = MetaRecipe.registerBase("AssembleAdvancedFramepack");
    public static final MetaRecipe ASSEMBLE_LARGE_FRAMEPACK = MetaRecipe.registerBase("AssembleLargeFramepack");
    public static final MetaRecipe ASSEMBLE_SHOULDER_ARMOR = MetaRecipe.registerBase("Assemble_Shoulder_Armor");
    public static final MetaRecipe BIND_SPEAR = MetaRecipe.registerBase("BindSpear");
    public static final MetaRecipe CAN_REINFORCE_WEAPON = MetaRecipe.registerBase("CanReinforceWeapon");
    public static final MetaRecipe CARVE_BAT = MetaRecipe.registerBase("CarveBat");
    public static final MetaRecipe FORGE_FILE = MetaRecipe.registerBase("Forge_File");
    public static final MetaRecipe FORGE_FINE_BUTTER_KNIVES = MetaRecipe.registerBase("Forge_Fine_Butter_Knives");
    public static final MetaRecipe FORGE_FINE_FORKS = MetaRecipe.registerBase("Forge_Fine_Forks");
    public static final MetaRecipe FORGE_FINE_SPOONS = MetaRecipe.registerBase("Forge_Fine_Spoons");
    public static final MetaRecipe FORGE_METALWORKING_CHISEL = MetaRecipe.registerBase("Forge_Metalworking_Chisel");
    public static final MetaRecipe KITCHEN_TOOLS = MetaRecipe.registerBase("KitchenTools");
    public static final MetaRecipe MAKE_BONE_ARMOR = MetaRecipe.registerBase("MakeBoneArmor");
    public static final MetaRecipe MAKE_BULLETPROOF_LIMB_ARMOR = MetaRecipe.registerBase("MakeBulletproofLimbArmor");
    public static final MetaRecipe MAKE_FLIES_CURE = MetaRecipe.registerBase("MakeFliesCure");
    public static final MetaRecipe MAKE_LARGE_BONE_BEAD = MetaRecipe.registerBase("MakeLargeBoneBead");
    public static final MetaRecipe MAKE_MAGAZINE_ARMOR = MetaRecipe.registerBase("MakeMagazineArmor");
    public static final MetaRecipe MAKE_RAILSPIKE_WEAPON = MetaRecipe.registerBase("MakeRailspikeWeapon");
    public static final MetaRecipe MAKE_SAWBLADE_WEAPON = MetaRecipe.registerBase("MakeSawbladeWeapon");
    public static final MetaRecipe MAKE_SPIKED_CLUB = MetaRecipe.registerBase("MakeSpikedClub");
    public static final MetaRecipe MAKE_STONE_BLADE = MetaRecipe.registerBase("MakeStoneBlade");
    public static final MetaRecipe MAKE_TIRE_ARMOR = MetaRecipe.registerBase("MakeTireArmor");
    public static final MetaRecipe MAKE_TIRE_SHOULDER_ARMOR_LEFT = MetaRecipe.registerBase("MakeTireShoulderArmorLeft");
    public static final MetaRecipe MAKE_WOOD_ARMOR = MetaRecipe.registerBase("MakeWoodArmor");
    public static final MetaRecipe SEW_BANDOLIER = MetaRecipe.registerBase("SewBandolier");
    public static final MetaRecipe SEW_CRUDE_LEATHER_BACKPACK = MetaRecipe.registerBase("SewCrudeLeatherBackpack");
    public static final MetaRecipe SEW_DRESS_KNEES = MetaRecipe.registerBase("SewDressKnees");
    public static final MetaRecipe SEW_HIDE_FANNY_BAG = MetaRecipe.registerBase("SewHideFannyBag");
    public static final MetaRecipe SEW_HIDE_PANTS = MetaRecipe.registerBase("SewHidePants");
    public static final MetaRecipe SEW_LONGJOHNS = MetaRecipe.registerBase("SewLongjohns");
    public static final MetaRecipe SEW_SHIRT = MetaRecipe.registerBase("SewShirt");
    public static final MetaRecipe SEW_SKIRT_KNEES = MetaRecipe.registerBase("SewSkirtKnees");
    public static final MetaRecipe SHARPEN_BONE = MetaRecipe.registerBase("SharpenBone");
    public static final MetaRecipe SPIKE_PADDING = MetaRecipe.registerBase("SpikePadding");
    private final String translationName;

    private MetaRecipe(String translationName) {
        this.translationName = translationName;
    }

    public static MetaRecipe register(String id) {
        return MetaRecipe.register(false, id);
    }

    private static MetaRecipe registerBase(String id) {
        return MetaRecipe.register(true, id);
    }

    private static MetaRecipe register(boolean allowDefaultNamespace, String id) {
        return Registries.META_RECIPE.register(RegistryReset.createLocation(id, allowDefaultNamespace), new MetaRecipe(id));
    }

    public static MetaRecipe get(ResourceLocation id) {
        return Registries.META_RECIPE.get(id);
    }

    public String toString() {
        return Registries.META_RECIPE.getLocation(this).toString();
    }

    @Override
    public ResourceLocation getRegistryId() {
        return Registries.META_RECIPE.getLocation(this);
    }

    @Override
    public String getTranslationName() {
        return this.translationName;
    }

    static {
        if (Core.IS_DEV) {
            for (MetaRecipe metaRecipe : Registries.META_RECIPE) {
                TranslationKeyValidator.of(metaRecipe.getTranslationName());
            }
        }
    }
}

