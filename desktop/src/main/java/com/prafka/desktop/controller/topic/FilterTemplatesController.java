package com.prafka.desktop.controller.topic;

import com.prafka.core.model.ConsumeFilter;
import com.prafka.desktop.controller.AbstractController;
import com.prafka.desktop.model.TopicFilterTemplateModel;
import com.prafka.desktop.service.TopicFilterTemplateService;
import com.prafka.desktop.util.JavaFXUtils;
import com.prafka.desktop.util.control.RetentionFileChooser;
import jakarta.inject.Inject;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Consumer;

import static com.prafka.desktop.concurrent.ServiceAdapter.task;
import static com.prafka.desktop.util.JavaFXUtils.label;

public class FilterTemplatesController extends AbstractController {

    public Pane paneContent;
    public Button buttonImport;
    public Button buttonExport;
    public Button buttonOk;

    private final TopicFilterTemplateService topicFilterTemplateService;
    private String topicName;
    private ConsumeFilter currentConsumeFilter;
    private Consumer<ConsumeFilter> onApply;

    @Inject
    public FilterTemplatesController(TopicFilterTemplateService topicFilterTemplateService) {
        this.topicFilterTemplateService = topicFilterTemplateService;
    }

    public void setData(String topicName, ConsumeFilter currentConsumeFilter, Consumer<ConsumeFilter> onApply) {
        this.topicName = topicName;
        this.currentConsumeFilter = currentConsumeFilter;
        this.onApply = onApply;
    }

    @Override
    public void initFxml() {
        super.initFxml();

        buttonImport.setOnAction(actionEvent -> {
            var fileChooser = new RetentionFileChooser();
            fileChooser.addExtensionFilter(new FileChooser.ExtensionFilter("json", "*.json"));
            var file = fileChooser.showOpenDialog(JavaFXUtils.getStage(actionEvent));
            if (file == null) return;
            task(() -> topicFilterTemplateService.importTemplates(topicName, Files.readString(Path.of(file.getAbsolutePath()))))
                    .onSuccess(it -> {
                        sceneService.showSnackbarSuccess(JavaFXUtils.getStage(actionEvent), Pos.BOTTOM_LEFT, i18nService.get("common.imported"));
                        loadTemplateList();
                    })
                    .onError(it -> sceneService.showSnackbarError(JavaFXUtils.getStage(actionEvent), Pos.BOTTOM_LEFT, i18nService.get("common.error"), it))
                    .start();
        });

        buttonExport.setOnAction(actionEvent -> {
            var fileChooser = new RetentionFileChooser();
            fileChooser.setInitialFileName(topicName + "_filters.json");
            fileChooser.addExtensionFilter(new FileChooser.ExtensionFilter("json", "*.json"));
            var file = fileChooser.showSaveDialog(JavaFXUtils.getStage(actionEvent));
            if (file == null) return;
            task(() -> Files.writeString(Path.of(file.getAbsolutePath()), topicFilterTemplateService.exportTemplates(topicName)))
                    .onSuccess(it -> sceneService.showSnackbarSuccess(JavaFXUtils.getStage(actionEvent), Pos.BOTTOM_LEFT, i18nService.get("common.exported")))
                    .onError(it -> sceneService.showSnackbarError(JavaFXUtils.getStage(actionEvent), Pos.BOTTOM_LEFT, i18nService.get("common.error"), it))
                    .start();
        });

        buttonOk.setOnAction(it -> JavaFXUtils.getStage(it).close());
    }

    @Override
    public void initData() {
        loadTemplateList();
    }

    private void loadTemplateList() {
        paneContent.getChildren().clear();
        task(() -> topicFilterTemplateService.getAll(topicName))
                .onSuccess(templateList -> {
                    var sortedTemplateList = templateList.stream().sorted(Comparator.comparing(TopicFilterTemplateModel::getCreatedAt).reversed()).toList();
                    var currentTemplateIndex = -1;
                    for (int i = 0; i < sortedTemplateList.size(); i++) {
                        if (currentConsumeFilter.equals(sortedTemplateList.get(i).getFilter())) {
                            currentTemplateIndex = i;
                            break;
                        }
                    }
                    if (currentTemplateIndex == -1) {
                        addNewTemplate();
                    }
                    for (int i = 0; i < sortedTemplateList.size(); i++) {
                        addExistingTemplate(sortedTemplateList.get(i), currentTemplateIndex == i);
                    }
                })
                .start();
    }

    private void addNewTemplate() {
        var pane = new BorderPane();
        pane.getStyleClass().add("info-block");

        var left = new VBox();
        left.setAlignment(Pos.CENTER_LEFT);
        var textField = new TextField(i18nService.get("filterTemplatesView.newTemplate"));
        textField.setFocusTraversable(false);
        left.getChildren().add(textField);
        pane.setCenter(left);
        BorderPane.setMargin(left, new Insets(0, 20, 0, 0));

        var right = new HBox();
        right.setAlignment(Pos.CENTER_RIGHT);
        var save = new Button(i18nService.get("common.save"));
        save.getStyleClass().add("secondary-outline");
        save.setOnAction(actionEvent -> {
            if (StringUtils.isBlank(textField.getText())) return;
            var template = new TopicFilterTemplateModel();
            template.setName(textField.getText());
            template.setFilter(currentConsumeFilter);
            task(() -> topicFilterTemplateService.save(topicName, template))
                    .onSuccess(it -> loadTemplateList())
                    .start();
        });
        textField.addEventHandler(KeyEvent.KEY_PRESSED, JavaFXUtils.onKeyEnter(save::fire));
        right.getChildren().add(save);
        pane.setRight(right);

        paneContent.getChildren().add(pane);
    }

    private void addExistingTemplate(TopicFilterTemplateModel template, boolean current) {
        var pane = new BorderPane();
        pane.getStyleClass().add("info-block");

        var left = new VBox();
        left.setAlignment(Pos.CENTER_LEFT);
        left.getChildren().add(JavaFXUtils.labelWithTooltip(template.getName(), "font-medium"));
        if (current) {
            left.getChildren().add(label(i18nService.get("common.current"), "label-desc"));
        }
        pane.setCenter(left);
        BorderPane.setMargin(left, new Insets(0, 20, 0, 0));

        var right = new HBox(10);
        right.setAlignment(Pos.CENTER_RIGHT);
        var apply = new Button(i18nService.get("common.apply"));
        apply.getStyleClass().add("secondary-outline");
        apply.setOnAction(it -> {
            onApply.accept(template.getFilter());
            JavaFXUtils.getStage(it).close();
        });
        var byDefault = new Button(i18nService.get("common.default"));
        byDefault.getStyleClass().add(template.isByDefault() ? "primary" : "secondary-outline");
        byDefault.setOnAction(actionEvent -> {
            task(() -> topicFilterTemplateService.saveDefault(topicName, template.isByDefault() ? Optional.empty() : Optional.of(template)))
                    .onSuccess(it -> loadTemplateList())
                    .start();
        });
        var delete = new Button(i18nService.get("common.delete"));
        delete.getStyleClass().add("secondary-outline");
        delete.setOnAction(actionEvent -> {
            task(() -> topicFilterTemplateService.delete(topicName, template))
                    .onSuccess(it -> loadTemplateList())
                    .start();
        });
        right.getChildren().addAll(apply, byDefault, delete);
        pane.setRight(right);

        paneContent.getChildren().add(pane);
    }
}
