/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug.debugWindows;

import imgui.extension.implot.ImPlot;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Objects;
import zombie.GameTime;
import zombie.debug.BaseDebugWindow;

public class Plotter
extends BaseDebugWindow {
    ArrayList<PlottedVar> vars = new ArrayList();
    ArrayList<Double> valuesX = new ArrayList();
    ArrayList<Double> valuesY = new ArrayList();
    Float tick = Float.valueOf(0.0f);
    float lastTime;

    @Override
    public String getTitle() {
        return "Plotter";
    }

    public Plotter(Object obj, Field field) {
        this.vars.add(new PlottedVar(this, obj, field));
        this.lastTime = GameTime.getInstance().timeOfDay - 0.01f;
    }

    public void addVariable(Object obj, Field field) {
        this.vars.add(new PlottedVar(this, obj, field));
    }

    @Override
    protected void doWindowContents() {
        int x;
        PlottedVar var;
        int i;
        float dif = GameTime.getInstance().timeOfDay - this.lastTime;
        if (dif > 0.01f || dif < 0.0f) {
            if (dif < 0.0f) {
                dif += 24.0f;
            }
            this.tick = Float.valueOf(this.tick.floatValue() + dif / 0.01f);
            for (i = 0; i < this.vars.size(); ++i) {
                var = this.vars.get(i);
                var.field.setAccessible(true);
                try {
                    var.valuesY.add(((Float)var.field.get(var.obj)).doubleValue());
                    continue;
                }
                catch (IllegalAccessException illegalAccessException) {
                    // empty catch block
                }
            }
            this.valuesX.add(this.tick.doubleValue());
            this.lastTime = GameTime.getInstance().timeOfDay;
        }
        for (i = 0; i < this.vars.size(); ++i) {
            var = this.vars.get(i);
            var.normalize();
        }
        double maxX = -1.0E8;
        double maxY = -1.0E8;
        double minX = 1.0E8;
        double minY = 1.0E8;
        for (x = 0; x < this.valuesX.size(); ++x) {
            minX = Math.min(minX, this.valuesX.get(x));
            maxX = Math.max(maxX, this.valuesX.get(x));
        }
        for (x = 0; x < this.vars.size(); ++x) {
            PlottedVar v = this.vars.get(x);
            minY = Math.min(minY, v.minY);
            maxY = Math.max(maxY, v.maxY);
        }
        if (!this.valuesX.isEmpty()) {
            ImPlot.setNextPlotLimits(minX, maxX, minY, maxY, 1);
        }
        if (ImPlot.beginPlot("plot", "time", "")) {
            for (int i2 = 0; i2 < this.vars.size(); ++i2) {
                PlottedVar var2 = this.vars.get(i2);
                var2.plot(this.valuesX);
            }
        }
        ImPlot.endPlot();
    }

    public class PlottedVar {
        public Object obj;
        public Field field;
        ArrayList<Double> valuesY;
        public double minY;
        public double maxY;

        public PlottedVar(Plotter this$0, Object obj, Field field) {
            Objects.requireNonNull(this$0);
            this.valuesY = new ArrayList();
            this.field = field;
            this.obj = obj;
        }

        public void normalize() {
            double maxY = -1.0E8;
            double minY = 1.0E8;
            for (int x = 0; x < this.valuesY.size(); ++x) {
                minY = Math.min(minY, this.valuesY.get(x));
                maxY = Math.max(maxY, this.valuesY.get(x));
            }
            if (minY >= 0.0 && maxY <= 1.0) {
                minY = 0.0;
                maxY = 1.0;
            }
            this.minY = minY;
            this.maxY = maxY;
        }

        public void plot(ArrayList<Double> valuesX) {
            ImPlot.plotLine((String)(this.obj.getClass().getSimpleName() + "." + this.field.getName()), (Number[])valuesX.toArray(new Double[0]), (Number[])this.valuesY.toArray(new Double[0]));
        }
    }
}

