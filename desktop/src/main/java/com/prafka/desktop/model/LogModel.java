package com.prafka.desktop.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Model for logging configuration settings.
 *
 * <p>Contains preferences for log level (debug mode) configuration.
 */
@Getter
@Setter
public class LogModel {

    private boolean debug;
}
