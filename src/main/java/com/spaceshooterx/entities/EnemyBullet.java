package com.spaceshooterx.entities;

import java.awt.*;
import java.awt.image.BufferedImage;

import com.spaceshooterx.audio.SoundManager;
import com.spaceshooterx.core.GameConfig;
import com.spaceshooterx.effects.ParticleSystem;

public class EnemyBullet {
    // Basic properties
    private float x, y;
    private float dx, dy;
    private int damage;
    private Color color;
    private boolean active;
    
    // Visual properties
    private BufferedImage bulletImage;
    private float rotation;
    private float lifetime = 300; // Frames before auto-destroy
    private float currentLife = 0;
    
    public EnemyBullet(float x, float y, float dx, float dy, int damage, Color color, String bulletType) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.damage = damage;
        this.color = color;
        this.active = true;
        this.rotation = 0;
        
        // Create proper bullet graphics
        createBulletGraphics();
        
        // Adjust speed for balanced gameplay
        adjustSpeed();
    }
    
    // Simple constructor for basic bullets with balanced speed
    public EnemyBullet(float x, float y, float speed) {
        this(x, y, 0.0f, speed, 12, new Color(220, 40, 40), "default");
    }
    
    private void createBulletGraphics() {
        // Create a proper red bullet with better visuals
        int size = 10;
        bulletImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = bulletImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Outer red glow
        GradientPaint outerGradient = new GradientPaint(
            size/2, 0, new Color(255, 60, 60, 200),
            size/2, size, new Color(180, 20, 20, 150)
        );
        g2d.setPaint(outerGradient);
        g2d.fillOval(0, 0, size, size);
        
        // Inner bright core
        g2d.setColor(new Color(255, 100, 100));
        g2d.fillOval(2, 2, size - 4, size - 4);
        
        // Hot center
        g2d.setColor(new Color(255, 180, 100));
        g2d.fillOval(4, 4, size - 8, size - 8);
        
        g2d.dispose();
    }
    
    private void adjustSpeed() {
        // Ensure bullets have balanced speed - not too fast, not too slow
        float currentSpeed = (float)Math.sqrt(dx * dx + dy * dy);
        float targetSpeed = 5.5f; // Balanced speed
        
        if (currentSpeed > 0) {
            float scale = targetSpeed / currentSpeed;
            dx *= scale;
            dy *= scale;
        } else {
            // Default downward speed
            dy = targetSpeed;
        }
        
        // Add slight random spread to prevent perfect alignment
        dx += (float)(Math.random() - 0.5) * 0.3f;
        dy += (float)(Math.random() - 0.5) * 0.2f;
    }
    
    public void update(float deltaTime) {
        if (!active) return;
        
        // Update life timer
        currentLife += deltaTime * 60;
        if (currentLife >= lifetime) {
            active = false;
            return;
        }
        
        // Update position
        x += dx * deltaTime * 60;
        y += dy * deltaTime * 60;
        
        // Update rotation based on direction
        if (Math.abs(dx) > 0.1f || Math.abs(dy) > 0.1f) {
            rotation = (float)Math.toDegrees(Math.atan2(dy, dx));
        }
        
        // Add slight gravity effect for curved trajectories
        dy += 0.02f * deltaTime * 60;
        
        // Check boundaries - deactivate if off screen
        if (y > GameConfig.HEIGHT + 30 || 
            y < -30 || 
            x < -30 || 
            x > GameConfig.WIDTH + 30) {
            active = false;
        }
    }
    
    // Simple update for 60 FPS
    public void update() {
        update(1.0f / 60.0f);
    }
    
    public void draw(Graphics2D g2d) {
        if (!active) return;
        
        // Draw glow trail effect
        drawTrail(g2d);
        
        // Draw bullet glow
        drawBulletGlow(g2d);
        
        // Draw bullet
        if (bulletImage != null) {
            // Save transform for rotation
            Graphics2D g2dCopy = (Graphics2D) g2d.create();
            
            // Rotate around bullet center
            g2dCopy.rotate(Math.toRadians(rotation), x + 5, y + 5);
            
            // Draw with pulsing effect
            float pulse = 0.8f + 0.2f * (float)Math.sin(currentLife * 0.1f);
            int drawSize = (int)(10 * pulse);
            int offset = (10 - drawSize) / 2;
            
            g2dCopy.drawImage(bulletImage, (int)x + offset, (int)y + offset, drawSize, drawSize, null);
            
            g2dCopy.dispose();
        } else {
            // Fallback: draw simple pulsing circle
            float pulse = 0.8f + 0.2f * (float)Math.sin(currentLife * 0.1f);
            g2d.setColor(color);
            g2d.fillOval((int)x, (int)y, (int)(10 * pulse), (int)(10 * pulse));
            
            g2d.setColor(color.brighter());
            g2d.fillOval((int)x + 2, (int)y + 2, (int)(6 * pulse), (int)(6 * pulse));
        }
    }
    
    private void drawTrail(Graphics2D g2d) {
        // Draw a subtle trail behind the bullet
        float trailLength = 8.0f;
        float trailWidth = 3.0f;
        
        // Trail gets more transparent as it gets further from bullet
        for (int i = 1; i <= 3; i++) {
            float alpha = 0.3f - i * 0.1f;
            if (alpha <= 0) continue;
            
            int trailX = (int)(x - dx * i * 2);
            int trailY = (int)(y - dy * i * 2);
            
            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(alpha * 255)));
            g2d.fillOval(trailX - (int)trailWidth/2, trailY - (int)trailWidth/2, 
                        (int)trailWidth, (int)trailWidth);
        }
    }
    
    private void drawBulletGlow(Graphics2D g2d) {
        // Draw outer glow
        int glowSize = 16;
        int glowAlpha = (int)(80 + 40 * Math.sin(currentLife * 0.15f));
        
        RadialGradientPaint glowPaint = new RadialGradientPaint(
            x + 5, y + 5, glowSize/2,
            new float[]{0.0f, 1.0f},
            new Color[]{
                new Color(255, 80, 80, glowAlpha),
                new Color(255, 40, 40, 0)
            }
        );
        
        g2d.setPaint(glowPaint);
        g2d.fillOval((int)(x + 5 - glowSize/2), (int)(y + 5 - glowSize/2), 
                    glowSize, glowSize);
    }
    
    public Rectangle getBounds() {
        // Slightly smaller hitbox for fair gameplay
        return new Rectangle((int)x + 2, (int)y + 2, 6, 6);
    }
    
    public void onHit() {
        active = false;
        
        // Create satisfying hit effect
        ParticleSystem.getInstance().createExplosion(
            x + 5, y + 5,
            15,
            color
        );
        
        // Add sparks
        for (int i = 0; i < 8; i++) {
            float angle = (float)(Math.random() * Math.PI * 2);
            float speed = 1.5f + (float)Math.random() * 2.0f;
            
            ParticleSystem.getInstance().createExplosion(
                x + 5, y + 5,
                8,
                new Color(255, 180, 100)
            );
        }
        
        SoundManager.playSound(SoundManager.SoundType.ENEMY_HIT, 0.4f, 1.1f);
    }
    
    // Getters
    public boolean isActive() { return active; }
    public float getX() { return x; }
    public float getY() { return y; }
    public int getDamage() { return damage; }
    public Color getColor() { return color; }
    
    // Setters
    public void setActive(boolean active) { this.active = active; }
    public void setDX(float dx) { this.dx = dx; }
    public void setDY(float dy) { this.dy = dy; }
    
    // Helper method to create spread bullets
    public static EnemyBullet[] createSpread(float x, float y, int count, float baseSpeed) {
        EnemyBullet[] bullets = new EnemyBullet[count];
        float spreadAngle = 30.0f; // Total spread in degrees
        
        for (int i = 0; i < count; i++) {
            float angle = (i - (count - 1) / 2.0f) * (spreadAngle / count);
            float rad = (float)Math.toRadians(angle);
            
            float bulletDx = (float)Math.sin(rad) * baseSpeed * 0.3f;
            float bulletDy = baseSpeed;
            
            bullets[i] = new EnemyBullet(x, y, bulletDx, bulletDy, 8, 
                                        new Color(220, 40, 40), "spread");
        }
        
        return bullets;
    }
    
    // Helper method to create rapid fire bullets
    public static EnemyBullet createRapid(float x, float y, float speed) {
        EnemyBullet bullet = new EnemyBullet(x, y, 0, speed * 1.2f, 6, 
                                           new Color(240, 60, 60), "rapid");
        // Make rapid bullets smaller visually
        bullet.damage = 8;
        return bullet;
    }
}