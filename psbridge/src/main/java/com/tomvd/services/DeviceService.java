package com.tomvd.services;

import com.tomvd.model.PowerStreamData;

public interface DeviceService {
    void publishPowerSetting(int i);
    void publishPowerSetting(int i, String deviceId);
    void setSl(ServiceLocator sl);
    PowerStreamData getPowerStreamData();
}
