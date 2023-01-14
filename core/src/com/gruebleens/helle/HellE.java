package com.gruebleens.helle;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import com.badlogic.gdx.utils.GdxRuntimeException;

public class HellE extends ApplicationAdapter {
	
	private static final Boolean debug = true;

	static final int WND_WIDTH       = 800;
	static final int WND_HEIGHT      = 480;
	static final int WND_HALF_WIDTH  = 400;
	static final int WND_HALF_HEIGHT = 240;

	static final int GAME_INIT   = 0;
	static final int GAME_ACTION = 1;
	static final int GAME_OVER   = 2;	

	private static final int     TOUCH_IMPULSE = 500; 
	private static final float   TAP_DRAW_TIME_MAX = 1.0f;
	private static final Vector2 DAMPING = new Vector2(0.99f, 0.99f);
	private static final int     MISSILE_SPEED = 60;

	int   gameState = GAME_INIT;
	float deltaPosition;
	float terrainOffset;
	float planeAnimationTime;
	
	TextureRegion bgRegion;
	TextureRegion gameOver;
	TextureRegion terrainAbove;
	TextureRegion terrainBelow;

	float tapDrawTime;
	TextureRegion tap1;
	TextureRegion tap2;
	TextureRegion pillarUp;
	TextureRegion pillarDown;

	Vector3 touchPosition = new Vector3();
	
	TextureAtlas atlas;

	Animation<TextureRegion> plane;
	Array<Vector2> pillars = new Array<Vector2>();
	Vector2 lastPillarPosition   = new Vector2();

	Array<TextureAtlas.AtlasRegion> MissileTextures = new Array<TextureAtlas.AtlasRegion>();
	TextureRegion selectedMissileTexture;
	boolean isMissileInScene;
	float nextMissileIn;
	Vector2 missilePosition = new Vector2();
	Vector2 missileVelocity = new Vector2();

	Rectangle obstacleRect = new Rectangle();
	Rectangle planeRect    = new Rectangle();

	Vector2 gravity              = new Vector2();
	Vector2 planeVelocity        = new Vector2();
	Vector2 planePosition        = new Vector2();
	Vector2 planeDefaultPosition = new Vector2();

	Vector2 scrollVelocity = new Vector2();
	Vector2 tmp            = new Vector2();

	SpriteBatch        batch;
	OrthographicCamera camera;
	Viewport           viewport;
	FPSLogger          fpsLogger;

	Music music;
	Sound tapSound;
	Sound crashSound;
	Sound spawnSound;

	
	@Override
	public void create () {
		fpsLogger = new FPSLogger();
		batch     = new SpriteBatch();
		camera    = new OrthographicCamera();
		// camera.setToOrtho(false, 800, 480);
		camera.position.set(WND_HALF_WIDTH, WND_HALF_HEIGHT, 0);
		viewport = new FitViewport(WND_WIDTH, WND_HEIGHT, camera);

		atlas = new TextureAtlas(Gdx.files.internal("packerout/helle.pack"));

		bgRegion     = atlas.findRegion("background");
		terrainBelow = atlas.findRegion("ground2");
		terrainAbove = new TextureRegion(terrainBelow);
		terrainAbove.flip(true, true);
		gameOver     = atlas.findRegion("gameover");

		plane = new Animation(0.05f,
			atlas.findRegion("helli1"),
			atlas.findRegion("helli2"),
			atlas.findRegion("helli3"),
			atlas.findRegion("helli4")			
		); 
		plane.setPlayMode(PlayMode.LOOP);

		tap1 = atlas.findRegion("tap1");
		tap2 = atlas.findRegion("tap2");

		pillarUp   = atlas.findRegion("tower-up");
		pillarDown = atlas.findRegion("ice-tower-down");

		MissileTextures.add(atlas.findRegion("missile-tiny"));
		MissileTextures.add(atlas.findRegion("missile-medium"));
		MissileTextures.add(atlas.findRegion("missile-large"));

		music = Gdx.audio.newMusic(Gdx.files.internal("sound/journey.mp3"));
		music.setLooping(true);
		music.play();

		tapSound   = Gdx.audio.newSound(Gdx.files.internal("sound/pop.ogg"));
		crashSound = Gdx.audio.newSound(Gdx.files.internal("sound/crash.ogg"));
		spawnSound = Gdx.audio.newSound(Gdx.files.internal("sound/alarm.ogg"));

		_resetScene();
	}

	@Override
	public void render () {	
		Gdx.gl.glClearColor(1, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		fpsLogger.log();

		_updateScene();
		_drawScene();
	}

	private void _resetScene() {
		tapDrawTime   = 0;

		terrainOffset = 0;
		gravity.set(0, -2);
		scrollVelocity.set(4, 0);

		planeAnimationTime = 0;
		planeVelocity.set(100, 0);		
		planeDefaultPosition.set(200 - 44, WND_HALF_HEIGHT - 73 / 2);
		planePosition.set(planeDefaultPosition.x, planeDefaultPosition.y);
	
		pillars.clear();
		_addPillar();

		isMissileInScene = false;
		nextMissileIn = (float)(Math.random() * MissileTextures.size);
	}

	private void _updateScene() {
		if(Gdx.input.justTouched()) {
			tapSound.play();

			if(gameState == GAME_INIT) { gameState = GAME_ACTION; return; }
			if(gameState == GAME_OVER) {
				gameState = GAME_INIT;
				_resetScene();
				return;
			}

			touchPosition.set(Gdx.input.getX(), Gdx.input.getY(), 0);
			camera.unproject(touchPosition);
			tmp.set(planePosition.x, planePosition.y);
			tmp.add(touchPosition.x, touchPosition.y).nor(); // sub
			planeVelocity.mulAdd(tmp,
				TOUCH_IMPULSE - MathUtils.clamp(
					Vector2.dst(touchPosition.x,
							    touchPosition.y,
								planePosition.x,
								planePosition.y),
				0,
			    TOUCH_IMPULSE)
			);
			tapDrawTime = TAP_DRAW_TIME_MAX;
		}

		if(gameState == GAME_INIT || gameState == GAME_OVER) return;

		float dT = Gdx.graphics.getDeltaTime();

		planeAnimationTime += dT;
		planeVelocity.scl(DAMPING);
		planeVelocity.add(gravity);
		planeVelocity.add(scrollVelocity);
		planePosition.mulAdd(planeVelocity, dT);

		deltaPosition = planePosition.x - planeDefaultPosition.x;
		terrainOffset -= deltaPosition;
		planePosition.x = planeDefaultPosition.x;
		planeRect.set(planePosition.x, planePosition.y, 160, 45);

		if(terrainOffset * -1 > terrainBelow.getRegionWidth())
			terrainOffset = 0;

		if(terrainOffset > 0)
			terrainOffset = -terrainBelow.getRegionWidth();

		// Pillars
		for(Vector2 vec: pillars) {
			vec.x -= deltaPosition;
			if(vec.x + pillarUp.getRegionWidth() < -10)
				pillars.removeValue(vec, false);
			if(vec.y == 1)
				obstacleRect.set(vec.x+10, 0, pillarUp.getRegionWidth()-20, pillarUp.getRegionHeight()-10);
			else
				obstacleRect.set(vec.x+10, WND_HEIGHT-pillarDown.getRegionHeight()+10,
									pillarDown.getRegionWidth()-20, pillarDown.getRegionHeight());

			if(planeRect.overlaps(obstacleRect)) 
				if(gameState != GAME_OVER){
					crashSound.play();
					gameState = GAME_OVER;
				}	 
		}
		if(lastPillarPosition.x < 400)
			_addPillar();

		// Missiles
		if(isMissileInScene) {
			missilePosition.mulAdd(missileVelocity, dT);
			missilePosition.x -= deltaPosition;
			if(missilePosition.x < -10)
				isMissileInScene = false;

			obstacleRect.set(missilePosition.x+2, missilePosition.y+2, 
				selectedMissileTexture.getRegionWidth()-4, selectedMissileTexture.getRegionHeight()-4);

			if(planeRect.overlaps(obstacleRect))
				if(gameState != GAME_OVER) {
					crashSound.play();
					gameState = GAME_OVER;
				}	
		}

		nextMissileIn -= dT;
		if(nextMissileIn <= 0)
			_launchMissile();

		if(planePosition.y < terrainBelow.getRegionHeight() - 60 ||
		   planePosition.y + 65 > WND_HEIGHT - terrainAbove.getRegionHeight() + 35)
			if(gameState != GAME_OVER) {
				crashSound.play();
				gameState = GAME_OVER;		     
			}	

		tapDrawTime -= dT;

	}

	private void _drawScene() {
		camera.update();
		batch.setProjectionMatrix(camera.combined);
		batch.begin();

		batch.disableBlending();
		batch.draw(bgRegion, 0, 0);

		batch.enableBlending();
		batch.draw(terrainBelow, terrainOffset, 0);
		batch.draw(terrainBelow, terrainOffset + terrainBelow.getRegionWidth(), 0);
		batch.draw(terrainAbove, terrainOffset, WND_HEIGHT - terrainAbove.getRegionHeight());
		batch.draw(terrainAbove, terrainOffset + terrainAbove.getRegionWidth(), WND_HEIGHT - terrainAbove.getRegionHeight());

		// Pillars
		for(Vector2 vec: pillars) {
			if(vec.y == 1)
				batch.draw(pillarUp, vec.x, 0, 50, 180);
			else
				batch.draw(pillarDown, vec.x, WND_HEIGHT - pillarDown.getRegionHeight(), 50, 180);
		}

		// Missiles 
		if(isMissileInScene)
			batch.draw(selectedMissileTexture, missilePosition.x, missilePosition.y);

		if(tapDrawTime > 0) // 29.5 - is a half width/height of image
			batch.draw(tap2, touchPosition.x - 29.5f, touchPosition.y - 29.5f);

		if(gameState == GAME_INIT)
			batch.draw(tap1, planePosition.x, planePosition.y - 80);

		if(gameState == GAME_OVER)
			batch.draw(gameOver, WND_HALF_WIDTH - 200, WND_HALF_HEIGHT - 10);	

		batch.draw(plane.getKeyFrame(planeAnimationTime), planePosition.x, planePosition.y, 160, 45);

		batch.end();
	}
	
	private void _addPillar() {
		Vector2 pillarPosition = new Vector2();
		
		pillarPosition.x = (pillars.size == 0) 
			? (float)(WND_WIDTH + Math.random() * 600) 
			: lastPillarPosition.x + (float)(600 + Math.random() * 600);


		pillarPosition.y = (MathUtils.randomBoolean()) ? 1 : -1;

		lastPillarPosition = pillarPosition;
		pillars.add(pillarPosition);

	}

	private void _launchMissile() {
		spawnSound.play();

		nextMissileIn = 1.5f + (float)(Math.random() * MissileTextures.size);

		if(isMissileInScene) return;
		isMissileInScene = true;

		int id = (int)(Math.random() * MissileTextures.size);
		selectedMissileTexture = MissileTextures.get(id);
		missilePosition.x = 810;
		missilePosition.y = (float)(80 + Math.random() * 320);

		Vector2 destination = new Vector2();
		destination.x = -10;
		destination.y = (float)(80 + Math.random() * 320);
		destination.sub(missilePosition).nor();

		missileVelocity.mulAdd(destination, MISSILE_SPEED);
	}


	@Override
	public void resize(int width, int height) {
		viewport.update(width, height);
	}

	@Override
	public void dispose () {
		pillars.clear();
		batch.dispose();
		atlas.dispose();
		MissileTextures.clear();

		tapSound.dispose();
		crashSound.dispose();
		spawnSound.dispose();
		music.dispose();
	}
}