package org.jvmxray.platform.shared.service;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class XRServiceDispatcher {

    private ExecutorService executorService;
    private ArrayList<XRIService> services = new ArrayList<>();

    public XRServiceDispatcher() {
        executorService = Executors.newSingleThreadExecutor();
    }

    public void add(XRIService service) {
        services.add(service);
    }

    public XRIService get(int index) {
        return services.get(index);
    }

    public void remove(int index) {
        services.remove(index);
    }

    public void clear() {
        services.clear();
    }

    public int size() {
        return services.size();
    }

    public boolean isEmpty() {
        return services.isEmpty();
    }

    public XRIService[] toArray() {
        return services.toArray(new XRIService[0]);
    }

    public void fire(XRIEvent event) {
        // Fire each event from a spawned thread.  Don't allow a hung service
        //   from interfering with other services.
        for(XRIService service : toArray() ) {
            executorService.submit(()-> {
                service.onEvent(event);
            });
        }
    }

    public void fire(XRIEvent event, int index) {
        executorService.submit(()-> {
            XRIService service = services.get(index);
            service.onEvent(event);
        });
    }

}
