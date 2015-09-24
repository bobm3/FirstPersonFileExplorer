package org.masonapps.firstpersonfileexplorer.firstperson;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btAxisSweep3;
import com.badlogic.gdx.physics.bullet.collision.btBroadphaseProxy;
import com.badlogic.gdx.physics.bullet.collision.btCapsuleShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btConvexShape;
import com.badlogic.gdx.physics.bullet.collision.btDefaultCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.collision.btGhostPairCallback;
import com.badlogic.gdx.physics.bullet.collision.btPairCachingGhostObject;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btKinematicCharacterController;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import org.masonapps.firstpersonfileexplorer.bullet.BaseBulletApp;
import org.masonapps.firstpersonfileexplorer.bullet.BulletWorld;
import org.masonapps.firstpersonfileexplorer.controls.SimpleJoystick;

/**
 * Created by Bob on 8/15/2015.
 */
public class FirstPersonApp extends BaseBulletApp {

    public btGhostPairCallback ghostPairCallback;
    public btPairCachingGhostObject ghostObject;
    public btConvexShape ghostShape;
    public btKinematicCharacterController characterController;
    public Matrix4 characterTransform;
    public Vector3 walkDirection = new Vector3();
    private Vector3 tempV = new Vector3();
    private Vector3 cross = new Vector3();
    public Stage stage;
    public SimpleJoystick moveJoystick;
    public FirstPersonCameraController cameraController;
    public float deltaTime;
    public float walkSpeed = 6f;

    @Override
    public BulletWorld createWorld() {
        btDefaultCollisionConfiguration collisionConfig = new btDefaultCollisionConfiguration();
        btCollisionDispatcher dispatcher = new btCollisionDispatcher(collisionConfig);
        btAxisSweep3 sweep = new btAxisSweep3(new Vector3(-1000, -1000, -1000), new Vector3(1000, 1000, 1000));
        btSequentialImpulseConstraintSolver solver = new btSequentialImpulseConstraintSolver();
        btDiscreteDynamicsWorld collisionWorld = new btDiscreteDynamicsWorld(dispatcher, sweep, solver, collisionConfig);
        ghostPairCallback = new btGhostPairCallback();
        sweep.getOverlappingPairCache().setInternalGhostPairCallback(ghostPairCallback);
        return new BulletWorld(collisionConfig, dispatcher, sweep, solver, collisionWorld);
    }

    @Override
    public void create() {
        super.create();
        stage = new Stage(new ScreenViewport());
        moveJoystick = new SimpleJoystick(stage);
        disposables.add(stage);
        disposables.add(moveJoystick);
        characterTransform = new Matrix4().setToTranslation(0f, 1f, 0f);
        ghostObject = new btPairCachingGhostObject();
        ghostObject.setWorldTransform(characterTransform);
        ghostShape = new btCapsuleShape(0.5f, 1f);
        ghostObject.setCollisionShape(ghostShape);
        ghostObject.setCollisionFlags(btCollisionObject.CollisionFlags.CF_CHARACTER_OBJECT);
        characterController = new btKinematicCharacterController(ghostObject, ghostShape, 0.2f);
        characterController.setJumpSpeed(20f);

        world.collisionWorld.addCollisionObject(ghostObject, (short) btBroadphaseProxy.CollisionFilterGroups.CharacterFilter, (short) (btBroadphaseProxy.CollisionFilterGroups.StaticFilter | btBroadphaseProxy.CollisionFilterGroups.DefaultFilter));
        ((btDiscreteDynamicsWorld) world.collisionWorld).addAction(characterController);

        characterTransform.getTranslation(camera.position);
        camera.up.set(0, 1, 0);
        camera.update();
        cameraController = new FirstPersonCameraController(camera);
        Gdx.input.setInputProcessor(new InputMultiplexer(stage, cameraController));
    }

    @Override
    public void update() {
        deltaTime = Math.min(1f / 30f, Gdx.graphics.getDeltaTime());
        stage.act();
        cameraController.update();
        ghostObject.setWorldTransform(characterTransform);
        walkDirection.set(0, 0, 0);
        cross.set(camera.up).crs(camera.direction);
        cross.y = 0;
        cross.nor();
        tempV.set(camera.direction);
        tempV.y = 0;
        tempV.nor();
        walkDirection.add(cross.scl(moveJoystick.getThumbPos().x * walkSpeed * deltaTime)).add(tempV.scl(-moveJoystick.getThumbPos().y * walkSpeed * deltaTime));
        characterController.setWalkDirection(walkDirection);
        super.update();
        ghostObject.getWorldTransform(characterTransform);
        characterTransform.getTranslation(camera.position);
        camera.position.add(0, 0.75f, 0);
        camera.update();
    }

    @Override
    public void render() {
        super.render();
        stage.draw();
    }

    @Override
    public void dispose() {
        ((btDiscreteDynamicsWorld) world.collisionWorld).removeAction(characterController);
        world.collisionWorld.removeCollisionObject(ghostObject);
        super.dispose();
        characterController.dispose();
        ghostObject.dispose();
        ghostShape.dispose();
        ghostPairCallback.dispose();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        final float density = Gdx.graphics.getDensity();
        final int joyStickSize = Math.round(140f * density);
        final int margin = Math.round(10f * density);
        moveJoystick.setSize(joyStickSize, joyStickSize);
        moveJoystick.setPosition(joyStickSize / 2 + margin, joyStickSize / 2 + margin);
    }
}