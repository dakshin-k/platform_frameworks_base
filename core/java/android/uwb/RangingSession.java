/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.uwb;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.os.Binder;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;

/**
 * This class provides a way to control an active UWB ranging session.
 * <p>It also defines the required {@link RangingSession.Callback} that must be implemented
 * in order to be notified of UWB ranging results and status events related to the
 * {@link RangingSession}.
 *
 * <p>To get an instance of {@link RangingSession}, first use
 * {@link UwbManager#openRangingSession(PersistableBundle, Executor, Callback)} to request to open a
 * session. Once the session is opened, a {@link RangingSession} object is provided through
 * {@link RangingSession.Callback#onOpenSuccess(RangingSession, PersistableBundle)}. If opening a
 * session fails, the failure is reported through
 * {@link RangingSession.Callback#onClosed(int, PersistableBundle)} with the failure reason.
 *
 * @hide
 */
public final class RangingSession implements AutoCloseable {
    private static final String TAG = "Uwb.RangingSession";
    private final SessionHandle mSessionHandle;
    private final IUwbAdapter mAdapter;
    private final Executor mExecutor;
    private final Callback mCallback;

    private enum State {
        INIT,
        OPEN,
        CLOSED,
    }

    private State mState;

    /**
     * Interface for receiving {@link RangingSession} events
     */
    public interface Callback {
        /**
         * Invoked when {@link UwbManager#openRangingSession(PersistableBundle, Executor, Callback)}
         * is successful
         *
         * @param session the newly opened {@link RangingSession}
         * @param sessionInfo session specific parameters from lower layers
         */
        void onOpenSuccess(@NonNull RangingSession session, @NonNull PersistableBundle sessionInfo);

        /**
         * @hide
         */
        @Retention(RetentionPolicy.SOURCE)
        @IntDef(value = {
                CLOSE_REASON_UNKNOWN,
                CLOSE_REASON_LOCAL_CLOSE_API,
                CLOSE_REASON_LOCAL_BAD_PARAMETERS,
                CLOSE_REASON_LOCAL_GENERIC_ERROR,
                CLOSE_REASON_LOCAL_MAX_SESSIONS_REACHED,
                CLOSE_REASON_LOCAL_SYSTEM_POLICY,
                CLOSE_REASON_REMOTE_GENERIC_ERROR,
                CLOSE_REASON_REMOTE_REQUEST})
        @interface CloseReason {}

        /**
         * Indicates that the session was closed or failed to open due to an unknown reason
         */
        int CLOSE_REASON_UNKNOWN = 0;

        /**
         * Indicates that the session was closed or failed to open because
         * {@link AutoCloseable#close()} or {@link RangingSession#close()} was called
         */
        int CLOSE_REASON_LOCAL_CLOSE_API = 1;

        /**
         * Indicates that the session failed to open due to erroneous parameters passed
         * to {@link UwbManager#openRangingSession(PersistableBundle, Executor, Callback)}
         */
        int CLOSE_REASON_LOCAL_BAD_PARAMETERS = 2;

        /**
         * Indicates that the session was closed due to some local error on this device besides the
         * error code already listed
         */
        int CLOSE_REASON_LOCAL_GENERIC_ERROR = 3;

        /**
         * Indicates that the session failed to open because the number of currently open sessions
         * is equal to {@link UwbManager#getMaxSimultaneousSessions()}
         */
        int CLOSE_REASON_LOCAL_MAX_SESSIONS_REACHED = 4;

        /**
         * Indicates that the session was closed or failed to open due to local system policy, such
         * as privacy policy, power management policy, permissions, and more.
         */
        int CLOSE_REASON_LOCAL_SYSTEM_POLICY = 5;

        /**
         * Indicates that the session was closed or failed to open due to an error with the remote
         * device besides error codes already listed.
         */
        int CLOSE_REASON_REMOTE_GENERIC_ERROR = 6;

        /**
         * Indicates that the session was closed or failed to open due to an explicit request from
         * the remote device.
         */
        int CLOSE_REASON_REMOTE_REQUEST = 7;

        /**
         * Indicates that the session was closed for a protocol specific reason. The associated
         * {@link PersistableBundle} should be consulted for additional information.
         */
        int CLOSE_REASON_PROTOCOL_SPECIFIC = 8;

        /**
         * Invoked when session is either closed spontaneously, or per user request via
         * {@link RangingSession#close()} or {@link AutoCloseable#close()}, or when session failed
         * to open.
         *
         * @param reason reason for the session closure
         * @param parameters protocol specific parameters related to the close reason
         */
        void onClosed(@CloseReason int reason, @NonNull PersistableBundle parameters);

        /**
         * Called once per ranging interval even when a ranging measurement fails
         *
         * @param rangingReport ranging report for this interval's measurements
         */
        void onReportReceived(@NonNull RangingReport rangingReport);
    }

    /**
     * @hide
     */
    public RangingSession(Executor executor, Callback callback, IUwbAdapter adapter,
            SessionHandle sessionHandle) {
        mState = State.INIT;
        mExecutor = executor;
        mCallback = callback;
        mAdapter = adapter;
        mSessionHandle = sessionHandle;
    }

    /**
     * @hide
     */
    public boolean isOpen() {
        return mState == State.OPEN;
    }

    /**
     * Close the ranging session
     * <p>If this session is currently open, it will close and stop the session.
     * <p>If the session is in the process of being opened, it will attempt to stop the session from
     * being opened.
     * <p>If the session is already closed, the registered
     * {@link Callback#onClosed(int, PersistableBundle)} callback will still be invoked.
     *
     * <p>{@link Callback#onClosed(int, PersistableBundle)} will be invoked using the same callback
     * object given to {@link UwbManager#openRangingSession(PersistableBundle, Executor, Callback)}
     * when the {@link RangingSession} was opened. The callback will be invoked after each call to
     * {@link #close()}, even if the {@link RangingSession} is already closed.
     */
    @Override
    public void close() {
        if (mState == State.CLOSED) {
            mExecutor.execute(() -> mCallback.onClosed(
                    Callback.CLOSE_REASON_LOCAL_CLOSE_API, new PersistableBundle()));
            return;
        }

        try {
            mAdapter.closeRanging(mSessionHandle);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    /**
     * @hide
     */
    public void onRangingStarted(@NonNull PersistableBundle parameters) {
        if (mState == State.CLOSED) {
            Log.w(TAG, "onRangingStarted invoked for a closed session");
            return;
        }

        mState = State.OPEN;
        final long identity = Binder.clearCallingIdentity();
        try {
            mExecutor.execute(() -> mCallback.onOpenSuccess(this, parameters));
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    /**
     * @hide
     */
    public void onRangingClosed(@Callback.CloseReason int reason, PersistableBundle parameters) {
        mState = State.CLOSED;
        final long identity = Binder.clearCallingIdentity();
        try {
            mExecutor.execute(() -> mCallback.onClosed(reason, parameters));
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    /**
     * @hide
     */
    public void onRangingResult(@NonNull RangingReport report) {
        if (!isOpen()) {
            Log.w(TAG, "onRangingResult invoked for non-open session");
            return;
        }

        final long identity = Binder.clearCallingIdentity();
        try {
            mExecutor.execute(() -> mCallback.onReportReceived(report));
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }
}
