/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.raknet;

import fmod.SoundBuffer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class VoiceDebug
extends JPanel {
    private static final int PREF_W = 400;
    private static final int PREF_H = 200;
    private static final int BORDER_GAP = 30;
    private static final Color LINE_CURRENT_COLOR = Color.blue;
    private static final Color LINE_LAST_COLOR = Color.red;
    private static final Color GRAPH_COLOR = Color.green;
    private static final Color GRAPH_POINT_COLOR = new Color(150, 50, 50, 180);
    private static final Stroke GRAPH_STROKE = new BasicStroke(3.0f);
    private static final int GRAPH_POINT_WIDTH = 12;
    private static final int Y_HATCH_CNT = 10;
    public List<Integer> scores;
    public int scoresMax;
    public String title;
    public int psize;
    public int last;
    public int current;
    private static VoiceDebug mainPanel;
    private static VoiceDebug mainPanel2;
    private static VoiceDebug mainPanel3;
    private static VoiceDebug mainPanel4;
    private static JFrame frame;

    public VoiceDebug(List<Integer> scores, String title) {
        this.scores = scores;
        this.title = title;
        this.psize = scores.size();
        this.last = 5;
        this.current = 8;
        this.scoresMax = 100;
    }

    @Override
    protected void paintComponent(Graphics g) {
        int x1;
        int i;
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double xScale = ((double)this.getWidth() - 60.0) / (double)(this.scores.size() - 1);
        double yScale = ((double)this.getHeight() - 60.0) / (double)(this.scoresMax - 1);
        int yShift = (int)(((double)this.getHeight() - 60.0) / 2.0);
        int stepx = (int)(1.0 / xScale);
        if (stepx == 0) {
            stepx = 1;
        }
        ArrayList<Point> graphPoints = new ArrayList<Point>();
        for (i = 0; i < this.scores.size(); i += stepx) {
            int x12 = (int)((double)i * xScale + 30.0);
            int y1 = (int)((double)(this.scoresMax - this.scores.get(i)) * yScale + 30.0 - (double)yShift);
            graphPoints.add(new Point(x12, y1));
        }
        g2.setColor(Color.black);
        g2.drawLine(30, this.getHeight() - 30, 30, 30);
        g2.drawLine(30, this.getHeight() - 30, this.getWidth() - 30, this.getHeight() - 30);
        for (i = 0; i < 10; ++i) {
            int y0;
            int x0 = 30;
            x1 = 42;
            int y1 = y0 = this.getHeight() - ((i + 1) * (this.getHeight() - 60) / 10 + 30);
            g2.drawLine(30, y0, 42, y1);
        }
        Stroke oldStroke = g2.getStroke();
        g2.setColor(GRAPH_COLOR);
        g2.setStroke(GRAPH_STROKE);
        for (int i2 = 0; i2 < graphPoints.size() - 1; ++i2) {
            x1 = ((Point)graphPoints.get((int)i2)).x;
            int y1 = ((Point)graphPoints.get((int)i2)).y;
            int x2 = ((Point)graphPoints.get((int)(i2 + 1))).x;
            int y2 = ((Point)graphPoints.get((int)(i2 + 1))).y;
            g2.drawLine(x1, y1, x2, y2);
        }
        double xScalePoints = ((double)this.getWidth() - 60.0) / (double)(this.psize - 1);
        g2.setColor(LINE_CURRENT_COLOR);
        int xCurrent = (int)((double)this.current * xScalePoints + 30.0);
        g2.drawLine(xCurrent, this.getHeight() - 30, xCurrent, 30);
        g2.drawString("Current", xCurrent, this.getHeight() - 30);
        g2.setColor(LINE_LAST_COLOR);
        int xLast = (int)((double)this.last * xScalePoints + 30.0);
        g2.drawLine(xLast, this.getHeight() - 30, xLast, 30);
        g2.drawString("Last", xLast, this.getHeight() - 30);
        g2.setColor(Color.black);
        g2.drawString(this.title, this.getWidth() / 2, 15);
        g2.drawString("Size: " + this.scores.size(), 30, 15);
        g2.drawString("Current/Write: " + this.current, 30, 30);
        g2.drawString("Last/Read: " + this.last, 30, 45);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(400, 200);
    }

    public static void createAndShowGui() {
        ArrayList<Integer> playBufScores = new ArrayList<Integer>();
        ArrayList<Integer> playBufScores100 = new ArrayList<Integer>();
        ArrayList<Integer> fmodPlayBufScores = new ArrayList<Integer>();
        ArrayList<Integer> fmodPlayBufScores100 = new ArrayList<Integer>();
        mainPanel = new VoiceDebug(playBufScores, "SoundBuffer");
        VoiceDebug.mainPanel.scoresMax = 32000;
        mainPanel2 = new VoiceDebug(playBufScores100, "SoundBuffer - first 100 sample");
        VoiceDebug.mainPanel2.scoresMax = 32000;
        mainPanel3 = new VoiceDebug(fmodPlayBufScores, "FMODSoundBuffer");
        VoiceDebug.mainPanel3.scoresMax = 32000;
        mainPanel4 = new VoiceDebug(fmodPlayBufScores100, "FMODSoundBuffer - first 100 sample");
        VoiceDebug.mainPanel4.scoresMax = 32000;
        frame = new JFrame("DrawGraph");
        frame.setDefaultCloseOperation(3);
        frame.setLayout(new GridLayout(2, 2));
        frame.getContentPane().add(mainPanel);
        frame.getContentPane().add(mainPanel2);
        frame.getContentPane().add(mainPanel3);
        frame.getContentPane().add(mainPanel4);
        frame.pack();
        frame.setLocationByPlatform(true);
        frame.setVisible(true);
    }

    public static void updateGui(SoundBuffer playBuf, byte[] recBuf) {
        int i;
        VoiceDebug.mainPanel.scores.clear();
        if (playBuf != null) {
            for (i = 0; i < playBuf.buf().length; ++i) {
                VoiceDebug.mainPanel.scores.add(Integer.valueOf(playBuf.buf()[i]));
            }
            VoiceDebug.mainPanel.current = playBuf.bufWrite;
            VoiceDebug.mainPanel.last = playBuf.bufRead;
            VoiceDebug.mainPanel.psize = playBuf.bufSize;
            VoiceDebug.mainPanel2.scores.clear();
            for (i = 0; i < 100; ++i) {
                VoiceDebug.mainPanel2.scores.add(Integer.valueOf(playBuf.buf()[i]));
            }
        }
        VoiceDebug.mainPanel3.scores.clear();
        VoiceDebug.mainPanel4.scores.clear();
        for (i = 0; i < recBuf.length / 2; i += 2) {
            VoiceDebug.mainPanel4.scores.add(recBuf[i + 1] * 256 + recBuf[i]);
        }
        frame.repaint();
    }
}

