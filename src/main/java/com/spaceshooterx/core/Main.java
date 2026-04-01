package com.spaceshooterx.core;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import com.spaceshooterx.audio.SoundManager;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Space Shooter X - Galactic Defense");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(true);

            ShootingGame game = new ShootingGame();
            game.setHostFrame(frame);

            frame.add(game);
            frame.pack();
            frame.setLocationRelativeTo(null);
            
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    SoundManager.shutdown();
                    System.exit(0);
                }
            });

            frame.setVisible(true);
        });
    }
}