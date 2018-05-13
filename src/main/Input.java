package main;

import mote4.scenegraph.Window;

import java.util.Arrays;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Centralized input management.  All input checks for the game should be done
 * through this class, as it provides an easy-to-edit abstraction.
 * @author Peter
 */
public class Input {

    private static Input instance;
    public static Input getInstance() {
        if (instance == null)
            instance = new Input();
        return instance;
    }

    ////////////

    public enum Key {
        YES,
        NO,

        UP,
        DOWN,
        LEFT,
        RIGHT,

        ACCEL,
        L_SHOULDER,
        R_SHOULDER,
        PITCH_UP,
        PITCH_DOWN;

        public final int INDEX;
        Key() {
            INDEX = ordinal();
        }
    }

    private boolean[] isNew, isDown;

    private Input() {
        createKeyCallback();
        // TODO add gamepad support
        isNew  = new boolean[Key.values().length];
        isDown = new boolean[Key.values().length];
    }

    public boolean isKeyDown(Key k) {
        return isDown[k.INDEX];
    }
    public boolean isKeyNew(Key k) {
        boolean b =  isNew[k.INDEX];
        isNew[k.INDEX] = false; // TODO this may be unnecessary since isNew ignores GLFW_REPEAT
        return b;
    }
    public void clearKeys() {
        Arrays.fill(isNew, false);
        Arrays.fill(isDown, false);
    }

    private void createKeyCallback() {
        glfwSetKeyCallback(Window.getWindowID(), (long window, int key, int scancode, int action, int mods) -> {
            // action is GLFW_PRESS, GLFW_REPEAT, or GLFW_RELEASE
            switch (key) {
                case GLFW_KEY_Z:
                    callbackAction(action, Key.YES);
                    break;
                case GLFW_KEY_X:
                    callbackAction(action, Key.NO);
                    break;

                //case GLFW_KEY_W:
                case GLFW_KEY_UP:
                    callbackAction(action, Key.ACCEL);
                    callbackAction(action, Key.UP);
                    break;
                //case GLFW_KEY_S:
                case GLFW_KEY_DOWN:
                    callbackAction(action, Key.DOWN);
                    break;
                //case GLFW_KEY_A:
                case GLFW_KEY_LEFT:
                    callbackAction(action, Key.LEFT);
                    break;
                //case GLFW_KEY_D:
                case GLFW_KEY_RIGHT:
                    callbackAction(action, Key.RIGHT);
                    break;

                case GLFW_KEY_Q:
                    callbackAction(action, Key.L_SHOULDER);
                    break;
                case GLFW_KEY_E:
                    callbackAction(action, Key.R_SHOULDER);
                    break;

                case GLFW_KEY_W:
                    callbackAction(action, Key.PITCH_UP);
                    break;
                case GLFW_KEY_S:
                    callbackAction(action, Key.PITCH_DOWN);
                    break;
                //case GLFW_KEY_SPACE:
                    //callbackAction(action, Key.ACCEL);
                    //break;
            }
        });
    }
    private void callbackAction(int action, Key key) {
        isNew[key.INDEX] = (action == GLFW_PRESS);
        isDown[key.INDEX] = (action != GLFW_RELEASE);
    }
}
