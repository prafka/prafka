package com.prafka.core.model;

import lombok.Getter;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.sourcelab.kafka.connect.apiclient.request.dto.ConnectorDefinition;
import org.sourcelab.kafka.connect.apiclient.request.dto.ConnectorStatus;
import org.sourcelab.kafka.connect.apiclient.request.dto.ConnectorTopics;

import java.util.List;
import java.util.Map;

/**
 * Represents a Kafka Connect connector.
 *
 * <p>Contains information about the connector including its name, type (source or sink),
 * current state, worker assignment, configuration, associated topics, and task statuses.
 *
 * @see ConnectorDefinition
 * @see ConnectorStatus
 */
@Getter
public class Connector {

    private final String name;
    private final Type type;
    private final State state;
    private final String workerId;
    private final Plugin plugin;
    private final Map<String, String> config;
    private final List<String> topics;
    private final List<Task> tasks;

    public Connector(ConnectorDefinition definition, ConnectorStatus status, ConnectorTopics topics) {
        name = definition.getName();
        type = Type.valueOf(StringUtils.upperCase(definition.getType()));
        state = State.valueOf(status.getConnector().get("state"));
        workerId = status.getConnector().get("worker_id");
        plugin = new Plugin(definition);
        config = definition.getConfig();
        this.topics = topics.getTopics();
        this.tasks = status.getTasks().stream().map(Task::new).toList();
    }

    public enum Type {
        SINK,
        SOURCE,
    }

    public enum State {
        RUNNING,
        FAILED,
        PAUSED,
        UNASSIGNED,
        TASK_FAILED,
    }

    @Getter
    public static class Plugin {

        private final String classFull;
        private final String classShort;

        public Plugin(ConnectorDefinition definition) {
            classFull = definition.getConfig().get("connector.class");
            classShort = ClassUtils.getShortClassName(classFull);
        }
    }

    @Getter
    public static class Task {

        private final int id;
        private final State state;
        private final String workerId;
        private final String trace;

        public Task(ConnectorStatus.TaskStatus task) {
            id = task.getId();
            state = State.valueOf(task.getState());
            workerId = task.getWorkerId();
            trace = task.getTrace();
        }

        public enum State {
            RUNNING,
            FAILED,
            PAUSED,
            RESTARTING,
            UNASSIGNED,
        }
    }

    public record Name(String connectId, String name) {
    }
}
