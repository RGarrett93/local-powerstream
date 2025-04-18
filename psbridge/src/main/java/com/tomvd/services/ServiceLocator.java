package com.tomvd.services;

import io.micronaut.context.annotation.Context;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

// need this horrible thingy to solve circular dependency, no time for fancy stuff
@Context
public class ServiceLocator {
    private final DeviceService deviceService;
    private final ApplicationService applicationService;

    @Inject
    public ServiceLocator(DeviceService deviceService, ApplicationService applicationService) {
        this.deviceService = deviceService;
        this.applicationService = applicationService;
    }

    @PostConstruct
    public void init() {
        this.deviceService.setSl(this);
        this.applicationService.setSl(this);
    }

    public DeviceService getDeviceService() {
        return deviceService;
    }

    public ApplicationService getApplicationService() {
        return applicationService;
    }
}
