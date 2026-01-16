package com.prafka.desktop.util.control;

import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import lombok.Getter;

import java.util.Comparator;
import java.util.Objects;

@Getter
public class NumberLabel extends Label {

    public static final Comparator<NumberLabel> COMPARATOR = Comparator.comparingLong(it -> it.getSource().longValue());

    private final Number source;

    public NumberLabel(Number source) {
        super(Objects.toString(source, null));
        this.source = source;
    }

    public NumberLabel(ImageView imageView) {
        super("", imageView);
        this.source = 0;
    }

    public NumberLabel(Number source, String text) {
        super(text);
        this.source = source;
    }
}
