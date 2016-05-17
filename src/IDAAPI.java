import org.json.JSONArray;

public interface IDAAPI {
    enum Event {
        INITIALIZATION_FAILED,
        INITIALIZED,
        SEARCHING_STARTED,
        NEW_IDA_DISCOVERED,
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
        void receive(String idf, IDFObject idf_object);
    }

    void subscribe(String[] idf_list, IDFHandler idf_handler);
    void unsubscribe(IDFHandler idf_handler);
    void search();
    void connect(String id);
    void write(String odf, JSONArray data);
    void disconnect();
}