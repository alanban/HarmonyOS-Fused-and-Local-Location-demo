package com.huawei.sample.harmony.location.interfaces;

import com.huawei.hms.location.harmony.LocationResult;

public interface ILocationCallback {
    void onLocationArrival(LocationResult locationResult);
}
