package com.prafka.desktop.util.control;

import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import lombok.Getter;

import java.util.Comparator;
import java.util.Objects;

/**
 * Label that wraps a numeric value for sortable table columns.
 *
 * <p>Stores the source number separately from the displayed text,
 * enabling proper numeric sorting in tables.
 */
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
