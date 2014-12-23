package com.znasibov.powerstats;


import java.util.LinkedHashSet;
import java.util.Set;

public class PowerRecordsListenerMixin {
    Set<PowerRecordsListener> prListeners;

    public PowerRecordsListenerMixin() {
        prListeners = new LinkedHashSet<>();
    }

    public PowerRecordsListenerMixin(PowerRecordsListenerMixin other) {
        this.prListeners = new LinkedHashSet<>(other.prListeners);
    }

    public PowerRecordsListenerMixin copy() {
        return new PowerRecordsListenerMixin(this);
    }

    public void subscribe(PowerRecordsListener obj) {
        prListeners.add(obj);
    }

    public void unsubscribe(PowerRecordsListener obj) {
        prListeners.remove(obj);
    }


    public void notify(PowerRecord pr) {
        for (PowerRecordsListener l : prListeners) {
            l.recordReceived(pr);
        }
    }
}
