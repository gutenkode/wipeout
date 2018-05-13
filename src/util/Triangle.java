package util;

import org.joml.Vector3f;

public class Triangle {
    public Vector3f a,b,c;
    public Triangle(Vector3f a, Vector3f b, Vector3f c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }
    public double area() { return Collisions.area(this); }

    /**
     * Set the vector n to the normal of this triangle.
     * @param n
     */
    public void setToNormal(Vector3f n) {
        Vector3f ab = new Vector3f();
        Vector3f ac = new Vector3f();
        b.sub(a,ab);
        c.sub(a,ac);
        ab.cross(ac, n);
        n.normalize();
        if (n.y < 0)
            n.mul(-1);
    }
}