import org.json.JSONArray;

public interface IDAAPI {
    enum Event {
        INITIALIZATION_FAILED,
        INITIALIZED,
        SEARCHING_STARTED,
        FOUND_NEW_IDA,
        SEARCHING_STOPPED,
        CONNECTION_FAILED,
        CONNECTED,
        WRITE_FAILED,
        WRITE_SUCCEED,
        READ_FAILED,
        DISCONNECTION_FAILED,
        DISCONNECTED,
    }
    
    static public class IDFObject {
        // data part
        public JSONArray data;
        
        // event part
        public Event event;
        public String message;

        public IDFObject (Event event, String message) {
            this.data = null;
            this.event = event;
            this.message = message;
        }

        public IDFObject (JSONArray data) {
            this.data = data;
            this.event = null;
            this.message = null;
        }
    }

    interface IDFHandler {
        void receive(final String idf, final IDFObject idf_object);
    }

    abstract class IDA {
        String id;
    }

    void subscribe(String[] idf, IDFHandler idf_handler);
    void subscribe(String idf, IDFHandler idf_handler);
    void unsubscribe(IDFHandler idf_handler);
    void search();
    void connect(IDA ida);
    void write(String odf, JSONArray data);
    void disconnect();
}