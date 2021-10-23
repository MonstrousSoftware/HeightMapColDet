package com.monstrous.heightmapcoldet;

/* Demo of using Bullet's height field for collision detection.

 */

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;

public class Main extends ApplicationAdapter {

		public PerspectiveCamera cam;
		public CameraInputController camController;
		public ModelBatch modelBatch;
		public World world;

		@Override
		public void create() {

			world = new World();

			// create perspective camera
			cam = new PerspectiveCamera(70, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
			cam.position.set(0, 80, 80);
			cam.lookAt(0, 0, 0);
			cam.far = 200000f;
			cam.near = 10f;
			cam.update();

			// add camera controller
			camController = new CameraInputController(cam);

			// input multiplexer to send inputs to GUI and to cam controller
			InputMultiplexer im = new InputMultiplexer();
			Gdx.input.setInputProcessor(im);
			im.addProcessor(camController);

			modelBatch = new ModelBatch();
		}

		@Override
		public void resize(int width, int height) {
			cam.viewportWidth = Gdx.graphics.getWidth();
			cam.viewportHeight = Gdx.graphics.getHeight();
			cam.update();
		}

		@Override
		public void render() {
			// update camera positioning
			camController.update();
			world.update();


			// clear screen
			Gdx.gl.glClearColor(0.4f, 0.4f, 0.9f, 1);
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

			// render model instance
			modelBatch.begin(cam);
			modelBatch.render(world.gameObjects, world.environment);
			modelBatch.end();

		}

		@Override
		public void dispose() {
			modelBatch.dispose();
		}

}
