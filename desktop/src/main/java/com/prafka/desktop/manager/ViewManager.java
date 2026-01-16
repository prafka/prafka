package com.prafka.desktop.manager;

import com.google.inject.Provider;
import com.prafka.core.model.*;
import com.prafka.core.model.Record;
import com.prafka.desktop.ApplicationProperties;
import com.prafka.desktop.controller.*;
import com.prafka.desktop.controller.acl.*;
import com.prafka.desktop.controller.broker.BrokerController;
import com.prafka.desktop.controller.broker.BrokerTabConfigurationController;
import com.prafka.desktop.controller.broker.BrokerTabLogDirsController;
import com.prafka.desktop.controller.broker.EditBrokerConfigController;
import com.prafka.desktop.controller.cluster.AddEditClusterController;
import com.prafka.desktop.controller.cluster.AddEditClusterTabKafkaClusterController;
import com.prafka.desktop.controller.cluster.AddEditClusterTabKafkaConnectController;
import com.prafka.desktop.controller.cluster.AddEditClusterTabSchemaRegistryController;
import com.prafka.desktop.controller.connect.*;
import com.prafka.desktop.controller.consumer.group.*;
import com.prafka.desktop.controller.quota.CreateQuotaController;
import com.prafka.desktop.controller.quota.EditQuotaController;
import com.prafka.desktop.controller.schema.registry.*;
import com.prafka.desktop.controller.topic.*;
import com.prafka.desktop.model.ClusterModel;
import com.prafka.desktop.service.I18nService;
import com.prafka.desktop.service.SettingsService;
import com.prafka.desktop.service.ThemeService;
import com.prafka.desktop.util.JavaFXUtils;
import io.confluent.kafka.schemaregistry.CompatibilityLevel;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.common.resource.ResourceType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Singleton
public class ViewManager {

    private static final Map<String, View<?>> CACHE = new ConcurrentHashMap<>();

    private final ApplicationProperties applicationProperties;
    private final Provider<FXMLLoader> fxmlLoaderProvider;
    private final SettingsService settingsService;
    private final ThemeService themeService;
    private final I18nService i18nService;

    @Inject
    public ViewManager(ApplicationProperties applicationProperties, Provider<FXMLLoader> fxmlLoaderProvider, SettingsService settingsService, ThemeService themeService, I18nService i18nService) {
        this.applicationProperties = applicationProperties;
        this.fxmlLoaderProvider = fxmlLoaderProvider;
        this.settingsService = settingsService;
        this.themeService = themeService;
        this.i18nService = i18nService;
    }

    public Stage showDashboardView() {
        var view = this.<DashboardController>getView("/view/DashboardView.fxml", null, false, false);
        var scene = createScene(view);
        var size = getInitialWidthHeight();
        var stage = createStage(null, scene, titleWithAppName(i18nService.get("common.topics")), size.getLeft(), size.getRight(), false);
        bindStageSize(stage, "dashboardView", size.getLeft(), size.getRight());
        view.controller().setStage(stage);
        stage.setOnHidden(it -> view.controller().close());
        stage.show();
        return stage;
    }

    /* ONBOARDING *****************************************************************************************************/

    public void showOnboardingSettingsView(Stage primaryStage) {
        var view = getView("/view/onboarding/OnboardingSettingsView.fxml", null, false, false);
        var scene = createScene(view);
        fillStage(primaryStage, scene, titleWithAppName(i18nService.get("onboardingSettingsView.stageTitle")), 600, 400, true);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public void showOnboardingCreateMasterPasswordView(Stage primaryStage) {
        var view = getView("/view/onboarding/OnboardingCreateMasterPasswordView.fxml", null, false, false);
        var scene = createScene(view);
        fillStage(primaryStage, scene, titleWithAppName(i18nService.get("onboardingCreateMasterPasswordView.stageTitle")), 600, 400, true);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    /* MASTER-PASSWORD ************************************************************************************************/

    public void showEnterMasterPasswordView(Stage primaryStage) {
        var view = getView("/view/master/password/EnterMasterPasswordView.fxml", null, false, false);
        var scene = createScene(view);
        fillStage(primaryStage, scene, titleWithAppName(i18nService.get("masterPassword.enterMasterPassword")), 600, 400, true);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public void showChangeMasterPasswordView(Stage primaryStage) {
        var view = getView("/view/master/password/ChangeMasterPasswordView.fxml", null, false, false);
        var scene = createScene(view);
        fillStage(primaryStage, scene, titleWithAppName(i18nService.get("masterPassword.changeMasterPassword")), 600, 400, true);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public void showResetMasterPasswordView(Stage primaryStage) {
        var view = getView("/view/master/password/ResetMasterPasswordView.fxml", null, false, false);
        var scene = createScene(view);
        fillStage(primaryStage, scene, titleWithAppName(i18nService.get("masterPassword.resetMasterPassword")), 600, 400, true);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    /* CLUSTER ********************************************************************************************************/

    public Node loadClusterOverviewView() {
        return getView("/view/cluster/ClusterOverviewView.fxml", null, true, true).root();
    }

    public Node loadClusterListView() {
        return getView("/view/cluster/ClusterListView.fxml", null, true, true).root();
    }

    public void showAddClusterView(Stage primaryStage, Runnable onSuccess) {
        var view = this.<AddEditClusterController>getView("/view/cluster/AddEditClusterView.fxml", it -> it.setData(onSuccess), false, false);
        var scene = createScene(view);
        var stage = createStage(primaryStage, scene, i18nService.get("addClusterView.stageTitle"), 800, 700, true);
        stage.show();
    }

    public void showEditClusterView(Stage primaryStage, ClusterModel cluster, Runnable onSuccess) {
        showEditClusterView(primaryStage, cluster, 0, onSuccess);
    }

    public void showEditClusterView(Stage primaryStage, ClusterModel cluster, int initialTabIndex, Runnable onSuccess) {
        var view = this.<AddEditClusterController>getView("/view/cluster/AddEditClusterView.fxml", it -> it.setData(cluster, initialTabIndex, onSuccess), false, false);
        var scene = createScene(view);
        var stage = createStage(primaryStage, scene, i18nService.get("editClusterView.stageTitle"), 800, 700, true);
        stage.show();
    }

    public View<AddEditClusterTabKafkaClusterController> loadAddEditClusterTabKafkaClusterView(AddEditClusterController parentController, Optional<ClusterModel> cluster) {
        return getView("/view/cluster/AddEditClusterTabKafkaClusterView.fxml", it -> it.setData(parentController, cluster), false, false);
    }

    public View<AddEditClusterTabSchemaRegistryController> loadAddEditClusterTabSchemaRegistryView(AddEditClusterController parentController, Optional<ClusterModel> cluster) {
        return getView("/view/cluster/AddEditClusterTabSchemaRegistryView.fxml", it -> it.setData(parentController, cluster), false, false);
    }

    public View<AddEditClusterTabKafkaConnectController> loadAddEditClusterTabKafkaConnectView(AddEditClusterController parentController, Optional<ClusterModel> cluster) {
        return getView("/view/cluster/AddEditClusterTabKafkaConnectView.fxml", it -> it.setData(parentController, cluster), false, false);
    }

    public void showDeleteClusterConfirmView(Stage primaryStage, Consumer<ConfirmController.ConfirmCallback> onButton) {
        var title = i18nService.get("deleteClusterConfirmView.title");
        var content = i18nService.get("deleteClusterConfirmView.content");
        var button = i18nService.get("cluster.deleteCluster");
        var view = this.<ConfirmController>getView("/view/ConfirmView.fxml", it -> it.setData(title, content, button, onButton), false, false);
        var scene = createScene(view);
        var stage = createStage(primaryStage, scene, title, 600, 300, true);
        stage.setResizable(false);
        stage.show();
    }

    /* TOPIC **********************************************************************************************************/

    public Node loadTopicListView() {
        return getView("/view/topic/TopicListView.fxml", null, true, true).root();
    }

    public Node loadTopicView(String topicName) {
        return this.<TopicController>getView("/view/topic/TopicView.fxml", it -> it.setTopicName(topicName), true, true).root();
    }

    public Node loadTopicTabConsumeView(String topicName) {
        return this.<TopicTabConsumeController>getView("/view/topic/TopicTabConsumeView.fxml", it -> it.setTopicName(topicName), true, true).root();
    }

    public Node loadTopicTabProducerView(String topicName) {
        return this.<TopicTabProduceController>getView("/view/topic/TopicTabProduceView.fxml", it -> it.setTopicName(topicName), true, true).root();
    }

    public Node loadTopicTabPartitionsView(String topicName) {
        return this.<TopicTabPartitionsController>getView("/view/topic/TopicTabPartitionsView.fxml", it -> it.setTopicName(topicName), true, true).root();
    }

    public Node loadTopicTabConfigurationView(String topicName) {
        return this.<TopicTabConfigurationController>getView("/view/topic/TopicTabConfigurationView.fxml", it -> it.setTopicName(topicName), true, true).root();
    }

    public Node loadTopicTabConsumerGroupView(String topicName) {
        return this.<TopicTabConsumerGroupController>getView("/view/topic/TopicTabConsumerGroupView.fxml", it -> it.setTopicName(topicName), true, true).root();
    }

    public Node loadTopicTabAclView(String topicName) {
        return this.<TabAclController>getView("/view/acl/TabAclView.fxml", it -> it.setData(ResourceType.TOPIC, topicName), true, true).root();
    }

    public void showCreateTopicView(Stage primaryStage, Runnable onSuccess) {
        var view = this.<CreateTopicController>getView("/view/topic/CreateTopicView.fxml", it -> it.setOnSuccess(onSuccess), false, false);
        var scene = createScene(view);
        var stage = createStage(primaryStage, scene, i18nService.get("topic.createTopic"), 800, 800, true);
        stage.show();
    }

    public void showEditTopicConfigView(Stage primaryStage, String topicName, Config config, Runnable onSuccess) {
        var view = this.<EditTopicConfigController>getView("/view/topic/EditTopicConfigView.fxml", it -> it.setData(topicName, config, onSuccess), false, false);
        var scene = createScene(view);
        var stage = createStage(primaryStage, scene, i18nService.get("editTopicConfigView.stageTitle"), 800, 600, true);
        stage.show();
    }

    public void showEmptyTopicConfirmView(Stage primaryStage, String topicName, Consumer<ConfirmController.ConfirmCallback> onButton) {
        var title = i18nService.get("emptyTopicConfirmView.title");
        var content = String.format(i18nService.get("emptyTopicConfirmView.content"), topicName);
        var button = i18nService.get("topic.emptyTopic");
        var view = this.<ConfirmController>getView("/view/ConfirmView.fxml", it -> it.setData(title, content, button, onButton), false, false);
        var scene = createScene(view);
        var stage = createStage(primaryStage, scene, title, 600, 300, true);
        stage.setResizable(false);
        stage.show();
    }

    public void showEmptyTopicsConfirmView(Stage primaryStage, Collection<String> topicNameList, Consumer<ConfirmController.ConfirmCallback> onButton) {
        var title = i18nService.get("emptyTopicsConfirmView.title");
        var content = String.format(i18nService.get("emptyTopicsConfirmView.content"), topicNameList.stream().map(it -> "\"" + it + "\"").collect(Collectors.joining(", ")));
        var button = i18nService.get("topic.emptyTopics");
        var view = this.<ConfirmController>getView("/view/ConfirmView.fxml", it -> it.setData(title, content, button, onButton), false, false);
        var scene = createScene(view);
        var stage = createStage(primaryStage, scene, title, 600, 300, true);
        stage.setResizable(false);
        stage.show();
    }

    public void showDeleteTopicConfirmView(Stage primaryStage, String topicName, Consumer<ConfirmController.ConfirmCallback> onButton) {
        var title = i18nService.get("deleteTopicConfirmView.title");
        var content = String.format(i18nService.get("deleteTopicConfirmView.content"), topicName);
        var button = i18nService.get("topic.deleteTopic");
        var view = this.<ConfirmController>getView("/view/ConfirmView.fxml", it -> it.setData(title, content, button, onButton), false, false);
        var scene = createScene(view);
        var stage = createStage(primaryStage, scene, title, 600, 300, true);
        stage.setResizable(false);
        stage.show();
    }

    public void showDeleteTopicsConfirmView(Stage primaryStage, Collection<String> topicNameList, Consumer<ConfirmController.ConfirmCallback> onButton) {
        var title = i18nService.get("deleteTopicsConfirmView.title");
        var content = String.format(i18nService.get("deleteTopicsConfirmView.content"), topicNameList.stream().map(it -> "\"" + it + "\"").collect(Collectors.joining(", ")));
        var button = i18nService.get("topic.deleteTopics");
        var view = this.<ConfirmController>getView("/view/ConfirmView.fxml", it -> it.setData(title, content, button, onButton), false, false);
        var scene = createScene(view);
        var stage = createStage(primaryStage, scene, title, 600, 300, true);
        stage.setResizable(false);
        stage.show();
    }

    public void showTopicRecordView(Stage primaryStage, String topicName, Record record) {
        var view = this.<TopicRecordController>getView("/view/topic/TopicRecordView.fxml", it -> it.setData(topicName, record), false, false);
        var scene = createScene(view);
        var stage = createStage(primaryStage, scene, i18nService.get("common.record"), 800, 800, true);
        stage.show();
    }

    public void showAddJsFilterView(Stage primaryStage, Consumer<ConsumeFilter.Expression> onSuccess) {
        var view = this.<AddEditJsFilterController>getView("/view/topic/AddEditJsFilterView.fxml", it -> it.setData(onSuccess), false, false);
        var scene = createScene(view);
        var stage = createStage(primaryStage, scene, i18nService.get("addJsFilterView.stageTitle"), 600, 400, true);
        stage.setOnHidden(it -> view.controller().close());
        stage.show();
    }

    public void showEditJsFilterView(Stage primaryStage, ConsumeFilter.Expression jsFilter, Consumer<ConsumeFilter.Expression> onSuccess) {
        var view = this.<AddEditJsFilterController>getView("/view/topic/AddEditJsFilterView.fxml", it -> it.setData(jsFilter, onSuccess), false, false);
        var scene = createScene(view);
        var stage = createStage(primaryStage, scene, i18nService.get("editJsFilterView.stageTitle"), 600, 400, true);
        stage.setOnHidden(it -> view.controller().close());
        stage.show();
    }

    public void showJsFilterDocumentationView(Stage primaryStage) {
        var view = getView("/view/topic/JsFilterDocumentationView.fxml", null, false, false);
        var scene = createScene(view);
        var stage = createStage(primaryStage, scene, i18nService.get("jsFilterDocumentationView.stageTitle"), 600, 600, true);
        stage.show();
    }

    public void showFilterTemplatesView(Stage primaryStage, String topicName, ConsumeFilter currentConsumeFilter, Consumer<ConsumeFilter> onApply) {
        var view = this.<FilterTemplatesController>getView("/view/topic/FilterTemplatesView.fxml", it -> it.setData(topicName, currentConsumeFilter, onApply), false, false);
        var scene = createScene(view);
        var stage = createStage(primaryStage, scene, i18nService.get("filterTemplatesView.stageTitle"), 800, 600, true);
        stage.show();
    }

    /* CONSUMER GROUP *************************************************************************************************/

    public Node loadConsumerGroupListView() {
        return getView("/view/consumer/group/ConsumerGroupListView.fxml", null, true, true).root();
    }

    public Node loadConsumerGroupView(ConsumerGroup.GroupIdState groupIdState) {
        return this.<ConsumerGroupController>getView("/view/consumer/group/ConsumerGroupView.fxml", it -> it.setGroupIdState(groupIdState), true, true).root();
    }

    public Node loadConsumerGroupTabMembersView(String groupId) {
        return this.<ConsumerGroupTabMembersController>getView("/view/consumer/group/ConsumerGroupTabMembersView.fxml", it -> it.setGroupId(groupId), true, true).root();
    }

    public Node loadConsumerGroupTabTopicsView(String groupId) {
        return this.<ConsumerGroupTabTopicsController>getView("/view/consumer/group/ConsumerGroupTabTopicsView.fxml", it -> it.setGroupId(groupId), true, true).root();
    }

    public Node loadConsumerGroupTabAclView(String groupId) {
        return this.<TabAclController>getView("/view/acl/TabAclView.fxml", it -> it.setData(ResourceType.GROUP, groupId), true, true).root();
    }

    public void showCreateConsumerGroupView(Stage primaryStage, Runnable onSuccess) {
        var view = this.<CreateConsumerGroupController>getView("/view/consumer/group/CreateConsumerGroupView.fxml", it -> it.setOnSuccess(onSuccess), false, false);
        var scene = createScene(view);
        var stage = createStage(primaryStage, scene, i18nService.get("createConsumerGroupView.stageTitle"), 800, 600, true);
        stage.show();
    }

    public void showEditConsumerGroupView(Stage primaryStage, String groupId, Runnable onSuccess) {
        var view = this.<EditConsumerGroupController>getView("/view/consumer/group/EditConsumerGroupView.fxml", it -> it.setData(groupId, onSuccess), false, false);
        var scene = createScene(view);
        var stage = createStage(primaryStage, scene, i18nService.get("editConsumerGroupView.stageTitle"), 900, 700, true);
        stage.show();
    }

    public void showDeleteConsumerGroupConfirmView(Stage primaryStage, Consumer<ConfirmController.ConfirmCallback> onButton) {
        var title = i18nService.get("deleteConsumerGroupConfirmView.title");
        var content = i18nService.get("deleteConsumerGroupConfirmView.content");
        var button = i18nService.get("deleteConsumerGroupConfirmView.button");
        var view = this.<ConfirmController>getView("/view/ConfirmView.fxml", it -> it.setData(title, content, button, onButton), false, false);
        var scene = createScene(view);
        var stage = createStage(primaryStage, scene, title, 600, 300, true);
        stage.setResizable(false);
        stage.show();
    }

    /* BROKER *********************************************************************************************************/

    public Node loadBrokerListView() {
        return getView("/view/broker/BrokerListView.fxml", null, true, true).root();
    }

    public Node loadBrokerView(Broker broker) {
        return this.<BrokerController>getView("/view/broker/BrokerView.fxml", it -> it.setBroker(broker), true, true).root();
    }

    public Node loadBrokerTabConfigurationView(Broker broker) {
        return this.<BrokerTabConfigurationController>getView("/view/broker/BrokerTabConfigurationView.fxml", it -> it.setBroker(broker), true, true).root();
    }

    public Node loadBrokerTabLogDirsView(Broker broker) {
        return this.<BrokerTabLogDirsController>getView("/view/broker/BrokerTabLogDirsView.fxml", it -> it.setBroker(broker), true, true).root();
    }

    public void showEditBrokerConfigView(Stage primaryStage, Broker broker, Config config) {
        var view = this.<EditBrokerConfigController>getView("/view/broker/EditBrokerConfigView.fxml", it -> it.setData(broker, config), false, false);
        var scene = createScene(view);
        var stage = createStage(primaryStage, scene, i18nService.get("editBrokerConfigView.stageTitle"), 800, 600, true);
        stage.show();
    }

    /* ACL ************************************************************************************************************/

    public Node loadAclListView() {
        return getView("/view/acl/AclListView.fxml", null, true, true).root();
    }

    public void showCreateAclView(Stage primaryStage, Runnable onSuccess) {
        var view = this.<CreateAclController>getView("/view/acl/CreateAclView.fxml", it -> it.setOnSuccess(onSuccess), false, false);
        var scene = createScene(view);
        var stage = createStage(primaryStage, scene, i18nService.get("acl.createAcl"), 800, 800, true);
        stage.show();
    }

    public Node loadCreateAclForConsumerView(CreateAclController parentController) {
        return this.<CreateAclForConsumerController>getView("/view/acl/CreateAclForConsumerView.fxml", it -> it.setParentController(parentController), false, false).root();
    }

    public Node loadCreateAclForProducerView(CreateAclController parentController) {
        return this.<CreateAclForProducerController>getView("/view/acl/CreateAclForProducerView.fxml", it -> it.setParentController(parentController), false, false).root();
    }

    public Node loadCreateAclForCustomNeedView(CreateAclController parentController) {
        return this.<CreateAclForCustomNeedController>getView("/view/acl/CreateAclForCustomNeedView.fxml", it -> it.setParentController(parentController), false, false).root();
    }

    public void showDeleteAclConfirmView(Stage primaryStage, Consumer<ConfirmController.ConfirmCallback> onButton) {
        var title = i18nService.get("deleteAclConfirmView.title");
        var content = i18nService.get("deleteAclConfirmView.content");
        var button = i18nService.get("deleteAclConfirmView.button");
        var view = this.<ConfirmController>getView("/view/ConfirmView.fxml", it -> it.setData(title, content, button, onButton), false, false);
        var scene = createScene(view);
        var stage = createStage(primaryStage, scene, title, 600, 300, true);
        stage.setResizable(false);
        stage.show();
    }

    /* QUOTA **********************************************************************************************************/

    public Node loadQuotaListView() {
        return getView("/view/quota/QuotaListView.fxml", null, true, true).root();
    }

    public void showCreateQuotaView(Stage primaryStage, Runnable onSuccess) {
        var view = this.<CreateQuotaController>getView("/view/quota/CreateQuotaView.fxml", it -> it.setOnSuccess(onSuccess), false, false);
        var scene = createScene(view);
        var stage = createStage(primaryStage, scene, i18nService.get("quota.createQuota"), 800, 600, true);
        stage.show();
    }

    public void showEditQuotaView(Stage primaryStage, Quota quota, Runnable onSuccess) {
        var view = this.<EditQuotaController>getView("/view/quota/EditQuotaView.fxml", it -> it.setData(quota, onSuccess), false, false);
        var scene = createScene(view);
        var stage = createStage(primaryStage, scene, i18nService.get("editQuotaView.stageTitle"), 800, 600, true);
        stage.show();
    }

    public void showDeleteQuotaConfirmView(Stage primaryStage, Consumer<ConfirmController.ConfirmCallback> onButton) {
        var title = i18nService.get("deleteQuotaConfirmView.title");
        var content = i18nService.get("deleteQuotaConfirmView.content");
        var button = i18nService.get("deleteQuotaConfirmView.button");
        var view = this.<ConfirmController>getView("/view/ConfirmView.fxml", it -> it.setData(title, content, button, onButton), false, false);
        var scene = createScene(view);
        var stage = createStage(primaryStage, scene, title, 600, 300, true);
        stage.setResizable(false);
        stage.show();
    }

    /* SCHEMA REGISTRY ************************************************************************************************/

    public Node loadSchemaListView() {
        return getView("/view/schema/registry/SchemaListView.fxml", null, true, true).root();
    }

    public Node loadSchemaView(String subject) {
        return this.<SchemaController>getView("/view/schema/registry/SchemaView.fxml", it -> it.setSubject(subject), false, true).root();
    }

    public Node loadSchemaTabSourceView(String subject, Runnable onChange) {
        return this.<SchemaTabSourceController>getView("/view/schema/registry/SchemaTabSourceView.fxml", it -> it.setData(subject, onChange), false, true).root();
    }

    public Node loadSchemaTabStructureView(String subject, Runnable onChange) {
        return this.<SchemaTabStructureController>getView("/view/schema/registry/SchemaTabStructureView.fxml", it -> it.setData(subject, onChange), false, true).root();
    }

    public void showCreateSchemaView(Stage primaryStage, Runnable onSuccess) {
        var view = this.<CreateSchemaController>getView("/view/schema/registry/CreateSchemaView.fxml", it -> it.setOnSuccess(onSuccess), false, false);
        var scene = createScene(view);
        var stage = createStage(primaryStage, scene, i18nService.get("schema.createSchema"), 800, 800, true);
        stage.setOnHidden(it -> view.controller().close());
        stage.show();
    }

    public void showEditSchemaView(Stage primaryStage, String subject, int version, Runnable onSuccess) {
        var view = this.<EditSchemaController>getView("/view/schema/registry/EditSchemaView.fxml", it -> it.setData(subject, version, onSuccess), false, false);
        var scene = createScene(view);
        var stage = createStage(primaryStage, scene, i18nService.get("editSchemaView.stageTitle"), 800, 800, true);
        stage.setOnHidden(it -> view.controller().close());
        stage.show();
    }

    public void showEditSchemaCompatibilityView(Stage primaryStage, String subject, CompatibilityLevel currentCompatibility, Runnable onSuccess) {
        var view = this.<EditSchemaCompatibilityController>getView("/view/schema/registry/EditSchemaCompatibilityView.fxml", it -> it.setData(subject, currentCompatibility, onSuccess), false, false);
        var scene = createScene(view);
        var stage = createStage(primaryStage, scene, i18nService.get("editSchemaCompatibilityView.stageTitle"), 700, 450, true);
        stage.show();
    }

    public void showDeleteSchemaConfirmView(Stage primaryStage, String subject, Runnable onSuccess) {
        var view = this.<DeleteSchemaConfirmController>getView("/view/schema/registry/DeleteSchemaConfirmView.fxml", it -> it.setData(subject, onSuccess), false, false);
        var scene = createScene(view);
        var stage = createStage(primaryStage, scene, i18nService.get("schema.deleteSchema"), 600, 300, true);
        stage.setResizable(false);
        stage.show();
    }

    public void showDeleteSchemaVersionConfirmView(Stage primaryStage, String subject, int version, Runnable onSuccess) {
        var view = this.<DeleteSchemaConfirmController>getView("/view/schema/registry/DeleteSchemaConfirmView.fxml", it -> it.setData(subject, version, onSuccess), false, false);
        var scene = createScene(view);
        var stage = createStage(primaryStage, scene, i18nService.get("deleteSchemaVersionConfirmView.stageTitle"), 600, 300, true);
        stage.setResizable(false);
        stage.show();
    }

    /* KAFKA CONNECT **************************************************************************************************/

    public Node loadConnectorListView() {
        return getView("/view/connect/ConnectorListView.fxml", null, true, true).root();
    }

    public Node loadConnectorView(Connector.Name cn) {
        return this.<ConnectorController>getView("/view/connect/ConnectorView.fxml", it -> it.setCn(cn), false, true).root();
    }

    public Node loadConnectorTabTasksView(Connector.Name cn, AtomicReference<Runnable> silentLoadTasks) {
        return this.<ConnectorTabTasksController>getView("/view/connect/ConnectorTabTasksView.fxml", it -> it.setData(cn, silentLoadTasks), false, true).root();
    }

    public Node loadConnectorTabConfigurationView(Connector.Name cn) {
        return this.<ConnectorTabConfigurationController>getView("/view/connect/ConnectorTabConfigurationView.fxml", it -> it.setCn(cn), false, true).root();
    }

    public void showCreateConnectorView(Stage primaryStage, Runnable onSuccess) {
        var view = this.<CreateConnectorController>getView("/view/connect/CreateConnectorView.fxml", it -> it.setOnSuccess(onSuccess), false, false);
        var scene = createScene(view);
        var stage = createStage(primaryStage, scene, i18nService.get("connect.createConnector"), 800, 600, true);
        stage.setOnHidden(it -> view.controller().close());
        stage.show();
    }

    public void showEditConnectorView(Stage primaryStage, Connector.Name cn, Runnable onSuccess) {
        var view = this.<EditConnectorController>getView("/view/connect/EditConnectorView.fxml", it -> it.setData(cn, onSuccess), false, false);
        var scene = createScene(view);
        var stage = createStage(primaryStage, scene, i18nService.get("connect.editConnector"), 800, 600, true);
        stage.setOnHidden(it -> view.controller().close());
        stage.show();
    }

    public void showDeleteConnectorConfirmView(Stage primaryStage, Consumer<ConfirmController.ConfirmCallback> onButton) {
        var title = i18nService.get("deleteConnectorConfirmView.title");
        var content = i18nService.get("deleteConnectorConfirmView.content");
        var button = i18nService.get("deleteConnectorConfirmView.button");
        var view = this.<ConfirmController>getView("/view/ConfirmView.fxml", it -> it.setData(title, content, button, onButton), false, false);
        var scene = createScene(view);
        var stage = createStage(primaryStage, scene, title, 600, 300, true);
        stage.setResizable(false);
        stage.show();
    }

    /* OTHER **********************************************************************************************************/

    public void showErrorDetailsView(Stage primaryStage, Throwable throwable) {
        var view = this.<ErrorDetailsController>getView("/view/ErrorDetailsView.fxml", it -> it.setData(throwable), false, false);
        var scene = createScene(view);
        var stage = createStage(primaryStage, scene, i18nService.get("exceptionDetailsView.stageTitle"), 600, 400, true);
        stage.show();
    }

    public void showErrorDetailsView(Stage primaryStage, String string) {
        var view = this.<ErrorDetailsController>getView("/view/ErrorDetailsView.fxml", it -> it.setData(string), false, false);
        var scene = createScene(view);
        var stage = createStage(primaryStage, scene, i18nService.get("exceptionDetailsView.stageTitle"), 600, 400, true);
        stage.show();
    }

    public void showRawConfigView(Stage primaryStage, List<Config> configList) {
        var view = this.<RawConfigController>getView("/view/RawConfigView.fxml", it -> it.setConfigList(configList), false, false);
        var scene = createScene(view);
        var stage = createStage(primaryStage, scene, i18nService.get("common.rawView"), 800, 800, true);
        stage.show();
    }

    public void showSettingsView(Stage primaryStage) {
        var view = getView("/view/SettingsView.fxml", null, false, false);
        var scene = createScene(view);
        var stage = createStage(primaryStage, scene, i18nService.get("settingsView.stageTitle"), 600, 450, true);
        stage.setResizable(false);
        stage.show();
    }

    public void showHelpView(Stage primaryStage) {
        var view = getView("/view/HelpView.fxml", null, false, false);
        var scene = createScene(view);
        var stage = createStage(primaryStage, scene, i18nService.get("helpView.stageTitle"), 500, 450, true);
        stage.setResizable(false);
        stage.show();
    }

    private Scene createScene(View<?> view) {
        var scene = new Scene(view.root());
        view.controller().initializeScene(scene);
        themeService.addStylesheets(scene);
        return scene;
    }

    private Stage createStage(Stage owner, Scene scene, String title, int width, int height, boolean closeOnEsc) {
        var stage = new Stage();
        if (owner != null) stage.initOwner(owner);
        fillStage(stage, scene, title, width, height, closeOnEsc);
        return stage;
    }

    private void fillStage(Stage stage, Scene scene, String title, int width, int height, boolean closeOnEsc) {
        stage.setScene(scene);
        stage.setTitle(title);
        stage.setWidth(width);
        stage.setHeight(height);
        if (closeOnEsc) stage.addEventHandler(KeyEvent.KEY_PRESSED, JavaFXUtils.onKeyEsc(stage::close));
        stage.getIcons().setAll(ThemeService.ICONS);
    }

    public void setStageTitleWithAppName(Stage stage, String title) {
        stage.setTitle(titleWithAppName(title));
    }

    private static String titleWithAppName(String title) {
        return title + " - " + ApplicationProperties.NAME;
    }

    public record View<T extends AbstractController>(Parent root, T controller) {
    }

    private final AtomicReference<View<?>> currentDashboardView = new AtomicReference<>();

    private <T extends AbstractController> View<T> getView(String fxml, Consumer<T> controllerCustomizer, boolean cache, boolean switchDashboardView) {
        var view = this.<T>getView(fxml, cache);
        if (switchDashboardView) {
            var prevView = currentDashboardView.getAndSet(view);
            if (prevView != null) prevView.controller().close();
        }
        if (controllerCustomizer != null) controllerCustomizer.accept(view.controller());
        view.controller().initializeUi();
        view.controller().initializeData();
        return view;
    }

    private <T extends AbstractController> View<T> getView(String fxml, boolean cache) {
        if (cache) {
            //noinspection unchecked
            return (View<T>) CACHE.computeIfAbsent(fxml, key -> createView(fxml));
        } else {
            return createView(fxml);
        }
    }

    private <T extends AbstractController> View<T> createView(String fxml) {
        var loader = fxmlLoaderProvider.get();
        loader.setResources(i18nService.getBundle());
        try (var inputStream = getClass().getResource(fxml).openStream()) {
            var root = loader.<Parent>load(inputStream);
            return new View<>(root, loader.getController());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void warmupQuick() {
        getView("/view/topic/TopicListView.fxml", false);
    }

    public void warmupLong() {
        getView("/view/topic/TopicView.fxml", false);
        getView("/view/topic/TopicTabConsumeView.fxml", false);
        getView("/view/topic/TopicTabProduceView.fxml", false);
        getView("/view/topic/TopicTabPartitionsView.fxml", false);
        getView("/view/topic/TopicTabConfigurationView.fxml", false);
        getView("/view/topic/TopicTabConsumerGroupView.fxml", false);

        getView("/view/acl/AclListView.fxml", false);
        getView("/view/acl/TabAclView.fxml", false);

        getView("/view/broker/BrokerListView.fxml", false);
        getView("/view/broker/BrokerTabConfigurationView.fxml", false);
        getView("/view/broker/BrokerTabLogDirsView.fxml", false);
        getView("/view/broker/BrokerView.fxml", false);

        getView("/view/cluster/ClusterListView.fxml", false);

        getView("/view/connect/ConnectorListView.fxml", false);
        getView("/view/connect/ConnectorTabConfigurationView.fxml", false);
        getView("/view/connect/ConnectorTabTasksView.fxml", false);
        getView("/view/connect/ConnectorView.fxml", false);

        getView("/view/consumer/group/ConsumerGroupListView.fxml", false);
        getView("/view/consumer/group/ConsumerGroupTabMembersView.fxml", false);
        getView("/view/consumer/group/ConsumerGroupTabTopicsView.fxml", false);
        getView("/view/consumer/group/ConsumerGroupView.fxml", false);

        getView("/view/schema/registry/SchemaListView.fxml", false);
        getView("/view/schema/registry/SchemaTabSourceView.fxml", false);
        getView("/view/schema/registry/SchemaTabStructureView.fxml", false);
        getView("/view/schema/registry/SchemaView.fxml", false);

        getView("/view/quota/QuotaListView.fxml", false);
    }

    public void clear() {
        CACHE.clear();
    }

    private static Pair<Integer, Integer> getInitialWidthHeight() {
        var sourceWidth = Screen.getPrimary().getBounds().getWidth();
        int width;
        if (sourceWidth <= 1024) {
            width = 900;
        } else if (sourceWidth <= 1280) {
            width = 1100;
        } else if (sourceWidth <= 1368) {
            width = 1200;
        } else if (sourceWidth <= 1600) {
            width = 1300;
        } else {
            width = 1400;
        }
        return Pair.of(width, (int) (width / 1.5));
    }

    private void bindStageSize(Stage stage, String id, int width, int height) {
        if (!applicationProperties.isTrackStageSize()) return;
        stage.setWidth(settingsService.getStageWidth(id, width));
        stage.setHeight(settingsService.getStageHeight(id, height));
        stage.widthProperty().addListener((observable, oldValue, newValue) -> {
            var diff = Math.abs(newValue.intValue() - settingsService.getStageWidth(id, width));
            if (diff > 10) settingsService.saveStageWidth(id, newValue.intValue());
        });
        stage.heightProperty().addListener((observable, oldValue, newValue) -> {
            var diff = Math.abs(newValue.intValue() - settingsService.getStageHeight(id, height));
            if (diff > 10) settingsService.saveStageHeight(id, newValue.intValue());
        });
    }
}
