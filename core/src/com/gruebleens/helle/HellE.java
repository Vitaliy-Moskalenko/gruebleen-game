package com.gruebleens.helle;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;


public class HellE extends Game {

	static final int WND_WIDTH       = 800;
	static final int WND_HEIGHT      = 480;
	static final int WND_HALF_WIDTH  = 400;
	static final int WND_HALF_HEIGHT = 240;

	FPSLogger          logger   = new FPSLogger();

	AssetManager       manager  = new AssetManager();
	TextureAtlas       atlas;
	SpriteBatch        batch;
	OrthographicCamera camera   = new OrthographicCamera();
	Viewport           viewport = new FitViewport(WND_WIDTH, WND_HEIGHT, camera);
	BitmapFont         font;


	@Override
	public void create() {
		camera.position.set(WND_HALF_WIDTH, WND_HALF_HEIGHT, 0);

		manager.load("sound/journey.mp3", Music.class);
		manager.load("sound/pop.ogg", Sound.class);
		manager.load("sound/crash.ogg", Sound.class);
		manager.load("sound/alarm.ogg", Sound.class);
		manager.load("sound/fuel.ogg", Sound.class);
		manager.load("sound/star.ogg", Sound.class);
		manager.load("packerout/helle.pack", TextureAtlas.class);
		manager.load("font/library_3_am-36.fnt", BitmapFont.class);
		manager.load("Smoke", ParticleEffect.class);
		manager.load("Explosion", ParticleEffect.class);
		manager.finishLoading();

		batch = new SpriteBatch();
		atlas = manager.get("packerout/helle.pack", TextureAtlas.class);
		font  = manager.get("font/library_3_am-36.fnt", BitmapFont.class);

		setScreen(new HellEScene(this));
	}

	@Override
	public void render() {
		logger.log();
		super.render();
	}

	@Override
	public void resize(int width, int height) {
		viewport.update(width, height);
	}

	@Override
	public void dispose() {
		batch.dispose();
		atlas.dispose();
		manager.dispose();
	}
}