package com.keger.utils.task;

import android.os.AsyncTask;

import java.lang.reflect.Field;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.keger.utils.internal.Preconditions.checkNotNull;

/**
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2015</p>
 * <p/>
 * @author keger
 * @date 15/4/19
 */
public final class NGExecutor {

    private static final Object LOCK = new Object();

    private static final int DEFAULT_CORE_POOL_SIZE = 5;
    private static final int DEFAULT_MAXIMUM_POOL_SIZE = 128;
    private static final int DEFAULT_KEEP_ALIVE = 1;

    private static final BlockingQueue<Runnable> DEFAULT_WORK_QUEUE =
            new LinkedBlockingQueue<Runnable>(10);

    private static final ThreadFactory DEFAULT_THREAD_FACTORY = new ThreadFactory() {
        private final AtomicInteger counter = new AtomicInteger(0);

        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, "NineGame Executor #" + counter.incrementAndGet());
        }
    };

    private static volatile Executor executor;

    public static Executor getExecutor() {
        synchronized (LOCK) {
            if (NGExecutor.executor == null) {
                Executor executor = getAsyncTaskExecutor();
                if (executor == null) {
                    executor = new ThreadPoolExecutor(
                            DEFAULT_CORE_POOL_SIZE, DEFAULT_MAXIMUM_POOL_SIZE, DEFAULT_KEEP_ALIVE,
                            TimeUnit.SECONDS, DEFAULT_WORK_QUEUE, DEFAULT_THREAD_FACTORY);
                }
                NGExecutor.executor = executor;
            }
        }
        return NGExecutor.executor;
    }

    private static Executor getAsyncTaskExecutor() {
        Field executorField = null;
        try {
            executorField = AsyncTask.class.getField("THREAD_POOL_EXECUTOR");
        } catch (NoSuchFieldException e) {
            return null;
        }

        Object executorObject = null;
        try {
            executorObject = executorField.get(null);
        } catch (IllegalAccessException e) {
            return null;
        }

        if (executorObject == null) {
            return null;
        }

        if (!(executorObject instanceof Executor)) {
            return null;
        }

        return (Executor) executorObject;
    }

    public static void setExecutor(Executor executor) {
        checkNotNull(executor, "executor == null");
        synchronized (LOCK) {
            NGExecutor.executor = executor;
        }
    }
}
