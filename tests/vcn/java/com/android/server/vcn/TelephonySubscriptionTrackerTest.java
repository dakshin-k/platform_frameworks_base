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

package com.android.server.vcn;

import static android.telephony.CarrierConfigManager.ACTION_CARRIER_CONFIG_CHANGED;
import static android.telephony.CarrierConfigManager.EXTRA_SLOT_INDEX;
import static android.telephony.CarrierConfigManager.EXTRA_SUBSCRIPTION_INDEX;
import static android.telephony.SubscriptionManager.INVALID_SIM_SLOT_INDEX;
import static android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID;

import static com.android.server.vcn.TelephonySubscriptionTracker.TelephonySubscriptionSnapshot;
import static com.android.server.vcn.TelephonySubscriptionTracker.TelephonySubscriptionTrackerCallback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.annotation.NonNull;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.ParcelUuid;
import android.os.test.TestLooper;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;
import android.util.ArraySet;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Tests for TelephonySubscriptionTracker */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class TelephonySubscriptionTrackerTest {
    private static final ParcelUuid TEST_PARCEL_UUID = new ParcelUuid(UUID.randomUUID());
    private static final int TEST_SIM_SLOT_INDEX = 1;
    private static final int TEST_SUBSCRIPTION_ID_1 = 2;
    private static final SubscriptionInfo TEST_SUBINFO_1 = mock(SubscriptionInfo.class);
    private static final int TEST_SUBSCRIPTION_ID_2 = 3;
    private static final SubscriptionInfo TEST_SUBINFO_2 = mock(SubscriptionInfo.class);
    private static final Map<Integer, ParcelUuid> TEST_SUBID_TO_GROUP_MAP;

    static {
        final Map<Integer, ParcelUuid> subIdToGroupMap = new HashMap<>();
        subIdToGroupMap.put(TEST_SUBSCRIPTION_ID_1, TEST_PARCEL_UUID);
        subIdToGroupMap.put(TEST_SUBSCRIPTION_ID_2, TEST_PARCEL_UUID);
        TEST_SUBID_TO_GROUP_MAP = Collections.unmodifiableMap(subIdToGroupMap);
    }

    @NonNull private final Context mContext;
    @NonNull private final TestLooper mTestLooper;
    @NonNull private final Handler mHandler;
    @NonNull private final TelephonySubscriptionTracker.Dependencies mDeps;

    @NonNull private final SubscriptionManager mSubscriptionManager;
    @NonNull private final CarrierConfigManager mCarrierConfigManager;

    @NonNull private TelephonySubscriptionTrackerCallback mCallback;
    @NonNull private TelephonySubscriptionTracker mTelephonySubscriptionTracker;

    public TelephonySubscriptionTrackerTest() {
        mContext = mock(Context.class);
        mTestLooper = new TestLooper();
        mHandler = new Handler(mTestLooper.getLooper());
        mDeps = mock(TelephonySubscriptionTracker.Dependencies.class);

        mSubscriptionManager = mock(SubscriptionManager.class);
        mCarrierConfigManager = mock(CarrierConfigManager.class);

        doReturn(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
                .when(mContext)
                .getSystemServiceName(SubscriptionManager.class);
        doReturn(mSubscriptionManager)
                .when(mContext)
                .getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);

        doReturn(Context.CARRIER_CONFIG_SERVICE)
                .when(mContext)
                .getSystemServiceName(CarrierConfigManager.class);
        doReturn(mCarrierConfigManager)
                .when(mContext)
                .getSystemService(Context.CARRIER_CONFIG_SERVICE);

        // subId 1, 2 are in same subGrp, only subId 1 is active
        doReturn(TEST_PARCEL_UUID).when(TEST_SUBINFO_1).getGroupUuid();
        doReturn(TEST_PARCEL_UUID).when(TEST_SUBINFO_2).getGroupUuid();
        doReturn(TEST_SIM_SLOT_INDEX).when(TEST_SUBINFO_1).getSimSlotIndex();
        doReturn(INVALID_SIM_SLOT_INDEX).when(TEST_SUBINFO_2).getSimSlotIndex();
        doReturn(TEST_SUBSCRIPTION_ID_1).when(TEST_SUBINFO_1).getSubscriptionId();
        doReturn(TEST_SUBSCRIPTION_ID_2).when(TEST_SUBINFO_2).getSubscriptionId();
    }

    @Before
    public void setUp() throws Exception {
        mCallback = mock(TelephonySubscriptionTrackerCallback.class);
        mTelephonySubscriptionTracker =
                new TelephonySubscriptionTracker(mContext, mHandler, mCallback, mDeps);
        mTelephonySubscriptionTracker.register();

        doReturn(true).when(mDeps).isConfigForIdentifiedCarrier(any());
        doReturn(Arrays.asList(TEST_SUBINFO_1, TEST_SUBINFO_2))
                .when(mSubscriptionManager)
                .getAllSubscriptionInfoList();
    }

    private IntentFilter getIntentFilter() {
        final ArgumentCaptor<IntentFilter> captor = ArgumentCaptor.forClass(IntentFilter.class);
        verify(mContext).registerReceiver(any(), captor.capture(), any(), any());

        return captor.getValue();
    }

    private OnSubscriptionsChangedListener getOnSubscriptionsChangedListener() {
        final ArgumentCaptor<OnSubscriptionsChangedListener> captor =
                ArgumentCaptor.forClass(OnSubscriptionsChangedListener.class);
        verify(mSubscriptionManager).addOnSubscriptionsChangedListener(any(), captor.capture());

        return captor.getValue();
    }

    private Intent buildTestBroadcastIntent(boolean hasValidSubscription) {
        Intent intent = new Intent(ACTION_CARRIER_CONFIG_CHANGED);
        intent.putExtra(EXTRA_SLOT_INDEX, TEST_SIM_SLOT_INDEX);
        intent.putExtra(
                EXTRA_SUBSCRIPTION_INDEX,
                hasValidSubscription ? TEST_SUBSCRIPTION_ID_1 : INVALID_SUBSCRIPTION_ID);

        return intent;
    }

    private TelephonySubscriptionSnapshot buildExpectedSnapshot(Set<ParcelUuid> activeSubGroups) {
        return buildExpectedSnapshot(TEST_SUBID_TO_GROUP_MAP, activeSubGroups);
    }

    private TelephonySubscriptionSnapshot buildExpectedSnapshot(
            Map<Integer, ParcelUuid> subIdToGroupMap, Set<ParcelUuid> activeSubGroups) {
        return new TelephonySubscriptionSnapshot(subIdToGroupMap, activeSubGroups);
    }

    private void verifyNoActiveSubscriptions() {
        verify(mCallback).onNewSnapshot(
                argThat(snapshot -> snapshot.getActiveSubscriptionGroups().isEmpty()));
    }

    private void setupReadySubIds() {
        mTelephonySubscriptionTracker.setReadySubIdsBySlotId(
                Collections.singletonMap(TEST_SIM_SLOT_INDEX, TEST_SUBSCRIPTION_ID_1));
    }

    @Test
    public void testRegister() throws Exception {
        verify(mContext)
                .registerReceiver(
                        eq(mTelephonySubscriptionTracker),
                        any(IntentFilter.class),
                        any(),
                        eq(mHandler));
        final IntentFilter filter = getIntentFilter();
        assertEquals(1, filter.countActions());
        assertTrue(filter.hasAction(ACTION_CARRIER_CONFIG_CHANGED));

        verify(mSubscriptionManager)
                .addOnSubscriptionsChangedListener(any(HandlerExecutor.class), any());
        assertNotNull(getOnSubscriptionsChangedListener());
    }

    @Test
    public void testUnregister() throws Exception {
        mTelephonySubscriptionTracker.unregister();

        verify(mContext).unregisterReceiver(eq(mTelephonySubscriptionTracker));

        final OnSubscriptionsChangedListener listener = getOnSubscriptionsChangedListener();
        verify(mSubscriptionManager).removeOnSubscriptionsChangedListener(eq(listener));
    }

    @Test
    public void testOnSubscriptionsChangedFired_NoReadySubIds() throws Exception {
        final OnSubscriptionsChangedListener listener = getOnSubscriptionsChangedListener();
        listener.onSubscriptionsChanged();
        mTestLooper.dispatchAll();

        verifyNoActiveSubscriptions();
    }

    @Test
    public void testOnSubscriptionsChangedFired_WithReadySubIds() throws Exception {
        setupReadySubIds();

        final OnSubscriptionsChangedListener listener = getOnSubscriptionsChangedListener();
        listener.onSubscriptionsChanged();
        mTestLooper.dispatchAll();

        final Set<ParcelUuid> activeSubGroups = Collections.singleton(TEST_PARCEL_UUID);
        verify(mCallback).onNewSnapshot(eq(buildExpectedSnapshot(activeSubGroups)));
    }

    @Test
    public void testReceiveBroadcast_ConfigReadyWithSubscriptions() throws Exception {
        mTelephonySubscriptionTracker.onReceive(mContext, buildTestBroadcastIntent(true));
        mTestLooper.dispatchAll();

        final Set<ParcelUuid> activeSubGroups = Collections.singleton(TEST_PARCEL_UUID);
        verify(mCallback).onNewSnapshot(eq(buildExpectedSnapshot(activeSubGroups)));
    }

    @Test
    public void testReceiveBroadcast_ConfigReadyNoSubscriptions() throws Exception {
        doReturn(new ArrayList<SubscriptionInfo>())
                .when(mSubscriptionManager)
                .getAllSubscriptionInfoList();

        mTelephonySubscriptionTracker.onReceive(mContext, buildTestBroadcastIntent(true));
        mTestLooper.dispatchAll();

        // Expect an empty snapshot
        verify(mCallback).onNewSnapshot(
                eq(buildExpectedSnapshot(Collections.emptyMap(), Collections.emptySet())));
    }

    @Test
    public void testReceiveBroadcast_SlotCleared() throws Exception {
        setupReadySubIds();

        mTelephonySubscriptionTracker.onReceive(mContext, buildTestBroadcastIntent(false));
        mTestLooper.dispatchAll();

        verifyNoActiveSubscriptions();
        assertTrue(mTelephonySubscriptionTracker.getReadySubIdsBySlotId().isEmpty());
    }

    @Test
    public void testReceiveBroadcast_ConfigNotReady() throws Exception {
        doReturn(false).when(mDeps).isConfigForIdentifiedCarrier(any());

        mTelephonySubscriptionTracker.onReceive(mContext, buildTestBroadcastIntent(true));
        mTestLooper.dispatchAll();

        // No interactions expected; config was not loaded
        verifyNoMoreInteractions(mCallback);
    }

    @Test
    public void testSubscriptionsClearedAfterValidTriggersCallbacks() throws Exception {
        final Set<ParcelUuid> activeSubGroups = Collections.singleton(TEST_PARCEL_UUID);

        mTelephonySubscriptionTracker.onReceive(mContext, buildTestBroadcastIntent(true));
        mTestLooper.dispatchAll();
        verify(mCallback).onNewSnapshot(eq(buildExpectedSnapshot(activeSubGroups)));
        assertNotNull(
                mTelephonySubscriptionTracker.getReadySubIdsBySlotId().get(TEST_SIM_SLOT_INDEX));

        doReturn(Collections.emptyList()).when(mSubscriptionManager).getAllSubscriptionInfoList();
        mTelephonySubscriptionTracker.onReceive(mContext, buildTestBroadcastIntent(true));
        mTestLooper.dispatchAll();
        verify(mCallback).onNewSnapshot(
                eq(buildExpectedSnapshot(Collections.emptyMap(), Collections.emptySet())));
    }

    @Test
    public void testSlotClearedAfterValidTriggersCallbacks() throws Exception {
        final Set<ParcelUuid> activeSubGroups = Collections.singleton(TEST_PARCEL_UUID);

        mTelephonySubscriptionTracker.onReceive(mContext, buildTestBroadcastIntent(true));
        mTestLooper.dispatchAll();
        verify(mCallback).onNewSnapshot(eq(buildExpectedSnapshot(activeSubGroups)));
        assertNotNull(
                mTelephonySubscriptionTracker.getReadySubIdsBySlotId().get(TEST_SIM_SLOT_INDEX));

        mTelephonySubscriptionTracker.onReceive(mContext, buildTestBroadcastIntent(false));
        mTestLooper.dispatchAll();
        verify(mCallback).onNewSnapshot(eq(buildExpectedSnapshot(Collections.emptySet())));
        assertNull(mTelephonySubscriptionTracker.getReadySubIdsBySlotId().get(TEST_SIM_SLOT_INDEX));
    }

    @Test
    public void testTelephonySubscriptionSnapshotGetGroupForSubId() throws Exception {
        final TelephonySubscriptionSnapshot snapshot =
                new TelephonySubscriptionSnapshot(TEST_SUBID_TO_GROUP_MAP, Collections.emptySet());

        assertEquals(TEST_PARCEL_UUID, snapshot.getGroupForSubId(TEST_SUBSCRIPTION_ID_1));
        assertEquals(TEST_PARCEL_UUID, snapshot.getGroupForSubId(TEST_SUBSCRIPTION_ID_2));
    }

    @Test
    public void testTelephonySubscriptionSnapshotGetAllSubIdsInGroup() throws Exception {
        final TelephonySubscriptionSnapshot snapshot =
                new TelephonySubscriptionSnapshot(TEST_SUBID_TO_GROUP_MAP, Collections.emptySet());

        assertEquals(
                new ArraySet<>(Arrays.asList(TEST_SUBSCRIPTION_ID_1, TEST_SUBSCRIPTION_ID_2)),
                snapshot.getAllSubIdsInGroup(TEST_PARCEL_UUID));
    }
}
