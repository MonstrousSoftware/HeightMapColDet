package com.monstrous.heightmapcoldet;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.btConstraintSolver;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;
import com.badlogic.gdx.utils.Array;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class World {
    final static short GROUND_FLAG = 1 << 8;
    final static short OBJECT_FLAG = 1 << 9;
    final static short ALL_FLAG = -1;

    public Environment environment;
    public Array<ModelInstance> instances = new Array<ModelInstance>();
    public Array<GameObject> gameObjects = new Array<GameObject>();

    private btDynamicsWorld dynamicsWorld = null;
    private btConstraintSolver constraintSolver;
    private btDbvtBroadphase broadphase;
    private btCollisionShape groundShape, rocketShape;
    private btCollisionConfiguration collisionConfig;
    private btDispatcher dispatcher;
    float totalTime = 0f;

    public World() {
        Bullet.init();

        // define some lighting
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.5f, 0.5f, 0.5f, 1f));
        environment.add(new DirectionalLight().set(1, 1, 1f, -.4f, -0.4f, -0.2f));

        initDynamics();

        populate();
        populate(); // for some reason, the first call of populate doesn't result in good collision detection
    }

    public void initDynamics() {
        if(dynamicsWorld != null)
            dynamicsWorld.dispose();

        collisionConfig = new btDefaultCollisionConfiguration();
        dispatcher = new btCollisionDispatcher(collisionConfig);
        broadphase = new btDbvtBroadphase();
        constraintSolver = new btSequentialImpulseConstraintSolver();
        dynamicsWorld = new btDiscreteDynamicsWorld(dispatcher, broadphase, constraintSolver, collisionConfig);
        dynamicsWorld.setGravity(new Vector3(0, -10f, 0));
    }

    public void populate() {
        for( int i = 0; i < gameObjects.size; i++)
            dynamicsWorld.removeRigidBody(gameObjects.get(i).body);
        gameObjects.clear();

        // terrain object
        GameObject goGround = addTerrain();
        gameObjects.add(goGround);
        dynamicsWorld.addRigidBody(goGround.body, GROUND_FLAG, ALL_FLAG);

        // some objects to fall down
        for(int i = 0; i < 50; i++) {
            GameObject go = addBox(100f-200f*(float)Math.random(), 100f-200f*(float)Math.random());
            gameObjects.add(go);
            dynamicsWorld.addRigidBody(go.body, OBJECT_FLAG, ALL_FLAG);
        }
        for(int i = 0; i < 50; i++) {
            GameObject go = addBall(100f-200f*(float)Math.random(), 100f-200f*(float)Math.random());
            gameObjects.add(go);
            dynamicsWorld.addRigidBody(go.body, OBJECT_FLAG, ALL_FLAG);
        }
    }



    private GameObject addTerrain() {
        GridModel gridModel = new GridModel();
        Model model = gridModel.getModel();

        int gridSize = gridModel.gridSize;
        float minHeight = gridModel.minHeight;
        float maxHeight = gridModel.maxHeight;
        float heightScale = 1; // not used for float buffer
        FloatBuffer rawData = gridModel.getRawData();
        int upAxis = 1;

        boolean flipQuadEdges = false;
        btCollisionShape shape = new btHeightfieldTerrainShape(gridSize, gridSize,rawData, heightScale, minHeight, maxHeight, upAxis, flipQuadEdges);
        float scale = gridModel.gridScale/(float)(gridModel.gridSize);
        shape.setLocalScaling(new Vector3(scale, 1.0f, scale));

        GameObject go = new GameObject(model, shape, 0, null);

        go.body.setRestitution(0.5f);
        return go;
    }

    private GameObject addBox(float x, float z) {
        ModelBuilder modelBuilder = new ModelBuilder();
        final float size = 10f;

        Model model = modelBuilder.createBox(size, size, size,
                new Material(ColorAttribute.createDiffuse(Color.GREEN)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal  );

        btCollisionShape shape = new btBoxShape(new Vector3(size / 2f, size / 2f, size / 2f));
        GameObject go = new GameObject(model, shape, 50.0f,null);
        go.transform.translate(x, 30, z);
        go.body.setWorldTransform(go.transform);

        return go;
    }

    private GameObject addBall(float x, float z) {
        ModelBuilder modelBuilder = new ModelBuilder();
        final float size = 8f;

        Model model = modelBuilder.createSphere(size, size, size,16, 16,
                new Material(ColorAttribute.createDiffuse(Color.RED)),
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal  );

        btCollisionShape shape = new btSphereShape(size / 2f);
        GameObject go = new GameObject(model, shape, 50.0f, null);
        go.transform.translate(x, 30, z);
        go.body.setWorldTransform(go.transform);
        go.body.setRestitution(.2f);   // to control bounciness
        return go;
    }

    public void markSleepers() {
        for( int i = 0; i < gameObjects.size; i++) {
            GameObject go = gameObjects.get(i);
            if(!go.body.isActive()) {
                go.materials.first().set(ColorAttribute.createDiffuse(Color.DARK_GRAY));
            }
        }
    }

    public void update( ) {

            final float delta = Math.min(1f/30f, Gdx.graphics.getDeltaTime());
            totalTime += delta;
            if(totalTime > 15f) {
                populate();
                totalTime = 0f;
            }
            dynamicsWorld.stepSimulation(delta, 5, 1f/60f);
            markSleepers();
    }


    public void dispose() {
        dynamicsWorld.dispose();
        constraintSolver.dispose();
        dispatcher.dispose();
        collisionConfig.dispose();
        broadphase.dispose();

        gameObjects.clear();
    }
}
