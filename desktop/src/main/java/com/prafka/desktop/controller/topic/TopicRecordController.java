package com.prafka.desktop.controller.topic;

import com.prafka.core.model.Record;
import com.prafka.desktop.controller.AbstractController;
import com.prafka.desktop.util.CodeHighlight;
import com.prafka.desktop.util.FormatUtils;
import com.prafka.desktop.util.JavaFXUtils;
import com.prafka.desktop.util.control.RetentionFileChooser;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static com.prafka.core.util.JsonFactory.gsonPretty;
import static com.prafka.desktop.concurrent.ServiceAdapter.task;

public class TopicRecordController extends AbstractController {

    public TabPane tabPane;
    public BorderPane paneKey;
    public CodeArea codeAreaKey;
    public BorderPane paneValue;
    public CodeArea codeAreaValue;
    public VBox boxHeaders;
    public GridPane paneMetadata;
    public Button buttonCancel;
    public Button buttonCopy;
    public Button buttonExport;
    // todo add support for resend message

    private String topicName;
    private Record record;

    public void setData(String topicName, Record record) {
        this.topicName = topicName;
        this.record = record;
    }

    @Override
    public void initFxml() {
        initializeKeyValue();
        initializeButtons();
    }

    @Override
    public void initUi() {
        codeAreaKey.replaceText(record.getKeyFormatted());
        task(() -> record.isKeyIsJson() ? CodeHighlight.highlightJson(record.getKeyFormatted()) : CodeHighlight.highlightDefault(record.getKeyFormatted()))
                .onSuccess(it -> codeAreaKey.setStyleSpans(0, it))
                .start();

        codeAreaValue.replaceText(record.getValueFormatted());
        task(() -> record.isValueIsJson() ? CodeHighlight.highlightJson(record.getValueFormatted()) : CodeHighlight.highlightDefault(record.getValueFormatted()))
                .onSuccess(it -> codeAreaValue.setStyleSpans(0, it))
                .start();

        initializeHeaders();
        initializeMetadata();
    }

    private void initializeKeyValue() {
        BiConsumer<BorderPane, Supplier<String>> addCopyBehavior = (pane, content) -> {
            var box = new VBox(new ImageView(themeService.getIcon16("copy.png")));
            box.setAlignment(Pos.CENTER_RIGHT);
            box.setVisible(false);
            pane.setRight(box);
            pane.setOnMouseEntered(it -> box.setVisible(true));
            pane.setOnMouseExited(it -> box.setVisible(false));
            pane.setOnMouseClicked(it -> {
                JavaFXUtils.copyToClipboard(content.get());
                sceneService.showSnackbarSuccess(JavaFXUtils.getStage(it), Pos.BOTTOM_LEFT, i18nService.get("common.copiedToClipboard"));
            });
        };

        codeAreaKey.setParagraphGraphicFactory(LineNumberFactory.get(codeAreaKey));
        codeAreaKey.getStyleClass().addAll("code-area-json", "code-area-border");
        addCopyBehavior.accept(paneKey, () -> codeAreaKey.getText());

        codeAreaValue.setParagraphGraphicFactory(LineNumberFactory.get(codeAreaValue));
        codeAreaValue.getStyleClass().addAll("code-area-json", "code-area-border");
        addCopyBehavior.accept(paneValue, () -> codeAreaValue.getText());
    }

    private void initializeHeaders() {
        boxHeaders.getChildren().addAll(
                record.getHeaders().entrySet().stream().map(header -> {
                    var node = createCardHeader(header.getKey(), header.getValue());
                    VBox.setMargin(node, new Insets(0, 0, 10, 0));
                    return node;
                }).toList()
        );
    }

    private void initializeMetadata() {
        var timestamp = createCardMetadata(i18nService.get("common.timestamp").toUpperCase(), settingsService.getTimestampFormat().getFullFormatter().format(Instant.ofEpochMilli(record.getTimestamp())));
        GridPane.setMargin(timestamp, new Insets(0, 5, 0, 0));
        paneMetadata.add(timestamp, 0, 0);

        var timestampType = createCardMetadata(i18nService.get("common.timestampType").toUpperCase(), record.getTimestampType().toString());
        GridPane.setMargin(timestampType, new Insets(0, 0, 0, 5));
        paneMetadata.add(timestampType, 1, 0);

        var partition = createCardMetadata(i18nService.get("common.partition").toUpperCase(), String.valueOf(record.getPartition()));
        GridPane.setMargin(partition, new Insets(10, 5, 0, 0));
        paneMetadata.add(partition, 0, 1);

        var offset = createCardMetadata(i18nService.get("common.offset").toUpperCase(), String.valueOf(record.getOffset()));
        GridPane.setMargin(offset, new Insets(10, 0, 0, 5));
        paneMetadata.add(offset, 1, 1);

        var keySize = createCardMetadata(i18nService.get("common.keySize").toUpperCase(), FormatUtils.prettySizeInBytes(record.getKeySize()));
        GridPane.setMargin(keySize, new Insets(10, 5, 0, 0));
        paneMetadata.add(keySize, 0, 2);

        var valueSize = createCardMetadata(i18nService.get("common.valueSize").toUpperCase(), FormatUtils.prettySizeInBytes(record.getValueSize()));
        GridPane.setMargin(valueSize, new Insets(10, 0, 0, 5));
        paneMetadata.add(valueSize, 1, 2);
    }

    private void initializeButtons() {
        buttonCancel.setOnAction(it -> JavaFXUtils.getStage(it).close());
        buttonCopy.setOnAction(it -> {
            JavaFXUtils.copyToClipboard(gsonPretty.toJson(record.toDto()));
            sceneService.showSnackbarSuccess(JavaFXUtils.getStage(it), Pos.BOTTOM_LEFT, i18nService.get("common.copiedToClipboard"));
        });
        buttonExport.setOnAction(actionEvent -> {
            var fileChooser = new RetentionFileChooser();
            fileChooser.setInitialFileName(String.format("%s_%d_%d_message.json", topicName, record.getPartition(), record.getOffset()));
            fileChooser.addExtensionFilter(new FileChooser.ExtensionFilter("json", "*.json"));
            var file = fileChooser.showSaveDialog(JavaFXUtils.getStage(actionEvent));
            if (file == null) return;
            task(() -> Files.writeString(Path.of(file.getAbsolutePath()), gsonPretty.toJson(record.toDto())))
                    .onSuccess(it -> sceneService.showSnackbarSuccess(JavaFXUtils.getStage(actionEvent), Pos.BOTTOM_LEFT, i18nService.get("common.exported")))
                    .onError(it -> sceneService.showSnackbarError(JavaFXUtils.getStage(actionEvent), Pos.BOTTOM_LEFT, i18nService.get("common.error"), it))
                    .start();
        });
    }

    private Node createCardHeader(String title, String content) {
        return createCard(title, "title-regular", content);
    }

    private Node createCardMetadata(String title, String content) {
        return createCard(title, "title", content);
    }

    private Node createCard(String title, String titleStyle, String content) {
        var labelTitle = new Label(title);
        labelTitle.getStyleClass().add(titleStyle);

        var labelContent = new Label(content);
        labelContent.getStyleClass().add("content");

        var left = new VBox(labelTitle, labelContent);
        var right = new VBox(new ImageView(themeService.getIcon16("copy.png")));
        right.setAlignment(Pos.CENTER_RIGHT);
        right.setVisible(false);

        var borderPane = new BorderPane();
        borderPane.getStyleClass().add("card");
        borderPane.setLeft(left);
        borderPane.setRight(right);
        borderPane.setOnMouseEntered(it -> right.setVisible(true));
        borderPane.setOnMouseExited(it -> right.setVisible(false));
        borderPane.setOnMouseClicked(it -> {
            JavaFXUtils.copyToClipboard(content);
            sceneService.showSnackbarSuccess(JavaFXUtils.getStage(it), Pos.BOTTOM_LEFT, i18nService.get("common.copiedToClipboard"));
        });

        return borderPane;
    }
}
