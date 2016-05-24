import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import DANapi.DAN;
import DANapi.DANapi;

public class DAI {
    static final DANapi dan_api = new DAN();
	static IDAapi internal_ida_api;
	static final String config_filename = "config.txt";
	static final String d_name = "Dandelion001";
	static final String dm_name = "Dandelion";
	static final String[] df_list = new String[]{"Size", "Angle"};

	public static void init(IDAapi internal_ida_api) {
	    logging(dan_api.version());
	    DAI.internal_ida_api = internal_ida_api;
        DANapi.ODFHandler dan_event_subscriber = new DANEventHandler();
        dan_api.init(dan_event_subscriber);
        
        String endpoint = "http://"+ get_config_ec() +":9999";
        
        JSONObject profile = new JSONObject();
        try {
            profile.put("d_name", d_name);
            profile.put("dm_name", dm_name);
            JSONArray feature_list = new JSONArray();
            for (String df_name: df_list) {
                feature_list.put(df_name);
            }
            profile.put("df_list", feature_list);
            profile.put("u_name", "yb");
            profile.put("is_sim", false);
            dan_api.register(endpoint, dm_name +"001", profile);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Runtime.getRuntime().addShutdownHook(new Thread () {
            @Override
            public void run () {
                DAI.deregister();
            }
        });
	}
	
	static private String get_config_ec () {
	    try {
	        /* assume that the config file has only one line,
	         *  which is the IP address of the EC (without port number)*/
	        BufferedReader br = new BufferedReader(new FileReader(config_filename));
    	    try {
    	        String line = br.readLine();
    	        if (line != null) {
    	            return line;
    	        }
                return "localhost";
    	    } finally {
    	        br.close();
    	    }
	    } catch (IOException e) {
	        return "localhost";
	    }
	}
	
	static public void deregister() {
        dan_api.deregister();
	}
	
	static public void pull(String odf, JSONArray data) {
	    internal_ida_api.write(odf, data);
	}
	
	static class DANEventHandler implements DANapi.ODFHandler {
		public void receive (String feature, DAN.ODFObject odf_object) {
			switch (odf_object.event) {
			case NEW_EC_DISCOVERED:
		        dan_api.reregister(odf_object.message);
			    break;
			case REGISTER_FAILED:
				handle_error("Register failed: "+ odf_object.message);
				break;
			case REGISTER_SUCCEED:
				//logging("Register successed: "+ odf_object.message);
				final DANapi.ODFHandler odf_subscriber = new DandelionODFHandler();
				dan_api.subscribe(df_list, odf_subscriber);
				break;
			default:
				break;
			}
		}
	}
	
	static class DandelionODFHandler implements DANapi.ODFHandler {
		@Override
		public void receive (String odf, DAN.ODFObject odf_object) {
			logging("New data: "+ odf +", "+ odf_object.data.toString());
			if(odf.equals("Size")) {
			    internal_ida_api.write(odf, odf_object.data);
			} else if(odf.equals("Angle")) {
                internal_ida_api.write(odf, odf_object.data);
			} else {
				handle_error("Feature '"+ odf +"' not found");
			}
		}
	}

	static String mac_addr_cache = "";
	static String get_mac_addr () {
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

	static final String local_log_tag = "DAI";
	static void logging (String message) {
		String padding = message.startsWith(" ") || message.startsWith("[") ? "" : " ";
		System.out.printf("[%s][%s]%s%s%n", dm_name, local_log_tag, padding, message);
	}
	
	static void handle_error (String message) {
		logging(message);
	}
}