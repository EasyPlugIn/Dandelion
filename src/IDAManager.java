import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public abstract class IDAManager {
    static private final Set<Subscriber> event_subscribers = Collections.synchronizedSet(new HashSet<Subscriber>());
    
    static public enum EventTag {
        START_SEARCHING,
        FOUND_NEW_IDA,
        STOP_SEARCHING,
        CONNECTION_FAILED,
        CONNECTED,
        DISCONNECTION_FAILED,
        DISCONNECTED,
        DATA_AVAILABLE,
    }
    
    /*
    	Public API
     */

    public abstract void init();

    public void subscribe (Subscriber s) {
        synchronized (event_subscribers) {
            if (!event_subscribers.contains(s)) {
                event_subscribers.add(s);
            }
        }
    }

    public void unsubscribe (Subscriber s) {
        synchronized (event_subscribers) {
            if (event_subscribers.contains(s)) {
                event_subscribers.remove(s);
            }
        }
    }
    
    public abstract void search();
    public abstract void stop_searching();
    public abstract void connect(final IDA ida);
    public abstract void write(byte[] command);
    public abstract void disconnect();
    
    public abstract class IDA {}

    static public abstract class Subscriber {
        abstract public void on_event (final EventTag event_tag, final Object message);
    }

    /*
    	Private helper functions
     */

    protected void broadcast_event (EventTag event_tag, Object message) {
        synchronized (event_subscribers) {
            for (Subscriber handler: event_subscribers) {
                handler.on_event(event_tag, message);
            }
        }
    }
}
