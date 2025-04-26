package com.tomvd.services;

import com.tomvd.model.PowerStreamData;

public interface DeviceService {
    void publishPowerSetting(int i);
    void setSl(ServiceLocator sl);
    PowerStreamData getPowerStreamData();
}
