package com.prafka.desktop.controller.acl;

import com.prafka.core.model.Acl;
import com.prafka.core.service.AclService;
import com.prafka.desktop.controller.AbstractTableController;
import com.prafka.desktop.controller.model.AbstractTableModelView;
import com.prafka.desktop.util.FormatUtils;
import com.prafka.desktop.util.JavaFXUtils;
import jakarta.inject.Inject;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.apache.commons.lang3.Strings;
import org.apache.kafka.common.resource.ResourceType;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Controller for displaying ACLs in a table within a topic or consumer group detail tab.
 *
 * <p>Shows ACLs filtered by a specific resource type and pattern, displaying principal,
 * resource, permission, operation, and host. Used as an embedded tab controller to show
 * ACLs related to a specific Kafka resource.
 */
public class TabAclController extends AbstractTableController<List<Acl>, String, TabAclController.AclModelView> {

    private final AclService aclService;
    private ResourceType resource;
    private String pattern;

    @Inject
    public TabAclController(AclService aclService) {
        this.aclService = aclService;
    }

    public void setData(ResourceType resource, String pattern) {
        this.resource = resource;
        this.pattern = pattern;
    }

    @Override
    protected void initTable() {
        var columnPrincipal = JavaFXUtils.<AclModelView, Label>tableColumn(i18nService.get("common.principal"));
        columnPrincipal.setCellValueFactory(it -> it.getValue().principalProperty());
        columnPrincipal.setComparator(JavaFXUtils.LABEL_COMPARATOR);
        columnPrincipal.setMinWidth(200);

        var columnResource = JavaFXUtils.<AclModelView, Node>tableColumn(i18nService.get("common.resource"));
        columnResource.setCellValueFactory(it -> it.getValue().resourceProperty());
        columnResource.setSortable(false);
        columnResource.setMinWidth(200);

        var columnPermission = JavaFXUtils.<AclModelView, Label>tableColumn(i18nService.get("common.permission"));
        columnPermission.setCellValueFactory(it -> it.getValue().permissionProperty());
        columnPermission.setComparator(JavaFXUtils.LABEL_COMPARATOR);
        columnPermission.setPrefWidth(130);

        var columnOperation = JavaFXUtils.<AclModelView, Label>tableColumn(i18nService.get("common.operation"));
        columnOperation.setCellValueFactory(it -> it.getValue().operationProperty());
        columnOperation.setComparator(JavaFXUtils.LABEL_COMPARATOR);
        columnOperation.setPrefWidth(180);

        var columnHost = JavaFXUtils.<AclModelView, Label>tableColumn(i18nService.get("common.host"));
        columnHost.setCellValueFactory(it -> it.getValue().hostProperty());
        columnHost.setComparator(JavaFXUtils.LABEL_COMPARATOR);
        columnHost.setMinWidth(150);

        var remainTableWidth = JavaFXUtils.getRemainTableWidth(tableView, columnPermission, columnOperation).multiply(0.9);
        columnPrincipal.prefWidthProperty().bind(remainTableWidth.multiply(0.4));
        columnResource.prefWidthProperty().bind(remainTableWidth.multiply(0.4));
        columnHost.prefWidthProperty().bind(remainTableWidth.multiply(0.2));

        //noinspection unchecked
        tableView.getColumns().addAll(columnPrincipal, columnResource, columnPermission, columnOperation, columnHost);
    }

    @Override
    protected CompletionStage<List<Acl>> getLoadTableDataFuture() {
        return aclService.getAllByResource(clusterId(), resource, pattern);
    }

    @Override
    protected List<Map.Entry<String, AclModelView>> mapLoadTableDataSource(List<Acl> aclList) {
        return aclList.stream()
                .sorted(Comparator.comparing(Acl::getPrincipal))
                .map(it -> Map.entry(UUID.randomUUID().toString(), new AclModelView(it)))
                .toList();
    }

    @Override
    protected boolean getFilterTableDataPredicate(AclModelView model) {
        var search = textFieldSearch.getText();
        if (isBlank(search)) return true;
        var acl = model.getSource();
        if (Strings.CI.contains(acl.getPrincipal(), search)
                || Strings.CI.contains(acl.getPatternValue(), search)
                || Strings.CI.contains(acl.getHost(), search)
                || Strings.CI.contains(acl.getPermission().name(), search)
                || Strings.CI.contains(acl.getOperation().name(), search)) {
            return true;
        }
        return false;
    }

    public class AclModelView extends AbstractTableModelView {

        private final Acl source;
        private final SimpleObjectProperty<Label> principal = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Label> host = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Node> resource = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Label> permission = new SimpleObjectProperty<>();
        private final SimpleObjectProperty<Label> operation = new SimpleObjectProperty<>();

        public AclModelView(Acl source) {
            this.source = source;
            principal.set(FormatUtils.prettyAclPrincipal(source.getPrincipal()));
            host.set("*".equals(source.getHost()) ? JavaFXUtils.label(source.getHost()) : JavaFXUtils.labelWithTooltip(source.getHost()));
            resource.set(new HBox(
                    JavaFXUtils.label(source.getPatternValue(), "pd-r-0_4"),
                    JavaFXUtils.label(source.getPatternType().name(), "badge", "badge-gray")
            ));
            permission.set(JavaFXUtils.label(source.getPermission().name(), "badge", "badge-green"));
            operation.set(JavaFXUtils.label(source.getOperation().name(), "badge", "badge-blue"));
        }

        public Acl getSource() {
            return source;
        }

        public String getPrincipal() {
            return source.getPrincipal();
        }

        public SimpleObjectProperty<Label> principalProperty() {
            return principal;
        }

        public SimpleObjectProperty<Label> hostProperty() {
            return host;
        }

        public SimpleObjectProperty<Node> resourceProperty() {
            return resource;
        }

        public SimpleObjectProperty<Label> permissionProperty() {
            return permission;
        }

        public SimpleObjectProperty<Label> operationProperty() {
            return operation;
        }
    }
}
