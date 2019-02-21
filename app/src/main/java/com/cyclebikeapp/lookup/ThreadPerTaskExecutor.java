package com.cyclebikeapp.lookup;

import java.util.concurrent.Executor;

/**
 * Created by TommyD on 1/21/2018.
 */

class ThreadPerTaskExecutor implements Executor {
        public void execute(Runnable r) {
            new Thread(r).start();
        }
}
