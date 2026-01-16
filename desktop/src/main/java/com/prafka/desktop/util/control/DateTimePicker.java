package com.prafka.desktop.util.control;

import com.prafka.desktop.util.JavaFXUtils;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.DatePicker;
import javafx.scene.input.KeyEvent;
import javafx.util.StringConverter;

import java.time.*;
import java.time.format.DateTimeFormatter;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class DateTimePicker extends DatePicker {

    private final ObjectProperty<LocalDateTime> dateTimeValue = new SimpleObjectProperty<>();
    private DateTimeFormatter formatter;

    public DateTimePicker() {
        super(LocalDate.now(ZoneOffset.UTC));
        dateTimeValue.set(LocalDateTime.now(ZoneOffset.UTC));
        setFormat("yyyy-MM-dd HH:mm:ss");
        valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                dateTimeValue.set(null);
            } else {
                var time = (dateTimeValue.get() != null) ? dateTimeValue.get().toLocalTime() : LocalTime.now(ZoneOffset.UTC);
                dateTimeValue.set(LocalDateTime.of(newValue, time));
            }
        });
        addEventHandler(KeyEvent.KEY_PRESSED, JavaFXUtils.onKeyEnter(this::commitValue));
    }

    public void setFormat(String format) {
        formatter = DateTimeFormatter.ofPattern(format).withZone(ZoneOffset.UTC);
        getEditor().setPrefColumnCount(format.length());
        setConverter(new InternalConverter());
    }

    public ObjectProperty<LocalDateTime> dateTimeValueProperty() {
        return dateTimeValue;
    }

    public LocalDateTime getDateTimeValue() {
        return dateTimeValue.get();
    }

    public long getTimestampValue() {
        return dateTimeValue.get().toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    public void setTimestampValue(long timestamp) {
        dateTimeValue.set(LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC));
    }

    private class InternalConverter extends StringConverter<LocalDate> {
        public String toString(LocalDate object) {
            var value = dateTimeValue.get();
            return (value != null) ? value.format(formatter) : null;
        }

        public LocalDate fromString(String string) {
            if (isBlank(string)) {
                dateTimeValue.set(null);
                return null;
            }
            try {
                dateTimeValue.set(LocalDateTime.parse(string, formatter));
            } catch (Exception e) {
                getEditor().setText(formatter.format(dateTimeValue.get()));
            }
            return dateTimeValue.get().toLocalDate();
        }
    }
}
