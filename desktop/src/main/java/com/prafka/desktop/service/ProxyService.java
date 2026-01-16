package com.prafka.desktop.service;

import com.prafka.desktop.model.ProxyModel;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

import static com.prafka.core.util.StreamUtils.tryIgnore;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Singleton
public class ProxyService {

    private final SettingsService settingsService;

    @Inject
    public ProxyService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void init() {
        var proxy = settingsService.getProxy();
        if (proxy.getType() == ProxyModel.Type.NO) {
            System.clearProperty("http.proxyHost");
            System.clearProperty("https.proxyHost");
            System.clearProperty("http.proxyPort");
            System.clearProperty("https.proxyPort");
            System.clearProperty("http.proxyUser");
            System.clearProperty("https.proxyUser");
            System.clearProperty("http.proxyPassword");
            System.clearProperty("https.proxyPassword");
            tryIgnore(() -> Authenticator.setDefault(null));
            return;
        }
        if (proxy.getType() == ProxyModel.Type.MANUAL) {
            if (isNotBlank(proxy.getHost())) {
                System.setProperty("http.proxyHost", proxy.getHost());
                System.setProperty("https.proxyHost", proxy.getHost());
            }
            if (proxy.getPort() != null) {
                System.setProperty("http.proxyPort", proxy.getPort().toString());
                System.setProperty("https.proxyPort", proxy.getPort().toString());
            }
            System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
            if (isNotBlank(proxy.getUser())) {
                System.setProperty("http.proxyUser", proxy.getUser());
                System.setProperty("https.proxyUser", proxy.getUser());
            }
            if (isNotBlank(proxy.getPassword())) {
                System.setProperty("http.proxyPassword", proxy.getPassword());
                System.setProperty("https.proxyPassword", proxy.getPassword());
            }
            if (isNotBlank(proxy.getUser()) && isNotBlank(proxy.getPassword())) {
                tryIgnore(() ->
                        Authenticator.setDefault(new Authenticator() {
                            @Override
                            protected PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication(proxy.getUser(), proxy.getPassword().toCharArray());
                            }
                        })
                );
            }
        }
    }
}
