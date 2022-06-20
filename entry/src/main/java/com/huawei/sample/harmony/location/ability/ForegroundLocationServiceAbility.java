package com.huawei.sample.harmony.location.ability;

import com.huawei.hms.location.harmony.*;
import com.huawei.sample.harmony.location.interfaces.ILocationCallback;
import ohos.aafwk.ability.Ability;
import ohos.aafwk.ability.LocalRemoteObject;
import ohos.aafwk.content.Intent;
import ohos.agp.utils.Color;
import ohos.event.notification.NotificationHelper;
import ohos.event.notification.NotificationRequest;
import ohos.event.notification.NotificationSlot;
import ohos.location.Locator;
import ohos.location.LocatorCallback;
import ohos.location.RequestParam;
import ohos.rpc.IRemoteObject;
import ohos.hiviewdfx.HiLog;
import ohos.hiviewdfx.HiLogLabel;
import ohos.rpc.RemoteException;

import java.util.ArrayList;
import java.util.List;

public class ForegroundLocationServiceAbility extends Ability {
    private static final HiLogLabel LABEL_LOG = new HiLogLabel(3, 0xD001100, "Demo");
    private ArrayList<ILocationCallback> locationCallbacks = new ArrayList<>();
    public FusedLocationClient fusedLocationClient;
    private Locator locator;

    MyLocatorCallback locatorCallback = new MyLocatorCallback();

    public class MyLocatorCallback implements LocatorCallback {

        @Override
        public void onLocationReport(ohos.location.Location location) {
            String result = "[new]onLocationResult location[Longitude,Latitude,Accuracy,"
                    + "CountryName,State,City,County,FeatureName,Provider]:"
                    + location.getLongitude() + "," + location.getLatitude() + ","
                    + location.getAccuracy();
            HiLog.error(LABEL_LOG, " Locator: onLocationReport "+result);
        }

        @Override
        public void onStatusChanged(int type) {
            HiLog.error(LABEL_LOG, "Locator onStatusChanged:" + type);
        }

        @Override
        public void onErrorReport(int type) {
            HiLog.error(LABEL_LOG, "Locator onErrorReport:" + type);
        }
    }

    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult != null) {
                List<Location> locations = locationResult.getLocations();
                if (!locations.isEmpty()) {
                    HiLog.error(LABEL_LOG, "onLocationResult location is not empty");
                    List<HWLocation> hwLocationList = locationResult.getHWLocationList();
                    if (!hwLocationList.isEmpty()) {
                        HiLog.error(LABEL_LOG, "onLocationResult hwLocationList is not empty");
                    }
                    for (HWLocation hwLocation : hwLocationList) {
                        String result = "[new]onLocationResult location[Longitude,Latitude,Accuracy,"
                                + "CountryName,State,City,County,FeatureName,Provider]:"
                                + hwLocation.getLongitude() + "," + hwLocation.getLatitude() + ","
                                + hwLocation.getAccuracy() + "," + hwLocation.getCountryName() + ","
                                + hwLocation.getState() + "," + hwLocation.getCity() + ","
                                + hwLocation.getCounty() + "," + hwLocation.getFeatureName() + ","
                                + hwLocation.getProvider();
                        HiLog.error(LABEL_LOG, "onLocationResult:"+result);
                        for (ILocationCallback locationCallback : locationCallbacks) {
                            locationCallback.onLocationArrival(locationResult);
                        }
                    }
                }
            }
        }

        @Override
        public void onLocationAvailability(LocationAvailability locationAvailability) {
            super.onLocationAvailability(locationAvailability);
            if (locationAvailability != null) {
                String result = "onLocationAvailability isLocationAvailable:"
                        + locationAvailability.isLocationAvailable();
                HiLog.error(LABEL_LOG, "onLocationAvailability:"+result);
            }
        }
    };

    @Override
    public void onStart(Intent intent) {
        HiLog.error(LABEL_LOG, "ForegroundLocationServiceAbility::onStart");
        super.onStart(intent);
        locator = new Locator(this);
        fusedLocationClient = new FusedLocationClient(this);
    }

    @Override
    public void onBackground() {
        super.onBackground();
        HiLog.info(LABEL_LOG, "ForegroundLocationServiceAbility::onBackground");
    }

    @Override
    public void onStop() {
        super.onStop();
        cancelNotification();
        HiLog.info(LABEL_LOG, "ForegroundLocationServiceAbility::onStop");
    }

    @Override
    public void onCommand(Intent intent, boolean restart, int startId) {
    }

    @Override
    public IRemoteObject onConnect(Intent intent) {
        return new LocationRemoteObject(this);
    }

    @Override
    public void onDisconnect(Intent intent) {
        super.onDisconnect(intent);
    }

    public void startLocation() {
        sendNotification("定位中...");
        requestLocationUpdates();
    }

    public void stopLocation() {
        cancelNotification();
        removeLocationUpdates();
    }

    public void addListener(ILocationCallback locationCallback){
        locationCallbacks.add(locationCallback);
    }

    public void removeListener(ILocationCallback locationCallback){
        locationCallbacks.remove(locationCallback);

    }

    private void startLocatorLocation(){
        RequestParam requestParam = new RequestParam(RequestParam.PRIORITY_FAST_FIRST_FIX,0,0);
        locator.startLocating(requestParam, locatorCallback);
    }

    private void stopLocatorLocation(){
        locator.stopLocating(locatorCallback);
    }

    private void requestLocationUpdates() {
        LocationRequest locationRequest = buildLocationRequest();
        HiLog.error(LABEL_LOG, "ForegroundLocationServiceAbility::requestLocationUpdates !!");
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback)
                .addOnSuccessListener(v -> {
                    HiLog.error(LABEL_LOG, "requestLocationUpdatesWithCallback onSuccess");
                })
                .addOnFailureListener(e -> {
                    HiLog.error(LABEL_LOG, "requestLocationUpdatesWithCallback onFailure:" + e.getMessage());
                });
    }

    private void removeLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
                .addOnSuccessListener(v -> {
                    HiLog.error(LABEL_LOG, "removeLocationUpdatesWithCallback onSuccess");
                })
                .addOnFailureListener(e -> {
                    HiLog.error(LABEL_LOG, "removeLocationUpdatesWithCallback onFailure:" + e.getMessage());
                });
    }

    private LocationRequest buildLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(100);
        locationRequest.setFastestInterval(100);
        locationRequest.setNeedAddress(true);
        locationRequest.setLanguage("zh");
        return locationRequest;
    }

    private void sendNotification(String str) {
        String slotId = "foregroundServiceId-Location";
        String slotName = "foregroundService-Location";
        NotificationSlot slot = new NotificationSlot(slotId, slotName, NotificationSlot.LEVEL_MIN);
        slot.setDescription("Foreground location service");
        slot.setEnableVibration(true);
        slot.setLockscreenVisibleness(NotificationRequest.VISIBLENESS_TYPE_PUBLIC);
        slot.setEnableLight(true);
        slot.setLedLightColor(Color.RED.getValue());
        try {
            NotificationHelper.addNotificationSlot(slot);
        } catch (RemoteException ex) {
            HiLog.error(LABEL_LOG, "Exception occurred during addNotificationSlot invocation.");
        }
        int notificationId = 10124;
        NotificationRequest request = new NotificationRequest(notificationId);
        request.setSlotId(slot.getId());
        String title = "后台定位服务";
        String text = "" + str;
        NotificationRequest.NotificationNormalContent content = new NotificationRequest.NotificationNormalContent();
        content.setTitle(title)
                .setText(text);
        NotificationRequest.NotificationContent notificationContent =
                new NotificationRequest.NotificationContent(content);
        request.setContent(notificationContent);
        keepBackgroundRunning(notificationId, request);
    }

    private void cancelNotification() {
        cancelBackgroundRunning();
    }


    public static class LocationRemoteObject extends LocalRemoteObject {
        private final ForegroundLocationServiceAbility locationService;

        LocationRemoteObject(ForegroundLocationServiceAbility locationService) {
            this.locationService = locationService;
        }

        public void startLocation(){
            this.locationService.startLocatorLocation();
        }

        public void stopLocation(){
            this.locationService.stopLocatorLocation();
        }
        public void addListener(ILocationCallback iLocationCallback){
            this.locationService.addListener(iLocationCallback);
        }
        public void removeListener(ILocationCallback iLocationCallback){
            this.locationService.removeListener(iLocationCallback);
        }
    }

}