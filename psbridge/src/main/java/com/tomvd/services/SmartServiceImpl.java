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
        Boolean chargerEnabled = sl.getApplicationService().getChargerEnabled();
        Integer soc = sl.getApplicationService().getSoc();
        if (data == null || gridPower == null || enabled == null || !enabled || soc == null) {return;}

        if (soc < 20) {
            // battery soc dropped too low, make sure we shut off the inverter and dont do anything more
            if (data.currentPower() > 0) {
                sl.getDeviceService().publishPowerSetting(0);
            }
            return;
        }

        if (chargerEnabled == null || !chargerEnabled) {
            if (gridPower > 0) // we are (still) pulling power from the grid - increase output
            {
                int newPowerSetting = Math.min(config.getMaxPower() == null?666: config.getMaxPower(), gridPower + data.currentPower());
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

    @Scheduled(fixedDelay = "15s")
    public void runCharger() {
        Integer gridPower = sl.getApplicationService().getGridPower();
        Boolean enabled = sl.getApplicationService().getSmartEnabled();
        Boolean chargerEnabled = sl.getApplicationService().getChargerEnabled();
        Integer soc = sl.getApplicationService().getSoc();
        if (gridPower == null || enabled == null || !enabled || soc == null) {return;}

        // charger only takes 400-500w but we take some margin to avoid turning it on/off the whole time
        if (soc < 100 && gridPower < -700 && (chargerEnabled == null || !chargerEnabled)) {
            sl.getApplicationService().setCharger(true);
            sl.getDeviceService().publishPowerSetting(0); // makes sure we are not charging and giving power
        }
        // if battery is full or we are using gridpower - turn charger off
        if (soc == 100 || (gridPower > 100 && (chargerEnabled == null || chargerEnabled))) {
            sl.getApplicationService().setCharger(false);
        }
    }
}
