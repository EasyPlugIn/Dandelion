import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import DANAPI.DAN;
import DANAPI.DANAPI;

public class DAI {
    
    static final DANAPI dan_api = new DAN();
	static IDAAPI internal_ida_api;
	static final String d_name = "Dandelion001";
	static final String dm_name = "Dandelion";
	static final String[] df_list = new String[]{"Size", "Angle"};

	public static void init(IDAAPI internal_ida_api) {
	    logging(dan_api.version());
	    DAI.internal_ida_api = internal_ida_api;
        DANAPI.ODFHandler dan_event_subscriber = new DANEventHandler();
        dan_api.init(dan_event_subscriber);
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
            dan_api.register("http://localhost:9999", dm_name +"001", profile);
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
	
	static public void deregister() {
        dan_api.deregister();
	}
	
	static public void pull(String odf, JSONArray data) {
	    internal_ida_api.write(odf, data);
	}
	
	static class DANEventHandler implements DANAPI.ODFHandler {
		public void receive (String feature, DAN.ODFObject odf_object) {
			switch (odf_object.event) {
			case FOUND_NEW_EC:
			    if (!dan_api.session_status()) {
			        dan_api.reregister(odf_object.message);
			    }
			    break;
			case REGISTER_FAILED:
				handle_error("Register failed: "+ odf_object.message);
				break;
			case REGISTER_SUCCEED:
				//logging("Register successed: "+ odf_object.message);
				final DANAPI.ODFHandler odf_subscriber = new DandelionODFHandler();
				dan_api.subscribe(df_list, odf_subscriber);
				break;
			default:
				break;
			}
		}
	}
	
	static class DandelionODFHandler implements DANAPI.ODFHandler {
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