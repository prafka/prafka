package com.prafka.desktop.controller.consumer.group;

import com.prafka.desktop.controller.AbstractController;
import jakarta.inject.Singleton;
import org.apache.kafka.common.GroupState;

@Singleton
public class ConsumerGroupHelper extends AbstractController {

    public String getStateDescription(GroupState state) {
        return switch (state) {
            case STABLE -> i18nService.get("consumerGroup.stableDescription");
            case PREPARING_REBALANCE -> i18nService.get("consumerGroup.preparingRebalanceDescription");
            case COMPLETING_REBALANCE -> i18nService.get("consumerGroup.completingRebalanceDescription");
            case ASSIGNING -> "Assigning";
            case RECONCILING -> "Reconciling";
            case NOT_READY -> "NotReady";
            case EMPTY -> i18nService.get("consumerGroup.emptyDescription");
            case DEAD -> i18nService.get("consumerGroup.deadDescription");
            case UNKNOWN -> i18nService.get("consumerGroup.unknownDescription");
        };
    }

    public String getStateStyle(GroupState state) {
        return switch (state) {
            case STABLE -> "badge-green";
            case COMPLETING_REBALANCE -> "badge-green";
            case PREPARING_REBALANCE -> "badge-yellow";
            case EMPTY -> "badge-yellow";
            case DEAD -> "badge-red";
            default -> "badge-gray";
        };
    }

    public boolean isEditable(GroupState state) {
        return state == GroupState.EMPTY || state == GroupState.DEAD;
    }
}
