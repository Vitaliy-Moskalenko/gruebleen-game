package com.gruebleens.helle;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import com.badlogic.gdx.utils.GdxRuntimeException;

public class HellE extends ApplicationAdapter {
	float terrainOffset;
	float planeAnimationTime;

	Vector2 gravity              = new Vector2();
	Vector2 planeVelocity        = new Vector2();
	Vector2 planePosition        = new Vector2();
	Vector2 planeDefaultPosition = new Vector2();

	private static final Vector2 damping = new Vector2(0.99f, 0.99f);

	SpriteBatch        batch;
	OrthographicCamera camera;

	TextureRegion bgRegion;
	TextureRegion terrainAbove;
	TextureRegion terrainBelow;
	
	TextureAtlas atlas;

	Viewport     viewport;

	Animation<TextureRegion> plane;

	FPSLogger    fpsLogger;

	
	@Override
	public void create () {
		fpsLogger = new FPSLogger();
		batch     = new SpriteBatch();
		camera    = new OrthographicCamera();
		// camera.setToOrtho(false, 800, 480);
		camera.position.set(400, 240, 0);
		viewport = new FitViewport(800, 480, camera);

		atlas = new TextureAtlas(Gdx.files.internal("helle.pack"));
		if (atlas == null)
			throw new GdxRuntimeException("Atlas failed to load..");		

		bgRegion     = atlas.findRegion("background");
		if (bgRegion == null)
			throw new GdxRuntimeException("Tileset region not found: 'background'");

		terrainBelow = atlas.findRegion("ground2");
		if (terrainBelow == null)
			throw new GdxRuntimeException("Tileset region not found: 'ground2'");

		terrainAbove = new TextureRegion(terrainBelow);
		terrainAbove.flip(true, true);

		plane = new Animation(0.05f,
			atlas.findRegion("helli1"),
			atlas.findRegion("helli2"),
			atlas.findRegion("helli3"),
			atlas.findRegion("helli2")			
		); 
		plane.setPlayMode(PlayMode.LOOP);

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
		terrainOffset      = 0;
		planeAnimationTime = 0;
		planeVelocity.set(200, 0);
		gravity.set(0, -2);
		planeDefaultPosition.set(400 - 44, 240 - 73 / 2);
		planePosition.set(planeDefaultPosition.x, planeDefaultPosition.y);
	}

	private void _updateScene() {
		float dT = Gdx.graphics.getDeltaTime();
		
		planeAnimationTime += dT;
		// planeVelocity.scl(damping);
		// planeVelocity.add(gravity);
		planePosition.mulAdd(planeVelocity, dT);

		terrainOffset -= planePosition.x - planeDefaultPosition.x;
		planePosition.x = planeDefaultPosition.x;

		// terrainOffset -= 200 * dT;
		if(terrainOffset * -1 > terrainBelow.getRegionWidth())
			terrainOffset = 0;

		if(terrainOffset > 0)
			terrainOffset = -terrainBelow.getRegionWidth();

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

		batch.draw(plane.getKeyFrame(planeAnimationTime), planePosition.x, planePosition.y);

		batch.end();
	}
	
	@Override
	public void resize(int widht, int height) {
		viewport.update(widht, height);
	}

	// @Override
	// public void dispose () {}
}
