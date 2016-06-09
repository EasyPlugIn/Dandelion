import org.json.JSONArray;
class InternalIDAapi implements IDAapi {
    /* Declaration of IDFhandler reference */
    private IDFhandler idf_handler_ref;
    /* Declaration of read() function */
    public void receive(String idf, JSONArray data) {
        idf_handler_ref.receive(idf, data);
    }
    /* Implementation of IDAapi functions */
    public void init(IDFhandler idf_handler_obj, Object... args) {
        idf_handler_ref = idf_handler_obj;
    }
    public void search() {}
    public void connect(String id) {}
    public void send(String odf, JSONArray data) {
        IDA.send(odf, data);
    }
    public void disconnect() {}
}