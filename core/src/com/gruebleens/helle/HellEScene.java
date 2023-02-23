package com.gruebleens.helle;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;


public class HellEScene extends ScreenAdapter {

	static final int GAME_INIT   = 0;
	static final int GAME_ACTION = 1;
	static final int GAME_OVER   = 2;	

	private static final int     TOUCH_IMPULSE = 500; 
	private static final float   TAP_DRAW_TIME_MAX = 1.0f;
	private static final Vector2 DAMPING = new Vector2(0.99f, 0.99f);
	private static final int     MISSILE_SPEED = 60;

	private static final int     PILLAR_WIDTH  = 30;
	private static final int     PILLAR_HEIGHT = 140;	

	HellE game;
	
	int   gameState = GAME_INIT;
	float deltaPosition;
	float terrainOffset;
	float tapDrawTime;

	float score;

	TextureAtlas       atlas;
	SpriteBatch        batch;
	OrthographicCamera camera;

	TextureRegion tap1;
	TextureRegion tap2;
	Vector3       touchPosition = new Vector3();	
	Rectangle     obstacleRect  = new Rectangle();

	TextureRegion bgRegion;
	TextureRegion terrainAbove;
	TextureRegion terrainBelow;
	TextureRegion gameOver;
	
	// Plane
	float planeAnimationTime;
	Animation<TextureRegion> plane;
	Rectangle planeRect            = new Rectangle();
	Vector2   gravity              = new Vector2();
	Vector2   planeVelocity        = new Vector2();
	Vector2   planePosition        = new Vector2();
	Vector2   planeDefaultPosition = new Vector2();

	Vector2 scrollVelocity = new Vector2();
	Vector2 tmp            = new Vector2();

	int   fuelPercentage;
	float starCounter;
	float fuelCounter;
	float shieldCounter;

	// Pillars
	TextureRegion  pillarUp;
	TextureRegion  pillarDown;
	Array<Vector2> pillars = new Array<Vector2>();
	Vector2        lastPillarPosition = new Vector2();	

	// Missiles
	boolean isMissileInScene;
	float nextMissileIn;	
	Array<TextureAtlas.AtlasRegion> MissileTextures = new Array<TextureAtlas.AtlasRegion>();
	TextureRegion selectedMissileTexture;
	Vector2 missilePosition = new Vector2();
	Vector2 missileVelocity = new Vector2();

	// Pickups
	Array<Pickup> pickupInScene = new Array<Pickup>();
	Pickup        tmpPickup;
	Vector3       pickupTiming = new Vector3();		

	// Effects
	ParticleEffect smoke;
	ParticleEffect explosion;

	BitmapFont font;

	Music music;
	Sound tapSound;
	Sound crashSound;
	Sound spawnSound;


	public HellEScene(HellE hellEGame) {
		game   = hellEGame;
		batch  = game.batch;
		camera = game.camera;
		atlas  = game.atlas;
		font   = game.font;

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

		// ToDo: Add fuel indicator
		// fuelIndicator = game.manager.get("life.png", Texture.class);

		smoke = game.manager.get("Smoke", ParticleEffect.class);
		explosion = game.manager.get("Explosion", ParticleEffect.class);

		music = Gdx.audio.newMusic(Gdx.files.internal("sound/journey.mp3"));
		music.setLooping(true);
		music.play();

		tapSound   = Gdx.audio.newSound(Gdx.files.internal("sound/pop.ogg"));
		crashSound = Gdx.audio.newSound(Gdx.files.internal("sound/crash.ogg"));
		spawnSound = Gdx.audio.newSound(Gdx.files.internal("sound/alarm.ogg"));

		_resetScene();
	}

	private void _resetScene() {
		tapDrawTime   = 0;
		terrainOffset = 0;
		gravity.set(0, -2);
		scrollVelocity.set(4, 0);

		planeAnimationTime = 0;
		planeVelocity.set(100, 0);		
		planeDefaultPosition.set(200 - 44, game.WND_HALF_HEIGHT - 73 / 2);
		planePosition.set(planeDefaultPosition.x, planeDefaultPosition.y);

		score = starCounter = 0;
		shieldCounter  = 15;
		fuelCounter    = 100;
		fuelPercentage = 114; 
	
		pillars.clear();
		_addPillar();

		isMissileInScene = false;
		nextMissileIn = (float)(Math.random() * MissileTextures.size);

		pickupTiming.x = 1 + (float)Math.random() * 2;
		pickupTiming.y = 3 + (float)Math.random() * 2;
		pickupTiming.z = 1 + (float)Math.random() * 3;
		pickupInScene.clear();

		smoke.setPosition(planePosition.x+20, planePosition.y+30);
	}

	private void _updateScene(float dT) {
		if(Gdx.input.justTouched()) {
			tapSound.play();

			if(gameState == GAME_INIT) { gameState = GAME_ACTION; return; }
			if(gameState == GAME_OVER) {
				gameState = GAME_INIT;
				_resetScene();
				return;
			}

			if(fuelCounter > 0) {
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
		} 

		if(gameState == GAME_INIT || gameState == GAME_OVER) {
			if(gameState == GAME_OVER) 
				explosion.update(dT);

			return;
		}	

		planeAnimationTime += dT;
		planeVelocity.scl(DAMPING);
		planeVelocity.add(gravity);
		planeVelocity.add(scrollVelocity);
		planePosition.mulAdd(planeVelocity, dT);

		deltaPosition = planePosition.x - planeDefaultPosition.x;
		terrainOffset -= deltaPosition;
		planePosition.x = planeDefaultPosition.x;
		planeRect.set(planePosition.x, planePosition.y, 160, 45);

		fuelCounter -= 6 * dT;
		fuelPercentage = (int)(114 * fuelCounter / 100);
		shieldCounter -= dT; // Check
		score += dT;

		smoke.setPosition(planePosition.x+20, planePosition.y+30);
		smoke.update(dT);

		if(terrainOffset * -1 > terrainBelow.getRegionWidth())
			terrainOffset = 0;

		if(terrainOffset > 0)
			terrainOffset = -terrainBelow.getRegionWidth();

		// Pillars
		for(Vector2 vec : pillars) {
			vec.x -= deltaPosition;
			if(vec.x + PILLAR_WIDTH < -10)
				pillars.removeValue(vec, false);
			if(vec.y == 1)
				obstacleRect.set(vec.x+20, 0, PILLAR_WIDTH, PILLAR_HEIGHT);
			else
				obstacleRect.set(vec.x+20, game.WND_HEIGHT-PILLAR_HEIGHT, PILLAR_WIDTH, PILLAR_HEIGHT);

			if(planeRect.overlaps(obstacleRect)) 
				_endGame();	 
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
				_endGame();
		}

		nextMissileIn -= dT;
		if(nextMissileIn <= 0)
			_launchMissile();

		// Pickups
		for(Pickup pickup : pickupInScene) {
			pickup.pickupPosition.x -= deltaPosition;
			if(pickup.pickupPosition.x + pickup.pickupTexture.getRegionWidth() < 10)
				pickupInScene.removeValue(pickup, false);

			obstacleRect.set(pickup.pickupPosition.x, pickup.pickupPosition.y,
							 pickup.pickupTexture.getRegionWidth(), pickup.pickupTexture.getRegionHeight());

			if(planeRect.overlaps(obstacleRect))
				_pickItUp(pickup);
		}

		_checkAndCreatePickup(dT);

		if(planePosition.y < terrainBelow.getRegionHeight() - 60 ||
		   planePosition.y + 65 > game.WND_HEIGHT - terrainAbove.getRegionHeight() + 35)
			_endGame();

		tapDrawTime -= dT;
	}

	public void render(float dT) {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		_updateScene(dT);
		_drawScene();
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
		batch.draw(terrainAbove, terrainOffset, game.WND_HEIGHT - terrainAbove.getRegionHeight());
		batch.draw(terrainAbove, terrainOffset + terrainAbove.getRegionWidth(), game.WND_HEIGHT - terrainAbove.getRegionHeight());

		// Pillars
		for(Vector2 vec: pillars) {
			if(vec.y == 1)
				batch.draw(pillarUp, vec.x, 0, PILLAR_WIDTH, PILLAR_HEIGHT);
			else
				batch.draw(pillarDown, vec.x, game.WND_HEIGHT-PILLAR_HEIGHT, PILLAR_WIDTH, PILLAR_HEIGHT);
		}

		// Missiles 
		if(isMissileInScene)
			batch.draw(selectedMissileTexture, missilePosition.x, missilePosition.y);

		// Pickups
		for(Pickup pickup : pickupInScene) 
			batch.draw(pickup.pickupTexture, pickup.pickupPosition.x, pickup.pickupPosition.y);

		if(tapDrawTime > 0) // 29.5 - is a half width/height of image
			batch.draw(tap2, touchPosition.x - 29.5f, touchPosition.y - 29.5f);

		if(gameState == GAME_INIT)
			batch.draw(tap1, planePosition.x, planePosition.y - 80);

		if(gameState == GAME_OVER) {
			batch.draw(gameOver, game.WND_HALF_WIDTH - 200, game.WND_HALF_HEIGHT - 10);
			explosion.draw(batch);
		}		

		batch.draw(plane.getKeyFrame(planeAnimationTime), planePosition.x, planePosition.y, 160, 45);
		smoke.draw(batch);

		batch.end();
	}

	private void _addPillar() {
		Vector2 pillarPosition = new Vector2();
		
		pillarPosition.x = (pillars.size == 0) 
			? (float)(game.WND_WIDTH + Math.random() * 600) 
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

	private void _checkAndCreatePickup(float dT) {
		pickupTiming.sub(dT);
		
		if(pickupTiming.x <= 0) {
			pickupTiming.x = (float)(0.5 + Math.random() * 0.5);
			if(_addPickup(Pickup.STAR))
				pickupTiming.x = 1 + (float)Math.random() * 2;
		}
	
		if(pickupTiming.y <= 0) {
			pickupTiming.y = (float)(0.5 + Math.random() * 0.5);
			if(_addPickup(Pickup.FUEL))
				pickupTiming.y = 3 + (float)Math.random() * 2;
		}

		if(pickupTiming.z <= 0) {
			pickupTiming.z = (float)(0.5 + Math.random() * 0.5);
			if(_addPickup(Pickup.SHIELD))
				pickupTiming.z = 10 + (float)Math.random() * 3;
		}

	}

	private boolean _addPickup(int type) {
		Vector2 randomPosition = new Vector2();
		randomPosition.x = game.WND_WIDTH + 20;
		randomPosition.y = (float)(PILLAR_HEIGHT + Math.random() * (game.WND_HEIGHT - PILLAR_HEIGHT << 1));

		for(Vector2 vec : pillars) {
			if(vec.y == 1) 
				obstacleRect.set(vec.x, 0, pillarUp.getRegionWidth(), pillarUp.getRegionHeight());
			else
				obstacleRect.set(vec.x, game.WND_HEIGHT - pillarDown.getRegionWidth(),
				 			     pillarUp.getRegionWidth(), pillarUp.getRegionHeight());	
		
			if(obstacleRect.contains(randomPosition))
				return false;
		}

		tmpPickup = new Pickup(type, game.manager);
		tmpPickup.pickupPosition.set(randomPosition);
		pickupInScene.add(tmpPickup);

		return true;
	}


	private void _pickItUp(Pickup pickup) {
		pickup.pickupSound.play();

		switch(pickup.pickupType) {
			case Pickup.STAR:   starCounter  += pickup.pickupValue; break;
			case Pickup.SHIELD: shieldCounter = pickup.pickupValue; break;
			case Pickup.FUEL:   fuelCounter   = pickup.pickupValue; break;
		}

		pickupInScene.removeValue(pickup, false);
	}

	private void _endGame() {
		if(gameState != GAME_OVER) {
			crashSound.play();
			gameState = GAME_OVER;
			explosion.reset();
			explosion.setPosition(planePosition.x + 40, planePosition.y + 40);
		}
	}

	@Override
	public void dispose() {
		pillars.clear();
		batch.dispose();
		atlas.dispose();
		MissileTextures.clear();
		smoke.dispose();
		explosion.dispose();

		tapSound.dispose();
		crashSound.dispose();
		spawnSound.dispose();
		music.dispose();
	}
}