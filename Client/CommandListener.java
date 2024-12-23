import java.awt.*;
import java.io.*;

// Classe pour gérer les commandes clavier et souris
class CommandListener implements Runnable {
    private final InputStream controlInputStream;
    private final Robot robot;
    private final double widthRatio;
    private final double heightRatio;

    CommandListener(InputStream controlInputStream, Robot robot, double widthRatio, double heightRatio) {
        this.controlInputStream = controlInputStream;
        this.robot = robot;
        this.widthRatio = widthRatio;
        this.heightRatio = heightRatio;
    }

    @Override
    public void run() {
        try (DataInputStream dataInputStream = new DataInputStream(controlInputStream)) {
            while (true) {
                String command = dataInputStream.readUTF();
                if (command == null || command.isBlank()) {
                    System.err.println("Commande vide reçue.");
                    continue;
                }

                String[] parts = command.split(":");
                if (parts.length < 2) {
                    System.err.println("Commande invalide : " + command);
                    continue;
                }

                switch (parts[0]) {
                    case "MOVE" -> handleMoveCommand(parts);
                    case "CLICK" -> handleClickCommand(parts);
                    case "SCROLL" -> handleScrollCommand(parts);
                    case "KEY_PRESS" -> handleKeyPressCommand(parts);
                    case "KEY_RELEASE" -> handleKeyReleaseCommand(parts);
                    default -> System.err.println("Commande inconnue : " + command);
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur dans CommandListener : " + e.getMessage());
        }
    }


    private void handleMoveCommand(String[] parts) {
        try {
            int serverX = Integer.parseInt(parts[1]);
            int serverY = Integer.parseInt(parts[2]);
            int clientX = (int) (serverX * widthRatio);
            int clientY = (int) (serverY * heightRatio);
            robot.mouseMove(clientX, clientY);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'exécution de MOVE : " + e.getMessage());
        }
    }

    private void handleClickCommand(String[] parts) {
        try {
            int button = Integer.parseInt(parts[1]);
            robot.mousePress(button);
            robot.mouseRelease(button);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'exécution de CLICK : " + e.getMessage());
        }
    }

    private void handleScrollCommand(String[] parts) {
        try {
            int scrollAmount = Integer.parseInt(parts[1]);
            robot.mouseWheel(scrollAmount);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'exécution de SCROLL : " + e.getMessage());
        }
    }

    private void handleKeyPressCommand(String[] parts) {
        try {
            int keyCode = Integer.parseInt(parts[1]);
            robot.keyPress(keyCode);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'exécution de KEY_PRESS : " + e.getMessage());
        }
    }

    private void handleKeyReleaseCommand(String[] parts) {
        try {
            int keyCode = Integer.parseInt(parts[1]);
            robot.keyRelease(keyCode);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'exécution de KEY_RELEASE : " + e.getMessage());
        }
    }
}
