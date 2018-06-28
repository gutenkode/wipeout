package entity;

import main.Input;
import mote4.util.matrix.TransformationMatrix;
import mote4.util.vertex.mesh.MeshMap;
import org.joml.Vector3d;
import org.joml.Vector3f;
import scene.Ingame;
import util.LineSegment;
import util.TrackBuilder;

public class Ship {

    private Vector3d pos, vel, rot, rVel;
    private double thrustAccel, turnAccel, brakeAccel;
    private final double rideHeight = 1.5,
                         turnAccelMax  = .004,  thrustAccelMax  = .04,   brakeAccelMax  = .015,
                         turnAccelGain = .001,  thrustAccelGain = .001, brakeAccelGain = .04,
                         turnAccelDamp = .96,    thrustAccelDamp = .98,   brakeAccelDamp = .85;

    //public double yRot, x, y, z;
    //private double xVel, yVel, zVel, turnVel,

    public Ship() {
        pos = new Vector3d();
        vel = new Vector3d();
        rot = new Vector3d();
        rVel = new Vector3d();
        resetPosition();
    }

    private Vector3f trackNormal = new Vector3f();
    public void update(double delta) {
        Vector3f rayOrigin = new Vector3f((float)pos.x, (float)pos.y, (float)pos.z);
        rayOrigin.add(-4*(float)Math.sin(rot.y),0,-4*(float)Math.cos(rot.y));
        double rearHeight = Ingame.track().getTrackCollisionHeight(rayOrigin, trackNormal);

        rayOrigin.set((float)pos.x, (float)pos.y, (float)pos.z);
        double centerHeight = Ingame.track().getTrackCollisionHeight(rayOrigin, trackNormal);

        physicsStep(centerHeight, rearHeight, trackNormal);
    }
    private void physicsStep(double centerHeight, double rearHeight, Vector3f surfaceNormal) {

        // gravity
        vel.y -= 9.8/60 * .3; // 9.8 m/s^2 over 60 fps

        // if the ship is hovering and not falling, apply track physics
        double heightAboveTrack = (pos.y-centerHeight);
        // if the ship is below the track, put it right above
        if (heightAboveTrack < 0 && heightAboveTrack > -3) {
            pos.y -= (heightAboveTrack - .01);
            heightAboveTrack = .01; // reset height for track collisions
        }
        if (heightAboveTrack < rideHeight && heightAboveTrack > 0)
        {
            // track pushes with force proportional to height above it
            vel.y *= .8;
            vel.y += .2 * (rideHeight - heightAboveTrack);

            // make the ship nose up/down based on the angle of the track
            if (rearHeight > 0) {
                double rearHeightAboveTrack = (pos.y - rearHeight);
                if (rearHeightAboveTrack > 0)
                    rot.x += .01 * Math.max(-1, Math.min(1, heightAboveTrack - rearHeightAboveTrack));
            }

            // the ship will slowly slide down hills
            vel.x -= surfaceNormal.x * -.001;
            vel.z -= surfaceNormal.z * -.001;
        }

        // thrust forward/backward
        if (Input.getInstance().isKeyDown(Input.Key.ACCEL))
        {
            if (thrustAccel < thrustAccelMax)
                thrustAccel += thrustAccelGain;
        }
        else if (Input.getInstance().isKeyDown(Input.Key.DOWN))
        {
            if (thrustAccel > -thrustAccelMax)
                thrustAccel -= thrustAccelGain * .333;
        }
        else
            thrustAccel *= thrustAccelDamp;

        // calculate amount of thrust in vertical direction
        double verticalCoef = Math.abs(rot.x)/(Math.PI/2);

        // add thrust to velocity
        vel.add(
        (1-verticalCoef) * thrustAccel*Math.sin(rot.y),
        verticalCoef * thrustAccel,
        (1-verticalCoef) * thrustAccel*Math.cos(rot.y)
        );

        // rotation
        if (Input.getInstance().isKeyDown(Input.Key.RIGHT)) {
            if (turnAccel > -turnAccelMax)
                turnAccel -= turnAccelGain;
        } else if (Input.getInstance().isKeyDown(Input.Key.LEFT)) {
            if (turnAccel < turnAccelMax)
                turnAccel += turnAccelGain;
        } else {
            turnAccel *= turnAccelDamp;
        }
        rotateVelocityVector(turnAccel*10);
        rVel.y += turnAccel;
        rot.y += rVel.y;
        rot.z += rVel.y*.5;

        // airbrakes
        double velocity2d = Math.sqrt(vel.x*vel.x+vel.z*vel.z);
        boolean braking = false;
        if (Input.getInstance().isKeyDown(Input.Key.L_SHOULDER)) {
            if (brakeAccel > 0) // if we're drifting to the other size, quickly cancel out the momentum
                brakeAccel *= .5;
            if (brakeAccel > -brakeAccelMax)
                brakeAccel -= brakeAccelGain;
            vel.mul(.99,1,.99);
            //rVel.y += .001*velocity2d;
            braking = true;
        }
        if (Input.getInstance().isKeyDown(Input.Key.R_SHOULDER)) {
            if (brakeAccel < 0)
                brakeAccel *= .5;
            if (brakeAccel < brakeAccelMax)
                brakeAccel += brakeAccelGain;
            vel.mul(.99,1,.99);
            //rVel.y -= .001*velocity2d;
            braking = true;
        }
        if (!braking) // dampen if not braking
            brakeAccel *= brakeAccelDamp;
        // make the ship slide left-right
        vel.z += brakeAccel * Math.sin(rot.y) * (thrustAccel/thrustAccelMax);
        vel.x += brakeAccel * -Math.cos(rot.y) * (thrustAccel/thrustAccelMax);
        rot.z -= brakeAccel * 1;

        // pitch
        if (Input.getInstance().isKeyDown(Input.Key.PITCH_DOWN)) {
            rot.x -= turnAccelGain * 12;
        }
        else if (Input.getInstance().isKeyDown(Input.Key.PITCH_UP)) {
            rot.x += turnAccelGain * 12;
        }

        // check for wall collisions, reflect velocity if needed
        if (heightAboveTrack < TrackBuilder.WALL_HEIGHT) {
            Vector3d steppedPos = new Vector3d();
            pos.add(vel, steppedPos);
            LineSegment line = new LineSegment(pos, steppedPos);
            Vector3d wallReflect = Ingame.track().getWallReflectionAngle(line);
            if (wallReflect != null) {
                double tmpy = vel.y;
                vel.reflect(wallReflect.normalize());
                vel.y = tmpy;
                vel.mul(.8);
            }
        }
        pos.add(vel);

        //rot.add(rVel);
        rot.x += rVel.x;
        rot.z += rVel.z;

        vel.mul(.985,1,.985);
        rVel.mul(.6,.8,.6);
        rot.mul(.98,1,.98);

        // check for falling off the track
        if (pos.y < -50)
            resetPosition();
    }
    private void resetPosition() {
        pos.set(0,rideHeight-.5f,0);
        vel.set(0,0,0);
        rot.set(0,(float)Math.PI/2,0);
        rVel.set(0,0,0);
        thrustAccel = turnAccel = brakeAccel = 0;
    }
    private void rotateVelocityVector(double angle) {
        // angle that thrust vector will be rotated
        double cs = Math.cos(-angle*.1);
        double sn = Math.sin(-angle*.1);

        double velXp = vel.x * cs - vel.z * sn;
        vel.z = vel.x * sn + vel.z * cs;
        vel.x = velXp;

        // slow down a tiny bit when rotating velocity
        vel.x *= 1d-Math.abs(angle)*.15;
        vel.z *= 1d-Math.abs(angle)*.15;
    }

    public void render(TransformationMatrix mat) {
        mat.setIdentity();
        mat.translate((float)pos.x, (float)pos.y, (float)pos.z);
        mat.rotate((float)rot.y, 0,1,0);
        mat.rotate((float)rot.x, 1,0,0);
        mat.rotate((float)-rot.z, 0,0,1);
        mat.translate(0,0,-1);
        mat.bind();
        MeshMap.render("ship");
    }

    public Vector3d pos() { return pos; }
    public Vector3d vel() { return vel; }
    public Vector3d rot() { return rot; }
    public Vector3d rVel() { return rVel; }
}
