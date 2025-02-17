/*
 * Copyright (C) 2020 The Android Open Source Project
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
package android.os;

import android.Manifest;
import android.annotation.NonNull;
import android.annotation.RequiresPermission;
import android.annotation.SystemApi;
import android.annotation.SystemService;
import android.content.Context;
import android.util.ArraySet;
import android.util.Log;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Allows apps outside the system process to access various bits of configuration defined in
 * /etc/sysconfig and its counterparts on OEM and vendor partitions.
 *
 * TODO: Intended for access by system mainline modules only. Marking as SystemApi until the
 * module-only API surface is available.
 * @hide
 */
@SystemApi
@SystemService(Context.SYSTEM_CONFIG_SERVICE)
public class SystemConfigManager {
    private static final String TAG = SystemConfigManager.class.getSimpleName();

    private final ISystemConfig mInterface;

    /** @hide **/
    public SystemConfigManager() {
        mInterface = ISystemConfig.Stub.asInterface(
                ServiceManager.getService(Context.SYSTEM_CONFIG_SERVICE));
    }

    /**
     * Returns a set of package names for carrier apps that are preinstalled on the device but
     * should be disabled until the matching carrier's SIM is inserted into the device.
     * @return A set of package names.
     */
    @RequiresPermission(Manifest.permission.READ_CARRIER_APP_INFO)
    public @NonNull Set<String> getDisabledUntilUsedPreinstalledCarrierApps() {
        try {
            List<String> apps = mInterface.getDisabledUntilUsedPreinstalledCarrierApps();
            return new ArraySet<>(apps);
        } catch (RemoteException e) {
            Log.e(TAG, "Caught remote exception");
            return Collections.emptySet();
        }
    }

    /**
     * Returns a map that describes helper apps associated with carrier apps that, like the apps
     * returned by {@link #getDisabledUntilUsedPreinstalledCarrierApps()}, should be disabled until
     * the correct SIM is inserted into the device.
     * @return A map with keys corresponding to package names returned by
     *         {@link #getDisabledUntilUsedPreinstalledCarrierApps()} and values as lists of package
     *         names of helper apps.
     */
    @RequiresPermission(Manifest.permission.READ_CARRIER_APP_INFO)
    public @NonNull Map<String, List<String>>
            getDisabledUntilUsedPreinstalledCarrierAssociatedApps() {
        try {
            return (Map<String, List<String>>)
                    mInterface.getDisabledUntilUsedPreinstalledCarrierAssociatedApps();
        } catch (RemoteException e) {
            Log.e(TAG, "Caught remote exception");
            return Collections.emptyMap();
        }
    }

    /**
     * Returns a map that describes helper apps associated with carrier apps that, like the apps
     * returned by {@link #getDisabledUntilUsedPreinstalledCarrierApps()}, should be disabled until
     * the correct SIM is inserted into the device.
     *
     * <p>TODO(b/159069037) expose this and get rid of the other method that omits SDK version.
     *
     * @return A map with keys corresponding to package names returned by
     *         {@link #getDisabledUntilUsedPreinstalledCarrierApps()} and values as lists of package
     *         names of helper apps and the SDK versions when they were first added.
     *
     * @hide
     */
    @RequiresPermission(Manifest.permission.READ_CARRIER_APP_INFO)
    public @NonNull Map<String, List<CarrierAssociatedAppEntry>>
            getDisabledUntilUsedPreinstalledCarrierAssociatedAppEntries() {
        try {
            return (Map<String, List<CarrierAssociatedAppEntry>>)
                    mInterface.getDisabledUntilUsedPreinstalledCarrierAssociatedAppEntries();
        } catch (RemoteException e) {
            Log.e(TAG, "Caught remote exception", e);
            return Collections.emptyMap();
        }
    }
}
