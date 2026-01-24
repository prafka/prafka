package com.prafka.desktop.controller;

import com.prafka.desktop.ApplicationProperties;
import com.prafka.desktop.service.BackendClient;
import com.prafka.desktop.service.OpenLinkService;
import com.prafka.desktop.util.JavaFXUtils;
import jakarta.inject.Inject;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.Strings;

import static com.prafka.desktop.concurrent.FutureServiceAdapter.futureTask;

/**
 * Controller for the help and about dialog.
 *
 * <p>Displays application version, links to documentation and website,
 * and checks for available updates.
 */
public class HelpController extends AbstractController {

    public Label labelCurrentVersion;
    public VBox paneNewVersion;
    public Hyperlink linkSite;
    public Hyperlink linkDocumentation;
    public Hyperlink linkChangelog;
    public Hyperlink linkEmail;
    public Button buttonCancel;

    private final BackendClient backendClient;
    private final ApplicationProperties applicationProperties;
    private final OpenLinkService openLinkService;

    @Inject
    public HelpController(BackendClient backendClient, ApplicationProperties applicationProperties, OpenLinkService openLinkService) {
        this.backendClient = backendClient;
        this.applicationProperties = applicationProperties;
        this.openLinkService = openLinkService;
    }

    @Override
    public void initFxml() {
        labelCurrentVersion.setText(i18nService.get("common.version") + " " + ApplicationProperties.VERSION);

        futureTask(backendClient::getCurrentVersion)
                .onSuccess(version -> {
                    if (Strings.CI.equals(ApplicationProperties.VERSION, version)) return;
                    var link = new Hyperlink(i18nService.get("helpView.newVersionAvailable").formatted(version));
                    link.setOnAction(it -> openLinkService.openLink(applicationProperties.distUrl() + "#download"));
                    VBox.setMargin(link, new Insets(10, 0, 0, 0));
                    paneNewVersion.getChildren().add(link);
                })
                .start();

        linkSite.setText(applicationProperties.domain());
        linkSite.setOnAction(it -> openLinkService.openLink(applicationProperties.distUrl()));
        linkDocumentation.setOnAction(it -> openLinkService.openLink(applicationProperties.docsUrl()));
        linkChangelog.setOnAction(it -> openLinkService.openLink(applicationProperties.docsUrl() + "/changelog"));

        themeService.setIcon16(linkEmail, "at.png");
        linkEmail.setText(applicationProperties.email());
        linkEmail.setOnAction(it -> openLinkService.openLink("mailto:" + applicationProperties.email()));
        var copyEmail = new MenuItem(i18nService.get("common.copy"));
        copyEmail.setOnAction(it -> JavaFXUtils.copyToClipboard(applicationProperties.email()));
        linkEmail.setContextMenu(new ContextMenu(copyEmail));

        buttonCancel.setOnAction(it -> JavaFXUtils.getStage(it).close());
    }
}
