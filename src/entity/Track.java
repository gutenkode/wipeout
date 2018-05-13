package entity;

import org.joml.Vector3d;
import util.*;
import mote4.util.vertex.builder.MeshBuilder;
import mote4.util.vertex.mesh.Mesh;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.*;

public class Track {

    private float[][] points1 = new float[][] {
            {0,0,0},
            {120,-20,20},
            {230,-20,100},
            {150,-30,200},
            {0,10,115},
            {-150,15,100},
            {-150,5,-50},
    };
    private float[][] points2 = new float[][] {
            {0,0,0, 0},
            {150,-10,0, .5f},
            {300,5,-50, 1},

            {450,15,150, -.5f},

            {300,25,350, -1f},
            {150,30,300, -2f},
            {0,35,300, -2.5f},
            {-150,-20,300, -.5f},
            {-300,-15,350, 0},

            {-450,-7,150, 0},

            {-300,0,-50, -1},
            {-150,-10,0, -1},
    };
    private Quad[] quads;
    private LineSegment[] walls;

    private Mesh mesh;

    public Track() {
        TrackBuilder.createTrackFromControlPoints(points2);

        MeshBuilder builder = new MeshBuilder(3);
        builder.includeTexCoords(2);
        for (float[] f : TrackBuilder.getVertices())
            builder.vertices(f);
        for (float[] f : TrackBuilder.getTexCoords())
            builder.texCoords(f);
        quads = TrackBuilder.getQuads();
        walls = TrackBuilder.getWalls();
        mesh = builder.constructVAO(GL_TRIANGLES);
    }

    public void render() {
        mesh.render();
    }

    public double getTrackCollisionHeight(Vector3f shipDownVec, Vector3f trackNormal) {
        for (Quad q : quads) {
            if (Collisions.isPointInsideQuad(shipDownVec, q)) {
                Triangle tri1 = q.tri1();
                if (Collisions.isPointInsideTri(shipDownVec, tri1)) {
                    tri1.setToNormal(trackNormal);
                    return Collisions.findInterpolatedHeight(shipDownVec, tri1);
                } else {
                    Triangle tri2 = q.tri2();
                    tri2.setToNormal(trackNormal);
                    return Collisions.findInterpolatedHeight(shipDownVec, tri2);
                }
            }

        }
        return Integer.MIN_VALUE;
    }
    public Vector3d getWallReflectionAngle(LineSegment shipLine) {
        for (LineSegment wall : walls) {
            if (Collisions.doLineSegmentsIntersect(shipLine, wall)) {
                return wall.getPerpendicular();
            }
        }
        return null;
    }
}
