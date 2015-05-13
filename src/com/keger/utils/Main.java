package com.keger.utils;

import com.keger.utils.task.NGActionQueue;

import java.util.ArrayList;
import java.util.concurrent.Executor;

/**
 * <p>Description: </p>
 * <p/>
 * <p>Copyright: Copyright (c) 2015</p>
 * <p/>
 * @author keger
 * @date 15/5/13
 */
public class Main {

    public static void main(String args[]) {
        CountingRunnable run = new CountingRunnable();

        ScriptableExecutor executor = new ScriptableExecutor();

        NGActionQueue manager = new NGActionQueue(1, executor);

        addActiveWorkItem(manager, run);

        executeNext(manager, executor);
    }

    private static void testSimpleCancel(){
        CountingRunnable run = new CountingRunnable();
        ScriptableExecutor executor = new ScriptableExecutor();

        NGActionQueue manager = new NGActionQueue(1, executor);

        addActiveWorkItem(manager, run);
        NGActionQueue.ActionItem work1 = addActiveWorkItem(manager, run);
        cancelWork(manager, work1);

        executeNext(manager, executor);
    }

    private static NGActionQueue.ActionItem addActiveWorkItem(NGActionQueue manager, Runnable runnable) {
        manager.validate();
        NGActionQueue.ActionItem workItem = manager.addActiveWorkItem(runnable);
        manager.validate();
        return workItem;
    }

    private static void executeNext(NGActionQueue manager, ScriptableExecutor executor) {
        manager.validate();
        executor.runNext();
        manager.validate();
    }

    private  static void cancelWork(NGActionQueue manager, NGActionQueue.ActionItem workItem) {
        manager.validate();
        workItem.cancel();
        manager.validate();
    }

    private  static void prioritizeWork(NGActionQueue manager, NGActionQueue.ActionItem workItem) {
        manager.validate();
        workItem.moveToFront();
        manager.validate();
    }

    static class CountingRunnable implements Runnable {
        private int runCount = 0;

        synchronized int getRunCount() {
            return runCount;
        }

        @Override
        public void run() {
            synchronized (this) {
                runCount++;
            }

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
            }
        }
    }

    static class ScriptableExecutor implements Executor {

        private final ArrayList<Runnable> runnables = new ArrayList<Runnable>();

        public int getPendingCount() {
            return runnables.size();
        }

        public void runNext() {
            runnables.get(0).run();
            runnables.remove(0);
        }

        public void runLast() {
            int index = runnables.size() - 1;
            runnables.get(index).run();
            runnables.remove(index);
        }

        @Override
        public void execute(Runnable runnable) {
            synchronized (this) {
                runnables.add(runnable);
            }
        }
    }
}
