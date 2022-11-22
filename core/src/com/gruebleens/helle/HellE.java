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

	private static final float   TAP_DRAW_TIME_MAX = 1.0f;
	private static final int     TOUCH_IMPULSE = 500; 
	private static final Vector2 DAMPING = new Vector2(0.99f, 0.99f);

	float terrainOffset;
	float planeAnimationTime;


	Texture gameOver;

	TextureRegion bgRegion;
	TextureRegion terrainAbove;
	TextureRegion terrainBelow;

	float tapDrawTime;
	TextureRegion tapIndicator;
	TextureRegion tap1;
	TextureRegion tap2;
	TextureRegion pillarUp;
	TextureRegion pillarDown;

	Vector3 touchPosition = new Vector3();
	
	TextureAtlas atlas;

	Animation<TextureRegion> plane;

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

	
	@Override
	public void create () {
		fpsLogger = new FPSLogger();
		batch     = new SpriteBatch();
		camera    = new OrthographicCamera();
		// camera.setToOrtho(false, 800, 480);
		camera.position.set(400, 240, 0);
		viewport = new FitViewport(800, 480, camera);

		atlas = new TextureAtlas(Gdx.files.internal("packerout/helle.pack"));

		bgRegion     = atlas.findRegion("background");
		terrainBelow = atlas.findRegion("ground2");
		terrainAbove = new TextureRegion(terrainBelow);
		terrainAbove.flip(true, true);

		plane = new Animation(0.05f,
			atlas.findRegion("helli1"),
			atlas.findRegion("helli2"),
			atlas.findRegion("helli3"),
			atlas.findRegion("helli4")			
		); 
		plane.setPlayMode(PlayMode.LOOP);

		tapIndicator = atlas.findRegion("tap2");

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
	
		planeAnimationTime = 0;
		planeVelocity.set(100, 0);
		gravity.set(0, -2);
		planeDefaultPosition.set(200 - 44, 240 - 73 / 2);
		planePosition.set(planeDefaultPosition.x, planeDefaultPosition.y);
	
		// pillars.clear();
		// addPillar();
	}

	private void _updateScene() {
		float dT = Gdx.graphics.getDeltaTime();

		planeAnimationTime += dT;
		planeVelocity.scl(DAMPING);
		planeVelocity.add(gravity);
		planePosition.mulAdd(planeVelocity, dT);		

		if(Gdx.input.justTouched()) {
			touchPosition.set(Gdx.input.getX(), Gdx.input.getY(), 0);
			camera.unproject(touchPosition);

			tmp.set(planePosition.x, planePosition.y);
			tmp.sub(touchPosition.x, touchPosition.y).nor();
			planeVelocity.mulAdd(
				tmp,
				TOUCH_IMPULSE - MathUtils.clamp(Vector2.dst(touchPosition.x,
											 			    touchPosition.y,
											  				planePosition.x,
											  				planePosition.y),
															0,
														    TOUCH_IMPULSE)
			);
			tapDrawTime = TAP_DRAW_TIME_MAX;
		}

		terrainOffset -= planePosition.x - planeDefaultPosition.x;
		planePosition.x = planeDefaultPosition.x;

		if(terrainOffset * -1 > terrainBelow.getRegionWidth())
			terrainOffset = 0;

		if(terrainOffset > 0)
			terrainOffset = -terrainBelow.getRegionWidth();

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
		batch.draw(terrainAbove, terrainOffset, 480 - terrainAbove.getRegionHeight());
		batch.draw(terrainAbove, terrainOffset + terrainAbove.getRegionWidth(), 480 - terrainAbove.getRegionHeight());

		if(tapDrawTime > 0)
			batch.draw(tapIndicator, touchPosition.x - 29.5f, touchPosition.y - 29.5f); 

		batch.draw(plane.getKeyFrame(planeAnimationTime), planePosition.x, planePosition.y);

		batch.end();
	}
	
	@Override
	public void resize(int widht, int height) {
		viewport.update(widht, height);
	}

	@Override
	public void dispose () {
		batch.dispose();
		atlas.dispose();
	}
}