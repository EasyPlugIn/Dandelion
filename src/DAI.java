import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import DAN.DAN;

public class DAI {
	
	public static abstract class IDA_Manager {
	
		public abstract void search();
		public abstract void init();
		public abstract void connect();
		public abstract void read();
		public abstract void write(double data, String frature);
		public abstract void disconnect();
	
	}
	
	public static void init() {
		
		final IDA_Manager dandelion_ida_manager =  new IDA_Manager() {

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
		    	if(feature.equals("Scale")) {
			    	logging("Update feature "+ feature +": "+ data);
		    	    dandelion.scale = (float) data;
		    	} else if(feature.equals("Angle")) {
			    	logging("Update feature "+ feature +": "+ data);
		    	    dandelion.angle = (float) data;
		    	} else {
		    		return;
		    	}
			}

			@Override
			public void disconnect() {
				// TODO Auto-generated method stub
				
			}
			
		};
		
		   final DAN.Subscriber odf_subscriber = new DAN.Subscriber () {
	        	public void odf_handler (DAN.ODFObject odf_object) {
	        		DAN.Data newest = odf_object.dataset.newest();
	            	logging("new data: "+ odf_object.feature +", "+ newest.data);
	                if(odf_object.feature.equals("Scale")) {
	                    dandelion_ida_manager.write(newest.data.getDouble(0), "Scale");
	                } else if(odf_object.feature.equals("Angle")) {
	                    dandelion_ida_manager.write(newest.data.getDouble(0), "Angle");
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
		        profile.put("d_name", "Dandelion"+ get_mac_addr());
		        profile.put("dm_name", "Dandelion");
		        JSONArray feature_list = new JSONArray();
	        	feature_list.put("Scale");
	        	feature_list.put("Angle");
		        profile.put("df_list", feature_list);
		        profile.put("u_name", "yb");
		        profile.put("is_sim", false);
		        DAN.register("http://localhost:9999", DAN.get_d_id(DAN.get_clean_mac_addr(get_mac_addr())), profile);
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

	static String mac_addr_cache = "";
    static public String get_mac_addr () {
    	if (!mac_addr_cache.equals("")) {
    		logging("Mac address cache: "+ mac_addr_cache);
    		return mac_addr_cache;
    	}
    	
    	InetAddress ip;
    	try {
    		ip = InetAddress.getLocalHost();
    		System.out.println("Current IP address : " + ip.getHostAddress());
    		NetworkInterface network = NetworkInterface.getByInetAddress(ip);
    		byte[] mac = network.getHardwareAddress();
    		mac_addr_cache += String.format("%02X", mac[0]);
    		for (int i = 1; i < mac.length; i++) {
    			mac_addr_cache += String.format(":%02X", mac[i]);
    		}
    		logging(mac_addr_cache);
    		return mac_addr_cache;
    	} catch (UnknownHostException e) {
    		e.printStackTrace();
    	} catch (SocketException e){
    		e.printStackTrace();
    	}

		logging("Mac address cache retriving failed, use random string");
        Random rn = new Random();
        for (int i = 0; i < 12; i++) {
            int a = rn.nextInt(16);
            mac_addr_cache += "0123456789abcdef".charAt(a);
        }
        return mac_addr_cache;
    }

    static private String log_tag = "Dandelion";
    static private final String local_log_tag = "DAI";
    static private void logging (String message) {
		String padding = message.startsWith(" ") || message.startsWith("[") ? "" : " ";
        System.out.printf("[%s][%s]%s%s%n", log_tag, local_log_tag, padding, message);
    }
	
}
