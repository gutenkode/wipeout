package util;

import org.joml.Vector3d;
import org.joml.Vector3f;

public class Collisions {

    public static Vector3f toVector3f(float... f) { return new Vector3f(f[0],f[1],f[2]); }
    public static Vector3d toVector3d(float... f) { return new Vector3d(f[0],f[1],f[2]); }
    public static Vector3d toVector3d(double... d) { return new Vector3d(d[0],d[1],d[2]); }


    public static boolean doLineSegmentsIntersect(LineSegment l1, LineSegment l2) {
        return doLineSegmentsIntersect(l1.a, l1.b, l2.a, l2.b);
    }
    public static boolean doLineSegmentsIntersect(Vector3d a, Vector3d b, Vector3d c, Vector3d d) {
        return ccw(a,c,d) != ccw(b,c,d) &&
               ccw(a,b,c) != ccw(a,b,d);
    }
    private static boolean ccw(Vector3d a, Vector3d b, Vector3d c) {
        return (c.z-a.z) * (b.x-a.x) > (b.z-a.z) * (c.x-a.x);
    }

    public static boolean isPointInsideQuad(Vector3f point, Quad quad) {
        double a = 0;
        a += area(point, quad.a, quad.b);
        a += area(point, quad.b, quad.c);
        a += area(point, quad.c, quad.d);
        a += area(point, quad.d, quad.a);
        return a <= quad.area() + .001;
    }
    public static boolean isPointInsideTri(Vector3f point, Triangle tri) {
        double a = 0;
        a += area(point, tri.a, tri.b);
        a += area(point, tri.b, tri.c);
        a += area(point, tri.c, tri.a);
        return a <= tri.area() + .001;
    }
    public static double findInterpolatedHeight(Vector3f point, Triangle tri) {
        double a = tri.area();
        double a1 = area(point, tri.a, tri.b);
        double a2 = area(point, tri.b, tri.c);
        double a3 = area(point, tri.c, tri.a);
        return tri.a.y*(a2/a) + tri.b.y*(a3/a) + tri.c.y*(a1/a);
    }
    public static boolean intersectRayWithSquare(Vector3f R1, Vector3f R2, Quad quad) {
        // 1.
        Vector3f dS21 = new Vector3f(); quad.b.sub(quad.a, dS21);
        Vector3f dS31 = new Vector3f(); quad.c.sub(quad.a, dS31);
        Vector3f n = new Vector3f(); dS21.cross(dS31, n);

        // 2.
        Vector3f dR = new Vector3f(); R1.sub(R2, dR);

        float ndotdR = n.dot(dR);

        if (Math.abs(ndotdR) < 1e-6f) { // Choose your tolerance
            return false;
        }

        float t = -n.dot(R1.sub(quad.a)) / ndotdR;
        Vector3f M = new Vector3f(); dR.mul(t, M); R1.add(M, M);

        // 3.
        Vector3f dMS1 = new Vector3f(); M.sub(quad.a, dMS1);
        float u = dMS1.dot(dS21);
        float v = dMS1.dot(dS31);

        // 4.
        return (u >= 0.0f && u <= dS21.dot(dS21)
                && v >= 0.0f && v <= dS31.dot(dS31));
    }

    public static double area(Quad quad) {
        return area(quad.a, quad.b, quad.d)+area(quad.c, quad.b, quad.d);
    }
    public static double area(Vector3f a, Vector3f b, Vector3f c, Vector3f d) {
        return area(a, b, d)+area(c, b, d);
    }
    public static double area(Triangle tri) {
        return Math.abs((tri.a.x-tri.c.x)*(tri.b.z-tri.a.z)-(tri.a.x-tri.b.x)*(tri.c.z-tri.a.z))*.5;
    }
    public static double area(Vector3f a, Vector3f b, Vector3f c) {
        return Math.abs((a.x-c.x)*(b.z-a.z)-(a.x-b.x)*(c.z-a.z))*.5;
    }
}
