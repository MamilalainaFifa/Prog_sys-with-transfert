import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.util.Properties;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.*;

public class Client {
    public static void main(String[] args) {
        Properties config = new Properties();
        try (InputStream input = new FileInputStream("../Configuration/config.properties")) {
            config.load(input);
        } catch (IOException ex) {
            System.err.println("Erreur lors de la lecture du fichier config.properties : " + ex.getMessage());
            return;
        }

        String serverIP = config.getProperty("server.ip", "127.0.0.1");
        int port = Integer.parseInt(config.getProperty("server.image.port", "5000"));
        int controlPort = Integer.parseInt(config.getProperty("server.control.port", "5001"));
        int fps = Integer.parseInt(config.getProperty("fps", "30"));
        int frameDelay = 1000 / fps;
        float compressionQuality = Float.parseFloat(config.getProperty("jpeg.quality", "0.7"));

        try (Socket imageSocket = new Socket(serverIP, port);
             Socket controlSocket = new Socket(serverIP, controlPort);
             OutputStream imageOutputStream = imageSocket.getOutputStream();
             InputStream controlInputStream = controlSocket.getInputStream();
             DataOutputStream controlDataOutputStream = new DataOutputStream(controlSocket.getOutputStream());
             DataInputStream controlDataInputStream = new DataInputStream(controlInputStream)) {

            Robot robot = new Robot();
            Rectangle clientScreenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());

             // Réception des dimensions de l'écran du serveur
             int serverWidth = controlDataInputStream.readInt();
             int serverHeight = controlDataInputStream.readInt();
 
             // Dimensions de l'écran client et serveur
             Dimension clientScreenSize = Toolkit.getDefaultToolkit().getScreenSize();
             Dimension serverScreenSize = new Dimension(serverWidth, serverHeight);
 
             // Calcul des ratios entre l'écran serveur et l'écran client
             double widthRatio = clientScreenSize.getWidth() / serverScreenSize.getWidth();
             double heightRatio = clientScreenSize.getHeight() / serverScreenSize.getHeight();
 
             // Thread pour gérer les commandes clavier et souris
             CommandListener commandListener = new CommandListener(controlInputStream, robot, widthRatio, heightRatio);
             new Thread(commandListener).start();

            JFrame frame = new JFrame("Contrôle distant");
            JButton transferToServerButton = new JButton("Transférer vers Serveur");
            JButton requestFromServerButton = new JButton("Recevoir du Serveur");

            transferToServerButton.addActionListener(e -> {
                JFileChooser fileChooser = new JFileChooser();
                int option = fileChooser.showOpenDialog(frame);
                if (option == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    sendFileToServer(controlDataOutputStream, selectedFile);
                }
            });

            requestFromServerButton.addActionListener(e -> {
                String filePath = JOptionPane.showInputDialog(frame, "Chemin du fichier sur le serveur :");
                if (filePath != null && !filePath.isBlank()) {
                    requestFileFromServer(controlDataOutputStream, controlDataInputStream, filePath);
                }
            });

            frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
            frame.add(transferToServerButton);
            frame.add(requestFromServerButton);
            frame.setSize(300, 200);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);

            while (true) {
                BufferedImage screenCapture = robot.createScreenCapture(clientScreenRect);

                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
                ImageWriteParam param = writer.getDefaultWriteParam();
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(compressionQuality);

                try (ImageOutputStream ios = ImageIO.createImageOutputStream(byteArrayOutputStream)) {
                    writer.setOutput(ios);
                    writer.write(null, new javax.imageio.IIOImage(screenCapture, null, null), param);
                }
                writer.dispose();

                byte[] imageBytes = byteArrayOutputStream.toByteArray();
                DataOutputStream dataOutputStream = new DataOutputStream(imageOutputStream);
                dataOutputStream.writeInt(imageBytes.length);
                dataOutputStream.write(imageBytes);

                Thread.sleep(frameDelay);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendFileToServer(DataOutputStream dataOutputStream, File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            dataOutputStream.writeUTF("TRANSFER_TO_SERVER");
            dataOutputStream.writeUTF(file.getName());
            dataOutputStream.writeLong(file.length());

            byte[] buffer = new byte[4096];
            int read;
            while ((read = fileInputStream.read(buffer)) != -1) {
                dataOutputStream.write(buffer, 0, read);
            }

            System.out.println("Fichier transféré au serveur : " + file.getName());
        } catch (IOException e) {
            System.err.println("Erreur lors du transfert vers le serveur : " + e.getMessage());
        }
    }

    private static void requestFileFromServer(DataOutputStream dataOutputStream, DataInputStream dataInputStream, String filePath) {
        try {
            dataOutputStream.writeUTF("TRANSFER_TO_CLIENT");
            dataOutputStream.writeUTF(filePath);

            String response = dataInputStream.readUTF();
            if (response.equals("TRANSFER_FILE")) {
                String fileName = dataInputStream.readUTF();
                long fileSize = dataInputStream.readLong();

                File outputFile = new File("downloads/" + fileName);
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

                System.out.println("Fichier reçu du serveur : " + fileName);
            } else {
                System.err.println("Erreur du serveur : " + response);
            }
        } catch (IOException e) {
            System.err.println("Erreur lors de la demande de fichier au serveur : " + e.getMessage());
        }
    }
}
