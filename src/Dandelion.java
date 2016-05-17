import org.json.JSONArray;

import processing.core.PApplet;


@SuppressWarnings("serial")
public class Dandelion extends PApplet implements IDAAPI {
    /* ********************* */
    /* IDAAPI implementation */
    /* ********************* */
    @Override
    public void search() {}

    @Override
    public void connect(String id) {}

    @Override
    public void write(String odf, JSONArray data) {
        logging("write: "+ odf +", "+ data.toString());
        if(odf.equals("Angle")) {
            target_angle = (float) data.getDouble(0);
        } else if(odf.equals("Size")) {
            target_size = (float) data.getDouble(0);
        } else {
            handle_error("Feature '"+ odf +"' not found");
        }
    }

    @Override
    public void disconnect() {}

    @Override
    public void subscribe(String[] idf_list, IDFHandler idf_handler) {}

    @Override
    public void unsubscribe(IDFHandler idf_handler) {}
    
    
    /* ****************** */
    /* IDA implementation */
    /* ****************** */
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

    float target_angle, target_size;
    float current_angle, current_size, current_layer;

    float b_x = B * cos(radians(60));
    float b_y = B * sin(radians(60));
    
    int delay = 150;
    float r = 1;						// rotate parameter
    float count_r = 30f;				// rotate angle
    float rule;

    @Override
    public void setup() {
        smooth();
        size(WIDTH, HEIGHT);

        target_angle = 0f;
        target_size = 0f;
        
        current_angle = 0f;
        current_size = 0f;
        
        DAI.init(this);
    }

    @Override
    public void draw(){
        current_angle += (target_angle - current_angle) / delay;
        current_size += (target_size - current_size) / delay;
        current_layer = current_size * MAX_LAYER;

        scale(2);

        smooth();
        background(BACKGROUND_GRAY_LEVEL);
        //frameRate(30);
        stroke(FOREGROUND_GRAY_LEVEL, LINE_WEIGHT);
        ///////////////////////////////////////////////////////////////////
        float ro = current_angle * 120f;
        count_r = degrees(radians(ro));

        rule = 1.45f - ((current_size * HEIGHT) / (float)WIDTH * 100f) / 130;

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

    public static void main(String[] args) {
        PApplet.main(new String[] {"Dandelion"});
    }
	
    static private void logging (String message) {
        System.out.println("[Dandelion] " + message);
    }
    
    static private void handle_error(String message) {
        logging(message);
    }
}
