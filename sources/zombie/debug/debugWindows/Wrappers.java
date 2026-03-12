/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug.debugWindows;

import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import zombie.core.math.PZMath;

abstract class Wrappers {
    private static final int[] valueInt = new int[1];
    private static final float[] valueFloat = new float[1];
    protected static final ImBoolean valueBoolean = new ImBoolean();
    protected static final ImString valueString = new ImString();
    private static boolean valueChanged;

    Wrappers() {
    }

    public static int sliderInt(String label, int value, int min, int max) {
        Wrappers.valueInt[0] = value;
        valueChanged |= ImGui.sliderInt(label, valueInt, min, max);
        return valueInt[0];
    }

    public static int sliderIntShowRange(String label, int value, int min, int max) {
        Wrappers.valueInt[0] = value;
        valueChanged |= ImGui.sliderInt(label, valueInt, min, max);
        ImGui.sameLine();
        ImGui.text(min + "-" + max);
        return valueInt[0];
    }

    public static int sliderInt(String label, int min, int max, Supplier<Integer> getter, Consumer<Integer> setter) {
        Wrappers.valueInt[0] = getter.get();
        valueChanged |= ImGui.sliderInt(label, valueInt, min, max);
        if (valueInt[0] != getter.get()) {
            setter.accept(valueInt[0]);
        }
        return valueInt[0];
    }

    public static <T> int sliderInt(String label, int min, int max, SupplyConsumer<T, Integer> getter, BiConsumer<T, Integer> setter, T supplyArg) {
        Wrappers.valueInt[0] = getter.get(supplyArg);
        valueChanged |= ImGui.sliderInt(label, valueInt, min, max);
        if (valueInt[0] != getter.get(supplyArg)) {
            setter.accept(supplyArg, valueInt[0]);
        }
        return valueInt[0];
    }

    public static float sliderFloat(String label, float value, float min, float max) {
        Wrappers.valueFloat[0] = value;
        valueChanged |= ImGui.sliderFloat(label, valueFloat, min, max, "%.3f");
        return valueFloat[0];
    }

    public static float sliderFloatShowRange(String label, float value, float min, float max) {
        Wrappers.valueFloat[0] = value;
        valueChanged |= ImGui.sliderFloat(label, valueFloat, min, max, "%.3f");
        ImGui.sameLine();
        ImGui.text(min + "-" + max);
        return valueFloat[0];
    }

    public static float sliderFloat(String label, float min, float max, Supplier<Float> getter, Consumer<Float> setter) {
        Wrappers.valueFloat[0] = getter.get().floatValue();
        valueChanged |= ImGui.sliderFloat(label, valueFloat, min, max, "%.3f");
        if (PZMath.roundFloat(valueFloat[0], 3) != PZMath.roundFloat(getter.get().floatValue(), 3)) {
            setter.accept(Float.valueOf(valueFloat[0]));
        }
        return valueFloat[0];
    }

    public static float sliderFloat(String label, float min, float max, int scale, Supplier<Float> getter, Consumer<Float> setter) {
        Wrappers.valueFloat[0] = getter.get().floatValue();
        valueChanged |= ImGui.sliderFloat(label, valueFloat, min, max, "%." + scale + "f");
        if (PZMath.roundFloat(valueFloat[0], scale) != PZMath.roundFloat(getter.get().floatValue(), scale)) {
            setter.accept(Float.valueOf(valueFloat[0]));
        }
        return valueFloat[0];
    }

    public static void sliderDouble(String label, double min, double max, Supplier<Double> getter, Consumer<Double> setter) {
        Wrappers.valueFloat[0] = getter.get().floatValue();
        valueChanged |= ImGui.sliderFloat(label, valueFloat, (float)min, (float)max, "%.3f");
        if (PZMath.roundFloat(valueFloat[0], 3) != PZMath.roundFloat(getter.get().floatValue(), 3)) {
            setter.accept(Double.valueOf(valueFloat[0]));
        }
    }

    public static float dragFloat(String label, float min, float max, float step, Supplier<Float> getter, Consumer<Float> setter) {
        Wrappers.valueFloat[0] = getter.get().floatValue();
        valueChanged |= ImGui.dragFloat(label, valueFloat, step, min, max);
        if (PZMath.roundFloat(valueFloat[0], 3) != PZMath.roundFloat(getter.get().floatValue(), 3)) {
            setter.accept(Float.valueOf(valueFloat[0]));
        }
        return valueFloat[0];
    }

    public static float dragFloat(String label, float value, float min, float max, float step) {
        Wrappers.valueFloat[0] = value;
        valueChanged |= ImGui.dragFloat(label, valueFloat, step, min, max);
        return valueFloat[0];
    }

    public static boolean checkbox(String label, boolean value) {
        valueBoolean.set(value);
        valueChanged |= ImGui.checkbox(label, valueBoolean);
        return valueBoolean.get();
    }

    public static boolean checkbox(String label, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        valueBoolean.set(getter.get());
        valueChanged |= ImGui.checkbox(label, valueBoolean);
        if (valueBoolean.get() != getter.get().booleanValue()) {
            setter.accept(valueBoolean.get());
        }
        return valueBoolean.get();
    }

    public static <T> boolean checkbox(String label, SupplyConsumer<T, Boolean> getter, BiConsumer<T, Boolean> setter, T supplyArg) {
        valueBoolean.set(getter.get(supplyArg));
        valueChanged |= ImGui.checkbox(label, valueBoolean);
        if (valueBoolean.get() != getter.get(supplyArg).booleanValue()) {
            setter.accept(supplyArg, valueBoolean.get());
        }
        return valueBoolean.get();
    }

    public static boolean selectable(String label, boolean value) {
        valueBoolean.set(value);
        valueChanged |= ImGui.selectable(label, valueBoolean);
        return valueBoolean.get();
    }

    public static void clearValueChanged() {
        valueChanged = false;
    }

    public static boolean didValuesChange() {
        return valueChanged;
    }

    public static interface SupplyConsumer<T, U> {
        public U get(T var1);
    }
}

