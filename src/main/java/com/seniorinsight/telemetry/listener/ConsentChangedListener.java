package com.seniorinsight.telemetry.listener;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import com.seniorinsight.telemetry.event.ConsentChangedEvent;
import com.seniorinsight.telemetry.service.DeviceStatusService;

@Component
public class ConsentChangedListener {
    private final DeviceStatusService statusService;
    public ConsentChangedListener(DeviceStatusService statusService) { this.statusService = statusService; }

    @EventListener
    public void onConsentChanged(ConsentChangedEvent event) {
        statusService.updateConsent(event.idosoId(), event.activeDataTypes());
    }
}
