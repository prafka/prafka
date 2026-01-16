package com.prafka.desktop.util;

import com.prafka.desktop.concurrent.FutureServiceAdapter;
import com.prafka.desktop.concurrent.ScheduledServiceAdapter;
import com.prafka.desktop.service.ThemeService;
import com.prafka.desktop.util.control.CheckComboBoxSkin;
import com.prafka.desktop.util.control.NumberLabel;
import com.prafka.desktop.util.control.TableViewSkinAutoHeight;
import impl.org.controlsfx.skin.ExpandableTableRowSkin;
import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.skin.TableColumnHeader;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.PopupWindow;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import javafx.util.StringConverter;
import javafx.util.converter.LongStringConverter;
import org.controlsfx.control.CheckComboBox;
import org.controlsfx.control.table.TableRowExpanderColumn;
import org.fxmisc.richtext.CodeArea;

import java.util.Comparator;
import java.util.EventObject;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.upperCase;

public class JavaFXUtils {

    public static final Comparator<Label> LABEL_COMPARATOR = Comparator.comparing(Labeled::getText);

    public static final Comparator<Node> BORDER_PANE_LEFT_LABEL_COMPARATOR =
            Comparator.comparing(it -> {
                if (it instanceof BorderPane pane) {
                    return ((Labeled) pane.getLeft()).getText();
                } else {
                    return "";
                }
            });

    public static Stage getStage(EventObject eventObject) {
        if (eventObject.getSource() instanceof Node nodeSource) {
            return getStage(nodeSource);
        }
        if (eventObject.getSource() instanceof MenuItem menuItemSource) {
            return getStage(menuItemSource.getParentPopup().getOwnerNode());
        }
        throw new IllegalArgumentException();
    }

    public static Stage getStage(Node node) {
        var window = node.getScene().getWindow();
        if (window instanceof Stage stage) {
            return stage;
        }
        if (window instanceof PopupWindow popupWindow) {
            return (Stage) popupWindow.getOwnerWindow();
        }
        throw new IllegalArgumentException();
    }

    public static <T> Label label(T value, String... styleClass) {
        var label = new Label(Objects.toString(value, null));
        label.getStyleClass().addAll(styleClass);
        return label;
    }

    public static <T> Label labelWithTooltip(T value, String... styleClass) {
        var label = label(value, styleClass);
        if (value != null && isNotBlank(value.toString()) && !"null".equals(value)) {
            label.setTooltip(tooltip(value.toString()));
        }
        return label;
    }

    public static <T> Label labelWithTooltip(T value, Tooltip tooltip, String... styleClass) {
        var label = label(value, styleClass);
        if (isNotBlank(tooltip.getText())) {
            label.setTooltip(tooltip);
        }
        return label;
    }

    public static Label labelLoader(Image image) {
        return new Label("", new ImageView(image));
    }

    public static NumberLabel numberLabel(Number value, String... styleClass) {
        var label = new NumberLabel(value);
        label.getStyleClass().addAll(styleClass);
        return label;
    }

    public static NumberLabel numberLabelText(Number value, String text, String... styleClass) {
        var label = new NumberLabel(value, text);
        label.getStyleClass().addAll(styleClass);
        return label;
    }

    public static NumberLabel numberLabelLoader(Image image) {
        return new NumberLabel(new ImageView(image));
    }

    public static Tooltip tooltip(String value) {
        return tooltip(value, 100);
    }

    public static Tooltip tooltip(String value, long showDelay) {
        var tooltip = new Tooltip(value);
        tooltip.setShowDelay(Duration.millis(showDelay));
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(500);
        return tooltip;
    }

    public static <S, T> TableColumn<S, T> tableColumn() {
        return tableColumn(null);
    }

    public static <S, T> TableColumn<S, T> tableColumn(String text) {
        var column = new TableColumn<S, T>(upperCase(text));
        column.setReorderable(false);
        column.setResizable(false);
        return column;
    }

    public static <S, T> TreeTableColumn<S, T> treeTableColumn() {
        return treeTableColumn(null);
    }

    public static <S, T> TreeTableColumn<S, T> treeTableColumn(String text) {
        var column = new TreeTableColumn<S, T>(upperCase(text));
        column.setReorderable(false);
        column.setResizable(false);
        return column;
    }

    public static CheckBox cellCheckBock() {
        var checkBox = new CheckBox();
        checkBox.setFocusTraversable(false);
        checkBox.getStyleClass().add("cell-check-box");
        return checkBox;
    }

    public static void addTableColumnHeaderTooltip(TableColumn<?, ?> column, String tooltip) {
        Platform.runLater(() -> {
            var header = (TableColumnHeader) column.getTableView().lookup("#" + column.getId()); // todo make better
            var label = (Label) header.lookup(".label");
            label.setTooltip(tooltip(tooltip));
        });
    }

    @SafeVarargs
    public static void setLabelNA(SimpleObjectProperty<Label>... property) {
        for (SimpleObjectProperty<Label> it : property) it.set(label(FormatUtils.NA));
    }

    @SafeVarargs
    public static void setNumberLabelNA(SimpleObjectProperty<NumberLabel>... property) {
        for (SimpleObjectProperty<NumberLabel> it : property) it.set(new NumberLabel(0, FormatUtils.NA));
    }

    public static void setPaneNA(Pane... pane) {
        for (Pane it : pane) setLabel(label(FormatUtils.NA), it);
    }

    @SafeVarargs
    public static void setLabelLoader(Image loader, SimpleObjectProperty<Label>... property) {
        for (SimpleObjectProperty<Label> it : property) it.set(labelLoader(loader));
    }

    @SafeVarargs
    public static void setNumberLabelLoader(Image loader, SimpleObjectProperty<NumberLabel>... property) {
        for (SimpleObjectProperty<NumberLabel> it : property) it.set(numberLabelLoader(loader));
    }

    public static void setPaneLoader(Image loader, Pane... pane) {
        for (Pane it : pane) it.getChildren().setAll(labelLoader(loader));
    }

    public static void setLabel(Label label, Pane pane) {
        pane.getChildren().setAll(label);
    }

    public static DoubleBinding getRemainTableWidth(TableView<?> table, TableColumn<?, ?>... columns) {
        var width = table.widthProperty().subtract(0);
        for (TableColumn<?, ?> column : columns) {
            width = width.subtract(column.widthProperty());
        }
        return width;
    }

    public static DoubleBinding getRemainTreeTableWidth(TreeTableView<?> table, TreeTableColumn<?, ?>... columns) {
        var width = table.widthProperty().subtract(0);
        for (TreeTableColumn<?, ?> column : columns) {
            width = width.subtract(column.widthProperty());
        }
        return width;
    }

    public static void copyToClipboard(String data) {
        var content = new ClipboardContent();
        content.putString(data);
        Clipboard.getSystemClipboard().setContent(content);
    }

    public static TextFormatter<Long> positiveLongTextFormatter(Long def) {
        return new TextFormatter<>(new LongStringConverter(), def, change -> {
            if ((change.isAdded() || change.isReplaced()) && !change.getControlNewText().matches("\\d+")) {
                change.setText("");
                change.setRange(change.getRangeStart(), change.getRangeStart());
            }
            return change;
        });
    }

    public static ChangeListener<Toggle> buttonToggleGroupListener() {
        return (observable, oldValue, newValue) -> {
            if (newValue == null && oldValue != null) oldValue.setSelected(true);
        };
    }

    public static <T> CheckComboBoxSkin<T> checkComboBoxSkin(CheckComboBox<T> checkComboBox) {
        return new CheckComboBoxSkin<>(checkComboBox);
    }

    public static void setTableViewAutoHeight(TableView<?> tableView, int maxVisibleRows) {
        tableView.setSkin(new TableViewSkinAutoHeight<>(tableView, maxVisibleRows));
    }

    public static void disableTableViewFocus(TableView<?> table) {
        table.setFocusTraversable(false);
        table.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (isTrue(newValue) && table.getParent() != null) {
                table.getParent().requestFocus();
            }
        });
    }

    public static void disableTreeTableViewFocus(TreeTableView<?> table) {
        table.setFocusTraversable(false);
        table.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (isTrue(newValue) && table.getParent() != null) {
                table.getParent().requestFocus();
            }
        });
    }

    public static <E extends Enum<E>> StringConverter<E> prettyEnumStringConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(E value) {
                return value == null ? null : FormatUtils.prettyEnum(value);
            }

            @Override
            public E fromString(String string) {
                return null;
            }
        };
    }

    public static <S> Callback<TableView<S>, TableRow<S>> clickRowFactory(Consumer<S> onClick) {
        return new Callback<>() {
            @Override
            public TableRow<S> call(TableView<S> param) {
                var row = new TableRow<S>();
                row.setOnMouseClicked(event -> {
                    if (event.getButton() == MouseButton.PRIMARY && !row.isEmpty()) {
                        onClick.accept(row.getItem());
                    }
                });
                return row;
            }
        };
    }

    public static <S> Callback<TableView<S>, TableRow<S>> toggleRowFactory(TableRowExpanderColumn<S> expander) {
        return new Callback<>() {
            @Override
            public TableRow<S> call(TableView<S> param) {
                var row = new TableRow<S>() {
                    @Override
                    protected Skin<?> createDefaultSkin() {
                        return new ExpandableTableRowSkin<>(this, expander);
                    }
                };
                row.setOnMouseClicked(event -> {
                    if (event.getButton() == MouseButton.PRIMARY && !row.isEmpty()) {
                        expander.toggleExpanded(row.getIndex());
                    }
                });
                return row;
            }
        };
    }

    public static <S> Callback<TableColumn<S, Boolean>, TableCell<S, Boolean>> toggleCellFactory(TableRowExpanderColumn<S> expander, ThemeService themeService) {
        return new Callback<>() {
            @Override
            public TableCell<S, Boolean> call(TableColumn<S, Boolean> param) {
                return new TableCell<>() {
                    private final Button button = new Button();

                    {
                        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                        button.setOnAction(event -> expander.toggleExpanded(getIndex()));
                        button.setStyle("-fx-padding: 0; -fx-background-color: transparent");
                        themeService.setIcon20(button, "chevron_forward.png");
                    }

                    @Override
                    protected void updateItem(Boolean item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            setGraphic(null);
                        } else {
                            if (item) {
                                themeService.setIcon20(button, "chevron_down.png");
                            } else {
                                themeService.setIcon20(button, "chevron_forward.png");
                            }
                            setGraphic(button);
                        }
                    }
                };
            }
        };
    }

    public static <T> ListCell<T> comboBoxWithPromptText(ComboBox<T> comboBox) {
        return new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(comboBox.getPromptText());
                } else if (item instanceof Node newNode) {
                    var currentNode = getGraphic();
                    if (currentNode == null || !currentNode.equals(newNode)) {
                        setText(null);
                        setGraphic(newNode);
                    }
                } else {
                    var converter = comboBox.getConverter();
                    var promptText = comboBox.getPromptText();
                    var text = item == null && promptText != null
                            ? promptText
                            : converter == null
                            ? item == null ? null : item.toString()
                            : converter.toString(item);
                    setText(text);
                    setGraphic(null);
                }
            }
        };
    }

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("^\\s+");

    public static void setCodeAreaAutoIntend(CodeArea codeArea) {
        codeArea.addEventHandler(KeyEvent.KEY_PRESSED, onKeyEnter(() -> {
            int caretPosition = codeArea.getCaretPosition();
            int currentParagraph = codeArea.getCurrentParagraph();
            var matcher = WHITESPACE_PATTERN.matcher(codeArea.getParagraph(currentParagraph - 1).getSegments().get(0));
            if (matcher.find()) Platform.runLater(() -> codeArea.insertText(caretPosition, matcher.group()));
        }));
    }

    public static void requestFocus(Node node) {
        ScheduledServiceAdapter.scheduleTask(() -> {
            if (!node.isFocused()) Platform.runLater(node::requestFocus);
        }).start(Duration.millis(50), 10);
    }

    public static EventHandler<KeyEvent> onKeyEsc(Runnable runnable) {
        return event -> {
            if (KeyCode.ESCAPE == event.getCode()) {
                runnable.run();
            }
        };
    }

    public static EventHandler<KeyEvent> onKeyEnter(Runnable runnable) {
        return event -> {
            if (KeyCode.ENTER == event.getCode()) {
                runnable.run();
            }
        };
    }

    public static void clearTasks(List<FutureServiceAdapter<?>> tasks) {
        tasks.forEach(FutureServiceAdapter::cancel);
        tasks.clear();
    }
}
