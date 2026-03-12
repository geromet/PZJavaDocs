/*
 * Decompiled with CFR 0.152.
 */
package zombie.chat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import zombie.GameTime;
import zombie.IndieGL;
import zombie.characters.IsoPlayer;
import zombie.characters.Talker;
import zombie.chat.ChatElementOwner;
import zombie.chat.NineGridTexture;
import zombie.iso.objects.IsoRadio;
import zombie.iso.objects.IsoTelevision;
import zombie.network.GameServer;
import zombie.radio.ZomboidRadio;
import zombie.scripting.objects.CharacterTrait;
import zombie.ui.TextDrawObject;
import zombie.ui.UIFont;
import zombie.vehicles.VehiclePart;

public class ChatElement
implements Talker {
    protected PlayerLines[] playerLines = new PlayerLines[4];
    protected ChatElementOwner owner;
    protected float historyVal = 1.0f;
    protected boolean historyInRange;
    protected float historyRange = 15.0f;
    protected boolean useEuclidean = true;
    protected boolean hasChatToDisplay;
    protected int maxChatLines = -1;
    protected int maxCharsPerLine = -1;
    protected String sayLine;
    protected String sayLineTag;
    protected TextDrawObject sayLineObject;
    protected boolean speaking;
    protected boolean speakingNpc;
    protected String talkerType = "unknown";
    public static boolean doBackDrop = true;
    public static NineGridTexture backdropTexture;
    private final int bufferX = 0;
    private final int bufferY = 0;
    private static final PlayerLinesList[] renderBatch;
    private static final HashSet<String> noLogText;

    public ChatElement(ChatElementOwner chatowner, int numberoflines, String talkertype) {
        this.owner = chatowner;
        this.setMaxChatLines(numberoflines);
        this.setMaxCharsPerLine(75);
        String string = this.talkerType = talkertype != null ? talkertype : this.talkerType;
        if (backdropTexture == null) {
            backdropTexture = new NineGridTexture("NineGridBlack", 5);
        }
    }

    public void setMaxChatLines(int num) {
        int n = num < 1 ? 1 : (num = num > 10 ? 10 : num);
        if (num != this.maxChatLines) {
            this.maxChatLines = num;
            for (int i = 0; i < this.playerLines.length; ++i) {
                this.playerLines[i] = new PlayerLines(this, this.maxChatLines);
            }
        }
    }

    public int getMaxChatLines() {
        return this.maxChatLines;
    }

    public void setMaxCharsPerLine(int maxChars) {
        for (int i = 0; i < this.playerLines.length; ++i) {
            this.playerLines[i].setMaxCharsPerLine(maxChars);
        }
        this.maxCharsPerLine = maxChars;
    }

    @Override
    public boolean IsSpeaking() {
        return this.speaking;
    }

    public boolean IsSpeakingNPC() {
        return this.speakingNpc;
    }

    @Override
    public String getTalkerType() {
        return this.talkerType;
    }

    public void setTalkerType(String type) {
        this.talkerType = type == null ? "" : type;
    }

    @Override
    public String getSayLine() {
        return this.sayLine;
    }

    public String getSayLineTag() {
        if (this.speaking && this.sayLineTag != null) {
            return this.sayLineTag;
        }
        return "";
    }

    public void setHistoryRange(float range) {
        this.historyRange = range;
    }

    public void setUseEuclidean(boolean b) {
        this.useEuclidean = b;
    }

    public boolean getHasChatToDisplay() {
        return this.hasChatToDisplay;
    }

    protected float getDistance(IsoPlayer player) {
        if (player == null) {
            return -1.0f;
        }
        if (this.useEuclidean) {
            return (float)Math.sqrt(Math.pow(this.owner.getX() - player.getX(), 2.0) + Math.pow(this.owner.getY() - player.getY(), 2.0));
        }
        return Math.abs(this.owner.getX() - player.getX()) + Math.abs(this.owner.getY() - player.getY());
    }

    protected boolean playerWithinBounds(IsoPlayer player, float dist) {
        if (player == null) {
            return false;
        }
        return player.getX() > this.owner.getX() - dist && player.getX() < this.owner.getX() + dist && player.getY() > this.owner.getY() - dist && player.getY() < this.owner.getY() + dist;
    }

    public void SayDebug(int n, String text) {
        if (!GameServer.server && n >= 0 && n < this.maxChatLines) {
            for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
                IsoPlayer player = IsoPlayer.players[i];
                if (player == null) continue;
                PlayerLines pLines = this.playerLines[i];
                if (n >= pLines.chatLines.length) continue;
                if (pLines.chatLines[n].getOriginal() != null && pLines.chatLines[n].getOriginal().equals(text)) {
                    pLines.chatLines[n].setInternalTickClock(pLines.lineDisplayTime);
                    continue;
                }
                pLines.chatLines[n].setSettings(true, true, true, true, true, true);
                pLines.chatLines[n].setInternalTickClock(pLines.lineDisplayTime);
                pLines.chatLines[n].setCustomTag("default");
                pLines.chatLines[n].setDefaultColors(1.0f, 1.0f, 1.0f, 1.0f);
                pLines.chatLines[n].ReadString(UIFont.Medium, text, this.maxCharsPerLine);
            }
            this.sayLine = text;
            this.sayLineTag = "default";
            this.hasChatToDisplay = true;
        }
    }

    @Override
    public void Say(String line) {
        this.addChatLine(line, 1.0f, 1.0f, 1.0f, UIFont.Dialogue, 25.0f, "default", false, false, false, false, false, true);
    }

    public void addChatLine(String msg, float r, float g, float b, float baseRange) {
        this.addChatLine(msg, r, g, b, UIFont.Dialogue, baseRange, "default", false, false, false, false, false, true);
    }

    public void addChatLine(String msg, float r, float g, float b) {
        this.addChatLine(msg, r, g, b, UIFont.Dialogue, 25.0f, "default", false, false, false, false, false, true);
    }

    public void addChatLine(String msg, float r, float g, float b, UIFont font, float baseRange, String customTag, boolean bbcode, boolean img, boolean icons, boolean colors, boolean fonts, boolean equalizeHeights) {
        if (!GameServer.server) {
            for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
                String msgadd;
                PlayerLines pLines;
                TextDrawObject lineObj;
                IsoTelevision isoTelevision;
                ChatElementOwner chatElementOwner;
                IsoPlayer player = IsoPlayer.players[i];
                if (player == null || player.hasTrait(CharacterTrait.DEAF) && (!((chatElementOwner = this.owner) instanceof IsoTelevision) ? this.owner instanceof IsoRadio || this.owner instanceof VehiclePart : !(isoTelevision = (IsoTelevision)chatElementOwner).isFacing(player))) continue;
                float scramble = this.getScrambleValue(player, baseRange);
                if (!(scramble < 1.0f) || (lineObj = (pLines = this.playerLines[i]).getNewLineObject()) == null) continue;
                lineObj.setSettings(bbcode, img, icons, colors, fonts, equalizeHeights);
                lineObj.setInternalTickClock(pLines.lineDisplayTime);
                lineObj.setCustomTag(customTag);
                if (scramble > 0.0f) {
                    msgadd = ZomboidRadio.getInstance().scrambleString(msg, (int)(100.0f * scramble), true, "...");
                    lineObj.setDefaultColors(0.5f, 0.5f, 0.5f, 1.0f);
                } else {
                    msgadd = msg;
                    lineObj.setDefaultColors(r, g, b, 1.0f);
                }
                lineObj.ReadString(font, msgadd, this.maxCharsPerLine);
                this.sayLine = msg;
                this.sayLineTag = customTag;
                this.hasChatToDisplay = true;
            }
        }
    }

    protected float getScrambleValue(IsoPlayer player, float baseRange) {
        float dist;
        if (this.owner == player) {
            return 0.0f;
        }
        float mod = 1.0f;
        boolean forceScramble = false;
        boolean forceDisplay = false;
        if (this.owner.getSquare() != null && player.getSquare() != null) {
            if (player.getBuilding() != null && this.owner.getSquare().getBuilding() != null && player.getBuilding() == this.owner.getSquare().getBuilding()) {
                if (player.getSquare().getRoom() == this.owner.getSquare().getRoom()) {
                    mod = (float)((double)mod * 2.0);
                    forceDisplay = true;
                } else if (Math.abs(player.getZ() - this.owner.getZ()) < 1.0f) {
                    mod = (float)((double)mod * 2.0);
                }
            } else if (player.getBuilding() != null || this.owner.getSquare().getBuilding() != null) {
                mod = (float)((double)mod * 0.5);
                forceScramble = true;
            }
            if (Math.abs(player.getZ() - this.owner.getZ()) >= 1.0f) {
                mod = (float)((double)mod - (double)mod * ((double)Math.abs(player.getZ() - this.owner.getZ()) * 0.25));
                forceScramble = true;
            }
        }
        float range = baseRange * mod;
        float scramble = 1.0f;
        if (mod > 0.0f && this.playerWithinBounds(player, range) && (dist = this.getDistance(player)) >= 0.0f && dist < range) {
            float fullRange = range * 0.6f;
            if (forceDisplay || !forceScramble && dist < fullRange) {
                scramble = 0.0f;
            } else if (range - fullRange != 0.0f && (scramble = (dist - fullRange) / (range - fullRange)) < 0.2f) {
                scramble = 0.2f;
            }
        }
        return scramble;
    }

    protected void updateChatLines() {
        this.speaking = false;
        this.speakingNpc = false;
        boolean foundChat = false;
        if (this.hasChatToDisplay) {
            this.hasChatToDisplay = false;
            for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
                float delta = 1.25f * GameTime.getInstance().getMultiplier();
                int lineDisplayTime = this.playerLines[i].lineDisplayTime;
                for (TextDrawObject line : this.playerLines[i].chatLines) {
                    float disptime = line.updateInternalTickClock(delta);
                    if (disptime <= 0.0f) continue;
                    this.hasChatToDisplay = true;
                    if (!foundChat && !line.getCustomTag().equals("radio")) {
                        float alpha = disptime / ((float)lineDisplayTime / 2.0f);
                        if (alpha >= 1.0f) {
                            this.speaking = true;
                        }
                        if (alpha >= 0.0f) {
                            this.speakingNpc = true;
                        }
                        foundChat = true;
                    }
                    delta *= 1.2f;
                }
            }
        }
        if (!this.speaking) {
            this.sayLine = null;
            this.sayLineTag = null;
        }
    }

    protected void updateHistory() {
        if (this.hasChatToDisplay) {
            this.historyInRange = false;
            IsoPlayer player = IsoPlayer.getInstance();
            if (player != null) {
                if (player == this.owner) {
                    this.historyVal = 1.0f;
                } else {
                    this.historyInRange = this.playerWithinBounds(player, this.historyRange);
                    if (this.historyInRange && this.historyVal != 1.0f) {
                        this.historyVal += 0.04f;
                        if (this.historyVal > 1.0f) {
                            this.historyVal = 1.0f;
                        }
                    }
                    if (!this.historyInRange && this.historyVal != 0.0f) {
                        this.historyVal -= 0.04f;
                        if (this.historyVal < 0.0f) {
                            this.historyVal = 0.0f;
                        }
                    }
                }
            }
        } else if (this.historyVal != 0.0f) {
            this.historyVal = 0.0f;
        }
    }

    public void update() {
        if (GameServer.server) {
            return;
        }
        this.updateChatLines();
        this.updateHistory();
    }

    public void renderBatched(int playerNum, int x, int y) {
        this.renderBatched(playerNum, x, y, false);
    }

    public void renderBatched(int playerNum, int x, int y, boolean ignoreRadioLines) {
        if (playerNum < this.playerLines.length && this.hasChatToDisplay && !GameServer.server) {
            this.playerLines[playerNum].renderX = x;
            this.playerLines[playerNum].renderY = y;
            this.playerLines[playerNum].ignoreRadioLines = ignoreRadioLines;
            if (renderBatch[playerNum] == null) {
                ChatElement.renderBatch[playerNum] = new PlayerLinesList(this);
            }
            renderBatch[playerNum].add(this.playerLines[playerNum]);
        }
    }

    public void clear(int playerIndex) {
        this.playerLines[playerIndex].clear();
    }

    public static void RenderBatch(int playerNum) {
        if (renderBatch[playerNum] != null && !renderBatch[playerNum].isEmpty()) {
            for (int i = 0; i < renderBatch[playerNum].size(); ++i) {
                PlayerLines element = (PlayerLines)renderBatch[playerNum].get(i);
                element.render();
            }
            renderBatch[playerNum].clear();
        }
    }

    public static void NoRender(int playerNum) {
        if (renderBatch[playerNum] != null) {
            renderBatch[playerNum].clear();
        }
    }

    public static void addNoLogText(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        noLogText.add(text);
    }

    static {
        renderBatch = new PlayerLinesList[4];
        noLogText = new HashSet();
    }

    class PlayerLines {
        protected int lineDisplayTime;
        protected int renderX;
        protected int renderY;
        protected boolean ignoreRadioLines;
        protected TextDrawObject[] chatLines;
        final /* synthetic */ ChatElement this$0;

        public PlayerLines(ChatElement this$0, int numLines) {
            ChatElement chatElement = this$0;
            Objects.requireNonNull(chatElement);
            this.this$0 = chatElement;
            this.lineDisplayTime = 314;
            this.chatLines = new TextDrawObject[numLines];
            for (int i = 0; i < this.chatLines.length; ++i) {
                this.chatLines[i] = new TextDrawObject(0, 0, 0, true, true, true, true, true, true);
                this.chatLines[i].setDefaultFont(UIFont.Medium);
            }
        }

        public void setMaxCharsPerLine(int maxChars) {
            for (int i = 0; i < this.chatLines.length; ++i) {
                this.chatLines[i].setMaxCharsPerLine(maxChars);
            }
        }

        public TextDrawObject getNewLineObject() {
            if (this.chatLines != null && this.chatLines.length > 0) {
                TextDrawObject last = this.chatLines[this.chatLines.length - 1];
                last.Clear();
                for (int i = this.chatLines.length - 1; i > 0; --i) {
                    this.chatLines[i] = this.chatLines[i - 1];
                }
                this.chatLines[0] = last;
                return this.chatLines[0];
            }
            return null;
        }

        public void render() {
            if (GameServer.server) {
                return;
            }
            if (this.this$0.hasChatToDisplay) {
                int index = 0;
                for (TextDrawObject line : this.chatLines) {
                    boolean outlines;
                    if (!line.getEnabled()) continue;
                    if (line.getWidth() <= 0 || line.getHeight() <= 0) {
                        ++index;
                        continue;
                    }
                    float disptime = line.getInternalClock();
                    if (disptime <= 0.0f || line.getCustomTag().equals("radio") && this.ignoreRadioLines) continue;
                    float alpha = disptime / ((float)this.lineDisplayTime / 4.0f);
                    if (alpha > 1.0f) {
                        alpha = 1.0f;
                    }
                    this.renderY -= line.getHeight() + 1;
                    boolean bl = outlines = line.getDefaultFontEnum() != UIFont.Dialogue;
                    if (doBackDrop && backdropTexture != null) {
                        IndieGL.glBlendFunc(770, 771);
                        backdropTexture.renderInnerBased(this.renderX - line.getWidth() / 2, this.renderY, line.getWidth(), line.getHeight(), 0.0f, 0.0f, 0.0f, 0.4f);
                    }
                    if (index == 0) {
                        line.Draw(this.renderX, this.renderY, outlines, alpha);
                    } else if (this.this$0.historyVal > 0.0f) {
                        line.Draw(this.renderX, this.renderY, outlines, alpha * this.this$0.historyVal);
                    }
                    ++index;
                }
            }
        }

        void clear() {
            if (!this.this$0.hasChatToDisplay) {
                return;
            }
            this.this$0.hasChatToDisplay = false;
            for (int i = 0; i < this.chatLines.length; ++i) {
                if (this.chatLines[i].getInternalClock() <= 0.0f) continue;
                this.chatLines[i].Clear();
                this.chatLines[i].updateInternalTickClock(this.chatLines[i].getInternalClock());
            }
            this.this$0.historyInRange = false;
            this.this$0.historyVal = 0.0f;
        }
    }

    class PlayerLinesList
    extends ArrayList<PlayerLines> {
        final /* synthetic */ ChatElement this$0;

        PlayerLinesList(ChatElement this$0) {
            ChatElement chatElement = this$0;
            Objects.requireNonNull(chatElement);
            this.this$0 = chatElement;
        }
    }
}

