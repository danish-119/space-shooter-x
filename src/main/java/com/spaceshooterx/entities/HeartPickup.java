package com.spaceshooterx.entities;

import java.awt.*;

import com.spaceshooterx.audio.SoundManager;
import com.spaceshooterx.effects.ParticleSystem;

public class HeartPickup {
    private float x, y;
    private boolean collected = false;
    private long spawnTime;
    
    public HeartPickup(float x, float y) {
        this.x = x;
        this.y = y;
        this.spawnTime = System.currentTimeMillis();
        
        // Create spawn effect with rising particles
        ParticleSystem.getInstance().createHealEffect(x, y, new Color(255, 100, 150));
    }
    
    public void update(float deltaTime) {
        // Despawn after 10 seconds
        if (System.currentTimeMillis() - spawnTime > 10000) {
            collected = true;
        }
    }
    
    public void draw(Graphics2D g2d) {
        if (collected) return;
        
        // Draw heart shape
        drawHeart(g2d, (int)x, (int)y, 16);
        
        // Pulsing glow effect
        float pulse = 0.5f + 0.5f * (float)Math.sin(System.currentTimeMillis() / 500.0);
        g2d.setColor(new Color(255, 0, 0, (int)(pulse * 100)));
        g2d.fillOval((int)x - 12, (int)y - 12, 32, 32);
    }
    
    private void drawHeart(Graphics2D g2d, int x, int y, int size) {
        // Create heart shape
        int[] xPoints = {
            x, x - size/2, x - size, x, x + size, x + size/2
        };
        int[] yPoints = {
            y + size/3, y - size/2, y - size/4, y + size, y - size/4, y - size/2
        };
        
        g2d.setColor(Color.RED);
        g2d.fillPolygon(xPoints, yPoints, 6);
        
        // White highlight
        g2d.setColor(Color.WHITE);
        g2d.fillOval(x - size/3, y - size/3, size/3, size/3);
    }
    
    public boolean checkCollision(Player player) {
        if (collected) return false;
        
        Rectangle pickupBounds = new Rectangle((int)x - 8, (int)y - 8, 16, 16);
        Rectangle playerBounds = player.getBounds();
        
        if (pickupBounds.intersects(playerBounds)) {
            collected = true;
            player.addHeart(); // Add one heart (max 3)
            
            // Create heal effect at collection point
            ParticleSystem.getInstance().createHealEffect(x, y, new Color(255, 100, 150));
            
            SoundManager.playSound(SoundManager.SoundType.PLAYER_POWERUP, 0.7f, 1.0f);
            return true;
        }
        return false;
    }
    
    public boolean isCollected() {
        return collected;
    }
}
