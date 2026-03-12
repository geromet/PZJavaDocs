/*
 * Decompiled with CFR 0.152.
 */
package zombie.ui;

import java.util.Stack;
import zombie.ui.TextManager;
import zombie.ui.UIElement;
import zombie.ui.UIFont;

public final class TextBox
extends UIElement {
    public boolean resizeParent;
    UIFont font;
    Stack<String> lines = new Stack();
    String text;
    public boolean centered;

    public TextBox(UIFont font, int x, int y, int width, String text) {
        this.font = font;
        this.x = x;
        this.y = y;
        this.text = text;
        this.width = width;
        this.Paginate();
    }

    @Override
    public void onresize() {
        this.Paginate();
    }

    @Override
    public void render() {
        if (!this.isVisible().booleanValue()) {
            return;
        }
        super.render();
        this.Paginate();
        int y = 0;
        for (String text : this.lines) {
            if (this.centered) {
                TextManager.instance.DrawStringCentre(this.font, (double)this.getAbsoluteX().intValue() + this.getWidth() / 2.0, this.getAbsoluteY().intValue() + y, text, 1.0, 1.0, 1.0, 1.0);
            } else {
                TextManager.instance.DrawString(this.font, this.getAbsoluteX().intValue(), this.getAbsoluteY().intValue() + y, text, 1.0, 1.0, 1.0, 1.0);
            }
            y += TextManager.instance.MeasureStringY(this.font, (String)this.lines.get(0));
        }
        this.setHeight(y);
    }

    @Override
    public void update() {
        this.Paginate();
        int y = 0;
        for (String text : this.lines) {
            y += TextManager.instance.MeasureStringY(this.font, (String)this.lines.get(0));
        }
        this.setHeight(y);
    }

    private void Paginate() {
        String[] textarr;
        int n = 0;
        this.lines.clear();
        block0: for (String text : textarr = this.text.split("<br>")) {
            if (text.isEmpty()) {
                this.lines.add(" ");
                continue;
            }
            do {
                int wid;
                int m;
                int z;
                if ((z = (m = text.indexOf(" ", n + 1))) == -1) {
                    z = text.length();
                }
                if ((double)(wid = TextManager.instance.MeasureStringX(this.font, text.substring(0, z))) >= this.getWidth()) {
                    String sub = text.substring(0, n);
                    text = text.substring(n + 1);
                    this.lines.add(sub);
                    m = 0;
                } else if (m == -1) {
                    this.lines.add(text);
                    continue block0;
                }
                n = m;
            } while (!text.isEmpty());
        }
    }

    public void SetText(String text) {
        this.text = text;
        this.Paginate();
    }
}

