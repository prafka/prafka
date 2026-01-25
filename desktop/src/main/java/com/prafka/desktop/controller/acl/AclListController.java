package com.prafka.desktop.controller.acl;

import com.prafka.core.model.Acl;
import com.prafka.core.service.AclService;
import com.prafka.desktop.controller.AbstractTableController;
import com.prafka.desktop.controller.model.AbstractTableModelView;
import com.prafka.desktop.util.FormatUtils;
import com.prafka.desktop.util.JavaFXUtils;
import jakarta.inject.Inject;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import org.apache.commons.lang3.Strings;
import org.apache.kafka.common.acl.AccessControlEntryFilter;
import org.apache.kafka.common.acl.AclBindingFilter;
import org.apache.kafka.common.resource.ResourcePatternFilter;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

import static com.prafka.desktop.concurrent.FutureServiceAdapter.futureTask;
import static com.prafka.desktop.util.JavaFXUtils.getStage;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Controller for the ACL list view displaying all cluster ACLs.
 *
 * <p>Shows a comprehensive table of ACLs including principal, resource, permission,
 * operation, and host. Provides filtering, summary cards, and actions to create or
 * delete ACLs. Main entry point for ACL management in the cluster.
 */
public class AclListController extends AbstractTableController<List<Acl>, String, AclListController.AclModelView> {

    public Button buttonCreateAcl;
    public Pane paneSummaryBlock;

    private final AclSummaryLoader aclSummaryLoader;
    private final AclService aclService;

    @Inject
    public AclListController(AclSummaryLoader aclSummaryLoader, AclService aclService) {
        this.aclSummaryLoader = aclSummaryLoader;
        this.aclService = aclService;
    }

    @Override
    public void initFxml() {
        super.initFxml();
        buttonCreateAcl.setOnAction(it -> {
            var stage = JavaFXUtils.getStage(it);
            viewManager.showCreateAclView(stage, () -> {
                sceneService.showSnackbarSuccess(stage, Pos.BOTTOM_RIGHT, i18nService.get("acl.aclCreated"));
                loadData();
            });
        });
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
        columnOperation.setComparator(JavaFXUtils.LABEL_COMPARATOR);
        columnHost.setMinWidth(150);

        var columnActions = JavaFXUtils.<AclModelView, Node>tableColumn();
        columnActions.setCellValueFactory(it -> it.getValue().actionsProperty());
        columnActions.setSortable(false);
        columnActions.setPrefWidth(60);

        var remainTableWidth = JavaFXUtils.getRemainTableWidth(tableView, columnPermission, columnOperation, columnActions).multiply(0.9);
        columnPrincipal.prefWidthProperty().bind(remainTableWidth.multiply(0.4));
        columnResource.prefWidthProperty().bind(remainTableWidth.multiply(0.4));
        columnHost.prefWidthProperty().bind(remainTableWidth.multiply(0.2));

        //noinspection unchecked
        tableView.getColumns().addAll(columnPrincipal, columnResource, columnPermission, columnOperation, columnHost, columnActions);
    }

    @Override
    protected void loadData() {
        super.loadData();
        paneSummaryBlock.getChildren().setAll(aclSummaryLoader.load(() -> getStage(paneRoot), () -> futureTasks));
    }

    @Override
    protected CompletionStage<List<Acl>> getLoadTableDataFuture() {
        return aclService.getAll(clusterId());
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
                    JavaFXUtils.label(source.getResource().name(), "badge", "badge-violet"),
                    JavaFXUtils.label(source.getPatternValue(), "pd-rl-0_4"),
                    JavaFXUtils.label(source.getPatternType().name(), "badge", "badge-gray")
            ));
            permission.set(JavaFXUtils.label(source.getPermission().name(), "badge", "badge-green"));
            operation.set(JavaFXUtils.label(source.getOperation().name(), "badge", "badge-blue"));
            setActions();
        }

        @Override
        protected void setActions() {
            var menuItemDeleteAcl = new MenuItem(i18nService.get("acl.deleteAcl"));
            menuItemDeleteAcl.setOnAction(sourceActionEvent ->
                    viewManager.showDeleteAclConfirmView(JavaFXUtils.getStage(sourceActionEvent), confirmCallback ->
                            futureTask(() -> aclService.delete(clusterId(), new AclBindingFilter(
                                    new ResourcePatternFilter(source.getResource(), source.getPatternValue(), source.getPatternType()),
                                    new AccessControlEntryFilter(source.getPrincipal(), source.getHost(), source.getOperation(), source.getPermission())
                            )))
                                    .onSuccess(it -> {
                                        confirmCallback.onSuccess();
                                        sceneService.showSnackbarSuccess(JavaFXUtils.getStage(sourceActionEvent), Pos.BOTTOM_RIGHT, i18nService.get("acl.aclDeleted"));
                                        loadData();
                                    })
                                    .onError(confirmCallback::onError)
                                    .start()
                    )
            );
            actions.set(sceneService.createCellActionsMenuButton(menuItemDeleteAcl));
        }

        public Acl getSource() {
            return source;
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
