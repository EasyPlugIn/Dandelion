import processing.core.PApplet;


@SuppressWarnings("serial")
public class dandelion extends PApplet {
	int LINE_WEIGHT = 1500;
	int WIDTH = 1000;
	int HEIGHT = WIDTH * 2 / 3;
	float SCALE = WIDTH / 900f;
    float UNIT = 3f;					// unit size
    float A = (2.5f + 2 * UNIT);			// short side
    float B = A / sin(radians(45));		// long side
    int ALPHA_MAX = 1000;
    int PARAMETER = 18;
    
    static double scale;
    static double angle;
    

    float posX, posY, nX, nY;
    int delay = 150;
    float r = 1;						// rotate parameter
    float count_r = 30f;				// rotate angle
    float rule;
    float deltaY;						// deltaY represents mouse speed on Y-axis
    Can[] can = new Can[33];

    class Can {
        private boolean isFilled = false;
        private float x = 0f;
        private float y = 0f;
        private float w = 0f;
        private float tempw = 0f;
        private int delta = 1;

        private float alpha = 0f;
        public Can (float x, float y, float w, float alpha) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.alpha = alpha;
        }

        public void de_mouse () {
            if ( overRect(x, y, w, w) ) {
                tempw = tempw + delta;
                if (tempw >=w) {
                    tempw = w;
                    isFilled = true;
                }
            } else if ( (tempw > 0) ) {
                tempw = tempw - delta;
                isFilled = false;
            }
        }

        public void update () {
            tempw = tempw + delta;
            if ( tempw >= w ) {
                tempw = w;
                isFilled = true;
            }
        }

        public void show () {
            noStroke();
            fill(255, 0, 0);
            rect(x, y, tempw, w);
        }

        public boolean isFilled () {
            return this.isFilled;
        }

        public void setFilled () {
            this.isFilled = true;
        }

        public void setUnFilled () {
            this.isFilled = false;
        }

        public float degree () {
            return tempw;
        }
    }
    /***************************/
    /****^^^ class Can ^^^******/
    /***************************/

    private void set_nx_ny (float new_nx, float new_ny) {
        nX = new_nx;
        nY = new_ny;
        println("(nX, nY): (" + nX + "," + nY + ")");
    }


    @Override
    public void setup() {
        smooth();
        size(WIDTH, HEIGHT);
        for(int i = 0; i < can.length; i++) {
            can[i] = new Can(0,0,0,0.0f);
        }
        background(0);
        
        DAI.init();
        
    }
    
    public static void write(double data, String feature) {
    	if(feature.equals("Scale")) {
    	    dandelion.scale = data;
    	}
    	else if(feature.equals("Angle")) {
    	    dandelion.angle = data;
    	}
    	
    }

    @Override
    public void stop() {
        logging("stop");
        DAN.deregister();
    }
    	

    @Override
    public void draw(){
        posX += (nX - posX) / delay;
		deltaY = (nY - posY) / delay;
        posY += deltaY;

        scale(2);

        smooth();
        background(0);
        //frameRate(30);
        stroke(255f, LINE_WEIGHT);
        ///////////////////////////////////////////////////////////////////
        float ro = (posX / (float) width) * 120f;
        count_r = degrees(radians(ro));

        rule = 1.45f-((posY / (float) width) * 100f)/130;

        translate(width/4, height/3.65f);
        line(0,0,0,1000 * SCALE);

        angle_branch(0, PARAMETER);
    }


    void angle_branch (int level, float branch_length) {
    	int local_level = level + 1;
    	if (local_level >= 12) {
    		return;
    	}
//    	if (2 <= local_level && local_level < 16) {
//    		if (can[local_level-2].alpha < ALPHA_MAX) {
//    			can[local_level-2].alpha += 1;
//    		}
//    	}
        float b_x = B * cos(radians(60));
        float b_y = B * sin(radians(60));
        float degree_a = degrees(acos(((branch_length - A) / 2) / B));
        float target_x = branch_length * cos(radians(120 - degree_a));
        float target_y = -branch_length * sin(radians(120 - degree_a));
        float branch_length_vr = branch_length;

        branch_length_vr = branch_length * rule;
        
        float recursive_limit = 35f;
        recursive_limit = 30f;

        if (deltaY < 0 && can[local_level-1].alpha > 0 && branch_length_vr * rule >= recursive_limit) {
        	// deltaY is negative, so this "add" is actually "sub"
        	can[local_level-1].alpha += deltaY;
        } else if (can[local_level-1].alpha < ALPHA_MAX) {
			can[local_level-1].alpha += local_level < 4 ? (5 - local_level) : deltaY;
		}

        if (branch_length_vr < recursive_limit) {
            pushMatrix();
            rotate(radians(-count_r));
//            print(local_level +"|");
            
        	stroke( can[local_level-1].alpha * 0.3f, LINE_WEIGHT);
            line(0, 0, A * SCALE, 0);
            line(0, 0, -b_x * SCALE, -b_y * SCALE);
            line(0, 0, target_x * SCALE, target_y * SCALE);//parameter
            translate(target_x * SCALE, target_y * SCALE);
            rotate(radians(2*degree_a-60));
            line(0, 0, A * SCALE,0);
            line(0, 0, -b_x * SCALE,b_y * SCALE);
            angle_branch(local_level, branch_length_vr);   
            popMatrix();
            pushMatrix();
            rotate(radians(count_r));
            
        	stroke( can[local_level-1].alpha * 0.3f, LINE_WEIGHT);
            line(0, 0, -A * SCALE, 0);
            line(0, 0, b_x * SCALE, -b_y * SCALE);
            line(0, 0, -target_x * SCALE, target_y * SCALE);//parameter
            translate(-target_x * SCALE, target_y * SCALE);
            rotate(-radians(2*degree_a-60));
            line(0, 0, -A * SCALE, 0);
            line(0, 0, b_x * SCALE, b_y * SCALE);
            angle_branch(local_level, branch_length_vr);   
            popMatrix();
        } else {
        	for (int i = local_level; i < can.length; i++) {
    			can[i-1].alpha = 0;
        	}
        }
    }

    @Override
    public void mouseMoved() {
        //if(mouseFlag) {
        set_nx_ny(mouseX, mouseY);
        println("mouse: " + nX + "," + nY);
        //}
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
        PApplet.main(new String[] { "dandelion" });
    }
	
    static private void logging (String message) {
        System.out.println("[Dandelion] " + message);
    }
}
