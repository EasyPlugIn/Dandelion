import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONObject;

import processing.core.PApplet;

import java.io.Console;

@SuppressWarnings("serial")
public class DAI implements DAN.DAN2DAI {
	static final String config_filename = "config.txt";
	static DAI dai = new DAI();
	static IDA ida = new IDA();
    static DAN dan = new DAN();
    static String d_name = "";
    static String u_name = "yb";
    static Boolean is_sim = false;
    static String endpoint = null; 
    static boolean is_resgister = false;
    static String info = "";
    
    static float bgcolor = 0;
    static float length = 16;  //stalk_len
    static float thickness = 1;
    static float rate = 20;    //stalk_increase_rate
    static int display_width = 1000;
    static int display_height = 700;
	static abstract class DF {
        public DF (String name) {
            this.name = name;
        }
        public String name;
        public boolean selected;
    }
	
	static abstract class IDF extends DF {
        public IDF (String name) {
            super(name);
        }
    }
	
    static abstract class ODF extends DF {
        public ODF (String name) {
            super(name);
        }
        abstract public void pull(JSONArray data);
    }
    
    static abstract class Command {
        public Command(String name) {
            this.name = name;
        }
        public String name;
        abstract public void run(JSONArray dl_cmd_params, JSONArray ul_cmd_params);
    }
    
    static ArrayList<DF> df_list = new ArrayList<DF>();
    static ArrayList<Command> cmd_list = new ArrayList<Command>();
    static boolean suspended = true;
    
    

    
    static void add_df (DF... dfs) {
        for (DF df: dfs) {
            df_list.add(df);
        }
    }
    
    static void add_command (Command... cmds) {
        for (Command cmd: cmds) {
            cmd_list.add(cmd);
        }
    }
    
    private static boolean is_selected(String df_name) {
        for (DF df: df_list) {
            if (df_name.equals(df.name)) {
                return df.selected;
            }
        }
        System.out.println("Device feature" + df_name + "is not found");
        return false;
    }
 
    private static Command get_cmd(String cmd_name) {
        for(Command cmd: cmd_list) {
            if(cmd_name.equals(cmd.name) || cmd_name.equals(cmd.name + "_RSP")) {
                return cmd;
            }
        }
        System.out.println("Command" + cmd_name + "is not found");
        return null;
    }
    
    private static DF get_df(String df_name) {
        for(DF df: df_list) {
            if(df_name.equals(df.name)) {
                return df;
            }
        }
        System.out.println("Device feature" + df_name + "is not found");
        return null;
    }
    
    
    /* Default command-1: SET_DF_STATUS */
    static class SET_DF_STATUS extends Command {
        public SET_DF_STATUS() {
            super("SET_DF_STATUS");
        }
        public void run(final JSONArray df_status_list,
                         final JSONArray updated_df_status_list) {
            if(df_status_list != null && updated_df_status_list == null) {
            	final String flags = df_status_list.getString(0);
                for(int i = 0; i < flags.length(); i++) {
                    if(flags.charAt(i) == '0') {
                        df_list.get(i).selected = false;
                    } else {
                        df_list.get(i).selected = true;
                    }
                }
	            get_cmd("SET_DF_STATUS_RSP").run(
            		null,
            		new JSONArray(){{
		            	put(flags);
		            }}
        		);
            }
            else if(df_status_list == null && updated_df_status_list != null) {
            	dan.push(
                		"Control",
                		new JSONArray(){{
    	                	put("SET_DF_STATUS_RSP");
    	                	put(new JSONObject(){{
    	                		put("cmd_params", updated_df_status_list);
    	                	}});
                		}}
            		);
            } else {
                System.out.println("Both the df_status_list and the updated_df_status_list are null");
            }
        }
    }
    /* Default command-2: RESUME */
    static class RESUME extends Command {
        public RESUME() {
            super("RESUME");
        }
        public void run(final JSONArray dl_cmd_params,
                         final JSONArray exec_result) {
            if(dl_cmd_params != null && exec_result == null) {
            	suspended = false;
                get_cmd("RESUME_RSP").run(
                	null, 
                	new JSONArray(){{
                	    put("OK");
	            }});
            }
            else if(dl_cmd_params == null && exec_result != null) {
            	dan.push(
                		"Control",
                		new JSONArray(){{
    	                	put("RESUME_RSP");
    	                	put(new JSONObject(){{
    	                		put("cmd_params", exec_result);
    	                	}});
                		}}
            		);
            } else {
            	System.out.println("Both the dl_cmd_params and the exec_result are null!");
            }
        }
    }
    /* Default command-3: SUSPEND */
    static class SUSPEND extends Command {
        public SUSPEND() {
            super("SUSPEND");
        }
        public void run(JSONArray dl_cmd_params,
                         final JSONArray exec_result) {
            if(dl_cmd_params != null && exec_result == null) {
            	suspended = true;
                get_cmd("SUSPEND_RSP").run(
                	null, 
                	new JSONArray(){{
                		put("OK");
    	        }});
            }
            else if(dl_cmd_params == null && exec_result != null) {
            	dan.push(
                		"Control",
                		new JSONArray(){{
    	                	put("SUSPEND_RSP");
    	                	put(new JSONObject(){{
    	                		put("cmd_params", exec_result);
    	                	}});
                		}}
            		);
            } else {
            	System.out.println("Both the dl_cmd_params and the exec_result are null!");
            }
        }
    }
    
	public void add_shutdownhook() {
		Runtime.getRuntime().addShutdownHook(new Thread () {
            @Override
            public void run () {
            	deregister();
            }
        });
	}
	
	/* deregister() */
	public void deregister() {
		dan.deregister();
	}
	
	@Override
	public void pull(final String odf_name, final JSONArray data) {
		if (odf_name.equals("Control")) {
            final String cmd_name = data.getString(0);
            JSONArray dl_cmd_params = data.getJSONObject(1).getJSONArray("cmd_params");
            Command cmd = get_cmd(cmd_name);
            if (cmd != null) {
                cmd.run(dl_cmd_params, null);
                return;
            }
            
            /* Reports the exception to IoTtalk*/
            dan.push("Control", new JSONArray(){{
            	put("UNKNOWN_COMMAND");
            	put(new JSONObject(){{
            		put("cmd_params", new JSONArray(){{
            			put(cmd_name);
            		}});
            	}});
            }});
        } else {
        	ODF odf = ((ODF)get_df(odf_name));
        	if (odf != null) {
        		odf.pull(data);
                return;
            }
            
            /* Reports the exception to IoTtalk*/
            dan.push("Control", new JSONArray(){{
            	put("UNKNOWN_ODF");
            	put(new JSONObject(){{
            		put("cmd_params", new JSONArray(){{
            			put(odf_name);
            		}});
            	}});
            }});
        }
	}
    
    
//   static private String get_config_ec () {
//        try {
//            /* assume that the config file has only one line,
//             *  which is the IP address of the EC (without port number)*/
//            BufferedReader br = new BufferedReader(new FileReader(config_filename));
//            try {
//                String line = br.readLine();
//                if (line != null) {
//                    return line;
//                }
//                return "localhost";
//            } finally {
//                br.close();
//            }
//        } catch (IOException e) {
//            return "localhost";
//        }
//    }
   static private void load_config () {
       BufferedReader br = null;
       try {
           br = new BufferedReader(new FileReader(config_filename));
           String line;
           while ((line = br.readLine()) != null) {
               line = line.trim();
               if (line.startsWith("#")) {
                   continue;
               }

               String[] tokens = line.split("=");
               if (tokens.length != 2) {
                   continue;
               }
               tokens[0] = tokens[0].trim().toLowerCase();
               tokens[1] = tokens[1].trim().toLowerCase();
               set_config_value(tokens[0], tokens[1]);
           }
       } catch (IOException e) {
       } finally {
           try {
               if (br != null) br.close();
           } catch (IOException e) {
               e.printStackTrace();
           }
       }
   }

   /*setting the parameter of dandelion*/
   static private void set_config_value (String key, String text) {
       switch (key) {
       case "endpoint":
           endpoint = text;
           if (!endpoint.startsWith("http://")) {
               endpoint = "http://" + endpoint;
           }
           if (endpoint.length() - endpoint.replace(":", "").length() == 1) {
               endpoint += ":9999";
           }
           break;
       case "bgcolor":
           bgcolor = Float.parseFloat(text);
           break;
       case "length":
           length = Float.parseFloat(text);
           break;
       case "thickness":
           thickness = Float.parseFloat(text);
           break;
       case "rate":
           rate = Float.parseFloat(text);
           break;
       case "display_width":
           display_width = Integer.parseInt(text);
           break;
       case "display_height":
           display_height = Integer.parseInt(text);
           break;
       default:
           System.out.printf("Unknown setting: [%s = %s]\n", key, text);
           break;
       }
   }
   

    /* The main() function */
    public static void main(String[] args) {
        add_command(
            new SET_DF_STATUS(),
            new RESUME(),
            new SUSPEND()
        );
        init_cmds();
        init_dfs();
        final JSONArray df_name_list = new JSONArray();
        for(int i = 0; i < df_list.size(); i++) {
            df_name_list.put(df_list.get(i).name);
        }
        load_config();
        
        JSONObject profile = new JSONObject() {{
            put("dm_name", dm_name); //deleted
            put("u_name", "yb");
            put("df_list", df_name_list);
            put("is_sim", false);
        }};
        
        String d_id = "";
        Random rn = new Random();
        for (int i = 0; i < 12; i++) {
            int a = rn.nextInt(16);
            d_id += "0123456789ABCDEF".charAt(a);
        }

        dan.init(dai, endpoint, d_id, profile);
        d_name = profile.getString("d_name");
        dai.add_shutdownhook();

        /* Performs the functionality of the IDA */
        ida.iot_app();             
    }
    
    /*--------------------------------------------------*/
    /* Customizable part */
    static String dm_name = "Dandelion";
    
    static class Mouse extends IDF {
        public Mouse () {
            super("Mouse");
        }
        public void push(double x, double y) {
        	if(selected && !suspended) {
	        	JSONArray data = new JSONArray();
	            data.put(x);
	            data.put(y);
	            dan.push(name, data);
        	}
        }
    }
    
    /* Declaration of ODF classes, generated by the DAC */
    static class Size extends ODF {
        public Size () {
            super("Size");
        }
        public void pull(JSONArray data) {
            System.out.println("Size: "+ data.toString());
            /* parse data from packet, assign to every yi */
        	if(selected && !suspended) {
        		ida.size = (int)data.getDouble(0);
        	}
        	else {
                ida.size = 0; // default value
        	}
        }
    }
    static class Angle extends ODF {
        public Angle () {
            super("Angle");
        }
        public void pull(JSONArray data) {
        	if(selected && !suspended) {
        	    ida.angle = (float)data.getDouble(0);
        	}
        	else {
                ida.angle = 0f;
        	}
        }
    }
    static class Color extends ODF {
        public Color () {
            super("Color-O");
        }
        public void pull(JSONArray data) {
        	if(selected && !suspended) {
        	    ida.color_r = data.getInt(0);
        	    ida.color_g = data.getInt(1);
        	    ida.color_b = data.getInt(2);
        	}
        	else {
        		ida.color_r = 0;
        	    ida.color_g = 0;
        	    ida.color_b = 0;
        	}
        }
    }
    
    /* Initialization of command list and DF list, generated by the DAC */
    static void init_cmds () {
        add_command(
        //    new SAMPLE_COMMAND ()
        );
    }
    static void init_dfs () {
        add_df(
    		new Mouse(),
            new Size(),
            new Angle(),
            new Color()
            
        );
    }
    
    /*--------------------------------------------------*/
    /* IDA Class */
    public static class IDA extends PApplet{
    	// ODFs
    	public int size = 0;
    	public float angle = 0;
    	public int color_r = 255;
    	public int color_g = 255;
    	public int color_b = 255;
    	
    	public void iot_app() {
    		PApplet.runSketch(new String[]{d_name}, this);
    	};
    	    	
    	// Dandelion animation
        //final int width = display_width;
        //final int height = display_height;
        //final int s0 = display_height / 2;
        final int s1 = 28;
        final int s2 = (int)((float)s1 / 1.618);  // golden ratio 1.618
        final int s3 = (int)((float)s2 * 0.618);  // golden ratio 0.618
        final float gamma = 115;  // included angle
        // Dandelion animation (others)
        int max_size = 10;
        float max_angle = 120;
        float current_angle = 0;
        float current_size = 0;
        float current_color_r = 0;
        float current_color_g = 0;
        float current_color_b = 0;
        float step = 4;
        int TEXT_SIZE = 16;
        String textinfo = "";
 
        @Override
        /* setup() */
        public void setup() {
            smooth();
            size(display_width, display_height);
        }

        @Override
        /* draw() */
        public void draw(){
        	if (size > max_size) { size = max_size; }
            if (angle > max_angle) { angle = max_angle; }
            
            current_size += ((float)size - current_size) / step;
            current_angle += (angle - current_angle) / step;
            current_color_r += ((float)color_r - current_color_r) / step;
            current_color_g += ((float)color_g - current_color_g) / step;
            current_color_b += ((float)color_b - current_color_b) / step;
            
            background((int)bgcolor);
                        
            // Showing info on display layout
            textSize(TEXT_SIZE - 3);          
            fill(abs(bgcolor - 255) / 2);
            textinfo = "Display width x height = " + display_width + " x " + display_height;            
            text(textinfo, 10, TEXT_SIZE + 10);
            textinfo = "(s0, s1, s2, s3) = (" + display_height / 2 + ", " + s1 + ", " + s2 + ", " + s3 + ")" ;
            text(textinfo, 10, 2 * TEXT_SIZE + 10);
            textinfo = "Device name = " + d_name;
            text(textinfo, 10, display_height - 3 * TEXT_SIZE - 10);
            textinfo = "ODF Size = " +  size + "  ,  now: " + current_size;
            text(textinfo, 10, display_height - 2 * TEXT_SIZE - 10);
            textinfo = "ODF Angle = " +  angle + "  ,  now: " + current_angle;
            text(textinfo, 10, display_height - 1 * TEXT_SIZE - 10);
            textinfo = "ODF Color-O (R, G, B) = (" + color_r + ", " + color_g + ", " + color_b + ")"
            		+ "  ,  now: (" + round(current_color_r) + ", " + round(current_color_g) + ", " + round(current_color_b) + ")";
            text(textinfo, 10, display_height - 0 * TEXT_SIZE - 10);
              
            translate(display_width / 2, display_height * 4 / 8);
            stroke(round(current_color_r), round(current_color_g), round(current_color_b));
            strokeWeight(thickness);          
            line(0, 0, 0, display_height * 4 / 8);
            grow(0);
        }
        
        /* grow() */
            void grow (int level) {
                if (level >= current_size) {
        	        return; 
                }
                int alpha = 0;
                float parameter_s = ((float)level / 20) + 1;
                float parameter_g = ((float)level / 35) + 1;
                
                int target_x = round(s1 * parameter_s * cos(radians(gamma * parameter_g / 2)) );
                int target_y = round(-s1 * parameter_s * sin(radians(gamma * parameter_g / 2)) );
                
                // transparency of outermost layer
                if (level + 1 > current_size) { 
                    alpha = round((current_size - (int)current_size) * 255);
                } else {
                    alpha = 255;
                }
                // right child
                pushMatrix();
                rotate(radians(-current_angle));
                stroke(round(current_color_r), round(current_color_g), round(current_color_b), alpha);
                line(0, 0, s2, 0);
                line(0, 0, -s2 * cos(radians(60)), -s2 * sin(radians(60)));
                line(0, 0, target_x, target_y);
                translate(target_x, target_y); 
                rotate(radians(180 - gamma * parameter_g));
                line(0, 0, s2, 0);
                line(0, 0, -s2 * cos(radians(60)), s2 * sin(radians(60)));
                grow(level + 1); 
                popMatrix();
                // left child
                pushMatrix();
                rotate(radians(current_angle));
                stroke(round(current_color_r), round(current_color_g), round(current_color_b), alpha);
                line(0, 0, -s2, 0);
                line(0, 0, s2 * cos(radians(60)), -s2 * sin(radians(60)));
                line(0, 0, -target_x, target_y);
                translate(-target_x, target_y);
                rotate(-radians(180 - gamma * parameter_g));
                line(0, 0, -s2, 0);
                line(0, 0, s2 * cos(radians(60)), s2 * sin(radians(60)));
                grow(level + 1);
                popMatrix();
            }
 
        @Override
        public void mouseMoved () {
            ((Mouse) get_df("Mouse")).push(mouseX, mouseY);
        }
    }
}
