import java.awt.event.*;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Listener pour gérer les clics de souris.
 */
class MouseClickListener extends MouseAdapter {
    private final DataOutputStream controlDataOutputStream;

    MouseClickListener(DataOutputStream controlDataOutputStream) {
        this.controlDataOutputStream = controlDataOutputStream;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        try {
            int button = switch (e.getButton()) {
                case MouseEvent.BUTTON1 -> InputEvent.BUTTON1_DOWN_MASK;
                case MouseEvent.BUTTON2 -> InputEvent.BUTTON2_DOWN_MASK;
                case MouseEvent.BUTTON3 -> InputEvent.BUTTON3_DOWN_MASK;
                default -> 0;
            };
            controlDataOutputStream.writeUTF("CLICK:" + button);
        } catch (IOException ex) {
            System.err.println("Erreur lors de l'envoi du clic : " + ex.getMessage());
        }
    }
}

/**
 * Listener pour gérer les mouvements de la souris.
 */
class MouseMoveListener extends MouseMotionAdapter {
    private final DataOutputStream controlDataOutputStream;

    MouseMoveListener(DataOutputStream controlDataOutputStream) {
        this.controlDataOutputStream = controlDataOutputStream;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        try {
            int x = e.getX();
            int y = e.getY();
            controlDataOutputStream.writeUTF("MOVE:" + x + ":" + y);
        } catch (IOException ex) {
            System.err.println("Erreur lors de l'envoi du mouvement : " + ex.getMessage());
        }
    }
}

/**
 * Listener pour gérer le défilement de la souris.
 */
class MouseScrollListener implements MouseWheelListener {
    private final DataOutputStream controlDataOutputStream;

    MouseScrollListener(DataOutputStream controlDataOutputStream) {
        this.controlDataOutputStream = controlDataOutputStream;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        try {
            int scrollAmount = e.getWheelRotation();
            controlDataOutputStream.writeUTF("SCROLL:" + scrollAmount);
        } catch (IOException ex) {
            System.err.println("Erreur lors de l'envoi du défilement : " + ex.getMessage());
        }
    }
}

/**
 * Listener pour gérer les actions clavier.
 */
class KeyActionListener extends KeyAdapter {
    private final DataOutputStream controlDataOutputStream;

    KeyActionListener(DataOutputStream controlDataOutputStream) {
        this.controlDataOutputStream = controlDataOutputStream;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        try {
            controlDataOutputStream.writeUTF("KEY_PRESS:" + e.getKeyCode());
        } catch (IOException ex) {
            System.err.println("Erreur lors de l'envoi de la pression de touche : " + ex.getMessage());
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        try {
            controlDataOutputStream.writeUTF("KEY_RELEASE:" + e.getKeyCode());
        } catch (IOException ex) {
            System.err.println("Erreur lors de l'envoi du relâchement de touche : " + ex.getMessage());
        }
    }
}
