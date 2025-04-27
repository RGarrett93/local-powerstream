package com.tomvd.services;

import com.tomvd.configuration.SmartConfiguration;
import com.tomvd.model.PowerStreamData;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class SmartServiceImpl implements SmartService {
    private ServiceLocator sl;
    private final SmartConfiguration config;

    @Inject
    public SmartServiceImpl(SmartConfiguration config) {
        this.config = config;
    }

    @Override
    public void setSl(ServiceLocator sl) {
        this.sl = sl;
    }

    @Scheduled(fixedDelay = "6s")
    public void run() {
        PowerStreamData data = sl.getDeviceService().getPowerStreamData();
        Integer gridPower = sl.getApplicationService().getGridPower();
        Boolean enabled = sl.getApplicationService().getSmartEnabled();
        if (data == null || gridPower == null || enabled == null || !enabled) {return;}

/*
        if (data.currentPower() > 0) {
            if (data.avgVoltage() < 24.3) {
                // battery voltage dropped too low, make sure we shut off the inverter
                sl.getDeviceService().publishPowerSetting(0);
                return;
            }
        }
        if (gridPower < 470 - 200 && charger is off) {
            // enable charger
            return;
        }
        if (gridPower > 0 && charger is on) {
            // disable charger
            return;
        }
*/
        if (gridPower > 0) // we are (still) pulling power from the grid - increase output
        {
            int newPowerSetting = Math.min(666, gridPower + data.currentPower());
            if (Math.abs(data.currentPower() - newPowerSetting) > 10) { // only publish a new powersetting if it changes > 10w
                sl.getDeviceService().publishPowerSetting(newPowerSetting);
            }
        }
        if (gridPower < 0 && data.currentPower() > 0) // we are sending battery power in the grid - lower output
        {
            int newPowerSetting = Math.max(0,data.currentPower()+gridPower);
            if (Math.abs(data.currentPower() - newPowerSetting) > 10) { // only publish a new powersetting if it changes > 10w
                sl.getDeviceService().publishPowerSetting(newPowerSetting);
            }
        }
    }
}
