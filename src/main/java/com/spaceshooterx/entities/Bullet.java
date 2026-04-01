package com.spaceshooterx.entities;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import com.spaceshooterx.core.GameConfig;
import com.spaceshooterx.util.ImageUtils;

public class Bullet {
    public enum Pattern {
        SINGLE(1),          // 1 bullet
        TRIANGLE(3),        // 3 bullets (triangle)
        DIAMOND(5),         // 5 bullets (diamond)
        PENTAGON(5),        // 5 bullets (pentagon)
        HEXAGON(7),         // 7 bullets (hexagon)
        CROSS(5),           // 5 bullets (cross)
        ARROW(4),           // 4 bullets (arrow shape)
        INVERSE_TRIANGLE(6); // 6 bullets (inverse triangle)

        private final int bulletCount;
        
        Pattern(int bulletCount) {
            this.bulletCount = bulletCount;
        }
        
        public int getBulletCount() {
            return bulletCount;
        }
    }

    private float x, y;
    private float dx;
    private float dy;
    private boolean visible;
    private BufferedImage bulletImage;
    private int width;
    private int height;
    private int baseDamage = 1;
    private Pattern pattern = Pattern.SINGLE;
    private List<Point> patternOffsets = new ArrayList<>();
    private Color color = Color.WHITE;

    public Bullet(int x, int y) {
        this(x, y, 0, -GameConfig.BULLET_SPEED, Pattern.SINGLE);
    }

    public Bullet(int x, int y, float dx, float dy, Pattern pattern) {
        this(x, y, dx, dy, pattern, 1, Color.WHITE);
    }

    public Bullet(int x, int y, float dx, float dy, Pattern pattern, int baseDamage, Color color) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.pattern = pattern;
        this.baseDamage = baseDamage;
        this.color = color;
        this.visible = true;
        
        loadBulletImage();
        setupPattern();
    }

    private void loadBulletImage() {
        BufferedImage tempImage = ImageUtils.loadImage("/Inventory/bullet.png");
        if (tempImage != null) {
            // Make bullets smaller for better pattern spacing
            int bulletSize = Math.max(8, GameConfig.BULLET_SIZE - 6);
            bulletImage = ImageUtils.resizeImage(tempImage, bulletSize, bulletSize);
            width = bulletSize;
            height = bulletSize;
        } else {
            width = GameConfig.BULLET_SIZE;
            height = GameConfig.BULLET_SIZE;
        }
    }

    private void setupPattern() {
        patternOffsets.clear();
        int bulletSpacing = width + 4; // Reduced spacing between bullets
        
        // For better visualization, let's make a pyramid pattern
        switch (pattern) {
            case SINGLE:
                // Single bullet at center
                patternOffsets.add(new Point(0, 0));
                break;
                
            case TRIANGLE:
                // Triangle pattern (3 bullets)
                // Pyramid shape: row of 2, then 1 above
                patternOffsets.add(new Point(0, -bulletSpacing));           // Top
                patternOffsets.add(new Point(-bulletSpacing, 0));           // Bottom-left
                patternOffsets.add(new Point(bulletSpacing, 0));            // Bottom-right
                break;
                
            case DIAMOND:
                // Diamond pattern (5 bullets)
                // Pyramid shape: 1, 2, 2
                patternOffsets.add(new Point(0, -bulletSpacing * 2));       // Top (1st row)
                patternOffsets.add(new Point(-bulletSpacing, -bulletSpacing)); // 2nd row left
                patternOffsets.add(new Point(bulletSpacing, -bulletSpacing));  // 2nd row right
                patternOffsets.add(new Point(-bulletSpacing, 0));           // 3rd row left
                patternOffsets.add(new Point(bulletSpacing, 0));            // 3rd row right
                break;
                
            case PENTAGON:
                // Pentagon pattern (5 bullets)
                // Pyramid shape: 1, 2, 2 (but wider)
                int pentagonSpacing = bulletSpacing;
                patternOffsets.add(new Point(0, -pentagonSpacing * 2));     // Top (1st row)
                patternOffsets.add(new Point(-pentagonSpacing, -pentagonSpacing)); // 2nd row left
                patternOffsets.add(new Point(pentagonSpacing, -pentagonSpacing));  // 2nd row right
                patternOffsets.add(new Point(-pentagonSpacing * 2, 0));     // 3rd row far left
                patternOffsets.add(new Point(pentagonSpacing * 2, 0));      // 3rd row far right
                break;
                
            case HEXAGON:
                // Hexagon pattern (7 bullets)
                // Pyramid shape: 1, 2, 3, 1
                int hexSpacing = bulletSpacing;
                patternOffsets.add(new Point(0, -hexSpacing * 3));          // Top (1st row)
                patternOffsets.add(new Point(-hexSpacing, -hexSpacing * 2)); // 2nd row left
                patternOffsets.add(new Point(hexSpacing, -hexSpacing * 2));  // 2nd row right
                patternOffsets.add(new Point(-hexSpacing * 2, -hexSpacing)); // 3rd row far left
                patternOffsets.add(new Point(0, -hexSpacing));              // 3rd row center
                patternOffsets.add(new Point(hexSpacing * 2, -hexSpacing));  // 3rd row far right
                patternOffsets.add(new Point(0, 0));                         // 4th row center
                break;
                
            case CROSS:
                // Cross pattern (5 bullets)
                // Plus shape
                patternOffsets.add(new Point(0, 0));                        // Center
                patternOffsets.add(new Point(0, -bulletSpacing));           // Top
                patternOffsets.add(new Point(bulletSpacing, 0));            // Right
                patternOffsets.add(new Point(0, bulletSpacing));            // Bottom
                patternOffsets.add(new Point(-bulletSpacing, 0));           // Left
                break;
                
            case ARROW:
                // Arrow pattern (4 bullets)
                // Pointing up
                patternOffsets.add(new Point(0, -bulletSpacing * 2));       // Tip
                patternOffsets.add(new Point(-bulletSpacing, -bulletSpacing)); // Left wing
                patternOffsets.add(new Point(0, -bulletSpacing));           // Center
                patternOffsets.add(new Point(bulletSpacing, -bulletSpacing));  // Right wing
                break;
                
            case INVERSE_TRIANGLE:
                // Inverse triangle (6 bullets)
                // Pyramid upside down: 3, 2, 1
                int invSpacing = bulletSpacing;
                patternOffsets.add(new Point(-invSpacing, -invSpacing));    // Top row left
                patternOffsets.add(new Point(0, -invSpacing));              // Top row center
                patternOffsets.add(new Point(invSpacing, -invSpacing));     // Top row right
                patternOffsets.add(new Point(-invSpacing/2, 0));            // Middle row left
                patternOffsets.add(new Point(invSpacing/2, 0));             // Middle row right
                patternOffsets.add(new Point(0, invSpacing));               // Bottom row center
                break;
        }
        
        // Validate that we have the correct number of bullets
        if (patternOffsets.size() != pattern.getBulletCount()) {
            System.err.println("Warning: Pattern " + pattern + " expects " + 
                             pattern.getBulletCount() + " bullets but has " + 
                             patternOffsets.size());
        }
    }

    public void draw(Graphics g) {
        if (!visible) return;
        
        if (bulletImage != null) {
            // Draw each bullet in the pattern
            for (Point offset : patternOffsets) {
                int drawX = (int) x + offset.x - width/2;
                int drawY = (int) y + offset.y - height/2;
                g.drawImage(bulletImage, drawX, drawY, null);
            }
        } else {
            // Fallback to colored circles with highlights
            for (Point offset : patternOffsets) {
                int drawX = (int) x + offset.x - width/2;
                int drawY = (int) y + offset.y - height/2;
                
                // Main bullet
                g.setColor(color);
                g.fillOval(drawX, drawY, width, height);
                
                // Highlight
                g.setColor(Color.WHITE);
                g.fillOval(drawX + width/4, drawY + height/4, width/2, height/2);
            }
        }
        
        // Optional: Draw pattern bounds for debugging
        if (GameConfig.DEBUG_MODE) {
            g.setColor(Color.YELLOW);
            Rectangle bounds = getBoundingBox();
            g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
            
            // Draw bullet count
            g.setColor(Color.WHITE);
            g.drawString("Bullets: " + patternOffsets.size(), 
                        (int)x - 20, (int)y - 30);
        }
    }

    public void update(float deltaTime) {
        if (!visible) return;
        
        x += dx * deltaTime * 60f;
        y += dy * deltaTime * 60f;
        
        // Check if all pattern bullets are off screen
        boolean allOffScreen = true;
        for (Point offset : patternOffsets) {
            float bulletX = x + offset.x;
            float bulletY = y + offset.y;
            
            // Add margin for pattern size
            float margin = Math.max(width, height) * 4;
            if (bulletY > -margin && bulletY < GameConfig.HEIGHT + margin &&
                bulletX > -margin && bulletX < GameConfig.WIDTH + margin) {
                allOffScreen = false;
                break;
            }
        }
        
        if (allOffScreen) {
            visible = false;
        }
    }

    public List<Rectangle> getAllHitboxes() {
        List<Rectangle> hitboxes = new ArrayList<>();
        for (Point offset : patternOffsets) {
            hitboxes.add(new Rectangle(
                (int) x + offset.x - width/2, 
                (int) y + offset.y - height/2, 
                width, 
                height
            ));
        }
        return hitboxes;
    }

    public Rectangle getBoundingBox() {
        if (patternOffsets.isEmpty()) {
            return new Rectangle((int) x - width/2, (int) y - height/2, width, height);
        }
        
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        
        for (Point offset : patternOffsets) {
            int bulletX = (int) x + offset.x - width/2;
            int bulletY = (int) y + offset.y - height/2;
            
            minX = Math.min(minX, bulletX);
            minY = Math.min(minY, bulletY);
            maxX = Math.max(maxX, bulletX + width);
            maxY = Math.max(maxY, bulletY + height);
        }
        
        return new Rectangle(minX, minY, maxX - minX, maxY - minY);
    }

    public boolean checkCollision(Rectangle other) {
        for (Rectangle hitbox : getAllHitboxes()) {
            if (hitbox.intersects(other)) {
                return true;
            }
        }
        return false;
    }

    // Getters and Setters
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
    
    public int getDamage() { 
        // Damage per bullet = total damage / number of bullets
        // Minimum 1 damage per bullet
        return Math.max(1, baseDamage / pattern.getBulletCount());
    }
    
    public int getTotalPatternDamage() {
        // Total damage if all bullets hit
        return baseDamage;
    }
    
    public int getBaseDamage() {
        return baseDamage;
    }
    
    public void setBaseDamage(int baseDamage) {
        this.baseDamage = baseDamage;
    }
    
    public Pattern getPattern() { return pattern; }
    public int getBulletCount() { return patternOffsets.size(); }
    public boolean isActive() { return visible; }
    public float getY() { return y; }
    public float getX() { return x; }
    public float getDX() { return dx; }
    public float getDY() { return dy; }
    public Color getColor() { return color; }
    
    public void setVelocity(float dx, float dy) {
        this.dx = dx;
        this.dy = dy;
    }
    
    // Helper method to create pattern variations
    public static List<Bullet> createPatternWithSpread(int centerX, int centerY, Pattern pattern, 
                                                      float baseSpeed, float spread) {
        List<Bullet> bullets = new ArrayList<>();
        
        if (pattern == Pattern.SINGLE) {
            bullets.add(new Bullet(centerX, centerY, 0, -baseSpeed, pattern));
            return bullets;
        }
        
        // For multi-bullet patterns, create a single bullet entity with the pattern
        bullets.add(new Bullet(centerX, centerY, 0, -baseSpeed, pattern));
        return bullets;
    }
}