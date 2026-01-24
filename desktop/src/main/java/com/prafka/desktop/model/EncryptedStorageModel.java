package com.prafka.desktop.model;

import com.prafka.desktop.ApplicationProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Model for encrypted persistent storage containing sensitive application data.
 *
 * <p>Stores cluster configurations, proxy settings, and topic filter templates
 * that require encryption with the master password.
 */
@Getter
@Setter
public class EncryptedStorageModel {

    private String version = ApplicationProperties.VERSION;
    private List<ClusterModel> clusters = new ArrayList<>();
    private ProxyModel proxy = new ProxyModel();
    private Map<String, Map<String, List<TopicFilterTemplateModel>>> clusterTopicsFilterTemplates = new HashMap<>();
}
