import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONObject;

import processing.core.PApplet;
@SuppressWarnings("serial")
public class DAI implements DAN.DAN2DAI {
	static DAI dai = new DAI();
	static IDA ida = new IDA();
    static DAN dan = new DAN();
	
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
    static final String config_filename = "config.txt";
    static String d_name = "";

    
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
        
        String endpoint = get_config_ec();
        if (!endpoint.startsWith("http://")) {
            endpoint = "http://" + endpoint;
        }
        if (endpoint.length() - endpoint.replace(":", "").length() == 1) {
            endpoint += ":9999";
        }
        
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
        		ida.size = data.getDouble(0);
        	}
        	else {
                ida.size = 0.0; // default value
        	}
        }
    }
    static class Angle extends ODF {
        public Angle () {
            super("Angle");
        }
        public void pull(JSONArray data) {
        	if(selected && !suspended) {
        	    ida.angle = data.getDouble(0);
        	}
        	else {
                ida.angle = 0.0;
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
    
    /* IDA Class */
    public static class IDA extends PApplet{
    	public double size = 0.0;
    	public double angle = 0.0;
    	public double color_r = 0;
    	public double color_g = 0;
    	public double color_b = 0;
    	
    	public void iot_app() {
    		PApplet.runSketch(new String[]{d_name}, this);
    	};
    	
    	public double approximate (double source, double target) {
    	    return source + (target - source) / delay;
    	}

        int TEXT_SIZE = 15;
    	int text_lines;
    	public void stack_text (String format, Object... args) {
    	    if (format.equals("")) {
    	        text_lines = 0;
    	    }
    	    text(String.format(format, args), 0, HEIGHT - text_lines * TEXT_SIZE);
    	    text_lines += 1;
    	}
    	
        final int WIDTH = 1920;
        final int HEIGHT = 1080;
        final float s1 = WIDTH * 0.012f;            // short side
        final float s2 = s1 / sin(radians(45));     // long side
        final float stalk_len = s1 * 2;
        final float b_x = s2 * cos(radians(60));
        final float b_y = s2 * sin(radians(60));
        final float BACKGROUND_GRAY_LEVEL = 255f;
        final float FOREGROUND_GRAY_LEVEL = 0f;
        final double delay = 100;
        double current_angle, current_size, current_layer;
        double current_color_r, current_color_g, current_color_b;
        float r = 1;                      // rotate parameter
        float count_r = 30f;              // rotate angle
        @Override
        public void setup() {
            smooth();
            size(WIDTH, HEIGHT);
        }
        @Override
        public void draw(){
            if (size > 1) {
                size = 1;
            }
            if (angle > 1) {
                angle = 1;
            }
            current_size = approximate(current_size, size);
            current_angle = approximate(current_angle, angle);
            current_color_r = approximate(current_color_r, color_r);
            current_color_g = approximate(current_color_g, color_g);
            current_color_b = approximate(current_color_b, color_b);
            current_layer = current_size * 10;                   //10 is max_layer
            strokeWeight(1.4f);
            background(BACKGROUND_GRAY_LEVEL);
            
            textSize(TEXT_SIZE);
            stack_text("");
            stack_text("Color: (%.2f, %.2f, %.2f) (%.2f, %.2f, %.2f)", color_r, color_g, color_b, current_color_r, current_color_g, current_color_b);
            stack_text("ODF Angle: %f (%f)", angle, current_angle);
            stack_text("ODF Size: %f (%f)", size, current_size);
            stack_text("Device name: %s", d_name);
            fill(0);
            
            stroke((float)current_color_r, (float)current_color_g, (float)current_color_b, 255);
            float ro = (float)current_angle * 120f;
            count_r = degrees(radians(ro));
            translate(width / 2, height * 5 / 8);
            line(0, 0, 0, height * 3 / 8);
            angle_branch(0);
        }
        void angle_branch (int level) {
            if (level >= 10 || level >= current_layer) {
                return;
            }
            float parameter = ((float)level / 20) + 1;
            float degree_a = degrees(acos(((stalk_len * parameter - s1) / 2) / s2));
            float target_x = stalk_len * cos(radians(120 - degree_a)) * parameter;
            float target_y = -stalk_len * sin(radians(120 - degree_a)) * parameter;
            float alpha;
            if (level + 1 > current_layer) { // outest layer
                alpha = (float)(current_layer - (int)current_layer) * 255;
            } else {
                alpha = 255;
            }
            pushMatrix();
            rotate(radians(-count_r));
            stroke((float)current_color_r, (float)current_color_g, (float)current_color_b, alpha);
            line(0, 0, s1, 0);
            line(0, 0, -b_x, -b_y);
            line(0, 0, target_x, target_y);
            translate(target_x, target_y);
            rotate(radians(2 * degree_a - 60));
            line(0, 0, s1, 0);
            line(0, 0, -b_x, b_y);
            angle_branch(level + 1); 
            popMatrix();
            pushMatrix();
            rotate(radians(count_r));
            stroke((float)current_color_r, (float)current_color_g, (float)current_color_b, alpha);
            line(0, 0, -s1, 0);
            line(0, 0, b_x, -b_y);
            line(0, 0, -target_x, target_y);
            translate(-target_x, target_y);
            rotate(-radians(2*degree_a-60));
            line(0, 0, -s1, 0);
            line(0, 0, b_x, b_y);
            angle_branch(level + 1);
            popMatrix();
        }
        @Override
        public void mouseMoved () {
            ((Mouse) get_df("Mouse")).push(mouseX, mouseY);
        }
    }
}
