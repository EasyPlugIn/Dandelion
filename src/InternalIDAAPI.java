import org.json.JSONArray;

public interface InternalIDAAPI {
    enum Event {
        INITIALIZATION_FAILED,
        INITIALIZED,
        SEARCHING_STARTED,
        FOUND_NEW_IDA,
        SEARCHING_STOPPED,
        CONNECTION_FAILED,
        CONNECTED,
        WRITE_FAILED,
        DATA_AVAILABLE,
        DISCONNECTION_FAILED,
        DISCONNECTED,
    }

    interface Subscriber {
        abstract public void on_event(final Event event, final Object message);
    }

    abstract class IDA {
        String id;
    }

    void subscribe(Subscriber s);
    void unsubscribe(Subscriber s);
    void search();
    void stop_searching();
    void connect(IDA ida);
    void write(String odf, JSONArray data);
    void disconnect();
}