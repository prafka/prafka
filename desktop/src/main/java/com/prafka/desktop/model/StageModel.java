package com.prafka.desktop.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StageModel {

    private StageSizeModel size = new StageSizeModel();

    @Getter
    @Setter
    public static class StageSizeModel {
        private Integer width;
        private Integer height;
    }
}
