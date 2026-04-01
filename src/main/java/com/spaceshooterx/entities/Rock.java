package com.spaceshooterx.entities;

import java.awt.*;
import java.awt.image.BufferedImage;

import com.spaceshooterx.core.GameConfig;
import com.spaceshooterx.util.ImageUtils;

public class Rock{
    private int x, y;
    private final int speed;
    private BufferedImage rockImage;

    public Rock() {
        this(1.0);
    }

    public Rock(double speedMultiplier) {
        x = (int) (Math.random() * (GameConfig.WIDTH - GameConfig.ROCK_SIZE));
        y = 0;
        speed = Math.max(1, (int) Math.round(GameConfig.ROCK_SPEED * speedMultiplier));

        rockImage = ImageUtils.loadImage("/Inventory/rock.png");
        if (rockImage != null) {
            rockImage = ImageUtils.resizeImage(rockImage, GameConfig.ROCK_SIZE, GameConfig.ROCK_SIZE);
        }
    }

    public void draw(Graphics g) {
        g.drawImage(rockImage, x, y, null);
    }

    public void update() {
        y += speed;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, GameConfig.ROCK_SIZE, GameConfig.ROCK_SIZE);
    }  

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
