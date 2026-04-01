package com.spaceshooterx.entities;

import java.awt.*;

import com.spaceshooterx.audio.SoundManager;
import com.spaceshooterx.effects.ParticleSystem;

public class ShieldPickup {
    private float x, y;
    private boolean collected = false;
    private long spawnTime;
    
    public ShieldPickup(float x, float y) {
        this.x = x;
        this.y = y;
        this.spawnTime = System.currentTimeMillis();
        
        // Create spawn effect
        ParticleSystem.getInstance().createShieldEffect(x, y, 20, new Color(100, 200, 255));
    }
    
    public void update(float deltaTime) {
        // Despawn after 10 seconds
        if (System.currentTimeMillis() - spawnTime > 10000) {
            collected = true;
        }
    }
    
    public void draw(Graphics2D g2d) {
        if (collected) return;
        
        // Draw shield shape
        drawShield(g2d, (int)x, (int)y, 16);
        
        // Pulsing glow effect
        float pulse = 0.5f + 0.5f * (float)Math.sin(System.currentTimeMillis() / 500.0);
        g2d.setColor(new Color(0, 150, 255, (int)(pulse * 100)));
        g2d.fillOval((int)x - 12, (int)y - 12, 32, 32);
    }
    
    private void drawShield(Graphics2D g2d, int x, int y, int size) {
        // Draw shield as a circle with cross
        g2d.setColor(Color.BLUE);
        g2d.fillOval(x - size/2, y - size/2, size, size);
        
        // White cross
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawLine(x - size/4, y, x + size/4, y);
        g2d.drawLine(x, y - size/4, x, y + size/4);
        
        // Outer ring
        g2d.setStroke(new BasicStroke(2));
        g2d.setColor(Color.CYAN);
        g2d.drawOval(x - size/2, y - size/2, size, size);
    }
    
    public boolean checkCollision(Player player) {
        if (collected) return false;
        
        Rectangle pickupBounds = new Rectangle((int)x - 8, (int)y - 8, 16, 16);
        Rectangle playerBounds = player.getBounds();
        
        if (pickupBounds.intersects(playerBounds)) {
            collected = true;
            player.activateShield(5000); // 5 second shield
            
            // Create shield effect at collection point
            ParticleSystem.getInstance().createShieldEffect(x, y, 30, new Color(100, 200, 255));
            
            SoundManager.playSound(SoundManager.SoundType.SHIELD_ACTIVATE, 0.7f, 1.0f);
            return true;
        }
        return false;
    }
    
    public boolean isCollected() {
        return collected;
    }
}
