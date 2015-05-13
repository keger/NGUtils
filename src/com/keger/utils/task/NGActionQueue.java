package com.keger.utils.task;

import java.util.concurrent.Executor;

/**
 * <p>Description: </p>
 * <p/>
 * <p>Copyright: Copyright (c) 2015</p>
 * <p/>
 * @author keger
 * @date 15/4/19
 */
public class NGActionQueue {
    public static final int DEFAULT_MAX_CONCURRENT = 9;

    private final Object workLock = new Object();
    private ActionNode pendingJobs;

    private final int maxConcurrent;
    private final Executor executor;

    private ActionNode runningJobs = null;
    private int runningCount = 0;

    public NGActionQueue() {
        this(DEFAULT_MAX_CONCURRENT);
    }

    public NGActionQueue(int maxConcurrent) {
        this(maxConcurrent, NGExecutor.getExecutor());
    }

    public NGActionQueue(int maxConcurrent, Executor executor) {
        this.maxConcurrent = maxConcurrent;
        this.executor = executor;
    }

    public ActionItem addActiveWorkItem(Runnable callback) {
        return addActiveWorkItem(callback, true);
    }

    public ActionItem addActiveWorkItem(Runnable callback, boolean addToFront) {
        ActionNode node = new ActionNode(callback);
        synchronized (workLock) {
            pendingJobs = node.addToList(pendingJobs, addToFront);
        }

        startItem();
        return node;
    }

    public void validate() {
        synchronized (workLock) {
            int count = 0;

            if (runningJobs != null) {
                ActionNode walk = runningJobs;
                do {
                    walk.verify(true);
                    count++;
                    walk = walk.getNext();
                } while (walk != runningJobs);
            }

            assert runningCount == count;
        }
    }

    private void startItem() {
        finishItemAndStartNew(null);
    }

    private void finishItemAndStartNew(ActionNode finished) {
        ActionNode ready = null;

        synchronized (workLock) {
            if (finished != null) {
                runningJobs = finished.removeFromList(runningJobs);
                runningCount--;
            }

            if (runningCount < maxConcurrent) {
                ready = pendingJobs; // Head of the pendingJobs queue
                if (ready != null) {
                    pendingJobs = ready.removeFromList(pendingJobs);
                    runningJobs = ready.addToList(runningJobs, false);
                    runningCount++;

                    ready.setIsRunning(true);
                }
            }
        }

        if (ready != null) {
            execute(ready);
        }
    }

    private void execute(final ActionNode node) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    node.getCallback().run();
                } finally {
                    finishItemAndStartNew(node);
                }
            }
        });
    }

    private class ActionNode implements ActionItem {
        private final Runnable callback;
        private ActionNode next;
        private ActionNode prev;
        private boolean isRunning;

        ActionNode(Runnable callback) {
            this.callback = callback;
        }

        @Override
        public boolean cancel() {
            synchronized (workLock) {
                if (!isRunning()) {
                    pendingJobs = removeFromList(pendingJobs);
                    return true;
                }
            }

            return false;
        }

        @Override
        public void moveToFront() {
            synchronized (workLock) {
                if (!isRunning()) {
                    pendingJobs = removeFromList(pendingJobs);
                    pendingJobs = addToList(pendingJobs, true);
                }
            }
        }

        @Override
        public boolean isRunning() {
            return isRunning;
        }

        Runnable getCallback() {
            return callback;
        }

        ActionNode getNext() {
            return next;
        }

        void setIsRunning(boolean isRunning) {
            this.isRunning = isRunning;
        }

        ActionNode addToList(ActionNode list, boolean addToFront) {
            assert next == null;
            assert prev == null;

            if (list == null) {
                list = next = prev = this;
            } else {
                next = list;
                prev = list.prev;
                next.prev = prev.next = this;
            }

            return addToFront ? this : list;
        }

        ActionNode removeFromList(ActionNode list) {
            assert next != null;
            assert prev != null;

            if (list == this) {
                if (next == this) {
                    list = null;
                } else {
                    list = next;
                }
            }

            next.prev = prev;
            prev.next = next;
            next = prev = null;

            return list;
        }

        void verify(boolean shouldBeRunning) {
            assert prev.next == this;
            assert next.prev == this;
            assert isRunning() == shouldBeRunning;
        }
    }

    public interface ActionItem {
        boolean cancel();
        boolean isRunning();
        void moveToFront();
    }
}
