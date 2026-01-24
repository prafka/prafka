package com.prafka.desktop.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Model for storing JavaFX stage (window) settings.
 *
 * <p>Persists window dimensions to restore user-preferred sizes between sessions.
 */
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
