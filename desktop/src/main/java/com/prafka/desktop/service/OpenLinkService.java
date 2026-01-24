package com.prafka.desktop.service;

import javafx.application.Application;
import javafx.application.HostServices;

/**
 * Opens external URLs in the system's default web browser.
 *
 * <p>Uses JavaFX host services to launch URLs in the user's preferred browser.
 */
public class OpenLinkService {

    private final HostServices hostServices;

    public OpenLinkService(Application application) {
        this.hostServices = application.getHostServices();
    }

    public void openLink(String url) {
        hostServices.showDocument(url);
    }
}
