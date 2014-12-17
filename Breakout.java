/* We spent a total of 8 hours on this assignment.
 * 
 * The following are the additional features we added:
 * 1) Letting the user play as many game as they want. User has to press mouse to start the game.
 * 2) Implemented sounds for bricks breaking and ball hitting the paddle. There are two sound clips
 * for bricks breaking that are chosen randomly each time.
 * 3) We also implemented a mute toggle for sound -- key press "M" toggles the sound.
 * 4) Score keeper. When ball hits the bricks, points get added to user's score.
 * Cyan bricks: +10 points. Green bricks: +20 points. Yellow bricks: +30 points. Orange bricks: +40 points. Red bricks: +50 points.
 * 5) Improved user control. Left 1/4 and right 1/4 of the paddle return the ball in the direction it came from.
 * 
 * Comments:
 * Most interesting assignment so far. We like how it is open-ended. 
 * This assignment was a good opportunity for us to implement a whole 
 * range of concepts that we have learnt in class
 * */

import acm.graphics.*;
import acm.program.*;
import acm.util.*;
import java.applet.*;
import java.awt.*;
import java.awt.event.*;

/** An instance is the game breakout. Start it by executing
  Breakout.main(null);
  */
public class Breakout extends GraphicsProgram {
    /** Width of the game display (al coordinates are in pixels) */
    private static final int GAME_WIDTH= 480;
    /** Height of the game display */
    private static final int GAME_HEIGHT= 620;
    
    /** Width of the paddle */
    private static final int PADDLE_WIDTH= 58;
    /** Height of the paddle */
    private static final int PADDLE_HEIGHT= 11;
    /**Distance of the (bottom of the) paddle up from the bottom */
    private static final int PADDLE_OFFSET= 30;
    
    /** Horizontal separation between bricks */
    public static final int BRICK_SEP_H= 5;
    /** Vertical separation between bricks */
    private static final int BRICK_SEP_V= 4;
    /** Height of a brick */
    private static final int BRICK_HEIGHT= 8;
    /** Offset of the top brick row from the top */
    private static final int BRICK_Y_OFFSET= 70;
    
    /** Number of bricks per row */
    public static int BRICKS_IN_ROW= 10;
    /** Number of rows of bricks, in range 1..10. */
    public static int BRICK_ROWS= 10;
    /** Width of a brick */
    public static int BRICK_WIDTH= GAME_WIDTH / BRICKS_IN_ROW - BRICK_SEP_H;
    /** Number of bricks in total */
    public static int BRICKS_TOTAL= 0;
    
    /** Diameter of the ball in pixels */
    private static final int BALL_DIAMETER= 18;
    
    /** Number of turns */
    private static final int NTURNS= 3;
    private static int bremain= NTURNS-1; // number of balls remaining
    
    /** Black paddle with left and right quarter paddles superimposed */
    private static GRect paddle;
    private static GRect paddleL; // left 1/4 of paddle
    private static GRect paddleR; // right 1/4 of paddle
    
    /** Ball */
    private static GOval ball;
    private static double vx, vy; // Horizontal and vertical components of the velocity of the ball (i.e. change in position in each loop iteration)
    
    /** Score */
    private static int score= 0;
    
    /** Sound status */
    private static boolean soundOn= true;
    
    /** Messages to be displayed */
    private static GLabel start= new GLabel("Click anywhere to start game"); // Start screen
    private static GLabel sound= new GLabel("Press M to toggle sound. Sound is currently " + (soundOn==true?"on":"off")); // Sound message
    private static GLabel scorec= new GLabel("Score: 0"); // Score counter
    
    /** rowColors[i] is the color of row i of bricks */
    private static final Color[] rowColors= {Color.red, Color.red, Color.orange, Color.orange,
        Color.yellow, Color.yellow, Color.green, Color.green,
        Color.cyan, Color.cyan};
    
    /** random number generator */
    private RandomGenerator rgen= new RandomGenerator();
    
    /** Sounds to play when the ball hits a brick or the paddle. */
    private static AudioClip bounceClip = MediaTools.loadAudioClip("bounce.au"); // sound clip played when ball hits paddle
    private static AudioClip breakClip1 = MediaTools.loadAudioClip("saucer1.wav"); // sound clip played when ball hits brick
    private static AudioClip breakClip2 = MediaTools.loadAudioClip("saucer2.wav"); // sound clip played when ball hits brick
    
    /** Run the program as an application. If args contains 2 elements that are positive
      integers, then use the first for the number of bricks per row and the second for
      the number of rows of bricks.
      A hint on how main works. The main program creates an instance of
      the class, giving the constructor the width and height of the graphics
      panel. The system then calls method run() to start the computation.
      */
    public static void main(String[] args) {
        fixBricks(args);
        String[] sizeArgs= {"width=" + GAME_WIDTH, "height=" + GAME_HEIGHT};
        new Breakout().start(sizeArgs);
    }
    
    /** If b is null, doesn't have exactly two elements, or the elements are not
      positive integers, DON'T DO ANYTHING.
      If b is non-null, has exactly two elements, and they are positive
      integers with no blanks surrounding them, then:
      Store the first int in BRICKS_IN_ROW, store the second int in BRICK_ROWS,
      and recompute BRICK_WIDTH using the formula given in its declaration.
      */
    public static void fixBricks(String[] b) {
        /** Hint. You have to make sure that the two Strings are positive integers.
          The simplest way to do that is to use the calls Integer.valueOf(b[0]) and
          Integer.valueOf(b[1]) within a try-statement in which the catch block is
          empty. Don't store any values in the static fields UNTIL YOU KNOW THAT
          both array elements are positive integers. */
        try {
            if (b==null || b.length != 2 || Integer.valueOf(b[0]) < 0 || Integer.valueOf(b[1]) < 0) return;
            // b is non-null, has exactly two elements, and they are positive integers
            BRICKS_IN_ROW= Integer.parseInt(b[0]);
            BRICK_ROWS= Integer.parseInt(b[1]);
            BRICK_WIDTH= GAME_WIDTH / BRICKS_IN_ROW - BRICK_SEP_H;
        } catch (NumberFormatException e) {
        }
    }
    
    /** Run the Breakout program. */
    public void run() {
        addKeyListeners(); // listen for key presses
        addMouseListeners(); // listen to mouse movements
        gameSetup(); // set up the game
        gamePlay(); // play the game
    }
    
    /** Restart the Breakout program. */
    public void restart() {
        gameSetup(); // set up the game
        gamePlay(); // play the game
    }
    
    /** Checks the four corners of the ball, one at a time; if one of them collides with the paddle or a brick, 
      * stop the checking immediately and return the object involved in the collision. 
      * Return null if no collision occurred. */
    public GObject getCollidingObject() {
        GObject tl= getElementAt(ball.getX(),ball.getY()); // checks top-left corner of ball
        GObject tr= getElementAt(ball.getX()+BALL_DIAMETER,ball.getY()); // checks top-right corner of ball
        GObject bl= getElementAt(ball.getX(),ball.getY()+BALL_DIAMETER); // checks bottom-left corner of ball
        GObject br= getElementAt(ball.getX()+BALL_DIAMETER,ball.getY()+BALL_DIAMETER); // checks bottom-right corner of ball
        if (tl!=null) return tl;
        if (tr!=null) return tr;
        if (bl!=null) return bl;
        if (br!=null) return br;
        return null;
    }
    
    /**= "ball hits bottom wall" */
    public boolean ballBottom() {
        if (ball.getY()+BALL_DIAMETER>=GAME_HEIGHT) return true;
        return false;
    }
    
    /** Set up the playing board. */
    public void gameSetup() {
        // Add rows of bricks
        for (int j=0; j<BRICK_ROWS; j++){
            // Add bricks in rows
            for(int i=0; i < BRICKS_IN_ROW; i++){
                Brick r= new Brick(BRICK_SEP_H/2 + i*(BRICK_WIDTH + BRICK_SEP_H), BRICK_Y_OFFSET+(j+1)*(BRICK_HEIGHT+BRICK_SEP_V), BRICK_WIDTH, BRICK_HEIGHT);
                r.setColor(rowColors[j%10]);
                r.setFilled(true);
                BRICKS_TOTAL= BRICKS_TOTAL + 1;
                add(r);
            }
        }
        
        // Add paddle
        paddle= new GRect(0, GAME_HEIGHT-PADDLE_OFFSET-PADDLE_HEIGHT, PADDLE_WIDTH, PADDLE_HEIGHT);
        paddle.setColor(Color.black);
        paddle.setFilled(true);
        paddleL= new GRect(0, GAME_HEIGHT-PADDLE_OFFSET-PADDLE_HEIGHT, PADDLE_WIDTH/4, PADDLE_HEIGHT);
        paddleL.setColor(Color.black);
        paddleL.setFilled(true);
        paddleR= new GRect(PADDLE_WIDTH*0.75, GAME_HEIGHT-PADDLE_OFFSET-PADDLE_HEIGHT, PADDLE_WIDTH/4, PADDLE_HEIGHT);
        paddleR.setColor(Color.black);
        paddleR.setFilled(true);
        add(paddle);
        add(paddleL);
        add(paddleR);
        
        // Create ball but not adding it
        ball= new GOval(GAME_WIDTH/2-BALL_DIAMETER/2, GAME_HEIGHT/2-BALL_DIAMETER/2, BALL_DIAMETER, BALL_DIAMETER);
        ball.setColor(Color.black);
        ball.setFilled(true);
        ball.setFillColor(Color.black);
        
        // Add initial score counter
        scorec= new GLabel("Score: 0");
        add(scorec);
        scorec.setLocation((GAME_WIDTH - scorec.getWidth())/2, GAME_HEIGHT - PADDLE_OFFSET/3);
        
        // Initialise ball direction
        vy= 3.0; // initialise vy such that ball heads downwards with initial velocity of +3.0
        vx= rgen.nextDouble(1.0, 3.0); // initialise vx to a random double value in the range of 1.0 to 3.0
        if (!rgen.nextBoolean(0.5)) vx= -vx; // makes vx negative half the time
        
        // Create start screen and add ball after user clicks
        add(start);
        start.setLocation((GAME_WIDTH - start.getWidth())/2, (GAME_HEIGHT - start.getHeight())/2);
        waitForClick();
        remove(start);
        add(ball);
        
        // Add initial sound mute message
        sound.setLocation((GAME_WIDTH - sound.getWidth())/2, BRICK_Y_OFFSET/4);
        add(sound);
    }
    
    /** Play the game */
    public void gamePlay() {
        // game continues as long as total number of bricks is not zero
        while (BRICKS_TOTAL!=0) {
            ball.setLocation(ball.getX()+vx, ball.getY()+vy);
            pause(10);
            
            // Collision behavior with paddle and bricks
            GObject gob= getCollidingObject();
            
            // Collision with paddle
            if (gob==paddle && vy<0);
            if (gob==paddle && vy>0) {
                vy= -vy;
                if (soundOn == true) bounceClip.play();
            }
            if ((gob==paddleL || gob==paddleR) && vy>0) {
                vy= -vy;
                vx= -vx;
                if (soundOn == true) bounceClip.play();
            }
            
            // Collision with bricks
            if (gob instanceof Brick){
                remove(gob);
                remove(scorec);
                BRICKS_TOTAL= BRICKS_TOTAL-1;
                vy= -vy;
                if (gob.getColor() == Color.cyan) { 
                    score= score+10;
                }
                if (gob.getColor() == Color.green) {
                    score= score+20;
                }
                if (gob.getColor() == Color.yellow) { 
                    score= score+30;
                }
                if (gob.getColor() == Color.orange) {
                    score= score+40;
                }
                if (gob.getColor() == Color.red) {
                    score= score+50;
                }
                if (!rgen.nextBoolean(0.5)){
                    if (soundOn == true) breakClip1.play();
                } else {
                    if (soundOn == true) breakClip2.play();
                }
                // Create updated score counter
                scorec= new GLabel("Score: " + score);
                add(scorec);
                scorec.setLocation((GAME_WIDTH - scorec.getWidth())/2, GAME_HEIGHT - PADDLE_OFFSET/3);
            }
            
            // Collision behavior with walls
            if (ball.getY() <= 0) vy= -vy; // reverse y-direction when ball hits top
            if (ball.getX() <= 0) vx= -vx; // reverse x-direction when ball hits left
            if (ball.getY()+BALL_DIAMETER >= GAME_HEIGHT) vy= -vy; // reverse y-direction when ball hits bottom
            if (ball.getX()+BALL_DIAMETER >= GAME_WIDTH) vx= -vx; // reverse x-direction when ball hits right
            
            // Ball hits bottom wall
            if (ballBottom()==true && bremain>0) {
                remove(ball);
                GLabel bottom= new GLabel("Ball lost! " + ((bremain==1)?"1 ball":(bremain + " balls")) + " remaining. New ball coming in 3 seconds.");
                add(bottom);
                bottom.setLocation((GAME_WIDTH - bottom.getWidth())/2, (GAME_HEIGHT - bottom.getHeight())/2);
                pause(3000);
                remove(bottom);
                bremain= bremain-1; // decrease ball counter by 1
                
                // Remove and create ball again, i.e. restart ball
                remove(ball);
                ball= new GOval(GAME_WIDTH/2-BALL_DIAMETER/2, GAME_HEIGHT/2-BALL_DIAMETER/2, BALL_DIAMETER, BALL_DIAMETER);
                ball.setColor(Color.black);
                ball.setFilled(true);
                ball.setFillColor(Color.black);
                add(ball);
                
                // Randomise ball direction again
                vy= 3.0; // initialise vy such that ball heads downwards with initial velocity of +3.0
                vx= rgen.nextDouble(1.0, 3.0); // initialise vx to a random double value in the range of 1.0 to 3.0
                if (!rgen.nextBoolean(0.5)) vx= -vx; // makes vx negative half the time
                
            } else if (ballBottom()==true && bremain==0) {
                GLabel lose= new GLabel("Game over. No balls remaining. Click to start a new game.");
                add(lose);
                lose.setLocation((GAME_WIDTH - lose.getWidth())/2, (GAME_HEIGHT - lose.getHeight())/2);
                ball.setLocation(0,0);
                remove(ball);
                remove(paddle);
                remove(paddleL);
                remove(paddleR);
                waitForClick();
                removeAll();
                add(sound);
                BRICKS_TOTAL= 0;
                score= 0;
                bremain= NTURNS-1;
                restart();
            }
        }
        
        // Ball hits last brick i.e. player wins
        remove(ball);
        remove(paddle);
        remove(paddleL);
        remove(paddleR);
        GLabel win= new GLabel("YOU WIN! Click to start a new game.");
        add(win);
        win.setLocation((GAME_WIDTH - win.getWidth())/2, (GAME_HEIGHT - win.getHeight())/2);
        waitForClick();
        removeAll();
        add(sound);
        BRICKS_TOTAL= 0;
        score= 0;
        bremain= NTURNS-1;
        restart();
    }
    
    /** Move the horizontal middle of the paddle to the x-coordinate of the mouse
      -- but keep the paddle completely on the board.
      Called by the system when the mouse is used. 
      */
    public void mouseMoved(MouseEvent e) {
        GPoint p= new GPoint(e.getPoint());
        // Set x to the left edge of the paddle so that the middle of the paddle
        // is where the mouse is --except that the mouse must stay completely
        // in the pane if the mouse moves past the left or right edge.
        double x1= Math.max(p.getX()-PADDLE_WIDTH/2,0); // determines behavior when mouse exits board on left
        double x2= Math.min(x1, GAME_WIDTH-PADDLE_WIDTH); // determines behavior when mouse exits board on right
        paddle.setLocation(x2, GAME_HEIGHT-PADDLE_OFFSET-PADDLE_HEIGHT);
        paddleL.setLocation(x2, GAME_HEIGHT-PADDLE_OFFSET-PADDLE_HEIGHT);
        paddleR.setLocation(x2 + PADDLE_WIDTH*0.75, GAME_HEIGHT-PADDLE_OFFSET-PADDLE_HEIGHT);
    }
    
    /** Toggle on/off for sound.
      Called by the system when the key 'M' is pressed */
    public void keyPressed(KeyEvent ke) {
        if (ke.getKeyChar() == 'm' ){
            if (soundOn == true) soundOn= false;
            else soundOn= true;
            remove(sound);
            sound= new GLabel("Press M to toggle sound. Sound is currently " + (soundOn==true?"on":"off"));
            sound.setLocation((GAME_WIDTH - sound.getWidth())/2, BRICK_Y_OFFSET/4);
            add(sound);
        }
    }
    
    /** = representation of array b: its elements separated by ", " and delimited by [].
      if b == null, return null. */
    public static String toString(String[] b) {
        if (b == null) return null;
        
        String res= "[";
        // inv res contains "[" + elements of b[0..k-1] separated by ", "
        for (int k= 0; k < b.length; k= k+1) {
            if (k > 0)
                res= res + ", ";
            res= res + b[k];
        }
        return res + "]";
    }
    
}

/** An instance is a Brick */
/*  Note: This program will not compile until you write the two
 constructors correctly, because GRect does not have a 
 constructor with no parameters.  (You know that if a constructor
 does not begin with a call off another constructor, Java inserts
 
 super();
 
 */
class Brick extends GRect {
    
    /** Constructor: a new brick with width w and height h*/
    public Brick(double w, double h) {
        super(w, h);
    }
    
    /** Constructor: a new brick at (x,y) with width w and height h*/
    public Brick(double x, double y, double w, double h) {
        super(x, y, w, h);
    }
}

