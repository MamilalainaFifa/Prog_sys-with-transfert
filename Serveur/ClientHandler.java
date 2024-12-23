import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;

import javax.imageio.ImageIO;
import javax.swing.*;

class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final Socket controlClientSocket;

    ClientHandler(Socket clientSocket, Socket controlClientSocket) {
        this.clientSocket = clientSocket;
        this.controlClientSocket = controlClientSocket;
    }

    @Override
    public void run() {
        try (InputStream controlInputStream = controlClientSocket.getInputStream();
             DataInputStream controlDataInputStream = new DataInputStream(controlInputStream);
             OutputStream controlOutputStream = controlClientSocket.getOutputStream();
             DataOutputStream controlDataOutputStream = new DataOutputStream(controlOutputStream);
             InputStream imageInputStream = clientSocket.getInputStream()) {

            // Envoyer les dimensions de l'écran du serveur au client
            Dimension serverScreenSize = Toolkit.getDefaultToolkit().getScreenSize();
            controlDataOutputStream.writeInt(serverScreenSize.width);
            controlDataOutputStream.writeInt(serverScreenSize.height);
            controlDataOutputStream.flush();

            // Initialisation de l'interface graphique
            JFrame frame = new JFrame("Affichage distant");
            JLabel imageLabel = new JLabel();
            frame.add(imageLabel);
            frame.setUndecorated(true);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            gd.setFullScreenWindow(frame);

            frame.setVisible(true);

            // Gestion des événements et affichage
            imageLabel.addMouseListener(new MouseClickListener(controlDataOutputStream));
            imageLabel.addMouseMotionListener(new MouseMoveListener(controlDataOutputStream));
            imageLabel.addMouseWheelListener(new MouseScrollListener(controlDataOutputStream));
            frame.addKeyListener(new KeyActionListener(controlDataOutputStream));

            while (true) {
                if (controlDataInputStream.available() > 0) {
                    String command = controlDataInputStream.readUTF();
                    if (command.startsWith("TRANSFER_TO_SERVER")) {
                        receiveFileFromClient(controlDataInputStream);
                    } else if (command.startsWith("TRANSFER_TO_CLIENT")) {
                        sendFileToClient(controlDataInputStream, controlDataOutputStream);
                    }
                }

                handleImageStream(imageInputStream, imageLabel, serverScreenSize);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleImageStream(InputStream imageInputStream, JLabel imageLabel, Dimension serverScreenSize) {
        try {
            DataInputStream dataInputStream = new DataInputStream(imageInputStream);

            if (dataInputStream.available() > 0) {
                int imageSize = dataInputStream.readInt();
                byte[] imageBytes = new byte[imageSize];
                dataInputStream.readFully(imageBytes);

                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(imageBytes);
                BufferedImage image = ImageIO.read(byteArrayInputStream);

                if (image != null) {
                    Image resizedImage = image.getScaledInstance(serverScreenSize.width, serverScreenSize.height, Image.SCALE_SMOOTH);
                    SwingUtilities.invokeLater(() -> {
                        imageLabel.setIcon(new ImageIcon(resizedImage));
                        imageLabel.repaint();
                    });
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur dans le flux d'image : " + e.getMessage());
        }
    }

    private void receiveFileFromClient(DataInputStream dataInputStream) throws IOException {
        String fileName = dataInputStream.readUTF();
        long fileSize = dataInputStream.readLong();

        File outputFile = new File("received_files/" + fileName);
        outputFile.getParentFile().mkdirs();

        try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[4096];
            long totalRead = 0;
            int read;
            while (totalRead < fileSize && (read = dataInputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, read);
                totalRead += read;
            }
        }

        System.out.println("Fichier reçu du client : " + fileName);
    }

    private void sendFileToClient(DataInputStream dataInputStream, DataOutputStream dataOutputStream) throws IOException {
        String filePath = dataInputStream.readUTF();
        File file = new File(filePath);

        if (!file.exists() || !file.isFile()) {
            dataOutputStream.writeUTF("ERROR: Fichier introuvable");
            return;
        }

        dataOutputStream.writeUTF("TRANSFER_FILE");
        dataOutputStream.writeUTF(file.getName());
        dataOutputStream.writeLong(file.length());

        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = fileInputStream.read(buffer)) != -1) {
                dataOutputStream.write(buffer, 0, read);
            }
        }

        System.out.println("Fichier envoyé au client : " + file.getName());
    }
}
