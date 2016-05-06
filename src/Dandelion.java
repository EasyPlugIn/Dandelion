import java.util.HashMap;

import processing.core.PApplet;
import DAN.DAN;


@SuppressWarnings("serial")
public class Dandelion extends PApplet {
	final int LINE_WEIGHT = 1500;
	final int WIDTH = 1000;
	final int HEIGHT = WIDTH * 2 / 3;
	final float WINDOW_SIZE_SCALE = WIDTH / 900f;
	final float UNIT = 3f;					// unit size
	final float A = (2.5f + 2 * UNIT);			// short side
	final float B = A / sin(radians(45));		// long side
	final int ALPHA_MAX = 1000;
	final int BRANCH_LENGTH = 18;
	final int MAX_LAYER = 10;
	
	final int BACKGROUND_GRAY_LEVEL = 255;
	final int FOREGROUND_GRAY_LEVEL = 0;

    float current_angle, current_scale, current_layer;

    float b_x = B * cos(radians(60));
    float b_y = B * sin(radians(60));
    
    int delay = 150;
    float r = 1;						// rotate parameter
    float count_r = 30f;				// rotate angle
    float rule;
    
    static final String dm_name = Dandelion.class.getSimpleName();
    static final String[] df_list = new String[]{"Scale", "Angle"};
    static final HashMap<String, Float> feature_map = new HashMap<String, Float>();
    
    static public void write (String feature, float para_data) {
        if (!feature_map.containsKey(feature)) {
            // error
            return;
        }
        feature_map.put(feature, para_data);
    }

    private void set_angle_scale (float new_angle, float new_scale) {
        feature_map.put("Scale", new_scale);
        feature_map.put("Angle", new_angle);
        println("(angle, scale): (" + new_angle + "," + new_scale + ")");
    }

    @Override
    public void setup() {
        smooth();
        size(WIDTH, HEIGHT);
        
        for (String df_name: df_list) {
            feature_map.put(df_name, 0f);
        }
        
        DAI.init(dm_name, df_list);
    }

    @Override
    public void stop() {
        logging("stop");
        DAN.deregister();
    }
    	

    @Override
    public void draw(){
        current_angle += (feature_map.get("Angle") - current_angle) / delay; // approximate to target_angle
        current_scale += (feature_map.get("Scale") - current_scale) / delay; // approximate to target_scale
        current_layer = current_scale * MAX_LAYER;

        scale(2);

        smooth();
        background(BACKGROUND_GRAY_LEVEL);
        //frameRate(30);
        stroke(FOREGROUND_GRAY_LEVEL, LINE_WEIGHT);
        ///////////////////////////////////////////////////////////////////
        float ro = current_angle * 120f;
        count_r = degrees(radians(ro));

        rule = 1.45f - ((current_scale * HEIGHT) / (float)WIDTH * 100f) / 130;

        translate(width/4, height/3.65f);
        line(0,0,0,1000 * WINDOW_SIZE_SCALE);

        angle_branch(0);
    }


    void angle_branch (int level) {
    	if (level >= MAX_LAYER || level > current_layer) {
    		return;
    	}
    	
        float parameter = ((float)level / MAX_LAYER) / 2 + 1;
        float degree_a = degrees(acos(((BRANCH_LENGTH * parameter - A) / 2) / B));
        float target_x = BRANCH_LENGTH * cos(radians(120 - degree_a)) * parameter;
        float target_y = -BRANCH_LENGTH * sin(radians(120 - degree_a)) * parameter;
        float alpha_rate = current_layer - (int)current_layer;
        float alpha = (FOREGROUND_GRAY_LEVEL - BACKGROUND_GRAY_LEVEL) * alpha_rate + BACKGROUND_GRAY_LEVEL;
        float line_gray_level = (level + 1 > current_layer) ? alpha : FOREGROUND_GRAY_LEVEL;

        pushMatrix();
        rotate(radians(-count_r));
    	stroke(line_gray_level, LINE_WEIGHT);
        line(0, 0, A * WINDOW_SIZE_SCALE, 0);
        line(0, 0, -b_x * WINDOW_SIZE_SCALE, -b_y * WINDOW_SIZE_SCALE);
        line(0, 0, target_x * WINDOW_SIZE_SCALE, target_y * WINDOW_SIZE_SCALE);
        translate(target_x * WINDOW_SIZE_SCALE, target_y * WINDOW_SIZE_SCALE);
        rotate(radians(2 * degree_a - 60));
        line(0, 0, A * WINDOW_SIZE_SCALE,0);
        line(0, 0, -b_x * WINDOW_SIZE_SCALE, b_y * WINDOW_SIZE_SCALE);
        angle_branch(level + 1); 
        popMatrix();
        
        pushMatrix();
        rotate(radians(count_r));
        stroke(line_gray_level, LINE_WEIGHT);
        line(0, 0, -A * WINDOW_SIZE_SCALE, 0);
        line(0, 0, b_x * WINDOW_SIZE_SCALE, -b_y * WINDOW_SIZE_SCALE);
        line(0, 0, -target_x * WINDOW_SIZE_SCALE, target_y * WINDOW_SIZE_SCALE);
        translate(-target_x * WINDOW_SIZE_SCALE, target_y * WINDOW_SIZE_SCALE);
        rotate(-radians(2*degree_a-60));
        line(0, 0, -A * WINDOW_SIZE_SCALE, 0);
        line(0, 0, b_x * WINDOW_SIZE_SCALE, b_y * WINDOW_SIZE_SCALE);
        angle_branch(level + 1);
        popMatrix();
    }

    @Override
    public void mouseMoved() {
        set_angle_scale((float)mouseX / (float)WIDTH, (float)mouseY / (float)HEIGHT);
    }

    @Override
    public void keyPressed() {
        // Finish the movie if space bar is pressed!
        if (key == ' ' ) {
            println( "finishing movie" );
            // Do not forget to finish the movie! Otherwise, it will not play properly.
        }
    }
    
    boolean overRect(float x, float y, float width, float height) {
        if (mouseX >= x && mouseX <= x+width &&
                mouseY >= y && mouseY <= y+height) {
            return true;
        } else {
            return false;
        }
    }

    public static void main(String[] args) {
        PApplet.main(new String[] {dm_name});
    }
	
    static private void logging (String message) {
        System.out.println("[Dandelion] " + message);
    }
}
