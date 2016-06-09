import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import DANapi.DAN;
import DANapi.DANapi;

public class DAI {
    static final DANapi dan_api = new DAN();
	static IDAapi internal_ida_api;
	static final String d_name = "Dandelion001";
	static final String dm_name = "Dandelion";
	static JSONArray df_list;
	static final IDF_handler idf_handler_obj = new IDF_handler();
	
	static class IDF_handler implements IDAapi.IDFhandler {
        @Override
        public void receive(String idf, JSONArray data) {
            logging("receive(%s, %s)", idf, data);
            if (idf.equals("Control")) {
                String command = data.getString(0);
                JSONArray args = data.getJSONObject(1).getJSONArray("args");
                logging("Control: (%s, %s)", command, args);
            } else {
                dan_api.push(idf, data);
            }
        }
	}

	public static void init(InternalIDAapi internal_ida_api) {
        logging("DAI.init(%s)", internal_ida_api);
        DAI.internal_ida_api = internal_ida_api;
        DAI.internal_ida_api.init(idf_handler_obj);
        
        logging(dan_api.version());
        DANapi.ODFHandler dan_event_subscriber = new DANEventHandler();
        dan_api.init(dan_event_subscriber);
        JSONObject profile = new JSONObject();
        try {
            df_list = new JSONArray();
            df_list.put("Size");
            df_list.put("Angle");
            df_list.put("Mouse");
            df_list.put("Control");
            profile.put("d_name", d_name);
            profile.put("dm_name", dm_name);
            profile.put("df_list", df_list);
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
	    internal_ida_api.send(odf, data);
	}
	
	static class DANEventHandler implements DANapi.ODFHandler {
		public void receive (String feature, DAN.ODFObject odf_object) {
			switch (odf_object.event) {
			case NEW_EC_DISCOVERED:
			    if (!dan_api.session_status()) {
			        dan_api.reregister(odf_object.message);
			    }
			    break;
			case REGISTER_FAILED:
				handle_error("Register failed: "+ odf_object.message);
				break;
			case REGISTER_SUCCEED:
				//logging("Register successed: "+ odf_object.message);
				final DANapi.ODFHandler odf_subscriber = new DandelionODFHandler();
				for (int i = 0; i < df_list.length(); i++) {
				    dan_api.subscribe(df_list.getString(i), odf_subscriber);
				}
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
                internal_ida_api.send(odf, odf_object.data);
            } else if(odf.equals("Angle")) {
                internal_ida_api.send(odf, odf_object.data);
            } else if(odf.equals("Control")) {
                internal_ida_api.send(odf, odf_object.data);
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
	
    static private void logging (String format, Object... args) {
        logging(String.format(format, args));
    }

    static void logging(String message) {
        System.out.println("["+ local_log_tag +"] " + message);
    }
	
	static void handle_error (String message) {
		logging(message);
	}
}