package com.prafka.desktop.util.control;

import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

public class RetentionFileChooser {

    private static final AtomicReference<File> prevDirectory = new AtomicReference<>(SystemUtils.getUserHome());
    private final FileChooser fileChooser;

    public RetentionFileChooser() {
        fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(prevDirectory.get());
    }

    public void setInitialFileName(String fileName) {
        fileChooser.setInitialFileName(fileName);
    }

    public void addExtensionFilter(FileChooser.ExtensionFilter filter) {
        fileChooser.getExtensionFilters().add(filter);
    }

    public File showOpenDialog(Window owner) {
        var file = fileChooser.showOpenDialog(owner);
        setPrevDirectory(file);
        return file;
    }

    public File showSaveDialog(Window owner) {
        var file = fileChooser.showSaveDialog(owner);
        setPrevDirectory(file);
        return file;
    }

    private static void setPrevDirectory(File file) {
        if (file == null || file.getParentFile() == null || !file.getParentFile().isDirectory()) return;
        prevDirectory.set(file.getParentFile());
    }
}
