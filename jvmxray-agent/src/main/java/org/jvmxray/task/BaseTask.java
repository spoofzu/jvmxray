package org.jvmxray.task;

import java.util.TimerTask;

public abstract class BaseTask extends TimerTask {

    private volatile boolean isRunning = false;

    @Override
    public void run() {
        _execute();
    }

    private void _execute() {
        isRunning = true;
        try {
            execute();
        }finally{
            isRunning = false;
        }
    }

    protected abstract void queueMessage(String value);

    public abstract void execute();

    public boolean isRunning() {
        return isRunning;
    }

    public void startup() {

    }

    public void shutdown() {

    }

}
