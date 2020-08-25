package com.demo.chat.controller;

import android.content.Context;
import android.location.LocationManager;

import com.demo.chat.ApplicationLoader;
import com.demo.chat.messager.AndroidUtilities;
import com.demo.chat.messager.NotificationCenter;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/25
 * @description null
 * @usage null
 */
class LocationController extends BaseController implements NotificationCenter.NotificationCenterDelegate {

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

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {

    }
}
