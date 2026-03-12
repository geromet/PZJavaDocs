/*
 * Decompiled with CFR 0.152.
 */
package zombie.network.packets;

import java.io.IOException;
import java.util.ArrayList;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.entity.components.fluids.FluidContainer;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Food;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.ContainerID;
import zombie.network.packets.INetworkPacket;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;

@PacketSetting(ordering=0, priority=1, reliability=2, requiredCapability=Capability.LoginOnServer, handlingType=3)
public class ItemStatsPacket
implements INetworkPacket {
    private static final int BIT_IS_EMPTY = 0;
    private static final int BIT_IS_FROZEN = 1;
    private static final int BIT_IS_TAINTED = 2;
    private static final int BIT_IS_COOKED = 4;
    private static final int BIT_IS_BURNED = 8;
    private static final int BIT_COOKING_TIME = 16;
    private static final int BIT_MINUTES_TO_COOK = 32;
    private static final int BIT_MINUTES_TO_BURN = 64;
    private static final int BIT_HUNG_CHANGE = 128;
    private static final int BIT_CALORIES = 256;
    private static final int BIT_THIRST_CHANGE = 512;
    private static final int BIT_FLU_REDUCTION = 1024;
    private static final int BIT_PAIN_REDUCTION = 2048;
    private static final int BIT_END_CHANGE = 4096;
    private static final int BIT_FOOD_SICKNESS_CHANGE = 8192;
    private static final int BIT_STRESS_CHANGE = 16384;
    private static final int BIT_FATIGUE_CHANGE = 32768;
    private static final int BIT_UNHAPPY_CHANGE = 65536;
    private static final int BIT_BOREDOM_CHANGE = 131072;
    private static final int BIT_POISON_POWER = 262144;
    private static final int BIT_POISON_DETECTION_LEVEL = 524288;
    private static final int BIT_IS_ALCOHOLIC = 0x100000;
    private static final int BIT_BASE_HUNGER = 0x200000;
    private static final int BIT_EXTRA_ITEMS = 0x400000;
    private static final int BIT_SPICES = 0x800000;
    private static final int BIT_IS_CUSTOM_NAME = 0x1000000;
    ContainerID containerId = new ContainerID();
    @JSONField
    int id;
    @JSONField
    int uses;
    @JSONField
    float usedDelta;
    @JSONField
    boolean isFood;
    @JSONField
    boolean isFrozen;
    @JSONField
    float heat;
    @JSONField
    float cookingTime;
    @JSONField
    float minutesToCook;
    @JSONField
    float minutesToBurn;
    @JSONField
    float hungChange;
    @JSONField
    float calories;
    @JSONField
    float carbohydrates;
    @JSONField
    float lipids;
    @JSONField
    float proteins;
    @JSONField
    float thirstChange;
    @JSONField
    int fluReduction;
    @JSONField
    float painReduction;
    @JSONField
    float endChange;
    @JSONField
    int foodSicknessChange;
    @JSONField
    float stressChange;
    @JSONField
    float fatigueChange;
    @JSONField
    float unhappyChange;
    @JSONField
    float boredomChange;
    @JSONField
    int poisonPower;
    @JSONField
    int poisonDetectionLevel;
    @JSONField
    private final ArrayList<String> extraItems = new ArrayList();
    @JSONField
    boolean isAlcoholic;
    @JSONField
    float baseHunger;
    @JSONField
    boolean isCustomName;
    @JSONField
    boolean isTainted;
    @JSONField
    boolean isFluidContainer;
    @JSONField
    FluidContainer fluidContainer = FluidContainer.CreateContainer();
    @JSONField
    boolean isCooked;
    @JSONField
    boolean isBurnt;
    @JSONField
    float freezingTime;
    @JSONField
    String name;
    @JSONField
    float actualWeight;
    @JSONField
    private final ArrayList<String> spices = new ArrayList();

    @Override
    public void setData(Object ... values2) {
        if (values2.length == 2) {
            ItemContainer container = (ItemContainer)values2[0];
            InventoryItem item = (InventoryItem)values2[1];
            if (container.getType().equals("floor") && item.getWorldItem() != null) {
                this.containerId.setFloor(container, item.getWorldItem().square);
            } else {
                this.containerId.set(container);
            }
            this.id = item.id;
            this.uses = item.getCurrentUses();
            this.usedDelta = item instanceof DrainableComboItem ? item.getCurrentUsesFloat() : 0.0f;
            FluidContainer copiedFluidContainer = item.getFluidContainer();
            if (copiedFluidContainer != null) {
                this.isFluidContainer = true;
                this.fluidContainer.setCapacity(copiedFluidContainer.getCapacity());
                this.fluidContainer.copyFluidsFrom(copiedFluidContainer);
            } else {
                this.isFluidContainer = false;
            }
            this.heat = item.getItemHeat();
            if (item instanceof Food) {
                Food food = (Food)item;
                this.isFood = true;
                this.isFrozen = food.isFrozen();
                this.isTainted = food.isTainted();
                this.heat = food.getHeat();
                this.isCooked = food.isCooked();
                this.isBurnt = food.isBurnt();
                this.cookingTime = food.getCookingTime();
                this.minutesToCook = food.getMinutesToCook();
                this.minutesToBurn = food.getMinutesToBurn();
                this.hungChange = food.getHungChange();
                this.calories = food.getCalories();
                this.proteins = food.getProteins();
                this.lipids = food.getLipids();
                this.carbohydrates = food.getCarbohydrates();
                this.thirstChange = food.getThirstChange();
                this.fluReduction = food.getFluReduction();
                this.painReduction = food.getPainReduction();
                this.endChange = food.getEndChange();
                this.foodSicknessChange = food.getFoodSicknessChange();
                this.stressChange = food.getStressChange();
                this.fatigueChange = food.getFatigueChange();
                this.unhappyChange = food.getUnhappyChange();
                this.boredomChange = food.getBoredomChange();
                this.poisonPower = food.getPoisonPower();
                this.poisonDetectionLevel = food.getPoisonDetectionLevel();
                this.isAlcoholic = food.isAlcoholic();
                this.baseHunger = food.getBaseHunger();
                this.extraItems.clear();
                if (food.extraItems != null) {
                    this.extraItems.addAll(food.extraItems);
                }
                this.spices.clear();
                if (food.spices != null) {
                    this.spices.addAll(food.spices);
                }
                this.actualWeight = food.getActualWeight();
                this.isCustomName = food.isCustomName();
                this.name = food.getDisplayName();
            } else {
                this.isFood = false;
            }
        } else {
            DebugLog.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.containerId.write(b);
        b.putInt(this.id);
        b.putInt(this.uses);
        b.putFloat(this.usedDelta);
        if (b.putBoolean(this.isFluidContainer)) {
            try {
                this.fluidContainer.save(b.bb);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        b.putFloat(this.heat);
        if (b.putBoolean(this.isFood)) {
            BitHeaderWrite bits = BitHeader.allocWrite(BitHeader.HeaderSize.Integer, b.bb);
            if (this.isFrozen) {
                bits.addFlags(1);
            }
            if (this.isTainted) {
                bits.addFlags(2);
            }
            if (this.isCooked) {
                bits.addFlags(4);
            }
            if (this.isBurnt) {
                bits.addFlags(8);
            }
            if (this.cookingTime != 0.0f) {
                bits.addFlags(16);
                b.putFloat(this.cookingTime);
            }
            if (this.minutesToCook != 0.0f) {
                bits.addFlags(32);
                b.putFloat(this.minutesToCook);
            }
            if (this.minutesToBurn != 0.0f) {
                bits.addFlags(64);
                b.putFloat(this.minutesToBurn);
            }
            if (this.hungChange != 0.0f) {
                bits.addFlags(128);
                b.putFloat(this.hungChange);
            }
            if (this.calories != 0.0f || this.proteins != 0.0f || this.lipids != 0.0f || this.carbohydrates != 0.0f) {
                bits.addFlags(256);
                b.putFloat(this.calories);
                b.putFloat(this.proteins);
                b.putFloat(this.lipids);
                b.putFloat(this.carbohydrates);
            }
            if (this.thirstChange != 0.0f) {
                bits.addFlags(512);
                b.putFloat(this.thirstChange);
            }
            if (this.fluReduction != 0) {
                bits.addFlags(1024);
                b.putInt(this.fluReduction);
            }
            if (this.painReduction != 0.0f) {
                bits.addFlags(2048);
                b.putFloat(this.painReduction);
            }
            if (this.endChange != 0.0f) {
                bits.addFlags(4096);
                b.putFloat(this.endChange);
            }
            if (this.foodSicknessChange != 0) {
                bits.addFlags(8192);
                b.putInt(this.foodSicknessChange);
            }
            if (this.stressChange != 0.0f) {
                bits.addFlags(16384);
                b.putFloat(this.stressChange);
            }
            if (this.fatigueChange != 0.0f) {
                bits.addFlags(32768);
                b.putFloat(this.fatigueChange);
            }
            if (this.unhappyChange != 0.0f) {
                bits.addFlags(65536);
                b.putFloat(this.unhappyChange);
            }
            if (this.boredomChange != 0.0f) {
                bits.addFlags(131072);
                b.putFloat(this.boredomChange);
            }
            bits.addFlags(262144);
            b.putByte(this.poisonPower);
            if (this.poisonDetectionLevel != -1) {
                bits.addFlags(524288);
                b.putByte(this.poisonDetectionLevel);
            }
            if (this.isAlcoholic) {
                bits.addFlags(0x100000);
            }
            if (this.baseHunger != 0.0f) {
                bits.addFlags(0x200000);
                b.putFloat(this.baseHunger);
            }
            if (!this.extraItems.isEmpty()) {
                bits.addFlags(0x400000);
                b.putByte(this.extraItems.size());
                for (String extraItem : this.extraItems) {
                    b.putUTF(extraItem);
                }
            }
            if (!this.spices.isEmpty()) {
                bits.addFlags(0x800000);
                b.putByte(this.spices.size());
                for (String spice : this.spices) {
                    b.putUTF(spice);
                }
            }
            if (this.isCustomName) {
                bits.addFlags(0x1000000);
            }
            b.putUTF(this.name);
            b.putFloat(this.actualWeight);
            bits.write();
            bits.release();
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.containerId.parse(b, connection);
        this.id = b.getInt();
        this.uses = b.getInt();
        this.usedDelta = b.getFloat();
        this.isFluidContainer = b.getBoolean();
        if (this.isFluidContainer) {
            try {
                this.fluidContainer.load(b.bb, 244);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        this.heat = b.getFloat();
        this.isFood = b.getBoolean();
        if (this.isFood) {
            BitHeaderRead bits = BitHeader.allocRead(BitHeader.HeaderSize.Integer, b.bb);
            if (!bits.equals(0)) {
                int i;
                this.isFrozen = bits.hasFlags(1);
                this.isTainted = bits.hasFlags(2);
                this.isCooked = bits.hasFlags(4);
                this.isBurnt = bits.hasFlags(8);
                if (bits.hasFlags(16)) {
                    this.cookingTime = b.getFloat();
                }
                if (bits.hasFlags(32)) {
                    this.minutesToCook = b.getFloat();
                }
                if (bits.hasFlags(64)) {
                    this.minutesToBurn = b.getFloat();
                }
                if (bits.hasFlags(128)) {
                    this.hungChange = b.getFloat();
                }
                if (bits.hasFlags(256)) {
                    this.calories = b.getFloat();
                    this.proteins = b.getFloat();
                    this.lipids = b.getFloat();
                    this.carbohydrates = b.getFloat();
                }
                if (bits.hasFlags(512)) {
                    this.thirstChange = b.getFloat();
                }
                if (bits.hasFlags(1024)) {
                    this.fluReduction = b.getInt();
                }
                if (bits.hasFlags(2048)) {
                    this.painReduction = b.getFloat();
                }
                if (bits.hasFlags(4096)) {
                    this.endChange = b.getFloat();
                }
                if (bits.hasFlags(8192)) {
                    this.foodSicknessChange = b.getInt();
                }
                if (bits.hasFlags(16384)) {
                    this.stressChange = b.getFloat();
                }
                if (bits.hasFlags(32768)) {
                    this.fatigueChange = b.getFloat();
                }
                if (bits.hasFlags(65536)) {
                    this.unhappyChange = b.getFloat();
                }
                if (bits.hasFlags(131072)) {
                    this.boredomChange = b.getFloat();
                }
                if (bits.hasFlags(262144)) {
                    this.poisonPower = b.getByte();
                }
                if (bits.hasFlags(524288)) {
                    this.poisonDetectionLevel = b.getByte();
                }
                this.isAlcoholic = bits.hasFlags(0x100000);
                if (bits.hasFlags(0x200000)) {
                    this.baseHunger = b.getFloat();
                }
                this.extraItems.clear();
                if (bits.hasFlags(0x400000)) {
                    int extraItemsSize = b.getByte();
                    for (i = 0; i < extraItemsSize; ++i) {
                        this.extraItems.add(b.getUTF());
                    }
                }
                this.spices.clear();
                if (bits.hasFlags(0x800000)) {
                    int spicesSize = b.getByte();
                    for (i = 0; i < spicesSize; ++i) {
                        this.spices.add(b.getUTF());
                    }
                }
                this.isCustomName = bits.hasFlags(0x1000000);
                this.name = b.getUTF();
                this.actualWeight = b.getFloat();
            }
            bits.release();
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        ItemContainer container = this.containerId.getContainer();
        if (container == null) {
            return;
        }
        InventoryItem item = container.getItemWithID(this.id);
        if (item == null) {
            return;
        }
        this.applyItemStats(item);
        this.sendToRelativeClients(PacketTypes.PacketType.ItemStats, connection, this.containerId.x, this.containerId.y);
    }

    @Override
    public void processClient(UdpConnection connection) {
        ItemContainer container = this.containerId.getContainer();
        if (container == null) {
            return;
        }
        InventoryItem item = container.getItemWithID(this.id);
        if (item == null) {
            return;
        }
        this.applyItemStats(item);
    }

    private void applyItemStats(InventoryItem item) {
        if (item instanceof DrainableComboItem) {
            item.setCurrentUses((int)((float)item.getMaxUses() * this.usedDelta));
        }
        if (this.isFluidContainer) {
            item.getFluidContainer().setCapacity(this.fluidContainer.getCapacity());
            item.getFluidContainer().copyFluidsFrom(this.fluidContainer);
        }
        item.setItemHeat(this.heat);
        if (this.isFood) {
            Food food = (Food)item;
            food.setFrozen(this.isFrozen);
            food.setTainted(this.isTainted);
            food.setHeat(this.heat);
            food.setCooked(this.isCooked);
            food.setBurnt(this.isBurnt);
            food.setCookingTime(this.cookingTime);
            food.setMinutesToCook(this.minutesToCook);
            food.setMinutesToBurn(this.minutesToBurn);
            food.setHungChange(this.hungChange);
            food.setCalories(this.calories);
            food.setCarbohydrates(this.carbohydrates);
            food.setLipids(this.lipids);
            food.setProteins(this.proteins);
            food.setThirstChange(this.thirstChange);
            food.setFluReduction(this.fluReduction);
            food.setPainReduction(this.painReduction);
            food.setEndChange(this.endChange);
            food.setFoodSicknessChange(this.foodSicknessChange);
            food.setStressChange(this.stressChange);
            food.setFatigueChange(this.fatigueChange);
            food.setUnhappyChange(this.unhappyChange);
            food.setBoredomChange(this.boredomChange);
            food.setPoisonPower(this.poisonPower);
            food.setPoisonDetectionLevel(this.poisonDetectionLevel);
            food.setActualWeight(this.actualWeight);
            if (food.extraItems == null) {
                food.extraItems = new ArrayList();
            }
            food.extraItems.clear();
            food.extraItems.addAll(this.extraItems);
            food.setAlcoholic(this.isAlcoholic);
            food.setBaseHunger(this.baseHunger);
            food.setCustomName(this.isCustomName);
            food.setName(this.name);
            if (food.spices == null) {
                food.spices = new ArrayList();
            }
            food.spices.clear();
            food.spices.addAll(this.spices);
        }
    }
}

