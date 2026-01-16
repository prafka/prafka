package com.prafka.desktop.util;

import com.prafka.desktop.service.ExecutorHolder;
import javafx.concurrent.Task;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.reactfx.Subscription;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class CodeHighlight {

    private static final Pattern JSON_PATTERN = Pattern.compile("(?<KEY>\"([^\"\\\\]|\\\\.)*\"\\s*:)|(?<BRACE>([{}]))|(?<BRACKET>([\\[\\]]))|(?<STRING>\"([^\"\\\\]|\\\\.)*\")|(?<NUMBER>-?[0-9]+(.[0-9]+)?(([eE])?[0-9]+)?)|(?<NULL>(?i:null))|(?<BOOLEAN>(?i:true|false))");

    public static StyleSpans<Collection<String>> highlightDefault(String text) {
        return new StyleSpansBuilder<Collection<String>>()
                .add(Collections.singleton("default"), text.length())
                .create();
    }

    public static StyleSpans<Collection<String>> highlightKV(String text) {
        var builder = new StyleSpansBuilder<Collection<String>>();
        var lineList = text.split("\n");
        for (int i = 0; i < lineList.length; i++) {
            var line = lineList[i];
            var entry = line.split("=");
            builder.add(Collections.singleton("key"), entry[0].length());
            builder.add(Collections.singleton("default"), 1);
            if (entry.length > 1) builder.add(Collections.singleton("value"), entry[1].length());
            if (i < lineList.length - 1) builder.add(Collections.singleton("default"), 1);
        }
        return builder.create();
    }

    public static StyleSpans<Collection<String>> highlightAvro(String text) {
        return highlightJson(text);
    }

    public static StyleSpans<Collection<String>> highlightJson(String text) {
        var builder = new StyleSpansBuilder<Collection<String>>();
        int lastPosition = 0;
        try {
            var matcher = JSON_PATTERN.matcher(text);
            while (matcher.find()) {
                var styleClass =
                        matcher.group("KEY") != null ? "field-name" :
                                matcher.group("BRACE") != null ? "default" :
                                        matcher.group("BRACKET") != null ? "default" :
                                                matcher.group("STRING") != null ? "value-string" :
                                                        matcher.group("NUMBER") != null ? "value-number" :
                                                                matcher.group("NULL") != null ? "value-string" :
                                                                        matcher.group("BOOLEAN") != null ? "value-bool" : "default";
                builder.add(Collections.emptyList(), matcher.start() - lastPosition);
                builder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
                lastPosition = matcher.end();
            }
        } catch (Exception ignored) {
            return highlightDefault(text);
        }
        if (lastPosition == 0) {
            builder.add(Collections.singleton("default"), text.length());
        }
        return builder.create();
    }

    public static StyleSpans<Collection<String>> highlightProtobuf(String text) {
        return highlightDefault(text); // todo add support for protobuf
    }

    public static StyleSpans<Collection<String>> highlightJs(String text) {
        return highlightDefault(text); // todo add support for js
    }

    public static Subscription codeHighlightSubscription(CodeArea codeArea, Supplier<StyleSpans<Collection<String>>> supplier) {
        return codeArea.multiPlainChanges()
                .successionEnds(Duration.ofMillis(500))
                .retainLatestUntilLater(ExecutorHolder.codeHighlightExecutor)
                .supplyTask(() -> {
                    var task = new Task<StyleSpans<Collection<String>>>() {
                        @Override
                        protected StyleSpans<Collection<String>> call() {
                            return supplier.get();
                        }
                    };
                    ExecutorHolder.codeHighlightExecutor.execute(task);
                    return task;
                })
                .awaitLatest(codeArea.multiPlainChanges())
                .filterMap(it -> it.isSuccess() ? Optional.of(it.get()) : Optional.empty())
                .subscribe(it -> codeArea.setStyleSpans(0, it));
    }
}
