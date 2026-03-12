/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters.BodyDamage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Objects;
import zombie.GameTime;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characterTextures.BloodClothingType;
import zombie.characters.BodyDamage.BodyDamage;
import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.BodyDamage.BodyPartContacts;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.characters.BodyDamage.Metabolics;
import zombie.characters.BodyDamage.Nutrition;
import zombie.characters.CharacterStat;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.Stats;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.visual.ItemVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Clothing;
import zombie.inventory.types.WeaponType;
import zombie.iso.weather.ClimateManager;
import zombie.iso.weather.Temperature;
import zombie.scripting.objects.ItemBodyLocation;
import zombie.scripting.objects.MoodleType;

public class Thermoregulator_tryouts {
    private final BodyDamage bodyDamage;
    private final IsoGameCharacter character;
    private final IsoPlayer player;
    private final Stats stats;
    private final Nutrition nutrition;
    private final ClimateManager climate;
    private static final ItemVisuals itemVisuals = new ItemVisuals();
    private static final ItemVisuals itemVisualsCache = new ItemVisuals();
    private static final ArrayList<BloodBodyPartType> coveredParts = new ArrayList();
    private static float simulationMultiplier = 1.0f;
    private float setPoint = 37.0f;
    private float metabolicRate;
    private float metabolicRateReal = this.metabolicRate = Metabolics.Default.getMet();
    private float metabolicTarget = Metabolics.Default.getMet();
    private double fluidsMultiplier = 1.0;
    private double energyMultiplier = 1.0;
    private double fatigueMultiplier = 1.0;
    private float bodyHeatDelta;
    private float coreHeatDelta;
    private boolean thermalChevronUp = true;
    private ThermalNode core;
    private ThermalNode[] nodes;
    private float totalHeatRaw;
    private float totalHeat;
    private float primTotal;
    private float secTotal;
    private float externalAirTemperature = 27.0f;
    private float airTemperature;
    private float airAndWindTemp;
    private float rateOfChangeCounter;
    private float coreCelciusCache = 37.0f;
    private float coreRateOfChange;
    private float thermalDamage;
    private float damageCounter;

    public Thermoregulator_tryouts(BodyDamage parent) {
        this.bodyDamage = parent;
        this.character = parent.getParentChar();
        this.stats = this.character.getStats();
        IsoGameCharacter isoGameCharacter = this.character;
        if (isoGameCharacter instanceof IsoPlayer) {
            IsoPlayer isoPlayer;
            this.player = isoPlayer = (IsoPlayer)isoGameCharacter;
            this.nutrition = isoPlayer.getNutrition();
        } else {
            this.player = null;
            this.nutrition = null;
        }
        this.climate = ClimateManager.getInstance();
        this.initNodes();
    }

    public static void setSimulationMultiplier(float multiplier) {
        simulationMultiplier = multiplier;
    }

    public void save(ByteBuffer output) throws IOException {
        output.putFloat(this.setPoint);
        output.putFloat(this.metabolicRate);
        output.putFloat(this.metabolicTarget);
        output.putFloat(this.bodyHeatDelta);
        output.putFloat(this.coreHeatDelta);
        output.putFloat(this.thermalDamage);
        output.putFloat(this.damageCounter);
        output.putInt(this.nodes.length);
        for (int i = 0; i < this.nodes.length; ++i) {
            ThermalNode node = this.nodes[i];
            output.putInt(BodyPartType.ToIndex(node.bodyPartType));
            output.putFloat(node.celcius);
            output.putFloat(node.skinCelcius);
            output.putFloat(node.heatDelta);
            output.putFloat(node.primaryDelta);
            output.putFloat(node.secondaryDelta);
        }
    }

    public void load(ByteBuffer input, int worldVersion) throws IOException {
        this.setPoint = input.getFloat();
        this.metabolicRate = input.getFloat();
        this.metabolicTarget = input.getFloat();
        this.bodyHeatDelta = input.getFloat();
        this.coreHeatDelta = input.getFloat();
        this.thermalDamage = input.getFloat();
        this.damageCounter = input.getFloat();
        int count = input.getInt();
        for (int i = 0; i < count; ++i) {
            int nodeIndex = input.getInt();
            float celcius = input.getFloat();
            float skinCelcius = input.getFloat();
            float heatDelta = input.getFloat();
            float primary = input.getFloat();
            float secondary = input.getFloat();
            ThermalNode node = this.getNodeForType(BodyPartType.FromIndex(nodeIndex));
            if (node != null) {
                node.celcius = celcius;
                node.skinCelcius = skinCelcius;
                node.heatDelta = heatDelta;
                node.primaryDelta = primary;
                node.secondaryDelta = secondary;
                continue;
            }
            DebugLog.log("Couldnt load node: " + BodyPartType.ToString(BodyPartType.FromIndex(nodeIndex)));
        }
    }

    public void reset() {
        this.setPoint = 37.0f;
        this.metabolicTarget = this.metabolicRate = Metabolics.Default.getMet();
        this.core.celcius = 37.0f;
        this.bodyHeatDelta = 0.0f;
        this.coreHeatDelta = 0.0f;
        this.thermalDamage = 0.0f;
        for (int i = 0; i < this.nodes.length; ++i) {
            ThermalNode node = this.nodes[i];
            if (node != this.core) {
                node.celcius = 35.0f;
            }
            node.primaryDelta = 0.0f;
            node.secondaryDelta = 0.0f;
            node.skinCelcius = 33.0f;
            node.heatDelta = 0.0f;
        }
    }

    private void initNodes() {
        int i;
        ArrayList<ThermalNode> nodesList = new ArrayList<ThermalNode>();
        for (i = 0; i < this.bodyDamage.getBodyParts().size(); ++i) {
            BodyPart bodyPart = this.bodyDamage.getBodyParts().get(i);
            ThermalNode node = null;
            switch (bodyPart.getType()) {
                case Torso_Upper: {
                    this.core = node = new ThermalNode(this, true, 37.0f, bodyPart, 0.25f);
                    break;
                }
                case Head: {
                    node = new ThermalNode(this, 37.0f, bodyPart, 1.0f);
                    break;
                }
                case Neck: {
                    node = new ThermalNode(this, 37.0f, bodyPart, 0.5f);
                    break;
                }
                case Torso_Lower: {
                    node = new ThermalNode(this, 37.0f, bodyPart, 0.25f);
                    break;
                }
                case Groin: {
                    node = new ThermalNode(this, 37.0f, bodyPart, 0.5f);
                    break;
                }
                case UpperLeg_L: 
                case UpperLeg_R: {
                    node = new ThermalNode(this, 37.0f, bodyPart, 0.5f);
                    break;
                }
                case LowerLeg_L: 
                case LowerLeg_R: {
                    node = new ThermalNode(this, 37.0f, bodyPart, 0.5f);
                    break;
                }
                case Foot_L: 
                case Foot_R: {
                    node = new ThermalNode(this, 37.0f, bodyPart, 0.5f);
                    break;
                }
                case UpperArm_L: 
                case UpperArm_R: {
                    node = new ThermalNode(this, 37.0f, bodyPart, 0.25f);
                    break;
                }
                case ForeArm_L: 
                case ForeArm_R: {
                    node = new ThermalNode(this, 37.0f, bodyPart, 0.25f);
                    break;
                }
                case Hand_L: 
                case Hand_R: {
                    node = new ThermalNode(this, 37.0f, bodyPart, 1.0f);
                    break;
                }
                default: {
                    DebugLog.log("Warning: couldnt init thermal node for body part '" + String.valueOf((Object)this.bodyDamage.getBodyParts().get(i).getType()) + "'.");
                }
            }
            if (node == null) continue;
            nodesList.add(node);
        }
        this.nodes = new ThermalNode[nodesList.size()];
        nodesList.toArray(this.nodes);
        for (i = 0; i < this.nodes.length; ++i) {
            BodyPartType[] children;
            ThermalNode node = this.nodes[i];
            BodyPartType parentType = BodyPartContacts.getParent(node.bodyPartType);
            if (parentType != null) {
                node.upstream = this.getNodeForType(parentType);
            }
            if ((children = BodyPartContacts.getChildren(node.bodyPartType)) == null || children.length <= 0) continue;
            node.downstream = new ThermalNode[children.length];
            for (int j = 0; j < children.length; ++j) {
                node.downstream[j] = this.getNodeForType(children[j]);
            }
        }
        this.core.celcius = this.setPoint;
    }

    public ThermalNode getNodeForType(BodyPartType type) {
        for (int i = 0; i < this.nodes.length; ++i) {
            if (this.nodes[i].bodyPartType != type) continue;
            return this.nodes[i];
        }
        return null;
    }

    public ThermalNode getNodeForBloodType(BloodBodyPartType type) {
        for (int i = 0; i < this.nodes.length; ++i) {
            if (this.nodes[i].bloodBpt != type) continue;
            return this.nodes[i];
        }
        return null;
    }

    public float getBodyHeatDelta() {
        return this.bodyHeatDelta;
    }

    public double getFluidsMultiplier() {
        return this.fluidsMultiplier;
    }

    public double getEnergyMultiplier() {
        return this.energyMultiplier;
    }

    public double getFatigueMultiplier() {
        return this.fatigueMultiplier;
    }

    public float getMovementModifier() {
        float delta = 1.0f;
        if (this.player != null) {
            int lvl = this.player.getMoodles().getMoodleLevel(MoodleType.HYPOTHERMIA);
            if (lvl == 2) {
                delta = 0.66f;
            } else if (lvl == 3) {
                delta = 0.33f;
            } else if (lvl == 4) {
                delta = 0.0f;
            }
            lvl = this.player.getMoodles().getMoodleLevel(MoodleType.HYPERTHERMIA);
            if (lvl == 2) {
                delta = 0.66f;
            } else if (lvl == 3) {
                delta = 0.33f;
            } else if (lvl == 4) {
                delta = 0.0f;
            }
        }
        return delta;
    }

    public float getCombatModifier() {
        float delta = 1.0f;
        if (this.player != null) {
            int lvl = this.player.getMoodles().getMoodleLevel(MoodleType.HYPOTHERMIA);
            if (lvl == 2) {
                delta = 0.66f;
            } else if (lvl == 3) {
                delta = 0.33f;
            } else if (lvl == 4) {
                delta = 0.1f;
            }
            lvl = this.player.getMoodles().getMoodleLevel(MoodleType.HYPERTHERMIA);
            if (lvl == 2) {
                delta = 0.66f;
            } else if (lvl == 3) {
                delta = 0.33f;
            } else if (lvl == 4) {
                delta = 0.1f;
            }
        }
        return delta;
    }

    public float getCoreTemperature() {
        return this.core.celcius;
    }

    public float getHeatGeneration() {
        return this.metabolicRateReal;
    }

    public float getMetabolicRate() {
        return this.metabolicRate;
    }

    public float getMetabolicTarget() {
        return this.metabolicTarget;
    }

    public float getMetabolicRateReal() {
        return this.metabolicRateReal;
    }

    public float getSetPoint() {
        return this.setPoint;
    }

    public float getCoreHeatDelta() {
        return this.coreHeatDelta;
    }

    public float getCoreRateOfChange() {
        return this.coreRateOfChange;
    }

    public float getExternalAirTemperature() {
        return this.externalAirTemperature;
    }

    public float getCoreTemperatureUI() {
        float v = PZMath.clamp(this.core.celcius, 20.0f, 42.0f);
        v = v < 37.0f ? (v - 20.0f) / 17.0f * 0.5f : 0.5f + (v - 37.0f) / 5.0f * 0.5f;
        return v;
    }

    public float getHeatGenerationUI() {
        float v = PZMath.clamp(this.metabolicRateReal, 0.0f, Metabolics.MAX.getMet());
        v = v < Metabolics.Default.getMet() ? v / Metabolics.Default.getMet() * 0.5f : 0.5f + (v - Metabolics.Default.getMet()) / (Metabolics.MAX.getMet() - Metabolics.Default.getMet()) * 0.5f;
        return v;
    }

    public boolean thermalChevronUp() {
        return this.thermalChevronUp;
    }

    public int thermalChevronCount() {
        if (this.coreRateOfChange > 0.01f) {
            return 3;
        }
        if (this.coreRateOfChange > 0.001f) {
            return 2;
        }
        if (this.coreRateOfChange > 1.0E-4f) {
            return 1;
        }
        return 0;
    }

    public float getCatchAColdDelta() {
        float delta = 0.0f;
        if (this.player.getMoodles().getMoodleLevel(MoodleType.HYPOTHERMIA) < 1) {
            return delta;
        }
        for (int i = 0; i < this.nodes.length; ++i) {
            ThermalNode node = this.nodes[i];
            float skin = 0.0f;
            if (node.skinCelcius < 33.0f) {
                skin = (node.skinCelcius - 20.0f) / 13.0f;
                skin = 1.0f - skin;
                skin *= skin;
            }
            float add = 0.25f * skin * node.skinSurface;
            if (node.bodyWetness > 0.0f) {
                add *= 1.0f + node.bodyWetness * 1.0f;
            }
            if (node.clothingWetness > 0.5f) {
                add *= 1.0f + (node.clothingWetness - 0.5f) * 2.0f;
            }
            if (node.bodyPartType == BodyPartType.Neck) {
                add *= 8.0f;
            } else if (node.bodyPartType == BodyPartType.Torso_Upper) {
                add *= 16.0f;
            } else if (node.bodyPartType == BodyPartType.Head) {
                add *= 4.0f;
            }
            delta += add;
        }
        if (this.player.getMoodles().getMoodleLevel(MoodleType.HYPOTHERMIA) > 1) {
            delta *= (float)this.player.getMoodles().getMoodleLevel(MoodleType.HYPOTHERMIA);
        }
        return delta;
    }

    public float getTimedActionTimeModifier() {
        float mod = 1.0f;
        for (int i = 0; i < this.nodes.length; ++i) {
            ThermalNode node = this.nodes[i];
            float skin = 0.0f;
            if (node.skinCelcius < 33.0f) {
                skin = (node.skinCelcius - 20.0f) / 13.0f;
                skin = 1.0f - skin;
                skin *= skin;
            }
            float penalty = 0.25f * skin * node.skinSurface;
            if (node.bodyPartType == BodyPartType.Hand_R || node.bodyPartType == BodyPartType.Hand_L) {
                mod += 0.3f * penalty;
                continue;
            }
            if (node.bodyPartType == BodyPartType.ForeArm_R || node.bodyPartType == BodyPartType.ForeArm_L) {
                mod += 0.15f * penalty;
                continue;
            }
            if (node.bodyPartType != BodyPartType.UpperArm_R && node.bodyPartType != BodyPartType.UpperArm_L) continue;
            mod += 0.1f * penalty;
        }
        return mod;
    }

    public static float getSkinCelciusMin() {
        return 20.0f;
    }

    public static float getSkinCelciusFavorable() {
        return 33.0f;
    }

    public static float getSkinCelciusMax() {
        return 42.0f;
    }

    public void setMetabolicTarget(Metabolics meta) {
        this.setMetabolicTarget(meta.getMet());
    }

    public void setMetabolicTarget(float target) {
        if (target < 0.0f || target < this.metabolicTarget) {
            return;
        }
        this.metabolicTarget = target;
        if (this.metabolicTarget > Metabolics.MAX.getMet()) {
            this.metabolicTarget = Metabolics.MAX.getMet();
        }
    }

    private void updateCoreRateOfChange() {
        this.rateOfChangeCounter += GameTime.instance.getMultiplier();
        if (this.rateOfChangeCounter > 100.0f) {
            this.rateOfChangeCounter = 0.0f;
            this.coreRateOfChange = this.core.celcius - this.coreCelciusCache;
            this.thermalChevronUp = this.coreRateOfChange >= 0.0f;
            this.coreRateOfChange = PZMath.abs(this.coreRateOfChange);
            this.coreCelciusCache = this.core.celcius;
        }
    }

    public float getSimulationMultiplier() {
        return simulationMultiplier;
    }

    public float getDefaultMultiplier() {
        return this.getSimulationMultiplier(Multiplier.Default);
    }

    public float getMetabolicRateIncMultiplier() {
        return this.getSimulationMultiplier(Multiplier.MetabolicRateInc);
    }

    public float getMetabolicRateDecMultiplier() {
        return this.getSimulationMultiplier(Multiplier.MetabolicRateDec);
    }

    public float getBodyHeatMultiplier() {
        return this.getSimulationMultiplier(Multiplier.BodyHeat);
    }

    public float getCoreHeatExpandMultiplier() {
        return this.getSimulationMultiplier(Multiplier.CoreHeatExpand);
    }

    public float getCoreHeatContractMultiplier() {
        return this.getSimulationMultiplier(Multiplier.CoreHeatContract);
    }

    public float getSkinCelciusMultiplier() {
        return this.getSimulationMultiplier(Multiplier.SkinCelcius);
    }

    public float getTemperatureAir() {
        return this.climate.getAirTemperatureForCharacter(this.character, false);
    }

    public float getTemperatureAirAndWind() {
        return this.climate.getAirTemperatureForCharacter(this.character, true);
    }

    public float getDbg_totalHeatRaw() {
        return this.totalHeatRaw;
    }

    public float getDbg_totalHeat() {
        return this.totalHeat;
    }

    public float getCoreCelcius() {
        return this.core != null ? this.core.celcius : 0.0f;
    }

    public float getDbg_primTotal() {
        return this.primTotal;
    }

    public float getDbg_secTotal() {
        return this.secTotal;
    }

    private float getSimulationMultiplier(Multiplier multiplierType) {
        float multiplier = GameTime.instance.getMultiplier();
        switch (multiplierType.ordinal()) {
            case 1: {
                multiplier *= 0.001f;
                break;
            }
            case 2: {
                multiplier *= 4.0E-4f;
                break;
            }
            case 3: {
                multiplier *= 2.5E-4f;
                break;
            }
            case 4: {
                multiplier *= 5.0E-5f;
                break;
            }
            case 5: {
                multiplier *= 5.0E-4f;
                break;
            }
            case 6: 
            case 8: {
                multiplier *= 0.0025f;
                break;
            }
            case 7: {
                multiplier *= 0.005f;
                break;
            }
            case 9: {
                multiplier *= 5.0E-4f;
                break;
            }
            case 10: {
                multiplier *= 2.5E-4f;
                break;
            }
        }
        return multiplier * simulationMultiplier;
    }

    public float getThermalDamage() {
        return this.thermalDamage;
    }

    private void updateThermalDamage(float airTemperature) {
        this.damageCounter += GameTime.instance.getRealworldSecondsSinceLastUpdate();
        if (this.damageCounter > 1.0f) {
            this.damageCounter = 0.0f;
            if (this.player.getMoodles().getMoodleLevel(MoodleType.HYPOTHERMIA) == 4 && airTemperature < 0.0f && this.core.celcius - this.coreCelciusCache <= 0.0f) {
                float frostSpeed = (this.core.celcius - 20.0f) / 5.0f;
                frostSpeed = 1.0f - frostSpeed;
                float seconds = 120.0f;
                this.thermalDamage += 1.0f / (seconds += 480.0f * frostSpeed) * PZMath.clamp_01(PZMath.abs(airTemperature) / 10.0f);
            } else if (this.player.getMoodles().getMoodleLevel(MoodleType.HYPERTHERMIA) == 4 && airTemperature > 37.0f && this.core.celcius - this.coreCelciusCache >= 0.0f) {
                float burnspeed = (this.core.celcius - 41.0f) / 1.0f;
                float seconds = 120.0f;
                this.thermalDamage += 1.0f / (seconds += 480.0f * burnspeed) * PZMath.clamp_01((airTemperature - 37.0f) / 8.0f);
                this.thermalDamage = Math.min(this.thermalDamage, 0.3f);
            } else {
                this.thermalDamage -= 0.011111111f;
            }
            this.thermalDamage = PZMath.clamp_01(this.thermalDamage);
        }
        this.player.getBodyDamage().setColdDamageStage(this.thermalDamage);
    }

    public void update() {
        this.airTemperature = this.climate.getAirTemperatureForCharacter(this.character, false);
        this.airAndWindTemp = this.climate.getAirTemperatureForCharacter(this.character, true);
        this.externalAirTemperature = this.airTemperature;
        this.setPoint = 37.0f;
        if (this.stats.isAboveMinimum(CharacterStat.SICKNESS)) {
            this.setPoint += this.stats.get(CharacterStat.SICKNESS) * 2.0f;
        }
        this.updateCoreRateOfChange();
        this.updateMetabolicRate();
        this.updateClothing();
        this.updateTest();
    }

    private void updateMetabolicRate() {
        this.setMetabolicTarget(Metabolics.Default.getMet());
        if (this.player != null) {
            if (this.player.isAttacking()) {
                WeaponType weaponType = WeaponType.getWeaponType(this.player);
                switch (weaponType) {
                    case UNARMED: {
                        this.setMetabolicTarget(Metabolics.MediumWork);
                        break;
                    }
                    case TWO_HANDED: {
                        this.setMetabolicTarget(Metabolics.HeavyWork);
                        break;
                    }
                    case ONE_HANDED: {
                        this.setMetabolicTarget(Metabolics.MediumWork);
                        break;
                    }
                    case HEAVY: {
                        this.setMetabolicTarget(Metabolics.Running15kmh);
                        break;
                    }
                    case KNIFE: {
                        this.setMetabolicTarget(Metabolics.LightWork);
                        break;
                    }
                    case SPEAR: {
                        this.setMetabolicTarget(Metabolics.MediumWork);
                        break;
                    }
                    case HANDGUN: {
                        this.setMetabolicTarget(Metabolics.UsingTools);
                        break;
                    }
                    case FIREARM: {
                        this.setMetabolicTarget(Metabolics.LightWork);
                        break;
                    }
                    case THROWING: {
                        this.setMetabolicTarget(Metabolics.MediumWork);
                        break;
                    }
                    case CHAINSAW: {
                        this.setMetabolicTarget(Metabolics.Running15kmh);
                    }
                }
            }
            if (this.player.isPlayerMoving()) {
                if (this.player.isSprinting()) {
                    this.setMetabolicTarget(Metabolics.Running15kmh);
                } else if (this.player.isRunning()) {
                    this.setMetabolicTarget(Metabolics.Running10kmh);
                } else if (this.player.isSneaking()) {
                    this.setMetabolicTarget(Metabolics.Walking2kmh);
                } else if (this.player.currentSpeed > 0.0f) {
                    this.setMetabolicTarget(Metabolics.Walking5kmh);
                }
            }
        }
        float excercise = PZMath.clamp_01(1.0f - this.stats.get(CharacterStat.ENDURANCE)) * Metabolics.DefaultExercise.getMet();
        this.setMetabolicTarget(excercise * this.getEnergy());
        float weight = PZMath.clamp_01(this.player.getInventory().getCapacityWeight() / (float)this.player.getMaxWeight());
        float ratio = 1.0f + weight * weight * 0.35f;
        this.setMetabolicTarget(this.metabolicTarget * ratio);
        if (!PZMath.equal(this.metabolicRate, this.metabolicTarget)) {
            float rate = this.metabolicTarget - this.metabolicRate;
            this.metabolicRate = this.metabolicTarget > this.metabolicRate ? (this.metabolicRate += rate * this.getSimulationMultiplier(Multiplier.MetabolicRateInc)) : (this.metabolicRate += rate * this.getSimulationMultiplier(Multiplier.MetabolicRateDec));
        }
        float metaMod = 1.0f;
        if (this.player.getMoodles().getMoodleLevel(MoodleType.HYPOTHERMIA) >= 1) {
            metaMod = this.getMovementModifier();
        }
        this.metabolicRateReal = this.metabolicRate * (0.2f + 0.8f * this.getEnergy() * metaMod);
        this.metabolicTarget = -1.0f;
    }

    private void updateTest() {
        float nodeHeatTotal = 0.0f;
        for (int i = 0; i < this.nodes.length; ++i) {
            ThermalNode node = this.nodes[i];
            node.calculateInsulation();
            float externalTemp = this.airTemperature;
            if (this.airAndWindTemp < this.airTemperature) {
                externalTemp -= (this.airTemperature - this.airAndWindTemp) / (1.0f + node.windresist);
            }
            node.heatDelta = externalTemp - node.skinCelcius;
            node.heatDelta /= 1.0f + node.insulation;
            node.heatDelta /= 3.75f;
            float metabolics = this.metabolicRateReal;
            node.heatDelta += (metabolics *= 1.0f + node.insulation * 0.25f);
            float influence = 1.0f;
            nodeHeatTotal += node.heatDelta * node.skinSurface * 1.0f;
            float skinMin = this.core.celcius - 20.0f;
            float skinMax = this.core.celcius;
            if (skinMin < this.airTemperature) {
                skinMin = this.airTemperature;
            }
            if (skinMin > skinMax) {
                skinMin = skinMax;
            }
            float skinTarget = node.skinCelcius + node.heatDelta;
            skinTarget = PZMath.clamp(skinTarget, skinMin, skinMax);
            float skinDelta = skinTarget - node.skinCelcius;
            float multi = 0.0025f;
            if (node.skinCelcius < 33.0f && skinDelta < 0.0f || node.skinCelcius > 33.0f && skinDelta > 0.0f) {
                multi = 2.5E-4f;
            }
            node.skinCelcius += skinDelta * multi;
        }
        this.coreHeatDelta = nodeHeatTotal;
        float coreMulti = 0.0025f;
        if (this.core.celcius < this.setPoint && this.coreHeatDelta < 0.0f) {
            coreMulti = 5.0E-5f;
        }
        if (this.core.celcius > this.setPoint && this.coreHeatDelta > 0.0f) {
            coreMulti = 2.5E-4f;
        }
        this.core.celcius += this.coreHeatDelta * coreMulti;
    }

    private void updateTest_4() {
        float nodeHeatTotal = 0.0f;
        for (int i = 0; i < this.nodes.length; ++i) {
            float coreSkinDelta;
            ThermalNode node = this.nodes[i];
            node.calculateInsulation();
            float nodeDistCoreInv = 1.0f - node.getDistToCore();
            float externalTemp = this.airTemperature;
            if (this.airAndWindTemp < this.airTemperature) {
                externalTemp -= (this.airTemperature - this.airAndWindTemp) / (1.0f + node.windresist);
            }
            node.heatDelta = externalTemp - node.skinCelcius;
            node.heatDelta /= 1.0f + node.insulation;
            node.heatDelta /= 12.0f;
            float influence = 1.0f;
            nodeHeatTotal += node.heatDelta * node.skinSurface * 1.0f;
            float skinMin = this.core.celcius - 20.0f;
            float skinMax = this.core.celcius;
            if (skinMin < this.airTemperature) {
                skinMin = this.airTemperature;
            }
            if (skinMin > skinMax) {
                skinMin = skinMax;
            }
            if ((coreSkinDelta = this.core.celcius - this.setPoint) > 0.0f) {
                coreSkinDelta *= 2.0f;
                coreSkinDelta *= 1.0f + nodeDistCoreInv * 0.25f;
            } else {
                coreSkinDelta *= 1.0f + node.distToCore * 0.25f;
            }
            float skinTarget = node.skinCelcius + node.heatDelta + coreSkinDelta;
            skinTarget = PZMath.clamp(skinTarget, skinMin, skinMax);
            float skinDelta = skinTarget - node.skinCelcius;
            float multi = 0.0025f;
            if (node.skinCelcius < 33.0f && skinDelta < 0.0f || node.skinCelcius > 33.0f && skinDelta > 0.0f) {
                multi = 2.5E-4f;
            }
            node.skinCelcius += skinDelta * multi;
        }
        float metabolicDelta = this.metabolicRateReal / Metabolics.Default.getMet() - 1.0f;
        if (metabolicDelta > 0.0f) {
            metabolicDelta /= Metabolics.Default.getMet() * 2.0f;
        }
        this.coreHeatDelta = nodeHeatTotal + metabolicDelta;
        float coreMulti = 0.0025f;
        if (this.core.celcius < this.setPoint && this.coreHeatDelta < 0.0f) {
            coreMulti = 5.0E-5f;
        }
        if (this.core.celcius > this.setPoint && this.coreHeatDelta > 0.0f) {
            coreMulti = 2.5E-4f;
        }
        this.core.celcius += this.coreHeatDelta * coreMulti;
    }

    private void updateTest_3() {
        for (int i = 0; i < this.nodes.length; ++i) {
            ThermalNode node = this.nodes[i];
            node.calculateInsulation();
            float externalTemp = this.airTemperature;
            if (this.airAndWindTemp < this.airTemperature) {
                externalTemp -= (this.airTemperature - this.airAndWindTemp) / (1.0f + node.windresist);
            }
            node.heatDelta = externalTemp - node.skinCelcius;
            node.heatDelta /= 1.0f + node.insulation;
            float nodeDistCoreMod = 1.0f + (1.0f - node.getDistToCore() * 0.5f);
            float nodeMet = this.metabolicRateReal * nodeDistCoreMod;
            node.heatDelta += (nodeMet *= 1.0f + node.insulation * 0.25f);
            float skinMin = this.core.celcius - 20.0f;
            float skinMax = this.core.celcius;
            if (skinMin < this.airTemperature) {
                skinMin = this.airTemperature;
            }
            if (skinMin > skinMax) {
                skinMin = skinMax;
            }
            float skinTarget = node.skinCelcius + node.heatDelta / 12.0f;
            skinTarget = PZMath.clamp(skinTarget, skinMin, skinMax);
            float skinDelta = skinTarget - node.skinCelcius;
            node.skinCelcius += skinDelta * this.getSimulationMultiplier(Multiplier.SkinCelcius);
        }
    }

    private void updateTest_2() {
        ThermalNode node;
        int i;
        for (i = 0; i < this.nodes.length; ++i) {
            node = this.nodes[i];
            node.calculateInsulation();
            float externalTemp = this.airTemperature;
            if (this.airAndWindTemp < this.airTemperature) {
                externalTemp = (this.airTemperature - this.airAndWindTemp) / (1.0f + node.windresist);
            }
            node.heatDelta = externalTemp - 27.0f;
            node.heatDelta /= 1.0f + node.insulation;
            float nodeDistCoreMod = 1.0f + (1.0f - node.getDistToCore() * 0.8f);
            float nodeMet = this.metabolicRateReal * nodeDistCoreMod;
            node.heatDelta += (nodeMet *= 1.0f + node.insulation);
            float primTarget = node.heatDelta / 12.0f;
            primTarget = PZMath.clamp(primTarget, -1.0f, 1.0f);
            float primDelta = primTarget - node.primaryDelta;
            node.primaryDelta += primDelta * this.getSimulationMultiplier(Multiplier.PrimaryDelta);
            node.primaryDelta = PZMath.clamp(node.primaryDelta, -1.0f, 1.0f);
        }
        for (i = 0; i < this.nodes.length; ++i) {
            node = this.nodes[i];
            float skinMin = this.core.celcius - 20.0f;
            float skinMax = this.core.celcius;
            if (skinMin < this.airTemperature) {
                skinMin = this.airTemperature;
            }
            float skinTarget = node.primaryDelta < 0.0f ? 33.0f - 20.0f * PZMath.abs(node.primaryDelta) : 33.0f + 20.0f * node.primaryDelta;
            node.skinCelcius = skinTarget = PZMath.clamp(skinTarget, skinMin, skinMax);
        }
    }

    private void updateTest_OLD() {
        ThermalNode node;
        int i;
        for (i = 0; i < this.nodes.length; ++i) {
            node = this.nodes[i];
            node.calculateInsulation();
            float externalTemp = this.airTemperature;
            if (this.airAndWindTemp < this.airTemperature) {
                externalTemp = (this.airTemperature - this.airAndWindTemp) / (1.0f + node.windresist);
            }
            node.heatDelta = externalTemp - node.skinCelcius;
            node.heatDelta /= 1.0f + node.insulation;
            float nodeDistCoreMod = 1.0f + (1.0f - node.getDistToCore() * 0.8f);
            float nodeMet = this.metabolicRateReal * nodeDistCoreMod;
            node.heatDelta += (nodeMet *= 1.0f + node.insulation);
            node.primaryDelta = node.heatDelta / 12.0f;
            node.primaryDelta = PZMath.clamp(node.primaryDelta, -1.0f, 1.0f);
        }
        for (i = 0; i < this.nodes.length; ++i) {
            node = this.nodes[i];
            float skinMin = this.core.celcius - 20.0f;
            float skinMax = this.core.celcius;
            if (skinMin < this.airTemperature) {
                skinMin = this.airTemperature;
            }
            float skinTarget = node.primaryDelta < 0.0f ? 33.0f - 20.0f * PZMath.abs(node.primaryDelta) : 33.0f + 20.0f * node.primaryDelta;
            node.skinCelcius = skinTarget = PZMath.clamp(skinTarget, skinMin, skinMax);
        }
    }

    private void updateNodesHeatDelta() {
        float weightDelta = PZMath.clamp_01((float)((this.player.getNutrition().getWeight() / 75.0 - 0.5) * (double)0.666f));
        weightDelta = (weightDelta - 0.5f) * 2.0f;
        float fitnessDelta = this.stats.get(CharacterStat.FITNESS);
        float primHeatMod = 1.0f;
        if (this.airAndWindTemp > this.setPoint - 2.0f) {
            if (this.airTemperature < this.setPoint + 2.0f) {
                primHeatMod = (this.airTemperature - (this.setPoint - 2.0f)) / 4.0f;
                primHeatMod = 1.0f - primHeatMod;
            } else {
                primHeatMod = 0.0f;
            }
        }
        float humidityMod = 1.0f;
        if (this.climate.getHumidity() > 0.5f) {
            float hum = (this.climate.getHumidity() - 0.5f) * 2.0f;
            humidityMod -= hum;
        }
        float nodesTotalHeat = 0.0f;
        for (int i = 0; i < this.nodes.length; ++i) {
            float energy;
            float contribution;
            float fluids;
            float nodeHeatDelta;
            ThermalNode node = this.nodes[i];
            node.calculateInsulation();
            float externalTemp = this.airTemperature;
            if (this.airAndWindTemp < this.airTemperature) {
                externalTemp = (this.airTemperature - this.airAndWindTemp) / (1.0f + node.windresist);
            }
            nodeHeatDelta = (nodeHeatDelta = externalTemp - node.skinCelcius) <= 0.0f ? (nodeHeatDelta *= 1.0f + 0.75f * node.bodyWetness) : (nodeHeatDelta /= 1.0f + 3.0f * node.bodyWetness);
            nodeHeatDelta *= 0.3f;
            node.heatDelta = (nodeHeatDelta /= 1.0f + node.insulation) * node.skinSurface;
            if (node.primaryDelta > 0.0f) {
                fluids = 0.2f + 0.8f * this.getBodyFluids();
                contribution = Metabolics.Default.getMet() * node.primaryDelta * node.skinSurface / node.insulation;
                contribution *= fluids * (0.1f + 0.9f * primHeatMod);
                contribution *= humidityMod;
                contribution *= 1.0f - 0.2f * weightDelta;
                node.heatDelta -= (contribution *= 1.0f + 0.2f * fitnessDelta);
            } else {
                energy = 0.2f + 0.8f * this.getEnergy();
                contribution = Metabolics.Default.getMet() * PZMath.abs(node.primaryDelta) * node.skinSurface;
                contribution *= energy;
                contribution *= 1.0f + 0.2f * weightDelta;
                node.heatDelta += (contribution *= 1.0f + 0.2f * fitnessDelta);
            }
            if (node.secondaryDelta > 0.0f) {
                fluids = 0.1f + 0.9f * this.getBodyFluids();
                contribution = Metabolics.MAX.getMet() * 0.75f * node.secondaryDelta * node.skinSurface / node.insulation;
                contribution *= fluids;
                contribution *= 0.85f + 0.15f * humidityMod;
                contribution *= 1.0f - 0.2f * weightDelta;
                node.heatDelta -= (contribution *= 1.0f + 0.2f * fitnessDelta);
            } else {
                energy = 0.1f + 0.9f * this.getEnergy();
                contribution = Metabolics.Default.getMet() * PZMath.abs(node.secondaryDelta) * node.skinSurface;
                contribution *= energy;
                contribution *= 1.0f + 0.2f * weightDelta;
                node.heatDelta += (contribution *= 1.0f + 0.2f * fitnessDelta);
            }
            nodesTotalHeat += node.heatDelta;
        }
        this.totalHeatRaw = nodesTotalHeat;
        this.totalHeat = nodesTotalHeat += this.metabolicRateReal;
    }

    private void updateNodesHeatDelta_OLD() {
        float weight = (float)this.player.getNutrition().getWeight();
        float weightMod = PZMath.clamp_01((weight / 75.0f - 0.5f) * 0.666f);
        float weightDelta = (weightMod - 0.5f) * 2.0f;
        float fitnessDelta = this.stats.get(CharacterStat.FITNESS);
        float primHeatMod = 1.0f;
        if (this.airAndWindTemp > this.setPoint - 2.0f) {
            if (this.airTemperature < this.setPoint + 2.0f) {
                primHeatMod = (this.airTemperature - (this.setPoint - 2.0f)) / 4.0f;
                primHeatMod = 1.0f - primHeatMod;
            } else {
                primHeatMod = 0.0f;
            }
        }
        float humidityMod = 1.0f;
        if (this.climate.getHumidity() > 0.5f) {
            float hum = (this.climate.getHumidity() - 0.5f) * 2.0f;
            humidityMod -= hum;
        }
        float coreNegTempMod = 1.0f;
        if (this.core.celcius < 37.0f) {
            coreNegTempMod = (this.core.celcius - 20.0f) / 17.0f;
            coreNegTempMod *= coreNegTempMod;
        }
        float nodesTotalHeat = 0.0f;
        for (int i = 0; i < this.nodes.length; ++i) {
            float energy;
            float contribution;
            float fluids;
            ThermalNode node = this.nodes[i];
            node.calculateInsulation();
            float extTemp = this.airTemperature;
            if (this.airAndWindTemp < this.airTemperature) {
                float diff = this.airTemperature - this.airAndWindTemp;
                extTemp -= (diff /= 1.0f + node.windresist);
            }
            float insulation = 1.0f + node.insulation;
            float heatDelta = extTemp - node.skinCelcius;
            if (heatDelta < 0.0f) {
                heatDelta *= 1.0f + 0.25f * node.bodyWetness;
            } else if (heatDelta > 0.0f) {
                heatDelta /= 1.0f + node.bodyWetness * 4.0f;
            }
            heatDelta *= 0.3f;
            node.heatDelta = (heatDelta /= insulation) * node.skinSurface;
            if (node.primaryDelta > 0.0f) {
                fluids = 0.2f + 0.8f * this.getBodyFluids();
                contribution = Metabolics.Default.getMet() * 1.0f * node.primaryDelta * node.skinSurface / insulation;
                contribution *= fluids * (0.1f + 0.9f * primHeatMod);
                contribution *= humidityMod;
                contribution *= 1.0f - 0.2f * weightDelta;
                node.heatDelta -= (contribution *= 1.0f + 0.2f * fitnessDelta);
            } else {
                energy = 0.2f + 0.8f * this.getEnergy();
                contribution = Metabolics.Default.getMet() * 1.0f * PZMath.abs(node.primaryDelta) * node.skinSurface;
                contribution *= energy;
                contribution *= 1.0f + 0.2f * weightDelta;
                contribution *= 1.0f + 0.2f * fitnessDelta;
                node.heatDelta += (contribution *= 0.25f + 0.75f * coreNegTempMod);
            }
            if (node.secondaryDelta > 0.0f) {
                fluids = 0.1f + 0.9f * this.getBodyFluids();
                contribution = Metabolics.MAX.getMet() * 0.75f * node.secondaryDelta * node.skinSurface / insulation;
                contribution *= fluids;
                contribution *= 0.75f + 0.25f * humidityMod;
                contribution *= 1.0f - 0.2f * weightDelta;
                node.heatDelta -= (contribution *= 1.0f + 0.2f * fitnessDelta);
            } else {
                energy = 0.1f + 0.9f * this.getEnergy();
                contribution = Metabolics.Default.getMet() * 1.0f * PZMath.abs(node.secondaryDelta) * node.skinSurface;
                contribution *= energy;
                contribution *= 1.0f + 0.2f * weightDelta;
                contribution *= 1.0f + 0.2f * fitnessDelta;
                node.heatDelta += (contribution *= 0.25f + 0.75f * coreNegTempMod);
            }
            nodesTotalHeat += node.heatDelta;
        }
        this.totalHeatRaw = nodesTotalHeat;
        this.totalHeat = nodesTotalHeat += this.metabolicRateReal;
    }

    private void updateHeatDeltas() {
        this.coreHeatDelta = this.totalHeat * this.getSimulationMultiplier(Multiplier.BodyHeat);
        if (this.coreHeatDelta < 0.0f) {
            if (this.core.celcius > this.setPoint) {
                this.coreHeatDelta *= 1.0f + (this.core.celcius - this.setPoint) / 2.0f;
            }
        } else if (this.core.celcius < this.setPoint) {
            this.coreHeatDelta *= 1.0f + (this.setPoint - this.core.celcius) / 4.0f;
        }
        this.core.celcius += this.coreHeatDelta;
        this.core.celcius = PZMath.clamp(this.core.celcius, 20.0f, 42.0f);
        this.stats.set(CharacterStat.TEMPERATURE, this.core.celcius);
        this.bodyHeatDelta = 0.0f;
        if (this.core.celcius > this.setPoint) {
            this.bodyHeatDelta = this.core.celcius - this.setPoint;
        } else if (this.core.celcius < this.setPoint) {
            this.bodyHeatDelta = this.core.celcius - this.setPoint;
        }
        if (this.bodyHeatDelta < 0.0f) {
            float bhd = PZMath.abs(this.bodyHeatDelta);
            if (bhd <= 1.0f) {
                this.bodyHeatDelta *= 0.8f;
            } else {
                bhd = PZMath.clamp(bhd, 1.0f, 11.0f) - 1.0f;
                this.bodyHeatDelta = -0.8f + -0.2f * (bhd /= 10.0f);
            }
        }
        this.bodyHeatDelta = PZMath.clamp(this.bodyHeatDelta, -1.0f, 1.0f);
    }

    private void updateNodes() {
        float prim = 0.0f;
        float sec = 0.0f;
        for (int i = 0; i < this.nodes.length; ++i) {
            ThermalNode node = this.nodes[i];
            float insulation = 1.0f + node.insulation;
            if (this.bodyHeatDelta < 0.0f) {
                float dist = node.distToCore;
                node.primaryDelta = this.bodyHeatDelta * (1.0f + dist);
            } else {
                node.primaryDelta = this.bodyHeatDelta * (1.0f + (1.0f - node.distToCore));
            }
            node.primaryDelta = PZMath.clamp(node.primaryDelta, -1.0f, 1.0f);
            node.secondaryDelta = node.primaryDelta * PZMath.abs(node.primaryDelta) * PZMath.abs(node.primaryDelta);
            prim += node.primaryDelta * node.skinSurface;
            sec += node.secondaryDelta * node.skinSurface;
            if (this.stats.get(CharacterStat.INTOXICATION) > 0.0f) {
                node.primaryDelta += this.stats.get(CharacterStat.INTOXICATION) * 0.02f;
            }
            node.primaryDelta = PZMath.clamp(node.primaryDelta, -1.0f, 1.0f);
            float skinMin = this.core.celcius - 20.0f;
            float skinMax = this.core.celcius;
            if (skinMin < this.airTemperature) {
                if (this.airTemperature < 33.0f) {
                    skinMin = this.airTemperature;
                } else {
                    float distCore = 0.4f + 0.6f * (1.0f - node.distToCore);
                    float skinmod = (this.airTemperature - 33.0f) / 6.0f;
                    skinMin = 33.0f;
                    skinMin += 4.0f * skinmod * distCore;
                    if ((skinMin = PZMath.clamp(skinMin, 33.0f, this.airTemperature)) > skinMax) {
                        skinMin = skinMax - 0.25f;
                    }
                }
            }
            float skinTarget = this.core.celcius - 4.0f;
            if (node.primaryDelta < 0.0f) {
                distCore = 0.4f + 0.6f * node.distToCore;
                target = skinTarget - 12.0f * distCore / insulation;
                skinTarget = PZMath.c_lerp(skinTarget, target, PZMath.abs(node.primaryDelta));
            } else {
                distCore = 0.4f + 0.6f * (1.0f - node.distToCore);
                target = skinTarget;
                float targetAdd = 4.0f * distCore;
                target = Math.min(target + (targetAdd *= Math.max(insulation * 0.5f * distCore, 1.0f)), skinMax);
                skinTarget = PZMath.c_lerp(skinTarget, target, node.primaryDelta);
            }
            skinTarget = PZMath.clamp(skinTarget, skinMin, skinMax);
            float skinDelta = skinTarget - node.skinCelcius;
            float multipl = 1.0f;
            if (skinDelta < 0.0f && node.skinCelcius > 33.0f) {
                multipl = 3.0f;
            } else if (skinDelta > 0.0f && node.skinCelcius < 33.0f) {
                multipl = 3.0f;
            }
            node.skinCelcius += skinDelta * multipl * this.getSimulationMultiplier(Multiplier.SkinCelcius);
            if (node == this.core) continue;
            node.celcius = node.skinCelcius >= this.core.celcius ? this.core.celcius : PZMath.lerp(node.skinCelcius, this.core.celcius, 0.5f);
        }
        this.primTotal = prim;
        this.secTotal = sec;
    }

    private void updateBodyMultipliers() {
        this.energyMultiplier = 1.0;
        this.fluidsMultiplier = 1.0;
        this.fatigueMultiplier = 1.0;
        float metaEnergy = this.metabolicRateReal / Metabolics.Default.getMet();
        this.energyMultiplier = PZMath.clamp(metaEnergy * metaEnergy, 1.0f, 5.0f);
        float mod = PZMath.abs(this.primTotal);
        mod *= mod;
        if (this.primTotal < 0.0f) {
            this.energyMultiplier += (double)(0.75f * mod);
            this.fatigueMultiplier += (double)(0.5f * mod);
        } else if (this.primTotal > 0.0f) {
            this.fluidsMultiplier += (double)(0.75f * mod);
            this.fatigueMultiplier += (double)(0.5f * mod);
        }
        mod = PZMath.abs(this.secTotal);
        mod *= mod;
        if (this.secTotal < 0.0f) {
            this.energyMultiplier += (double)(8.0f * mod);
            this.fatigueMultiplier += (double)(3.5f * mod);
        } else if (this.secTotal > 0.0f) {
            this.fluidsMultiplier += (double)(6.0f * mod);
            this.fatigueMultiplier += (double)(3.5f * mod);
        }
    }

    private void updateClothing() {
        int i;
        boolean doUpdate;
        this.character.getItemVisuals(itemVisuals);
        boolean bl = doUpdate = itemVisuals.size() != itemVisualsCache.size();
        if (!doUpdate) {
            for (i = 0; i < itemVisuals.size(); ++i) {
                if (i < itemVisualsCache.size() && itemVisuals.get(i) == itemVisualsCache.get(i)) continue;
                doUpdate = true;
                break;
            }
        }
        if (doUpdate) {
            for (i = 0; i < this.nodes.length; ++i) {
                this.nodes[i].clothing.clear();
            }
            itemVisualsCache.clear();
            for (i = 0; i < itemVisuals.size(); ++i) {
                ItemBodyLocation bodyLocation;
                Clothing clothing;
                ItemVisual itemVisual = (ItemVisual)itemVisuals.get(i);
                InventoryItem item = itemVisual.getInventoryItem();
                itemVisualsCache.add(itemVisual);
                if (!(item instanceof Clothing) || !((clothing = (Clothing)item).getInsulation() > 0.0f) && !(clothing.getWindresistance() > 0.0f)) continue;
                boolean added = false;
                ArrayList<BloodClothingType> types = item.getBloodClothingType();
                if (types != null) {
                    coveredParts.clear();
                    BloodClothingType.getCoveredParts(types, coveredParts);
                    for (int j = 0; j < coveredParts.size(); ++j) {
                        BloodBodyPartType part = coveredParts.get(j);
                        if (part.index() < 0 || part.index() >= this.nodes.length) continue;
                        added = true;
                        this.nodes[part.index()].clothing.add(clothing);
                    }
                }
                if (added || clothing.getBodyLocation() == null || !(bodyLocation = clothing.getBodyLocation()).equals(ItemBodyLocation.HAT) && !bodyLocation.equals(ItemBodyLocation.MASK)) continue;
                this.nodes[BodyPartType.ToIndex((BodyPartType)BodyPartType.Head)].clothing.add(clothing);
            }
        }
    }

    public float getEnergy() {
        float h = 1.0f - this.stats.get(CharacterStat.HUNGER) * this.stats.get(CharacterStat.HUNGER);
        float f = 1.0f - this.stats.get(CharacterStat.FATIGUE) * this.stats.get(CharacterStat.FATIGUE);
        return 0.6f * h + 0.4f * f;
    }

    public float getBodyFluids() {
        return 1.0f - this.stats.get(CharacterStat.THIRST);
    }

    public class ThermalNode {
        private final float distToCore;
        private final float skinSurface;
        private final BodyPartType bodyPartType;
        private final BloodBodyPartType bloodBpt;
        private final BodyPart bodyPart;
        private final boolean isCore;
        private final float insulationLayerMultiplierUi;
        private ThermalNode upstream;
        private ThermalNode[] downstream;
        private float insulation;
        private float windresist;
        private float celcius;
        private float skinCelcius;
        private float heatDelta;
        private float primaryDelta;
        private float secondaryDelta;
        private float clothingWetness;
        private float bodyWetness;
        private final ArrayList<Clothing> clothing;

        public ThermalNode(Thermoregulator_tryouts this$0, float initTemperature, BodyPart bodyPart, float insulationMultiplier) {
            this(this$0, false, initTemperature, bodyPart, insulationMultiplier);
        }

        public ThermalNode(Thermoregulator_tryouts this$0, boolean isCore, float initTemperature, BodyPart bodyPart, float insulationMultiplier) {
            Objects.requireNonNull(this$0);
            this.skinCelcius = 33.0f;
            this.clothing = new ArrayList();
            this.isCore = isCore;
            this.celcius = initTemperature;
            this.distToCore = BodyPartType.GetDistToCore(bodyPart.type);
            this.skinSurface = BodyPartType.GetSkinSurface(bodyPart.type);
            this.bodyPartType = bodyPart.type;
            this.bloodBpt = BloodBodyPartType.FromIndex(BodyPartType.ToIndex(bodyPart.type));
            this.bodyPart = bodyPart;
            this.insulationLayerMultiplierUi = insulationMultiplier;
        }

        private void calculateInsulation() {
            int layers = this.clothing.size();
            this.insulation = 0.0f;
            this.windresist = 0.0f;
            this.clothingWetness = 0.0f;
            this.bodyWetness = this.bodyPart != null ? this.bodyPart.getWetness() * 0.01f : 0.0f;
            this.bodyWetness = PZMath.clamp_01(this.bodyWetness);
            if (layers > 0) {
                for (int i = 0; i < layers; ++i) {
                    boolean hasHole;
                    Clothing item = this.clothing.get(i);
                    ItemVisual itemVisual = item.getVisual();
                    float itemWetness = PZMath.clamp(item.getWetness() * 0.01f, 0.0f, 1.0f);
                    this.clothingWetness += itemWetness;
                    boolean bl = hasHole = itemVisual.getHole(this.bloodBpt) > 0.0f;
                    if (hasHole) continue;
                    float itemInsulation = Temperature.getTrueInsulationValue(item.getInsulation());
                    float itemWindResist = Temperature.getTrueWindresistanceValue(item.getWindresistance());
                    float itemCondition = PZMath.clamp(item.getCurrentCondition() * 0.01f, 0.0f, 1.0f);
                    itemCondition = 0.5f + 0.5f * itemCondition;
                    this.insulation += (itemInsulation *= (1.0f - itemWetness * 0.75f) * itemCondition);
                    this.windresist += (itemWindResist *= (1.0f - itemWetness * 0.45f) * itemCondition);
                }
                this.clothingWetness /= (float)layers;
                this.insulation += (float)layers * 0.05f;
                this.windresist += (float)layers * 0.05f;
            }
        }

        public boolean hasUpstream() {
            return this.upstream != null;
        }

        public boolean hasDownstream() {
            return this.downstream != null && this.downstream.length > 0;
        }

        public float getDistToCore() {
            return this.distToCore;
        }

        public float getSkinSurface() {
            return this.skinSurface;
        }

        public boolean isCore() {
            return this.isCore;
        }

        public float getInsulation() {
            return this.insulation;
        }

        public float getWindresist() {
            return this.windresist;
        }

        public float getCelcius() {
            return this.celcius;
        }

        public float getSkinCelcius() {
            return this.skinCelcius;
        }

        public float getHeatDelta() {
            return this.heatDelta;
        }

        public float getPrimaryDelta() {
            return this.primaryDelta;
        }

        public float getSecondaryDelta() {
            return this.secondaryDelta;
        }

        public float getClothingWetness() {
            return this.clothingWetness;
        }

        public float getBodyWetness() {
            return this.bodyWetness;
        }

        public float getBodyResponse() {
            return PZMath.lerp(this.primaryDelta, this.secondaryDelta, 0.5f);
        }

        public float getSkinCelciusUI() {
            float v = PZMath.clamp(this.getSkinCelcius(), 20.0f, 42.0f);
            v = v < 33.0f ? (v - 20.0f) / 13.0f * 0.5f : 0.5f + (v - 33.0f) / 9.0f;
            return v;
        }

        public float getHeatDeltaUI() {
            return PZMath.clamp((this.heatDelta * 0.2f + 1.0f) / 2.0f, 0.0f, 1.0f);
        }

        public float getPrimaryDeltaUI() {
            return PZMath.clamp((this.primaryDelta + 1.0f) / 2.0f, 0.0f, 1.0f);
        }

        public float getSecondaryDeltaUI() {
            return PZMath.clamp((this.secondaryDelta + 1.0f) / 2.0f, 0.0f, 1.0f);
        }

        public float getInsulationUI() {
            return PZMath.clamp(this.insulation * this.insulationLayerMultiplierUi, 0.0f, 1.0f);
        }

        public float getWindresistUI() {
            return PZMath.clamp(this.windresist * this.insulationLayerMultiplierUi, 0.0f, 1.0f);
        }

        public float getClothingWetnessUI() {
            return PZMath.clamp(this.clothingWetness, 0.0f, 1.0f);
        }

        public float getBodyWetnessUI() {
            return PZMath.clamp(this.bodyWetness, 0.0f, 1.0f);
        }

        public float getBodyResponseUI() {
            return PZMath.clamp((this.getBodyResponse() + 1.0f) / 2.0f, 0.0f, 1.0f);
        }
    }

    private static enum Multiplier {
        Default,
        MetabolicRateInc,
        MetabolicRateDec,
        BodyHeat,
        CoreHeatExpand,
        CoreHeatContract,
        SkinCelcius,
        SkinCelciusContract,
        SkinCelciusExpand,
        PrimaryDelta,
        SecondaryDelta;

    }
}

