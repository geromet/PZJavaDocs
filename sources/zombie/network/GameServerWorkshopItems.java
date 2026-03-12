/*
 * Decompiled with CFR 0.152.
 */
package zombie.network;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Objects;
import zombie.ZomboidFileSystem;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.core.znet.ISteamWorkshopCallback;
import zombie.core.znet.SteamUGCDetails;
import zombie.core.znet.SteamUtils;
import zombie.core.znet.SteamWorkshop;
import zombie.core.znet.SteamWorkshopItem;
import zombie.debug.DebugLog;
import zombie.network.CoopSlave;
import zombie.network.GameServer;

public class GameServerWorkshopItems {
    private static void noise(String s) {
        DebugLog.log("Workshop: " + s);
    }

    public static boolean Install(ArrayList<Long> itemIDList) {
        if (!GameServer.server) {
            return false;
        }
        if (itemIDList.isEmpty()) {
            return true;
        }
        ArrayList<WorkshopItem> workshopItems = new ArrayList<WorkshopItem>();
        int[] installTries = new int[itemIDList.size()];
        for (long itemID : itemIDList) {
            WorkshopItem item = new WorkshopItem(itemID);
            workshopItems.add(item);
        }
        if (GameServer.coop) {
            CoopSlave.status("UI_ServerStatus_Requesting_Workshop_Item_Details");
        }
        if (!GameServerWorkshopItems.QueryItemDetails(workshopItems)) {
            return false;
        }
        int processedCount = 0;
        boolean[] completed = new boolean[workshopItems.size()];
        if (GameServer.coop) {
            CoopSlave.instance.sendMessage("status", null, Translator.getText("UI_ServerStatus_Downloaded_Workshop_Items_Progress", processedCount, workshopItems.size()));
        }
        while (true) {
            SteamUtils.runLoop();
            boolean busy = false;
            for (int i = 0; i < workshopItems.size(); ++i) {
                WorkshopItem item = workshopItems.get(i);
                item.update();
                if (item.state == WorkshopInstallState.Fail) {
                    CoopSlave.status(SteamWorkshop.instance.GetItemInstallFolder(item.id));
                    int n = i;
                    installTries[n] = installTries[n] + 1;
                    if (installTries[n] >= 3) {
                        return false;
                    }
                    if (GameServer.coop) {
                        CoopSlave.instance.sendMessage("status", null, Translator.getText("UI_ServerStatus_Downloaded_Workshop_Reinstall_Progress", processedCount, workshopItems.size()));
                    }
                    ZomboidFileSystem.deleteDirectory(SteamWorkshop.instance.GetItemInstallFolder(item.id));
                    item.setState(WorkshopInstallState.CheckItemState);
                }
                if (item.state != WorkshopInstallState.Ready) {
                    busy = true;
                    break;
                }
                if (completed[i]) continue;
                completed[i] = true;
                if (!GameServer.coop) continue;
                CoopSlave.instance.sendMessage("status", null, Translator.getText("UI_ServerStatus_Downloaded_Workshop_Items_Progress", ++processedCount, workshopItems.size()));
            }
            if (!busy) break;
            try {
                Thread.sleep(33L);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        GameServer.workshopInstallFolders = new String[itemIDList.size()];
        GameServer.workshopTimeStamps = new long[itemIDList.size()];
        for (int i = 0; i < itemIDList.size(); ++i) {
            long itemID = itemIDList.get(i);
            String folder = SteamWorkshop.instance.GetItemInstallFolder(itemID);
            if (folder == null) {
                GameServerWorkshopItems.noise("GetItemInstallFolder() failed ID=" + itemID);
                return false;
            }
            GameServerWorkshopItems.noise(itemID + " installed to " + folder);
            GameServer.workshopInstallFolders[i] = folder;
            GameServer.workshopTimeStamps[i] = SteamWorkshop.instance.GetItemInstallTimeStamp(itemID);
        }
        return true;
    }

    private static boolean QueryItemDetails(ArrayList<WorkshopItem> workshopItems) {
        long[] itemIDs = new long[workshopItems.size()];
        for (int i = 0; i < workshopItems.size(); ++i) {
            WorkshopItem item = workshopItems.get(i);
            itemIDs[i] = item.id;
        }
        ItemQuery query = new ItemQuery();
        query.handle = SteamWorkshop.instance.CreateQueryUGCDetailsRequest(itemIDs, query);
        if (query.handle == 0L) {
            return false;
        }
        while (true) {
            SteamUtils.runLoop();
            if (query.isCompleted()) break;
            if (query.isNotCompleted()) {
                return false;
            }
            try {
                Thread.sleep(33L);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        block4: for (SteamUGCDetails details : query.details) {
            for (WorkshopItem workshopItem : workshopItems) {
                if (workshopItem.id != details.getID()) continue;
                workshopItem.details = details;
                continue block4;
            }
        }
        return true;
    }

    private static class WorkshopItem
    implements ISteamWorkshopCallback {
        long id;
        WorkshopInstallState state = WorkshopInstallState.CheckItemState;
        long downloadStartTime;
        long downloadQueryTime;
        String error;
        SteamUGCDetails details;

        WorkshopItem(long id) {
            this.id = id;
        }

        void update() {
            switch (this.state.ordinal()) {
                case 0: {
                    this.CheckItemState();
                    break;
                }
                case 1: {
                    this.DownloadPending();
                    break;
                }
            }
        }

        void setState(WorkshopInstallState newState) {
            GameServerWorkshopItems.noise("item state " + String.valueOf((Object)this.state) + " -> " + String.valueOf((Object)newState) + " ID=" + this.id);
            this.state = newState;
        }

        void CheckItemState() {
            long itemState = SteamWorkshop.instance.GetItemState(this.id);
            GameServerWorkshopItems.noise("GetItemState()=" + SteamWorkshopItem.ItemState.toString(itemState) + " ID=" + this.id);
            if (SteamWorkshopItem.ItemState.Installed.and(itemState) && this.details != null && this.details.getTimeCreated() != 0L && this.details.getTimeUpdated() != SteamWorkshop.instance.GetItemInstallTimeStamp(this.id)) {
                GameServerWorkshopItems.noise("Installed status but timeUpdated doesn't match!!!");
                this.RemoveFolderForReinstall();
                itemState |= (long)SteamWorkshopItem.ItemState.NeedsUpdate.getValue();
            }
            if (itemState == (long)SteamWorkshopItem.ItemState.None.getValue() || SteamWorkshopItem.ItemState.NeedsUpdate.and(itemState) || !new File(SteamWorkshop.instance.GetItemInstallFolder(this.id)).exists()) {
                if (SteamWorkshop.instance.DownloadItem(this.id, true, this)) {
                    this.setState(WorkshopInstallState.DownloadPending);
                    this.downloadStartTime = System.currentTimeMillis();
                    return;
                }
                this.error = "DownloadItemFalse";
                this.setState(WorkshopInstallState.Fail);
                return;
            }
            if (SteamWorkshopItem.ItemState.Installed.and(itemState)) {
                this.setState(WorkshopInstallState.Ready);
                return;
            }
            this.error = "UnknownItemState";
            this.setState(WorkshopInstallState.Fail);
        }

        void RemoveFolderForReinstall() {
            String folder = SteamWorkshop.instance.GetItemInstallFolder(this.id);
            if (folder == null) {
                GameServerWorkshopItems.noise("not removing install folder because GetItemInstallFolder() failed ID=" + this.id);
                return;
            }
            Path path = Paths.get(folder, new String[0]);
            if (!Files.exists(path, new LinkOption[0])) {
                GameServerWorkshopItems.noise("not removing install folder because it does not exist : \"" + folder + "\"");
                return;
            }
            try {
                Files.walkFileTree(path, (FileVisitor<? super Path>)new SimpleFileVisitor<Path>(this){
                    {
                        Objects.requireNonNull(this$0);
                    }

                    @Override
                    public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                        Files.delete(path);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path directory, IOException ioException) throws IOException {
                        Files.delete(directory);
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
            catch (Exception ex) {
                ExceptionLogger.logException(ex);
            }
        }

        void DownloadPending() {
            long time = System.currentTimeMillis();
            if (this.downloadQueryTime + 100L > time) {
                return;
            }
            this.downloadQueryTime = time;
            long itemState = SteamWorkshop.instance.GetItemState(this.id);
            GameServerWorkshopItems.noise("DownloadPending GetItemState()=" + SteamWorkshopItem.ItemState.toString(itemState) + " ID=" + this.id);
            if (SteamWorkshopItem.ItemState.NeedsUpdate.and(itemState)) {
                long[] progress = new long[2];
                if (SteamWorkshop.instance.GetItemDownloadInfo(this.id, progress)) {
                    GameServerWorkshopItems.noise("download " + progress[0] + "/" + progress[1] + " ID=" + this.id);
                }
                return;
            }
        }

        @Override
        public void onItemCreated(long itemID, boolean bUserNeedsToAcceptWorkshopLegalAgreement) {
        }

        @Override
        public void onItemNotCreated(int result) {
        }

        @Override
        public void onItemUpdated(boolean bUserNeedsToAcceptWorkshopLegalAgreement) {
        }

        @Override
        public void onItemNotUpdated(int result) {
        }

        @Override
        public void onItemSubscribed(long itemID) {
            GameServerWorkshopItems.noise("onItemSubscribed itemID=" + itemID);
        }

        @Override
        public void onItemNotSubscribed(long itemID, int result) {
            GameServerWorkshopItems.noise("onItemNotSubscribed itemID=" + itemID + " result=" + result);
        }

        @Override
        public void onItemDownloaded(long itemID) {
            GameServerWorkshopItems.noise("onItemDownloaded itemID=" + itemID + " time=" + (System.currentTimeMillis() - this.downloadStartTime) + " ms");
            if (itemID != this.id) {
                return;
            }
            SteamWorkshop.instance.RemoveCallback(this);
            this.setState(WorkshopInstallState.CheckItemState);
        }

        @Override
        public void onItemNotDownloaded(long itemID, int result) {
            GameServerWorkshopItems.noise("onItemNotDownloaded itemID=" + itemID + " result=" + result);
            if (itemID != this.id) {
                return;
            }
            SteamWorkshop.instance.RemoveCallback(this);
            this.error = "ItemNotDownloaded";
            this.setState(WorkshopInstallState.Fail);
        }

        @Override
        public void onItemQueryCompleted(long handle, int numResults) {
            GameServerWorkshopItems.noise("onItemQueryCompleted handle=" + handle + " numResult=" + numResults);
        }

        @Override
        public void onItemQueryNotCompleted(long handle, int result) {
            GameServerWorkshopItems.noise("onItemQueryNotCompleted handle=" + handle + " result=" + result);
        }
    }

    private static enum WorkshopInstallState {
        CheckItemState,
        DownloadPending,
        Ready,
        Fail;

    }

    private static final class ItemQuery
    implements ISteamWorkshopCallback {
        long handle;
        ArrayList<SteamUGCDetails> details;
        boolean completed;
        boolean notCompleted;

        private ItemQuery() {
        }

        public boolean isCompleted() {
            return this.completed;
        }

        public boolean isNotCompleted() {
            return this.notCompleted;
        }

        @Override
        public void onItemCreated(long itemID, boolean bUserNeedsToAcceptWorkshopLegalAgreement) {
        }

        @Override
        public void onItemNotCreated(int result) {
        }

        @Override
        public void onItemUpdated(boolean bUserNeedsToAcceptWorkshopLegalAgreement) {
        }

        @Override
        public void onItemNotUpdated(int result) {
        }

        @Override
        public void onItemSubscribed(long itemID) {
        }

        @Override
        public void onItemNotSubscribed(long itemID, int result) {
        }

        @Override
        public void onItemDownloaded(long itemID) {
        }

        @Override
        public void onItemNotDownloaded(long itemID, int result) {
        }

        @Override
        public void onItemQueryCompleted(long handle, int numResults) {
            GameServerWorkshopItems.noise("onItemQueryCompleted handle=" + handle + " numResult=" + numResults);
            if (handle != this.handle) {
                return;
            }
            SteamWorkshop.instance.RemoveCallback(this);
            ArrayList<SteamUGCDetails> detailsList = new ArrayList<SteamUGCDetails>();
            for (int i = 0; i < numResults; ++i) {
                SteamUGCDetails details = SteamWorkshop.instance.GetQueryUGCResult(handle, i);
                if (details == null) continue;
                detailsList.add(details);
            }
            this.details = detailsList;
            SteamWorkshop.instance.ReleaseQueryUGCRequest(handle);
            this.completed = true;
        }

        @Override
        public void onItemQueryNotCompleted(long handle, int result) {
            GameServerWorkshopItems.noise("onItemQueryNotCompleted handle=" + handle + " result=" + result);
            if (handle != this.handle) {
                return;
            }
            SteamWorkshop.instance.RemoveCallback(this);
            SteamWorkshop.instance.ReleaseQueryUGCRequest(handle);
            this.notCompleted = true;
        }
    }
}

