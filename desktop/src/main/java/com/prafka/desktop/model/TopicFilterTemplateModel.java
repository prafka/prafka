package com.prafka.desktop.model;

import com.prafka.core.model.ConsumeFilter;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TopicFilterTemplateModel extends AbstractTrackedModel {

    private String name;
    private boolean byDefault;
    private ConsumeFilter filter;
}
