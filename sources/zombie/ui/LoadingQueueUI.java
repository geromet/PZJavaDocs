/*
 * Decompiled with CFR 0.152.
 */
package zombie.ui;

import java.util.HashMap;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.textures.Texture;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.iso.IsoUtils;
import zombie.ui.TextManager;
import zombie.ui.UIElement;
import zombie.ui.UIFont;
import zombie.ui.UIManager;
import zombie.world.Wind;

public class LoadingQueueUI
extends UIElement {
    private final String strLoadingQueue;
    private final String strQueuePlace;
    private static int placeInQueue = -1;
    private static final HashMap<String, Object> serverInformation = new HashMap();
    private double timerServerInformationAnim;
    private final Texture arrowBg;
    private final Texture arrowFg;
    private final Texture[] moons = new Texture[8];
    private final Texture[] windsock = new Texture[6];
    private double timerMultiplierAnim;
    private int animOffset = -1;

    public LoadingQueueUI() {
        int i;
        this.strLoadingQueue = Translator.getText("UI_GameLoad_LoadingQueue");
        this.strQueuePlace = Translator.getText("UI_GameLoad_PlaceInQueue");
        this.arrowBg = Texture.getSharedTexture("media/ui/ArrowRight_Disabled.png");
        this.arrowFg = Texture.getSharedTexture("media/ui/ArrowRight.png");
        for (i = 0; i < 8; ++i) {
            this.moons[i] = Texture.getSharedTexture("media/ui/queue/moonN" + (i + 1) + ".png");
        }
        for (i = 0; i < 6; ++i) {
            this.windsock[i] = Texture.getSharedTexture("media/ui/queue/windsock" + (i + 1) + ".png");
        }
        placeInQueue = -1;
        this.onresize();
    }

    @Override
    public void update() {
    }

    @Override
    public void onresize() {
        this.x = 288.0;
        this.y = 101.0;
        this.width = (float)((double)Core.getInstance().getScreenWidth() - 2.0 * this.x);
        this.height = (float)((double)Core.getInstance().getScreenHeight() - 2.0 * this.y);
    }

    @Override
    public void render() {
        this.onresize();
        double r = 0.4f;
        double g = 0.4f;
        double b = 0.4f;
        double a = 1.0;
        this.DrawTextureScaledColor(null, 0.0, 0.0, 1.0, Double.valueOf(this.height), (double)0.4f, (double)0.4f, (double)0.4f, 1.0);
        this.DrawTextureScaledColor(null, 1.0, 0.0, (double)this.width - 2.0, 1.0, (double)0.4f, (double)0.4f, (double)0.4f, 1.0);
        this.DrawTextureScaledColor(null, (double)this.width - 1.0, 0.0, 1.0, Double.valueOf(this.height), (double)0.4f, (double)0.4f, (double)0.4f, 1.0);
        this.DrawTextureScaledColor(null, 1.0, (double)this.height - 1.0, (double)this.width - 2.0, 1.0, (double)0.4f, (double)0.4f, (double)0.4f, 1.0);
        this.DrawTextureScaledColor(null, 1.0, 1.0, (double)this.width - 2.0, Double.valueOf(this.height - 2.0f), 0.0, 0.0, 0.0, 0.5);
        TextManager.instance.DrawStringCentre(UIFont.Large, this.x + (double)(this.width / 2.0f), this.y + 60.0, this.strLoadingQueue, 1.0, 1.0, 1.0, 1.0);
        this.DrawTextureColor(this.arrowBg, (this.width - (float)this.arrowBg.getWidth()) / 2.0f - 15.0f, 120.0, 1.0, 1.0, 1.0, 1.0);
        this.DrawTextureColor(this.arrowBg, (this.width - (float)this.arrowBg.getWidth()) / 2.0f, 120.0, 1.0, 1.0, 1.0, 1.0);
        this.DrawTextureColor(this.arrowBg, (this.width - (float)this.arrowBg.getWidth()) / 2.0f + 15.0f, 120.0, 1.0, 1.0, 1.0, 1.0);
        this.timerMultiplierAnim += UIManager.getMillisSinceLastRender();
        if (this.timerMultiplierAnim <= 500.0) {
            this.animOffset = Integer.MIN_VALUE;
        } else if (this.timerMultiplierAnim <= 1000.0) {
            this.animOffset = -15;
        } else if (this.timerMultiplierAnim <= 1500.0) {
            this.animOffset = 0;
        } else if (this.timerMultiplierAnim <= 2000.0) {
            this.animOffset = 15;
        } else {
            this.timerMultiplierAnim = 0.0;
        }
        if (this.animOffset != Integer.MIN_VALUE) {
            this.DrawTextureColor(this.arrowFg, (this.width - (float)this.arrowBg.getWidth()) / 2.0f + (float)this.animOffset, 120.0, 1.0, 1.0, 1.0, 1.0);
        }
        if (placeInQueue >= 0) {
            TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0f), this.y + 180.0, String.format(this.strQueuePlace, placeInQueue), 1.0, 1.0, 1.0, 1.0);
        }
        if (serverInformation != null) {
            try {
                float stageStep1;
                this.timerServerInformationAnim += UIManager.getMillisSinceLastRender();
                if (this.timerServerInformationAnim / 40000.0 > 1.0) {
                    this.timerServerInformationAnim -= 40000.0;
                }
                float state1 = IsoUtils.smoothstep(0.0f, 2000.0f, (float)this.timerServerInformationAnim) * IsoUtils.smoothstep(10000.0f, 8000.0f, (float)this.timerServerInformationAnim);
                float state2 = IsoUtils.smoothstep(10000.0f, 12000.0f, (float)this.timerServerInformationAnim) * IsoUtils.smoothstep(20000.0f, 18000.0f, (float)this.timerServerInformationAnim);
                float state3 = IsoUtils.smoothstep(20000.0f, 22000.0f, (float)this.timerServerInformationAnim) * IsoUtils.smoothstep(30000.0f, 28000.0f, (float)this.timerServerInformationAnim);
                float state4 = IsoUtils.smoothstep(30000.0f, 32000.0f, (float)this.timerServerInformationAnim) * IsoUtils.smoothstep(40000.0f, 38000.0f, (float)this.timerServerInformationAnim);
                if (state1 > 0.0f) {
                    int y2 = 240;
                    float stageStepDelta = 0.2f;
                    float stageStep12 = 0.5f;
                    TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0f), this.y + (double)y2, String.format(Translator.getText("UI_GameLoad_PlayerPopulation"), serverInformation.get("countPlayers"), serverInformation.get("maxPlayers")), 1.0, 1.0, 1.0, IsoUtils.smoothstep(stageStep12 - 0.2f, stageStep12, state1));
                    y2 += 30;
                    stageStep12 += 0.1f;
                    Integer zombieKilledToday = (Integer)serverInformation.get("ZombiesKilledToday");
                    if (zombieKilledToday == 0) {
                        TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0f), this.y + (double)y2, Translator.getText("UI_GameLoad_zombieKilledToday0"), 1.0, 1.0, 1.0, IsoUtils.smoothstep(stageStep12 - 0.2f, stageStep12, state1));
                    } else if (zombieKilledToday == 1) {
                        TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0f), this.y + (double)y2, Translator.getText("UI_GameLoad_zombieKilledToday1"), 1.0, 1.0, 1.0, IsoUtils.smoothstep(stageStep12 - 0.2f, stageStep12, state1));
                    } else {
                        TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0f), this.y + (double)y2, String.format(Translator.getText("UI_GameLoad_zombieKilledTodayN"), zombieKilledToday), 1.0, 1.0, 1.0, IsoUtils.smoothstep(stageStep12 - 0.2f, stageStep12, state1));
                    }
                    y2 += 30;
                    stageStep12 += 0.1f;
                    Integer zombifiedPlayersToday = (Integer)serverInformation.get("ZombifiedPlayersToday");
                    if (zombifiedPlayersToday == 0) {
                        TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0f), this.y + (double)y2, Translator.getText("UI_GameLoad_zombifiedPlayersToday0"), 1.0, 1.0, 1.0, IsoUtils.smoothstep(stageStep12 - 0.2f, stageStep12, state1));
                    } else if (zombifiedPlayersToday == 1) {
                        TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0f), this.y + (double)y2, Translator.getText("UI_GameLoad_zombifiedPlayersToday1"), 1.0, 1.0, 1.0, IsoUtils.smoothstep(stageStep12 - 0.2f, stageStep12, state1));
                    } else {
                        TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0f), this.y + (double)y2, String.format(Translator.getText("UI_GameLoad_zombifiedPlayersTodayN"), zombifiedPlayersToday), 1.0, 1.0, 1.0, IsoUtils.smoothstep(stageStep12 - 0.2f, stageStep12, state1));
                    }
                    y2 += 30;
                    stageStep12 += 0.1f;
                    Integer burnedZombiesToday = (Integer)serverInformation.get("BurnedCorpsesToday");
                    if (burnedZombiesToday == 0) {
                        TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0f), this.y + (double)y2, Translator.getText("UI_GameLoad_burnedZombiesToday0"), 1.0, 1.0, 1.0, IsoUtils.smoothstep(stageStep12 - 0.2f, stageStep12, state1));
                    } else if (burnedZombiesToday == 1) {
                        TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0f), this.y + (double)y2, Translator.getText("UI_GameLoad_burnedZombiesToday1"), 1.0, 1.0, 1.0, IsoUtils.smoothstep(stageStep12 - 0.2f, stageStep12, state1));
                    } else {
                        TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0f), this.y + (double)y2, String.format(Translator.getText("UI_GameLoad_burnedZombiesTodayN"), burnedZombiesToday), 1.0, 1.0, 1.0, IsoUtils.smoothstep(stageStep12 - 0.2f, stageStep12, state1));
                    }
                }
                if (state2 > 0.0f) {
                    float stageStepDelta = 0.2f;
                    stageStep1 = 0.5f;
                    int y2 = 240;
                    Byte timeHour = (Byte)serverInformation.get("Hour");
                    Byte timeMinute = (Byte)serverInformation.get("Minutes");
                    TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0f), this.y + (double)y2, String.format(Translator.getText("UI_GameLoad_time"), timeHour, timeMinute), 1.0, 1.0, 1.0, IsoUtils.smoothstep(stageStep1 - 0.2f, stageStep1, state2));
                    TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0f), this.y + (double)(y2 += 30), String.format("%d/%d/%d", serverInformation.get("Month"), serverInformation.get("Day"), serverInformation.get("Year")), 1.0, 1.0, 1.0, IsoUtils.smoothstep((stageStep1 += 0.1f) - 0.2f, stageStep1, state2));
                    TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0f), this.y + (double)(y2 += 30), String.format(Translator.getText("UI_GameLoad_temperature"), serverInformation.get("Temperature")), 1.0, 1.0, 1.0, IsoUtils.smoothstep((stageStep1 += 0.1f) - 0.2f, stageStep1, state2));
                    TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0f), this.y + (double)(y2 += 30), String.format(Translator.getText("UI_GameLoad_humidity"), Float.valueOf(((Float)serverInformation.get("Humidity")).floatValue() * 100.0f)), 1.0, 1.0, 1.0, IsoUtils.smoothstep((stageStep1 += 0.1f) - 0.2f, stageStep1, state2));
                }
                if (state3 > 0.0f) {
                    float stageStepDelta = 0.2f;
                    stageStep1 = 0.5f;
                    int y2 = 240;
                    float windKph = ((Float)serverInformation.get("WindspeedKph")).floatValue();
                    int windBeaufortNumber = Wind.getBeaufortNumber(windKph);
                    String windName = Wind.getName(windBeaufortNumber);
                    String windDescription = Wind.getDescription(windBeaufortNumber);
                    this.DrawTextureScaled(this.windsock[Wind.getWindsockSegments(windKph)], (this.width - 100.0f) / 2.0f, y2, 100.0, 100.0, IsoUtils.smoothstep(stageStep1 - 0.2f, stageStep1, state3));
                    TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0f), this.y + (double)(y2 += 130), String.format(Translator.getText("UI_GameLoad_windSpeed"), windName, (int)Wind.getWindKnots(windKph), (int)windKph), 1.0, 1.0, 1.0, IsoUtils.smoothstep((stageStep1 += 0.1f) - 0.2f, stageStep1, state3));
                    TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0f), this.y + (double)(y2 += 30), windDescription, 1.0, 1.0, 1.0, IsoUtils.smoothstep((stageStep1 += 0.1f) - 0.2f, stageStep1, state3));
                    y2 += 30;
                    stageStep1 += 0.1f;
                    float fog = ((Float)serverInformation.get("Fog")).floatValue();
                    if ((double)fog < 0.2) {
                        TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0f), this.y + (double)y2, Translator.getText("UI_GameLoad_fogNo"), 1.0, 1.0, 1.0, IsoUtils.smoothstep(stageStep1 - 0.2f, stageStep1, state3));
                    } else if ((double)fog < 0.8) {
                        TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0f), this.y + (double)y2, Translator.getText("UI_GameLoad_fogMedium"), 1.0, 1.0, 1.0, IsoUtils.smoothstep(stageStep1 - 0.2f, stageStep1, state3));
                    } else {
                        TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0f), this.y + (double)y2, Translator.getText("UI_GameLoad_fogHeavy"), 1.0, 1.0, 1.0, IsoUtils.smoothstep(stageStep1 - 0.2f, stageStep1, state3));
                    }
                    TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0f), this.y + (double)(y2 += 30), Translator.getText("UI_GameLoad_season" + String.valueOf(serverInformation.get("SeasonId"))), 1.0, 1.0, 1.0, IsoUtils.smoothstep((stageStep1 += 0.1f) - 0.2f, stageStep1, state3));
                }
                if (state4 > 0.0f) {
                    float stageStepDelta = 0.2f;
                    stageStep1 = 0.5f;
                    this.DrawTextureScaled(this.moons[Math.max(0, Math.min(7, ((Byte)serverInformation.get("Moon")).byteValue()))], (this.width - 100.0f) / 2.0f, 240.0, 100.0, 100.0, IsoUtils.smoothstep(stageStep1 - 0.2f, stageStep1, state4));
                    TextManager.instance.DrawStringCentre(UIFont.Medium, this.x + (double)(this.width / 2.0f), this.y + 240.0 + 30.0 + 100.0, Translator.getText("UI_GameLoad_moon" + String.valueOf(serverInformation.get("Moon"))), 1.0, 1.0, 1.0, IsoUtils.smoothstep((stageStep1 += 0.1f) - 0.2f, stageStep1, state4));
                }
            }
            catch (Exception e) {
                DebugLog.General.printException(e, "LoadingQueueUI render failed", LogSeverity.Error);
            }
        }
    }

    public void setPlaceInQueue(int placeInQueue) {
        LoadingQueueUI.placeInQueue = placeInQueue;
    }

    public void setServerInformation(HashMap<String, Object> serverInformation) {
        LoadingQueueUI.serverInformation.clear();
        LoadingQueueUI.serverInformation.putAll(serverInformation);
    }
}

