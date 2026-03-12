/*
 * Decompiled with CFR 0.152.
 */
package zombie.asset;

import java.util.ArrayList;
import java.util.Objects;
import zombie.asset.AssetManager;
import zombie.asset.AssetPath;
import zombie.asset.AssetStateObserver;
import zombie.asset.AssetTask;
import zombie.asset.AssetType;

public abstract class Asset {
    protected final AssetManager assetManager;
    private final AssetPath path;
    private int refCount = 0;
    final PRIVATE priv = new PRIVATE(this);
    protected boolean isDefered;

    protected Asset(AssetPath path, AssetManager manager) {
        this.path = path;
        this.assetManager = manager;
    }

    public abstract AssetType getType();

    public State getState() {
        return this.priv.currentState;
    }

    public boolean isEmpty() {
        return this.priv.currentState == State.EMPTY;
    }

    public boolean isReady() {
        return this.priv.currentState == State.READY || this.isDefered;
    }

    public boolean isFailure() {
        return this.priv.currentState == State.FAILURE;
    }

    public void onCreated(State state) {
        this.priv.onCreated(state);
    }

    public int getRefCount() {
        return this.refCount;
    }

    public ObserverCallback getObserverCb() {
        if (this.priv.cb == null) {
            this.priv.cb = new ObserverCallback();
        }
        return this.priv.cb;
    }

    public AssetPath getPath() {
        return this.path;
    }

    public AssetManager getAssetManager() {
        return this.assetManager;
    }

    protected void onBeforeReady() {
    }

    protected void onBeforeEmpty() {
    }

    public void addDependency(Asset dependentAsset) {
        this.priv.addDependency(dependentAsset);
    }

    public void removeDependency(Asset dependentAsset) {
        this.priv.removeDependency(dependentAsset);
    }

    int addRef() {
        return ++this.refCount;
    }

    int rmRef() {
        return --this.refCount;
    }

    public void setAssetParams(AssetManager.AssetParams params) {
    }

    final class PRIVATE
    implements AssetStateObserver {
        State currentState;
        State desiredState;
        int emptyDepCount;
        int failedDepCount;
        ObserverCallback cb;
        AssetTask task;
        final /* synthetic */ Asset this$0;

        PRIVATE(Asset this$0) {
            Asset asset = this$0;
            Objects.requireNonNull(asset);
            this.this$0 = asset;
            this.currentState = State.EMPTY;
            this.desiredState = State.EMPTY;
            this.emptyDepCount = 1;
        }

        void onCreated(State state) {
            assert (this.emptyDepCount == 1);
            assert (this.failedDepCount == 0);
            this.currentState = state;
            this.desiredState = State.READY;
            this.failedDepCount = state == State.FAILURE ? 1 : 0;
            this.emptyDepCount = 0;
        }

        void addDependency(Asset dependentAsset) {
            assert (this.desiredState != State.EMPTY);
            dependentAsset.getObserverCb().add(this);
            if (dependentAsset.isEmpty()) {
                ++this.emptyDepCount;
            }
            if (dependentAsset.isFailure()) {
                ++this.failedDepCount;
            }
            this.checkState();
        }

        void removeDependency(Asset dependentAsset) {
            dependentAsset.getObserverCb().remove(this);
            if (dependentAsset.isEmpty()) {
                assert (this.emptyDepCount > 0);
                --this.emptyDepCount;
            }
            if (dependentAsset.isFailure()) {
                assert (this.failedDepCount > 0);
                --this.failedDepCount;
            }
            this.checkState();
        }

        @Override
        public void onStateChanged(State oldState, State newState, Asset asset) {
            assert (oldState != newState);
            assert (this.currentState != State.EMPTY || this.desiredState != State.EMPTY);
            if (oldState == State.EMPTY) {
                assert (this.emptyDepCount > 0);
                --this.emptyDepCount;
            }
            if (oldState == State.FAILURE) {
                assert (this.failedDepCount > 0);
                --this.failedDepCount;
            }
            if (newState == State.EMPTY) {
                ++this.emptyDepCount;
            }
            if (newState == State.FAILURE) {
                ++this.failedDepCount;
            }
            this.checkState();
        }

        void onLoadingSucceeded() {
            assert (this.currentState != State.READY);
            assert (this.emptyDepCount == 1);
            --this.emptyDepCount;
            this.task = null;
            this.checkState();
        }

        void onLoadingFailed() {
            assert (this.currentState != State.READY);
            assert (this.emptyDepCount == 1);
            ++this.failedDepCount;
            --this.emptyDepCount;
            this.task = null;
            this.checkState();
        }

        void checkState() {
            State oldState = this.currentState;
            if (this.failedDepCount > 0 && this.currentState != State.FAILURE) {
                this.currentState = State.FAILURE;
                this.this$0.getAssetManager().onStateChanged(oldState, this.currentState, this.this$0);
                if (this.cb != null) {
                    this.cb.invoke(oldState, this.currentState, this.this$0);
                }
            }
            if (this.failedDepCount == 0) {
                if (this.emptyDepCount == 0 && this.currentState != State.READY && this.desiredState != State.EMPTY) {
                    this.this$0.onBeforeReady();
                    this.currentState = State.READY;
                    this.this$0.getAssetManager().onStateChanged(oldState, this.currentState, this.this$0);
                    if (this.cb != null) {
                        this.cb.invoke(oldState, this.currentState, this.this$0);
                    }
                }
                if (this.emptyDepCount > 0 && this.currentState != State.EMPTY) {
                    this.this$0.onBeforeEmpty();
                    this.currentState = State.EMPTY;
                    this.this$0.getAssetManager().onStateChanged(oldState, this.currentState, this.this$0);
                    if (this.cb != null) {
                        this.cb.invoke(oldState, this.currentState, this.this$0);
                    }
                }
            }
        }
    }

    public static enum State {
        EMPTY,
        READY,
        FAILURE;

    }

    public static final class ObserverCallback
    extends ArrayList<AssetStateObserver> {
        public void invoke(State oldState, State newState, Asset asset) {
            int n = this.size();
            for (int i = 0; i < n; ++i) {
                ((AssetStateObserver)this.get(i)).onStateChanged(oldState, newState, asset);
            }
        }
    }
}

