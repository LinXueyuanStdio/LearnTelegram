package com.demo.chat.controller;

import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.LongSparseArray;

import com.demo.chat.ApplicationLoader;
import com.demo.chat.SQLite.SQLiteCursor;
import com.demo.chat.SQLite.SQLitePreparedStatement;
import com.demo.chat.messager.AndroidUtilities;
import com.demo.chat.messager.FileLog;
import com.demo.chat.messager.NativeByteBuffer;
import com.demo.chat.messager.NotificationCenter;
import com.demo.chat.messager.Utilities;
import com.demo.chat.model.Chat;
import com.demo.chat.model.Message;
import com.demo.chat.model.MessageObject;
import com.demo.chat.model.User;
import com.demo.chat.service.LocationSharingService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/25
 * @description null
 * @usage null
 */
public class LocationController extends BaseController
        implements NotificationCenter.NotificationCenterDelegate,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private LocationManager locationManager;
    private LongSparseArray<SharingLocationInfo> sharingLocationsMap = new LongSparseArray<>();
    private ArrayList<SharingLocationInfo> sharingLocations = new ArrayList<>();

    public ArrayList<SharingLocationInfo> sharingLocationsUI = new ArrayList<>();
    private LongSparseArray<SharingLocationInfo> sharingLocationsMapUI = new LongSparseArray<>();


    //region GoogleApiClient.ConnectionCallbacks
    private LocationRequest locationRequest;
    private Boolean playServicesAvailable;
    private boolean wasConnectedToPlayServices;
    private GoogleApiClient googleApiClient;
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private final static long UPDATE_INTERVAL = 1000, FASTEST_INTERVAL = 1000;
    private final static int BACKGROUD_UPDATE_TIME = 30 * 1000;
    private final static int LOCATION_ACQUIRE_TIME = 10 * 1000;
    private final static int FOREGROUND_UPDATE_TIME = 20 * 1000;
    private final static int WATCH_LOCATION_TIMEOUT = 65 * 1000;
    private final static int SEND_NEW_LOCATION_TIME = 2 * 1000;

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
    //endregion


    public static class SharingLocationInfo {
        public long did;
        public int mid;
        public int stopTime;
        public int period;
        public int account;
        public MessageObject messageObject;
    }

    private static volatile LocationController[] Instance = new LocationController[UserConfig.MAX_ACCOUNT_COUNT];

    public static LocationController getInstance(int num) {
        LocationController localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (LocationController.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new LocationController(num);
                }
            }
        }
        return localInstance;
    }

    public LocationController(int instance) {
        super(instance);

        locationManager = (LocationManager) ApplicationLoader.applicationContext.getSystemService(Context.LOCATION_SERVICE);
        googleApiClient = new GoogleApiClient.Builder(ApplicationLoader.applicationContext)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).build();

        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);

        AndroidUtilities.runOnUIThread(() -> {
            LocationController locationController = getAccountInstance().getLocationController();
            getNotificationCenter().addObserver(locationController, NotificationCenter.didReceiveNewMessages);
            getNotificationCenter().addObserver(locationController, NotificationCenter.messagesDeleted);
            getNotificationCenter().addObserver(locationController, NotificationCenter.replaceMessagesObjects);
        });
        loadSharingLocations();
    }


    private void loadSharingLocations() {
        getMessagesStorage().getStorageQueue().postRunnable(() -> {
            final ArrayList<SharingLocationInfo> result = new ArrayList<>();
            final ArrayList<User> users = new ArrayList<>();
            final ArrayList<Chat> chats = new ArrayList<>();
            try {
                ArrayList<Integer> usersToLoad = new ArrayList<>();
                ArrayList<Integer> chatsToLoad = new ArrayList<>();
                SQLiteCursor cursor = getMessagesStorage().getDatabase().queryFinalized("SELECT uid, mid, date, period, message FROM sharing_locations WHERE 1");
                while (cursor.next()) {
                    SharingLocationInfo info = new SharingLocationInfo();
                    info.did = cursor.longValue(0);
                    info.mid = cursor.intValue(1);
                    info.stopTime = cursor.intValue(2);
                    info.period = cursor.intValue(3);
                    info.account = currentAccount;
                    NativeByteBuffer data = cursor.byteBufferValue(4);
                    if (data != null) {
                        info.messageObject = new MessageObject(currentAccount, Message.TLdeserialize(data, data.readInt32(false), false), false);
                        MessagesStorage.addUsersAndChatsFromMessage(info.messageObject.messageOwner, usersToLoad, chatsToLoad);
                        data.reuse();
                    }
                    result.add(info);
                    int lower_id = (int) info.did;
                    int high_id = (int) (info.did >> 32);
                    if (lower_id != 0) {
                        if (lower_id < 0) {
                            if (!chatsToLoad.contains(-lower_id)) {
                                chatsToLoad.add(-lower_id);
                            }
                        } else {
                            if (!usersToLoad.contains(lower_id)) {
                                usersToLoad.add(lower_id);
                            }
                        }
                    } else {
                        /*if (!encryptedChatIds.contains(high_id)) {
                            encryptedChatIds.add(high_id);
                        }*/
                    }
                }
                cursor.dispose();
                if (!chatsToLoad.isEmpty()) {
                    getMessagesStorage().getChatsInternal(TextUtils.join(",", chatsToLoad), chats);
                }
                if (!usersToLoad.isEmpty()) {
                    getMessagesStorage().getUsersInternal(TextUtils.join(",", usersToLoad), users);
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
            if (!result.isEmpty()) {
                AndroidUtilities.runOnUIThread(() -> {
                    getMessagesController().putUsers(users, true);
                    getMessagesController().putChats(chats, true);
                    Utilities.stageQueue.postRunnable(() -> {
                        sharingLocations.addAll(result);
                        for (int a = 0; a < sharingLocations.size(); a++) {
                            SharingLocationInfo info = sharingLocations.get(a);
                            sharingLocationsMap.put(info.did, info);
                        }
                        AndroidUtilities.runOnUIThread(() -> {
                            sharingLocationsUI.addAll(result);
                            for (int a = 0; a < result.size(); a++) {
                                SharingLocationInfo info = result.get(a);
                                sharingLocationsMapUI.put(info.did, info);
                            }
                            startService();
                            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.liveLocationsChanged);
                        });
                    });
                });
            }
        });
    }

    private void startService() {
        try {
            /*if (Build.VERSION.SDK_INT >= 26) {
                ApplicationLoader.applicationContext.startForegroundService(new Intent(ApplicationLoader.applicationContext, LocationSharingService.class));
            } else {*/
            ApplicationLoader.applicationContext.startService(new Intent(ApplicationLoader.applicationContext, LocationSharingService.class));
            //}
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }
    public void removeAllLocationSharings() {
        Utilities.stageQueue.postRunnable(() -> {
            for (int a = 0; a < sharingLocations.size(); a++) {
                SharingLocationInfo info = sharingLocations.get(a);
                //TODO
            }
            sharingLocations.clear();
            sharingLocationsMap.clear();
            saveSharingLocation(null, 2);
            stop(true);
            AndroidUtilities.runOnUIThread(() -> {
                sharingLocationsUI.clear();
                sharingLocationsMapUI.clear();
                stopService();
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.liveLocationsChanged);
            });
        });
    }

    private void saveSharingLocation(final SharingLocationInfo info, final int remove) {
        getMessagesStorage().getStorageQueue().postRunnable(() -> {
            try {
                if (remove == 2) {
                    getMessagesStorage().getDatabase().executeFast("DELETE FROM sharing_locations WHERE 1").stepThis().dispose();
                } else if (remove == 1) {
                    if (info == null) {
                        return;
                    }
                    getMessagesStorage().getDatabase().executeFast("DELETE FROM sharing_locations WHERE uid = " + info.did).stepThis().dispose();
                } else {
                    if (info == null) {
                        return;
                    }
                    SQLitePreparedStatement state = getMessagesStorage().getDatabase().executeFast("REPLACE INTO sharing_locations VALUES(?, ?, ?, ?, ?)");
                    state.requery();

                    NativeByteBuffer data = new NativeByteBuffer(info.messageObject.messageOwner.getObjectSize());
                    info.messageObject.messageOwner.serializeToStream(data);

                    state.bindLong(1, info.did);
                    state.bindInteger(2, info.mid);
                    state.bindInteger(3, info.stopTime);
                    state.bindInteger(4, info.period);
                    state.bindByteBuffer(5, data);

                    state.step();
                    state.dispose();
                    data.reuse();
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }


    private void stop(boolean empty) {
        if (lookingForPeopleNearby || shareMyCurrentLocation) {
            return;
        }
        started = false;
        if (checkPlayServices()) {
            try {
                LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, fusedLocationListener);
                googleApiClient.disconnect();
            } catch (Throwable e) {
                FileLog.e(e);
            }
        }
        locationManager.removeUpdates(gpsLocationListener);
        if (empty) {
            locationManager.removeUpdates(networkLocationListener);
            locationManager.removeUpdates(passiveLocationListener);
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {

    }
}
