package br.unb.unbiquitous.ubiquitos.runFast.minigames.breakmg;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import br.unb.unbiquitous.ubiquitos.runFast.mid.MidManager;
import br.unb.unbiquitous.ubiquitos.runFast.minigames.general.SoundEffect;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;

/**
 * Break game surface. Controls the game actions and implements the game view surface.
 *
 */
public class BreakGameSurface extends SurfaceView implements Runnable, OnTouchListener{

	//Game delays
	private static final int DELAY = 33;
	private static final int RANDOM_DELAY = 500;
	
	//Game bounds
	private static final int BOTTOM_LINE = 500;
	private static final int RIGHT_LINE = 400;
	
	//Game score bounds
	private static final int SCORE_MAX = 240;
	private static final int SCORE_LEFT_LINE = 60;
	private static final int SCORE_MAX_GROW_UP = 365;
	
	
	private SurfaceHolder holder;
	private Thread thread = null;
	private boolean isRunning = false;
	
	private int score,scoreState,scoreMax;
	private Rect scoreRect, scoreBackRect;
	private Paint paintScore, paintBackScore;
	private Bitmap[] brokenCar;
	
	private BreakLoadManager loadManager = null;
	private List<BreakAnimatedObject> animatedObjects;
	
	private Random random = null;
	private int randomTime;
	
	private SoundEffect sound;
	
	public BreakGameSurface(Context context,Resources resources,int helpNumber) {
		super(context);
		holder = getHolder();
		
		loadManager = new BreakLoadManager(resources);
		animatedObjects = new ArrayList<BreakAnimatedObject>();
			
		initScores(helpNumber);
		initPaints();
		
		random = new Random();
		randomTime = 0;
		
		sound = new SoundEffect(context);
		
		this.setOnTouchListener(this);
	}
	
	/**
	 * Initialize the game needed score to finish it.
	 * @param helpNumber
	 */
	private void initScores(int helpNumber){
		scoreMax = SCORE_MAX - helpNumber*20;
		if(scoreMax<80)
			scoreMax = 80 - helpNumber;
		score= 0;
		scoreBackRect = new Rect(50,680,430,700);
		scoreRect = new Rect(55,685,60,695);
		
		scoreState = 0;
		brokenCar = loadManager.getBrokenCar();
	}
	
	private void initPaints(){
		paintScore = new Paint();
		paintScore.setColor(Color.YELLOW);
		paintScore.setTextSize(35);
		
		paintBackScore = new Paint();
		paintBackScore.setColor(Color.WHITE);
		paintBackScore.setTextSize(35);
	}

	/**
	 * Pause the game.
	 */
	public void pause(){
		isRunning = false;
		try {
			thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		thread = null;
	}
	
	/**
	 * Resume the game.
	 */
	public void resume(){
		isRunning = true;
		thread = new Thread(this);
		thread.start();
	}
	
	@Override
	public void run() {
		int frameStart, dt, sleep;
		dt = DELAY;
		
		while(isRunning){
			frameStart = (int)System.currentTimeMillis();
			
			/*UPDATE*/
			update(dt);
			
			/*RENDER*/
			render();
			
			/*Delay*/
            dt = (int)System.currentTimeMillis() - frameStart;
            sleep = DELAY - dt;

            if (sleep < 0)
                sleep = 2;
            if(dt<DELAY){
            	try {
            		Thread.sleep(sleep);
            	} catch (InterruptedException e) {
            		System.out.println("interrupted");
            	}
            	dt = DELAY;
            }
		}
	}

	/**
	 * Updates the game objects.
	 * @param dt
	 */
	private void update(int dt){
		generator(dt);
		verifyValidations();
		
		int i;
		for(i=0; i<animatedObjects.size(); ++i)
			animatedObjects.get(i).update(dt);
		
		if(score >= scoreMax)
			MidManager.endMGBreak(MidManager.getGameDevice());
			//pause();
	}
	
	/**
	 * Generates objects through the screen.
	 * @param dt
	 */
	private void generator(int dt){
		randomTime += dt;
		if(randomTime > RANDOM_DELAY){
			randomTime = 0;
			
			animatedObjects.add(new BreakAnimatedObject(Math.abs(random.nextInt()%RIGHT_LINE),
					Math.abs(random.nextInt()%BOTTOM_LINE), random, loadManager));
			
		}
	}
	
	/**
	 * Verify if some object must be removed.
	 */
	private void verifyValidations(){
		int i;
		for(i=0; i<animatedObjects.size(); ++i){
			if((animatedObjects.get(i).getY() > BOTTOM_LINE)||(animatedObjects.get(i).isDone())){
				animatedObjects.remove(i);
				--i;
			}
		}
	}
	
	/**
	 * Renders all game objects and informations.
	 */
	private void render(){
		int i;
		if(holder.getSurface().isValid()){
			Canvas canvas = holder.lockCanvas();
			canvas.drawRGB(70,80,90);
			
			for(i=0; i<animatedObjects.size(); ++i)
				animatedObjects.get(i).render(canvas);
			
			//canvas.drawText(""+score, 350, 700, paintScore);
			canvas.drawRect(scoreBackRect, paintBackScore);
			canvas.drawRect(scoreRect, paintScore);
			canvas.drawBitmap(brokenCar[scoreState],190,617,null);
			
			holder.unlockCanvasAndPost(canvas);
		}
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if(event.getAction() != MotionEvent.ACTION_DOWN)
			return false;

		for(int i=0; i<animatedObjects.size(); ++i){
		
			if(animatedObjects.get(i).isTouching(event.getX(), event.getY())){	
				score += animatedObjects.get(i).getScore();
				
				if(animatedObjects.get(i).getScore() > 0)
					sound.playCoin();
				else
					sound.playExplosion();
				
				if(score<0)
					score = 0;
				
				scoreRect.right = (score*SCORE_MAX_GROW_UP)/scoreMax + SCORE_LEFT_LINE;
				scoreState = (score*(brokenCar.length-1))/scoreMax;
				
				animatedObjects.get(i).killAnimatedObject();
			}
		}
		
		return true;

	}
}

