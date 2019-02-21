package com.cyclebikeapp.lookup;

import android.support.annotation.NonNull;

import java.util.ArrayDeque;
import java.util.concurrent.Executor;

import static android.os.AsyncTask.THREAD_POOL_EXECUTOR;

/**
 * Created by TommyD on 1/21/2018.
 */

 class SerialExecutor implements Executor {
    private final ArrayDeque<Runnable> mTasks = new ArrayDeque<Runnable>();
    private Runnable mActive;

    public synchronized void execute(@NonNull final Runnable r) {
        mTasks.offer(new Runnable() {
            public void run() {
                try {
                    r.run();
                } finally {
                    scheduleNext();
                }
            }
        });
        if (mActive == null) {
            scheduleNext();
        }
    }

    private synchronized void scheduleNext() {
        if ((mActive = mTasks.poll()) != null) {
            THREAD_POOL_EXECUTOR.execute(mActive);
        }
    }
}
