package com.prafka.desktop.model;

import com.prafka.core.model.ConsumeFilter;
import lombok.Getter;
import lombok.Setter;

/**
 * Model for a saved topic message filter template.
 *
 * <p>Stores named filter configurations for topic message consumption,
 * with support for marking a template as the default.
 */
@Getter
@Setter
public class TopicFilterTemplateModel extends AbstractTrackedModel {

    private String name;
    private boolean byDefault;
    private ConsumeFilter filter;
}
