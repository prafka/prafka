package com.prafka.desktop;

import javafx.concurrent.Task;

import java.lang.reflect.InvocationTargetException;

public class TestUtils {

    @SuppressWarnings("unchecked")
    public static <T> T invokeCall(Task<T> task) throws Exception {
        var method = Task.class.getDeclaredMethod("call");
        method.setAccessible(true);
        try {
            return (T) method.invoke(task);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception ex) {
                throw ex;
            }
            throw e;
        }
    }
}
