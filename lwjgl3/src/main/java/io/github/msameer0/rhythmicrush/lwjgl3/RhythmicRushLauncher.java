package io.github.msameer0.rhythmicrush.lwjgl3;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * A pre-launcher UI that allows the player to configure RAM before starting the game.
 */
public class RhythmicRushLauncher {

    public static void main(String[] args) {
        // If we are already running the game (passed from the launcher), skip the UI
        if (System.getProperty("rhythmicrush.launched") != null) {
            Lwjgl3Launcher.main(args);
            return;
        }

        SwingUtilities.invokeLater(RhythmicRushLauncher::showLauncherUI);
    }

    private static void showLauncherUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        JFrame frame = new JFrame("RhythmicRush - Launcher");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 250);
        frame.setLayout(new BorderLayout(10, 10));
        frame.setLocationRelativeTo(null);

        // Header
        JLabel header = new JLabel("Game Configuration", SwingConstants.CENTER);
        header.setFont(new Font("SansSerif", Font.BOLD, 18));
        header.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        frame.add(header, BorderLayout.NORTH);

        // Center Panel
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        JLabel ramLabel = new JLabel("Memory Allocation (RAM):");
        String[] options = {"512 MB (Default)", "1 GB", "2 GB", "4 GB", "8 GB"};
        JComboBox<String> ramBox = new JComboBox<>(options);
        ramBox.setSelectedIndex(2); // Default to 2GB in the UI

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(ramLabel, gbc);
        gbc.gridx = 1;
        panel.add(ramBox, gbc);

        frame.add(panel, BorderLayout.CENTER);

        // Launch Button
        JButton launchBtn = new JButton("Launch Game");
        launchBtn.setPreferredSize(new Dimension(150, 40));
        launchBtn.setBackground(new Color(50, 150, 50));
        launchBtn.setForeground(Color.WHITE);
        launchBtn.setFocusPainted(false);

        launchBtn.addActionListener(e -> {
            String selected = (String) ramBox.getSelectedItem();
            String xmx = "512m";
            if (selected.contains("1 GB")) xmx = "1024m";
            else if (selected.contains("2 GB")) xmx = "2048m";
            else if (selected.contains("4 GB")) xmx = "4096m";
            else if (selected.contains("8 GB")) xmx = "8192m";

            launchGame(xmx);
            System.exit(0);
        });

        JPanel southPanel = new JPanel();
        southPanel.add(launchBtn);
        southPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        frame.add(southPanel, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private static void launchGame(String ram) {
        try {
            String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            String classpath = System.getProperty("java.class.path");
            
            List<String> command = new ArrayList<>();
            command.add(javaBin);
            command.add("-Xmx" + ram);
            command.add("-Dactive.launcher=true");
            command.add("-Drhythmicrush.launched=true");
            command.add("-cp");
            command.add(classpath);
            command.add("io.github.msameer0.rhythmicrush.lwjgl3.Lwjgl3Launcher");

            ProcessBuilder builder = new ProcessBuilder(command);
            builder.inheritIO();
            // Important: Set working directory to assets so game finds textures
            File assetsDir = new File("assets");
            if (!assetsDir.exists()) assetsDir = new File("."); 
            builder.directory(assetsDir);
            
            builder.start();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error launching game: " + e.getMessage());
        }
    }
}
