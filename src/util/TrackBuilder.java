package util;

import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class TrackBuilder {

    public static final float TRACK_WIDTH = 10, WALL_SLANT = 1.25f, WALL_HEIGHT = 2.75f;

    // stores each unique coordinate in this section, as a grid
    private static List<float[]> vertices;
    private static List<float[]> texCoords;
    // the track represented as Quad objects, for collisions
    private static List<Quad> quads;
    // the walls of the track as LineSegments, for collisions
    private static List<LineSegment> walls;

    /**
     * Constructs a track from control points.
     * The control points are a list of four-element vectors, where the first
     * three values are the X,Y,Z position and the fourth is the track width modifier.
     * A width value of 0 is the "normal" width.
     * The vertices, texcoords, and other data fields are populated after this call.
     * @param points
     */
    public static void createTrackFromControlPoints(float[][] points) {
        Vector3d[] vecPoints = new Vector3d[points.length];
        float[] widths = new float[points.length];
        for (int i = 0; i < points.length; i++) {
            vecPoints[i] = new Vector3d(points[i][0],points[i][1],points[i][2]);
            widths[i] = points[i][3];
        }
        createTrackFromControlPoints(vecPoints, widths);
    }
    public static void createTrackFromControlPoints(Vector3d[] controlPoints, float[] widths) {
        vertices = new ArrayList<>();
        texCoords = new ArrayList<>();
        quads = new ArrayList<>();
        walls = new ArrayList<>();

        // contains: forward vector for span in front, forward/right/left vectors for control point
        Vector3d[][] vectors = new Vector3d[controlPoints.length][4];
        double[] angles = new double[controlPoints.length];

        for (int i = 0; i < controlPoints.length; i++) {
            // forward vector for the span from i to i+1
            vectors[i][0] = new Vector3d();
            controlPoints[(i+1)%controlPoints.length].sub(controlPoints[i], vectors[i][0]);
            //vectors[i][0].normalize();
        }

        for (int i = 0; i < controlPoints.length; i++) {
            // forward vector at control point i,
            // average of forward vectors for spans on either side
            vectors[i][1] = new Vector3d();
            int j = i-1; if (j < 0) j += controlPoints.length;
            vectors[j][0].add(vectors[i][0], vectors[i][1]);
            vectors[i][1].normalize();

            // right-facing vector at control point
            vectors[i][2] = new Vector3d();
            vectors[i][1].rotateAxis(-(float) Math.PI / 2, 0, 1, 0, vectors[i][2]);
            // left-facing vector at control point
            vectors[i][3] = new Vector3d();
            vectors[i][1].rotateAxis((float) Math.PI / 2, 0, 1, 0, vectors[i][3]);

            // turn angle between forward vectors at this control point
            angles[i] = vectors[j][0].angle(vectors[i][0]);
        }

        for (int i = 0; i < controlPoints.length; i++) {
            createSpan(controlPoints, vectors, angles, widths, i);
        }
    }

    /**
     *
     * @param controlPoints X,Y,Z, coordinates of each control point.
     * @param vectors Forward vector for span in front of this control point, forward/right/left vectors for this control point.
     * @param angles Angle between forward vectors of the spans before and after this control point.
     * @param widths Widths at each control point.
     * @param ind1 Index of the current control point/span to generate.
     */
    private static void createSpan(Vector3d[] controlPoints, Vector3d[][] vectors, double[] angles, float[] widths, int ind1) {
        int ind0 = ind1-1; if (ind0 < 0) ind0 += vectors.length; // previous control point
        int ind2 = (ind1+1)%vectors.length; // next control point
        int ind3 = (ind1+2)%vectors.length; // next next control point

        // the effective length of this span, to determine an appropriate number of subdivisions
        double splineLength = findApproximateCubicSplineLength(controlPoints[ind0],controlPoints[ind1],controlPoints[ind2],controlPoints[ind3]);
        int steps = (int)(splineLength/(TRACK_WIDTH)); // each track segment should be the same length as
        steps = Math.max(1,steps);
        float[][][] coords = new float[steps+1][5][];

        Vector3d interpPos = new Vector3d();
        Vector3d interpRight = new Vector3d();
        Vector3d interpLeft = new Vector3d();
        for (int i = 0; i < steps+1; i++) {
            float j = (float)i/steps;

            // interpPos is the coordinate of the track middle point at this step
            //controlPoints[ind1].lerp(controlPoints[ind2], j, interpPos);
            interpolateCubicVector3d(controlPoints[ind0],controlPoints[ind1],controlPoints[ind2],controlPoints[ind3], interpPos, j);
            // calculate the left/right vectors at this step
            interpolateCubicVector3d(vectors[ind0][2],vectors[ind1][2],vectors[ind2][2],vectors[ind3][2], interpRight, j);
            interpolateCubicVector3d(vectors[ind0][3],vectors[ind1][3],vectors[ind2][3],vectors[ind3][3], interpLeft, j);

            // calculate track bank angle
            double bank = interpolateCubic1D((float)angles[ind0],(float)angles[ind1],(float)angles[ind2],(float)angles[ind3], j);
            float width = TRACK_WIDTH + (float)interpolateCubic1D(widths[ind0],widths[ind1],widths[ind2],widths[ind3], j);

            // calculate the three coordinates at this step
            // left side
            coords[i][0] = new float[]{
                    (float)(interpPos.x + width*interpLeft.x),
                    (float)(interpPos.y + bank),
                    (float)(interpPos.z + width*interpLeft.z)
            };
            // middle
            coords[i][1] = new float[]{(float)interpPos.x, (float)interpPos.y, (float)interpPos.z};
            // right side
            coords[i][2] = new float[]{
                    (float)(interpPos.x + width*interpRight.x),
                    (float)(interpPos.y - bank),
                    (float)(interpPos.z + width*interpRight.z)
            };
            // left wall
            coords[i][3] = new float[]{
                    (float)(interpPos.x + (width+WALL_SLANT)*interpLeft.x),
                    (float)(interpPos.y + bank +WALL_HEIGHT),
                    (float)(interpPos.z + (width+WALL_SLANT)*interpLeft.z)
            };
            // right wall
            coords[i][4] = new float[]{
                    (float)(interpPos.x + (width+WALL_SLANT)*interpRight.x),
                    (float)(interpPos.y - bank +WALL_HEIGHT),
                    (float)(interpPos.z + (width+WALL_SLANT)*interpRight.z)
            };
        }
        createTrackData(coords);
    }

    /**
     * Construct vertex and texcoord data from the raw list of track coordinates.
     * @param coords
     */
    private static void createTrackData(float[][][] coords) {
        // create Quad objects used for collision detection
        for (int i = 0; i < coords.length-1; i++) {
            quads.add(new Quad( // left quad
                    Collisions.toVector3f(coords[i][0]),
                    Collisions.toVector3f(coords[i+1][0]),
                    Collisions.toVector3f(coords[i+1][1]),
                    Collisions.toVector3f(coords[i][1])
            ));
            quads.add(new Quad( // right quad
                    Collisions.toVector3f(coords[i][1]),
                    Collisions.toVector3f(coords[i+1][1]),
                    Collisions.toVector3f(coords[i+1][2]),
                    Collisions.toVector3f(coords[i][2])
            ));
        }
        // create LineSegment objects used for collision detection
        for (int i = 0; i < coords.length-1; i++) {
            walls.add(new LineSegment( // left wall
                    Collisions.toVector3d(coords[i][0]),
                    Collisions.toVector3d(coords[i+1][0])
            ));
            walls.add(new LineSegment( // right wall
                    Collisions.toVector3d(coords[i+1][2]),
                    Collisions.toVector3d(coords[i][2])
            ));
        }

        // create the mesh triangles
        for (int i = 0; i < coords.length-1; i++) {
            // left wall
            vertices.add(coords[i+1][0]);
            vertices.add(coords[i+1][3]);
            vertices.add(coords[i][3]);

            vertices.add(coords[i][3]);
            vertices.add(coords[i][0]);
            vertices.add(coords[i+1][0]);
            // left side
            vertices.add(coords[i][1]);
            vertices.add(coords[i+1][1]);
            vertices.add(coords[i+1][0]);

            vertices.add(coords[i+1][0]);
            vertices.add(coords[i][0]);
            vertices.add(coords[i][1]);
            // right side
            vertices.add(coords[i+1][2]);
            vertices.add(coords[i+1][1]);
            vertices.add(coords[i][1]);

            vertices.add(coords[i][1]);
            vertices.add(coords[i][2]);
            vertices.add(coords[i+1][2]);
            // right wall
            vertices.add(coords[i][4]);
            vertices.add(coords[i+1][4]);
            vertices.add(coords[i+1][2]);

            vertices.add(coords[i+1][2]);
            vertices.add(coords[i][2]);
            vertices.add(coords[i][4]);

            // texture coordinates
            // left wall
            texCoords.add(new float[] {.25f,0});
            texCoords.add(new float[] {0,0});
            texCoords.add(new float[] {0,1});

            texCoords.add(new float[] {0,1});
            texCoords.add(new float[] {.25f,1});
            texCoords.add(new float[] {.25f,0});
            // left side
            texCoords.add(new float[] {.5f,1});
            texCoords.add(new float[] {.5f,0});
            texCoords.add(new float[] {.25f,0});

            texCoords.add(new float[] {.25f,0});
            texCoords.add(new float[] {.25f,1});
            texCoords.add(new float[] {.5f,1});
            // right side
            texCoords.add(new float[] {.75f,0});
            texCoords.add(new float[] {.5f,0});
            texCoords.add(new float[] {.5f,1});

            texCoords.add(new float[] {.5f,1});
            texCoords.add(new float[] {.75f,1});
            texCoords.add(new float[] {.75f,0});
            // right wall
            texCoords.add(new float[] {1,1});
            texCoords.add(new float[] {1,0});
            texCoords.add(new float[] {.75f,0});

            texCoords.add(new float[] {.75f,0});
            texCoords.add(new float[] {.75f,1});
            texCoords.add(new float[] {1,1});
        }
    }

    public static float[][] getVertices() { return vertices.toArray(new float[vertices.size()][3]); }
    public static float[][] getTexCoords() { return texCoords.toArray(new float[texCoords.size()][2]); }
    public static Quad[] getQuads() { return quads.toArray(new Quad[quads.size()]); }
    public static LineSegment[] getWalls() { return walls.toArray(new LineSegment[walls.size()]); }

    private static void interpolateCubicVector3d(Vector3d a, Vector3d b, Vector3d c, Vector3d d, Vector3d result, float x) {
        result.set(
                interpolateCubic1D(a.x,b.x,c.x,d.x,x),
                interpolateCubic1D(a.y,b.y,c.y,d.y,x),
                interpolateCubic1D(a.z,b.z,c.z,d.z,x)
        );
    }
    private static void interpolateCosineVector3f(Vector3f a, Vector3f b, Vector3f result, float x) {
        result.set(
                interpolateCosine1D(a.x,b.x,x),
                interpolateCosine1D(a.y,b.y,x),
                interpolateCosine1D(a.z,b.z,x)
        );
    }
    private static float interpolateCosine1D(float a, float b, float x) {
        double ft = x * Math.PI;
        double f = (1 - Math.cos(ft)) * .5;

        return (float)(a*(1-f) + b*f);
    }
    private static float interpolateLinear1D(float a, float b, float x) {
        return a*(1-x) + b*x;
    }
    private static double interpolateCubic1D(double v0, double v1, double v2, double v3, double x) {
        double P = (v3 - v2) - (v0 - v1);
        double Q = (v0 - v1) - P;
        double R = v2 - v0;
        double S = v1;

        return P*Math.pow(x,3) + Q*Math.pow(x,2) + R*x + S;
    }

    private static double findApproximateCubicSplineLength(Vector3d a, Vector3d b, Vector3d c, Vector3d d) {
        double length = 0;
        int steps = 40;
        Vector3d vec1 = new Vector3d();
        Vector3d vec2 = new Vector3d();
        for (int i = 0; i < steps-1; i++) {
            float j = (float)i/steps;
            float j2 = (float)(i+1)/steps;

            interpolateCubicVector3d(a,b,c,d, vec1, j);
            interpolateCubicVector3d(a,b,c,d, vec2, j2);
            vec1.sub(vec2);
            length += vec1.length();
        }
        return length;
    }
}
