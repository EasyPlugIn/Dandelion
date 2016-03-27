import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DAI {

	public static void init() {
		
		IDA_Manager dandelion_ida_manager =  new IDA_Manager() {

			@Override
			public void search() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void init() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void connect() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void read() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void write(double data, String feature) {
				// TODO Auto-generated method stub
				dandelion.write(data, feature);
				
			}

			@Override
			public void disconnect() {
				// TODO Auto-generated method stub
				
			}
			
		};
		
		   DAN.Subscriber odf_subscriber = new DAN.Subscriber () {
	        	public void odf_handler (DAN.ODFObject odf_object) {
	        		DAN.Data newest = odf_object.dataset.newest();
	                if(odf_object.feature.equals("Scale")) {
	                    dandelion_ida_manager.write(newest.data.getDouble(0), "Scale");
	                }
	                if(odf_object.feature.equals("Angle")) {
	                    dandelion_ida_manager.write(newest.data.getDouble(1), "Angle");
	                }
	        	}
	        };
	        
	        DAN.Subscriber event_subscriber = new DAN.Subscriber() {
	        	public void odf_handler (DAN.ODFObject odf_object) {
	        		switch (odf_object.event_tag) {
	        		case REGISTER_FAILED:
	        			//logging("Register failed: "+ odf_object.message);
	        			break;
	        		case REGISTER_SUCCEED:
	        			//logging("Register successed: "+ odf_object.message);
	        			DAN.subscribe("Scale", odf_subscriber);
	        			DAN.subscribe("Angle", odf_subscriber);
	        			break;
	        		default:
	        			break;
	        		}
	        	}
	        };

	        DAN.init("Dandelion");
	        DAN.subscribe("Control_channel", event_subscriber);
	        JSONObject profile = new JSONObject();
	        try {
		        profile.put("d_name", "Dandelion"+ DAN.get_mac_addr());
		        profile.put("dm_name", "Dandelion");
		        JSONArray feature_list = new JSONArray();
	        	feature_list.put("Growth");
		        profile.put("df_list", feature_list);
		        profile.put("u_name", "yb");
		        profile.put("is_sim", false);
		        DAN.register(DAN.get_d_id(DAN.get_mac_addr()), profile);
			} catch (JSONException e) {
				e.printStackTrace();
			}
	        
	        //logging("[Dandelion] EasyConnect Host: " + csmapi.ENDPOINT);
	        
	    	Runtime.getRuntime().addShutdownHook(new Thread () {
	        	@Override
	        	public void run () {
	               //logging("shutdown hook");
	                DAN.deregister();
	                dandelion_ida_manager.disconnect();
	        	}
	        });
	}
	
}
