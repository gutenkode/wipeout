package scene;

import entity.Ship;
import entity.Track;
import mote4.scenegraph.Scene;
import mote4.util.matrix.Transform;
import mote4.util.shader.ShaderMap;
import mote4.util.texture.TextureMap;

import static org.lwjgl.opengl.GL11.*;

public class Ingame implements Scene {

    private static Ship ship;
    private static Track track;

    private float cameraPan, cameraZoom;

    static {
        ship = new Ship();
        track = new Track();
    }

    private Transform trans;

    public Ingame() {
        trans = new Transform();
    }

    @Override
    public void update(double time, double delta) {
        ship.update(delta);

        trans.view.setIdentity();

        cameraPan *= .95;
        cameraZoom *= .8;
        cameraPan += (float)ship.rVel().y;
        cameraZoom += (float)ship.vel().length()*.3;

        // camera pos
        trans.view.translate(cameraPan,-.5f,-8-cameraZoom*.5f);
        trans.view.rotate(.2f, 1,0,0);

        // move to ship
        // tilt left/right to match turning angle
        trans.view.rotate(-(float)ship.rot().z/4,
                0,0,1);
        // rotate up/down to match pitch
        trans.view.rotate((float)ship.rot().x * .75f,
                1,0,0);
        // rotate left/right to match turning speed
        trans.view.rotate(-(float)ship.rot().y + (float)Math.PI + cameraPan,
                0,1,0);
        trans.view.translate(-(float)ship.pos().x, -(float)ship.pos().y, -(float)ship.pos().z);
    }

    @Override
    public void render(double time, double delta) {
        glEnable(GL_DEPTH_TEST);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        ShaderMap.use("texture");
        trans.model.setIdentity();
        trans.bind();


        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        TextureMap.bind("track");
        track.render();
        glCullFace(GL_FRONT);
        TextureMap.bind("track_back");
        track.render();
        glDisable(GL_CULL_FACE);

        TextureMap.bind("ship");
        ship.render(trans.model);
    }

    @Override
    public void framebufferResized(int width, int height) {
        trans.projection.setPerspective(width, height, .5f, 1000f, 65);
    }

    @Override
    public void destroy() {

    }

    public static Ship ship() { return ship; }
    public static Track track() { return track; }
}
