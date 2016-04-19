import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import DAN.DAN;
import DAN.DAN.ODFObject;

public class DAI {
	static final IDAManager dandelion_ida_manager =  new DandelionIDAManager();
	static final IDAManager.Subscriber ida_event_subscriber = new IDAEventSubscriber();

	public static void init() {
		dandelion_ida_manager.init();
		dandelion_ida_manager.subscribe(ida_event_subscriber);
		dandelion_ida_manager.search();
	}
	
	static class DandelionIDAManager extends IDAManager {
		class DandelionIDA extends IDAManager.IDA {
			public String name;

	        public DandelionIDA (String name) {
	            this.name = name;
	        }

	        @Override
	        public boolean equals (Object obj) {
	            if (!(obj instanceof IDA)) {
	                return false;
	            }

	            DandelionIDA another = (DandelionIDA) obj;
	            if (this.name == null) {
	                return false;
	            }
	            return this.name.equals(another.name);
	        }
		}
		
		@Override
		public void init () {}

		@Override
		public void search() {
			broadcast_event(IDAManager.EventTag.FOUND_NEW_IDA, new DandelionIDA("Dandelion"));
		}

		@Override
		public void stop_searching() {}

		@Override
		public void connect(IDA ida) {
			broadcast_event(IDAManager.EventTag.CONNECTED, ida);
		}

		@Override
		public void write(byte[] command) {
			IDACommand ida_command = IDACommand.fromBytes(command);
			String feature = ida_command.feature;
			double data = ida_command.data;
			
			if(feature.equals("Scale")) {
				logging("Update feature "+ feature +": "+ data);
				dandelion.scale = (float) data;
			} else if(feature.equals("Angle")) {
				logging("Update feature "+ feature +": "+ data);
				dandelion.angle = (float) data;
			} else {
				handle_error("Feature '"+ feature +"' not found");
				return;
			}
		}

		@Override
		public void disconnect() {}
	}
	
	static class IDAEventSubscriber extends DandelionIDAManager.Subscriber {
		@Override
		public void on_event(IDAManager.EventTag event_tag, Object message) {
			switch (event_tag) {
			case FOUND_NEW_IDA:
				dandelion_ida_manager.connect((DandelionIDAManager.DandelionIDA)message);
				break;
			case CONNECTED:
				DAN.Subscriber dan_event_subscriber = new DANEventSubscriber();
				DAN.init("Dandelion");
				DAN.subscribe("Control_channel", dan_event_subscriber);
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
				break;
			default:
				handle_error("Events other than FOUND_NEW_IDA and CONNECTED are invalid");
				break;
			}
		}
	}
	
	static class IDACommand {
		public String feature;
		public double data;
		
		public IDACommand (String feature, double data) {
			this.feature = feature;
			this.data = data;
		}
		
		public byte[] toBytes () {
			byte[] ret = new byte[8 + feature.length()];
			ByteBuffer byte_buffer = ByteBuffer.wrap(ret);
			byte_buffer.putDouble(data);
			byte_buffer.put(feature.getBytes());
		    return ret;
		}
		
		static public IDACommand fromBytes (byte[] bytes) {
			ByteBuffer byte_buffer = ByteBuffer.wrap(bytes);
			byte[] feature_bytes = new byte[bytes.length - 8];
			double data = byte_buffer.getDouble();
			byte_buffer.get(feature_bytes);
			String feature = new String(feature_bytes);
			return new IDACommand(feature, data);
		}
	}
	
	static class DANEventSubscriber extends DAN.Subscriber {
		public void odf_handler (DAN.ODFObject odf_object) {
			switch (odf_object.event_tag) {
			case REGISTER_FAILED:
				handle_error("Register failed: "+ odf_object.message);
				break;
			case REGISTER_SUCCEED:
				//logging("Register successed: "+ odf_object.message);
				final DAN.Subscriber odf_subscriber = new ODFSubscriber();
				DAN.subscribe("Scale", odf_subscriber);
				DAN.subscribe("Angle", odf_subscriber);
				break;
			default:
				break;
			}
		}
	}
	
	static class ODFSubscriber extends DAN.Subscriber {
		@Override
		public void odf_handler(ODFObject odf_object) {
			DAN.Data newest = odf_object.dataset.newest();
			logging("new data: "+ odf_object.feature +", "+ newest.data);
			if(odf_object.feature.equals("Scale")) {
				IDACommand ida_command = new IDACommand("Scale", newest.data.getDouble(0));
				dandelion_ida_manager.write(ida_command.toBytes());
			} else if(odf_object.feature.equals("Angle")) {
				IDACommand ida_command = new IDACommand("Angle", newest.data.getDouble(0));
				dandelion_ida_manager.write(ida_command.toBytes());
			} else {
				handle_error("Feature '"+ odf_object.feature +"' not found");
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

	static String log_tag = "Dandelion";
	static final String local_log_tag = "DAI";
	static void logging (String message) {
		String padding = message.startsWith(" ") || message.startsWith("[") ? "" : " ";
		System.out.printf("[%s][%s]%s%s%n", log_tag, local_log_tag, padding, message);
	}
	
	static void handle_error (String message) {
		logging(message);
	}

}
