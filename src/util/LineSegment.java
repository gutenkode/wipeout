package util;

import org.joml.Vector3d;

public class LineSegment {
    public Vector3d a,b;
    public LineSegment(Vector3d a, Vector3d b) {
        this.a = a;
        this.b = b;
    }
    public Vector3d getPerpendicular() {
        Vector3d c = new Vector3d();
        a.sub(b,c);
        c.y = 0;
        c.normalize();
        c.rotateAxis((float)Math.PI/2,0,1,0, c);
        return c;
    }
}
