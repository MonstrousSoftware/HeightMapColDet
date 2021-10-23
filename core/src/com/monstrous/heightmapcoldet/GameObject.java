/**
 * 
 */
package com.monstrous.heightmapcoldet;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState;
import com.badlogic.gdx.utils.Disposable;

/**
 * @author Marco
 *
 */
public class GameObject extends ModelInstance implements Disposable {
	public btRigidBody  body = null;
	public ModelInstance proxy = null;  // model instance corresponding to collision shape (can be null)
	public MyMotionState motionState = null;
	public boolean hidden = false;

	// btMotionState serves to automatically synch transforms of rigid body and game object
	static class MyMotionState extends btMotionState {
		Matrix4 transform;
		@Override
		public void getWorldTransform (Matrix4 worldTrans) {
			worldTrans.set(transform);
		}
		@Override
		public void setWorldTransform (Matrix4 worldTrans) {
			transform.set(worldTrans);
		}
	}

	public void setProxyModel(Model proxyModel) {
		if(proxyModel != null) {
			proxy = new ModelInstance(proxyModel);
			proxy.transform = this.transform; // by reference so it always follows
		}
	}
	
	public GameObject(Model model, btCollisionShape shape, float mass , Model proxyModel) {
		super(model);

		setProxyModel(proxyModel);
		if(shape != null) {
			motionState = new MyMotionState();
			motionState.transform = transform;

			Vector3 localInertia = new Vector3();
			if (mass > 0f)
				shape.calculateLocalInertia(mass, localInertia);
			else
				localInertia.set(0, 0, 0);
			body = new btRigidBody(mass, null, shape, localInertia);
			body.setMotionState(motionState);        // to make sure game object auto-tracks the rigid body transform
		}
	}
	
	@Override
	public void dispose () {
		if(body != null)
			body.dispose();
		if(motionState != null)
			motionState.dispose();
	}
}
