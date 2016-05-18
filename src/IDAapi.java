import org.json.JSONArray;

public interface IDAapi {
    enum Event {
        INITIALIZATION_FAILED,
        INITIALIZATION_SUCCEEDED,
        SEARCH_STARTED,
        IDA_DISCOVERED,
        SEARCH_STOPPED,
        CONNECTION_FAILED,
        CONNECTION_SUCCEEDED,
        WRITE_FAILED,
        WRITE_SUCCEEDED,
        READ_FAILED,
        DISCONNECTION_FAILED,
        DISCONNECTION_SUCCEEDED,
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