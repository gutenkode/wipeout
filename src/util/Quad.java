package util;

import org.joml.Vector3f;

public class Quad {
    public Vector3f a,b,c,d;
    public Quad(Vector3f a, Vector3f b, Vector3f c, Vector3f d) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }
    public double area() { return Collisions.area(this); }

    /**
     * Returns a Triangle representing one half of this quad.
     * @return
     */
    public Triangle tri1() { return new Triangle(a,b,c); }
    /**
     * Returns a Triangle representing one half of this quad.
     * If the vectors wind clockwise, this triangle will not
     * overlap with the one returned by tri1().
     * @return
     */
    public Triangle tri2() { return new Triangle(d,a,c); }
}
