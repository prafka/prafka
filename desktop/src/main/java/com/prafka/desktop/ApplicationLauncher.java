package com.prafka.desktop;

/**
 * Entry point for application.
 *
 * <p>Delegates to {@link Application#main(String[])} for actual application launch.
 * This class exists to avoid issues with JavaFX module system when launching directly.
 */
public class ApplicationLauncher {
    public static void main(String[] args) {
        Application.main(args);
    }
}
