/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.advancedanimation;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableHandle;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableSlotBool;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableSlotEnum;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableSlotFloat;
import zombie.core.skinnedmodel.advancedanimation.AnimationVariableSlotString;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableCallbackMap;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableMap;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlot;
import zombie.core.skinnedmodel.advancedanimation.IAnimationVariableSlotDescriptor;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

public class AnimationVariableSource
implements IAnimationVariableMap,
IAnimationVariableCallbackMap {
    private boolean isEmpty = true;
    private IAnimationVariableSlot[] cachedGameVariableSlots = new IAnimationVariableSlot[0];

    @Override
    public IAnimationVariableSlot getVariable(AnimationVariableHandle handle) {
        if (handle == null) {
            return null;
        }
        int handleIdx = handle.getVariableIndex();
        if (handleIdx < 0) {
            return null;
        }
        if (this.cachedGameVariableSlots == null || handleIdx >= this.cachedGameVariableSlots.length) {
            return null;
        }
        return this.cachedGameVariableSlots[handleIdx];
    }

    public boolean isEmpty() {
        return this.isEmpty;
    }

    private IAnimationVariableSlot getOrCreateVariable(AnimationVariableHandle handle, AnimationVariableSlotGenerator creator) {
        IAnimationVariableSlot slot = this.getVariable(handle);
        if (slot == null) {
            slot = creator.Create(handle.getVariableName(), null);
            this.setVariable(slot);
        }
        return slot;
    }

    private IAnimationVariableSlot getOrCreateVariable(String key, AnimationVariableSlotGenerator creator) {
        AnimationVariableHandle handle = AnimationVariableHandle.alloc(key);
        if (handle == null) {
            return null;
        }
        IAnimationVariableSlot slot = this.getVariable(handle);
        if (slot == null) {
            slot = creator.Create(handle.getVariableName(), null);
            this.setVariable(slot);
        }
        return slot;
    }

    private <EnumType extends Enum<EnumType>> IAnimationVariableSlot getOrCreateVariable_Enum(String key, EnumType initialVal) {
        AnimationVariableHandle handle = AnimationVariableHandle.alloc(key);
        if (handle == null) {
            return null;
        }
        AnimationVariableSlotEnum slot = this.getVariable(handle);
        if (slot == null) {
            Class<?> enumClass = initialVal.getClass();
            slot = new AnimationVariableSlotEnum(enumClass, key, initialVal, null);
            this.setVariable(slot);
        }
        return slot;
    }

    private IAnimationVariableSlot getOrCreateVariable_Bool(String key) {
        return this.getOrCreateVariable(key, AnimationVariableSlotBool::new);
    }

    private IAnimationVariableSlot getOrCreateVariable_String(String key) {
        return this.getOrCreateVariable(key, AnimationVariableSlotString::new);
    }

    private IAnimationVariableSlot getOrCreateVariable_Float(String key) {
        return this.getOrCreateVariable(key, AnimationVariableSlotFloat::new);
    }

    private IAnimationVariableSlot getOrCreateVariable_Bool(AnimationVariableHandle handle) {
        return this.getOrCreateVariable(handle, AnimationVariableSlotBool::new);
    }

    @Override
    public void setVariable(IAnimationVariableSlot var) {
        AnimationVariableHandle handle = var.getHandle();
        int handleIdx = handle.getVariableIndex();
        if (handleIdx >= this.cachedGameVariableSlots.length) {
            IAnimationVariableSlot[] newArray = new IAnimationVariableSlot[handleIdx + 1];
            IAnimationVariableSlot[] oldArray = this.cachedGameVariableSlots;
            if (oldArray != null) {
                PZArrayUtil.arrayCopy(newArray, oldArray, 0, oldArray.length);
            }
            this.cachedGameVariableSlots = newArray;
        }
        this.cachedGameVariableSlots[handleIdx] = var;
        if (var != null) {
            this.isEmpty = false;
        } else if (this.isEmpty) {
            this.isEmpty = !this.getGameVariables().iterator().hasNext();
        }
    }

    @Override
    public IAnimationVariableSlot setVariable(String key, String value) {
        IAnimationVariableSlot slot = this.getOrCreateVariable_String(key);
        slot.setValue(value);
        return slot;
    }

    @Override
    public IAnimationVariableSlot setVariable(String key, boolean value) {
        IAnimationVariableSlot slot = this.getOrCreateVariable_Bool(key);
        slot.setValue(value);
        return slot;
    }

    @Override
    public IAnimationVariableSlot setVariable(String key, float value) {
        IAnimationVariableSlot slot = this.getOrCreateVariable_Float(key);
        slot.setValue(value);
        return slot;
    }

    @Override
    public IAnimationVariableSlot setVariable(AnimationVariableHandle handle, boolean value) {
        IAnimationVariableSlot slot = this.getOrCreateVariable_Bool(handle);
        slot.setValue(value);
        return slot;
    }

    @Override
    public <EnumType extends Enum<EnumType>> IAnimationVariableSlot setVariableEnum(String key, EnumType value) {
        IAnimationVariableSlot slot = this.getOrCreateVariable_Enum(key, value);
        slot.setEnumValue(value);
        return slot;
    }

    @Override
    public void clearVariable(String key) {
        IAnimationVariableSlot var = this.getVariable(key);
        if (var != null) {
            var.clear();
        }
    }

    @Override
    public void clearVariables() {
        for (IAnimationVariableSlot var : this.getGameVariables()) {
            var.clear();
        }
    }

    public void removeAllVariables() {
        this.cachedGameVariableSlots = new IAnimationVariableSlot[0];
        this.isEmpty = true;
    }

    @Override
    public Iterable<IAnimationVariableSlot> getGameVariables() {
        return () -> new Iterator<IAnimationVariableSlot>(this){
            private int nextSlotIndex;
            private IAnimationVariableSlot nextSlot;
            final /* synthetic */ AnimationVariableSource this$0;
            {
                AnimationVariableSource animationVariableSource = this$0;
                Objects.requireNonNull(animationVariableSource);
                this.this$0 = animationVariableSource;
                this.nextSlotIndex = -1;
                this.nextSlot = this.findNextSlot();
            }

            @Override
            public boolean hasNext() {
                return this.nextSlot != null;
            }

            @Override
            public IAnimationVariableSlot next() {
                if (!this.hasNext()) {
                    throw new NoSuchElementException();
                }
                IAnimationVariableSlot currentSlot = this.nextSlot;
                this.nextSlot = this.findNextSlot();
                return currentSlot;
            }

            private IAnimationVariableSlot findNextSlot() {
                IAnimationVariableSlot nextSlot = null;
                while (this.nextSlotIndex + 1 < this.this$0.cachedGameVariableSlots.length) {
                    IAnimationVariableSlot slot;
                    if ((slot = this.this$0.cachedGameVariableSlots[++this.nextSlotIndex]) == null) continue;
                    nextSlot = slot;
                    break;
                }
                return nextSlot;
            }
        };
    }

    @Override
    public boolean isVariable(String name, String val) {
        return StringUtils.equalsIgnoreCase(this.getVariableString(name), val);
    }

    @Override
    public boolean containsVariable(String key) {
        return this.getVariable(key) != null;
    }

    public static interface AnimationVariableSlotGenerator {
        public IAnimationVariableSlot Create(String var1, IAnimationVariableSlotDescriptor var2);
    }
}

