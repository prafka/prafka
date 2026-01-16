package com.prafka.desktop.service;

import com.prafka.core.util.ExecutorUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExecutorHolder {

    public static final ExecutorService codeHighlightExecutor = Executors.newSingleThreadExecutor();
    public static final ExecutorService taskExecutor = Executors.newCachedThreadPool();

    public static void close() {
        ExecutorUtils.closeExecutor(codeHighlightExecutor);
        ExecutorUtils.closeExecutor(taskExecutor);
    }
}
