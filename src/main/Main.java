package main;

import mote4.scenegraph.Window;
import mote4.util.shader.ShaderUtils;
import mote4.util.texture.TextureMap;
import mote4.util.vertex.builder.StaticMeshBuilder;
import mote4.util.vertex.mesh.MeshMap;
import scene.Ingame;

import static org.lwjgl.glfw.GLFW.GLFW_DONT_CARE;
import static org.lwjgl.glfw.GLFW.glfwSetWindowSizeLimits;
import static org.lwjgl.opengl.GL11.*;

public class Main {
    public static void main(String[] args) {
        if (System.getProperty("os.name").toLowerCase().contains("mac"))
            System.setProperty("java.awt.headless", "true"); // prevents ImageIO from hanging on OS X
        Window.setVsync(true);
        Window.setTitle("WipEout");
        Window.initWindowedPercent(.666, 16/9.0);

        //Input.createCharCallback();
        //Input.createKeyCallback();
        //Input.pushLock(Input.Lock.PLAYER);
        loadResources();

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        //glfwSetInputMode(Window.getWindowID(), GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        glfwSetWindowSizeLimits(Window.getWindowID(), 640, 360, GLFW_DONT_CARE, GLFW_DONT_CARE);
        //glfwSetWindowAspectRatio(Window.getWindowID(), 16, 9);

        //Layer root = new Root();
        //root.addScene(new Ingame());
        //Window.addLayer(root);
        Window.addScene(new Ingame());
        Window.loop();
    }
    private static void loadResources() {
        TextureMap.load("FeisarTex","ship");
        TextureMap.load("track","track");
        TextureMap.load("track_back","track_back");
        ShaderUtils.addProgram("texture.vert","texture.frag","texture");
        MeshMap.add(StaticMeshBuilder.constructVAOFromOBJ("Wip3out_ships/Feisar/Feisar", false), "ship");
    }
}
