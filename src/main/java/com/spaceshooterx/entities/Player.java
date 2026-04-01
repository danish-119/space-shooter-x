package com.spaceshooterx.entities;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.awt.image.BufferedImage;

import com.spaceshooterx.audio.SoundManager;
import com.spaceshooterx.core.GameConfig;
import com.spaceshooterx.util.ImageUtils;
import com.spaceshooterx.effects.ParticleSystem;
import com.spaceshooterx.effects.AnimationManager;

public class Player {
    private float x, y;
    private float dx, dy;
    private float targetDx, targetDy;
    
    // State management
    private float health = GameConfig.PLAYER_HEALTH;
    private float shield = GameConfig.PLAYER_INITIAL_SHIELD;
    private boolean isInvulnerable = false;
    private long invulnerableEndTime = 0;
    private long shieldActiveUntil = 0;
    
    // Bullets
    private final List<Bullet> bullets;
    
    // Bullet pattern system
    private Bullet.Pattern currentPattern = Bullet.Pattern.SINGLE;
    private int patternLevel = 1;
    private int maxUnlockedPattern = 1; // Maximum pattern level unlocked based on game level
    private boolean rapidFireActive = false;
    private long rapidFireEndTime = 0;
    private boolean spreadActive = false;
    private long spreadEndTime = 0;
    
    // Visual properties
    private BufferedImage spaceshipImage;
    private BufferedImage shieldImage;
    private BufferedImage[] engineFlames;
    
    // Animation & effects
    private float damageFlash = 0.0f;
    private float engineGlow = 0.0f;
    
    // Timing & cooldowns
    private long lastShotTime = 0;
    private float shotCooldown = GameConfig.PLAYER_SHOT_COOLDOWN;
    private int shotDamage = GameConfig.PLAYER_SHOT_DAMAGE;
    
    // Input state
    private boolean isFiring = false;
    private boolean isMovingLeft = false;
    private boolean isMovingRight = false;
    private boolean isMovingUp = false;
    private boolean isMovingDown = false;
    
    public Player() {
        // Initialize position
        x = GameConfig.WIDTH / 2.0f - GameConfig.PLAYER_SIZE / 2.0f;
        y = GameConfig.HEIGHT - GameConfig.PLAYER_SIZE - 50.0f;
        
        // Initialize collections
        bullets = new ArrayList<>();
        
        // Load graphics
        loadShipGraphics();
        
        SoundManager.playEngineHum(GameConfig.ENGINE_HUM_VOLUME);
    }
    
    private void loadShipGraphics() {
        try {
            // Load main ship sprite
            spaceshipImage = ImageUtils.loadImage("/Inventory/spaceship1.png");
            if (spaceshipImage == null) {
                spaceshipImage = createProceduralShip();
            }
            
            // Resize
            spaceshipImage = ImageUtils.resizeImage(spaceshipImage, GameConfig.PLAYER_SIZE, GameConfig.PLAYER_SIZE);
            
            // Create engine flames
            engineFlames = new BufferedImage[8];
            for (int i = 0; i < 8; i++) {
                engineFlames[i] = createEngineFlame(i);
            }
            
        } catch (Exception e) {
            System.err.println("Error loading player graphics: " + e.getMessage());
            createFallbackGraphics();
        }
    }
    
    private BufferedImage createProceduralShip() {
        BufferedImage img = new BufferedImage(
            GameConfig.PLAYER_SIZE, 
            GameConfig.PLAYER_SIZE, 
            BufferedImage.TYPE_INT_ARGB
        );
        
        Graphics2D g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Ship body
        g2d.setColor(new Color(GameConfig.PLAYER_COLOR_R, GameConfig.PLAYER_COLOR_G, GameConfig.PLAYER_COLOR_B));
        g2d.fillRect(0, 0, GameConfig.PLAYER_SIZE, GameConfig.PLAYER_SIZE);
        
        // Cockpit
        g2d.setColor(new Color(100, 200, 255));
        g2d.fillOval(GameConfig.PLAYER_SIZE/4, GameConfig.PLAYER_SIZE/4, 
                     GameConfig.PLAYER_SIZE/2, GameConfig.PLAYER_SIZE/2);
        
        g2d.dispose();
        return img;
    }
    
    private BufferedImage createEngineFlame(int frame) {
        BufferedImage img = new BufferedImage(24, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Dynamic intensity with sine wave animation
        float intensity = 0.7f + 0.3f * (float)Math.sin(frame * Math.PI / 4);
        float sizeVariation = 0.8f + 0.2f * (float)Math.cos(frame * Math.PI / 3);
        
        // Flame dimensions
        int flameWidth = (int)(12 * sizeVariation);
        int flameHeight = (int)(30 * intensity);
        int centerX = 12;
        
        // Outer glow (blue-white)
        g2d.setColor(new Color(100, 150, 255, (int)(80 * intensity)));
        g2d.fillOval(centerX - flameWidth - 2, 0, (flameWidth + 2) * 2, flameHeight + 10);
        
        // Middle flame (orange-yellow)
        GradientPaint gradient = new GradientPaint(
            centerX, 0, new Color(255, 220, 100, (int)(230 * intensity)),
            centerX, flameHeight, new Color(255, 100, 0, (int)(150 * intensity))
        );
        g2d.setPaint(gradient);
        g2d.fillOval(centerX - flameWidth, 0, flameWidth * 2, flameHeight);
        
        // Inner core (bright white-yellow)
        g2d.setColor(new Color(255, 255, 200, (int)(200 * intensity)));
        int coreWidth = (int)(flameWidth * 0.5f);
        int coreHeight = (int)(flameHeight * 0.6f);
        g2d.fillOval(centerX - coreWidth, 0, coreWidth * 2, coreHeight);
        
        // Hot spot at the top
        g2d.setColor(new Color(255, 255, 255, (int)(180 * intensity)));
        g2d.fillOval(centerX - 2, 0, 4, 6);
        
        g2d.dispose();
        return img;
    }
    
    private void createFallbackGraphics() {
        spaceshipImage = new BufferedImage(
            GameConfig.PLAYER_SIZE, 
            GameConfig.PLAYER_SIZE, 
            BufferedImage.TYPE_INT_ARGB
        );
        
        Graphics2D g2d = spaceshipImage.createGraphics();
        g2d.setColor(Color.CYAN);
        g2d.fillRect(0, 0, GameConfig.PLAYER_SIZE, GameConfig.PLAYER_SIZE);
        g2d.dispose();
    }
    
    public void update(float deltaTime, int panelWidth, int panelHeight) {
        // Update movement
        updateMovement(deltaTime, panelWidth, panelHeight);
        
        // Update invulnerability
        updateInvulnerability();
        
        // Update power-ups
        updatePowerUps();
        
        // Update bullets
        updateBullets(deltaTime, panelHeight);
        
        // Auto-fire if holding fire button
        if (isFiring) {
            tryShoot();
        }
        
        // Update animations
        updateAnimations(deltaTime);
    }
    
    public void update(int panelWidth, int panelHeight) {
        update(1.0f / 60.0f, panelWidth, panelHeight);
    }
    
    private void updateMovement(float deltaTime, int panelWidth, int panelHeight) {
        // Calculate movement based on input
        dx = 0;
        dy = 0;
        
        if (isMovingLeft) dx = -GameConfig.PLAYER_SPEED;
        if (isMovingRight) dx = GameConfig.PLAYER_SPEED;
        if (isMovingUp) dy = -GameConfig.PLAYER_SPEED;
        if (isMovingDown) dy = GameConfig.PLAYER_SPEED;
        
        // Normalize diagonal movement
        if (dx != 0 && dy != 0) {
            float len = (float)Math.sqrt(dx * dx + dy * dy);
            dx = dx / len * GameConfig.PLAYER_SPEED;
            dy = dy / len * GameConfig.PLAYER_SPEED;
        }
        
        // Update position
        x += dx * deltaTime * 60;
        y += dy * deltaTime * 60;
        
        // Apply boundaries
        x = Math.max(0, Math.min(panelWidth - GameConfig.PLAYER_SIZE, x));
        y = Math.max(0, Math.min(panelHeight - GameConfig.PLAYER_SIZE, y));
        
        // Update engine glow based on movement
        float speed = (float)Math.sqrt(dx * dx + dy * dy);
        engineGlow = Math.min(1.0f, speed / GameConfig.PLAYER_SPEED);
    }
    
    private void updateInvulnerability() {
        if (isInvulnerable && System.currentTimeMillis() > invulnerableEndTime) {
            isInvulnerable = false;
        }
    }
    
    private void updatePowerUps() {
        if (rapidFireActive && System.currentTimeMillis() > rapidFireEndTime) {
            rapidFireActive = false;
        }
        
        if (spreadActive && System.currentTimeMillis() > spreadEndTime) {
            spreadActive = false;
            // Reset to current pattern level
            updatePatternFromLevel();
        }
    }
    
    private void updatePatternFromLevel() {
        // Set pattern based on level
        switch (patternLevel) {
            case 1: currentPattern = Bullet.Pattern.SINGLE; break;
            case 2: currentPattern = Bullet.Pattern.TRIANGLE; break;
            case 3: currentPattern = Bullet.Pattern.ARROW; break;
            case 4: currentPattern = Bullet.Pattern.DIAMOND; break;
            case 5: currentPattern = Bullet.Pattern.PENTAGON; break;
            case 6: currentPattern = Bullet.Pattern.CROSS; break;
            case 7: currentPattern = Bullet.Pattern.INVERSE_TRIANGLE; break;
            case 8: 
            default: currentPattern = Bullet.Pattern.HEXAGON; break;
        }
    }
    
    private void updateBullets(float deltaTime, int panelHeight) {
        Iterator<Bullet> bulletIter = bullets.iterator();
        while (bulletIter.hasNext()) {
            Bullet bullet = bulletIter.next();
            bullet.update(deltaTime);
            if (!bullet.isActive() || bullet.getY() < -50) {
                bulletIter.remove();
            }
        }
    }
    
    private void updateAnimations(float deltaTime) {
        // Fade damage flash
        if (damageFlash > 0) {
            damageFlash = Math.max(0, damageFlash - deltaTime * 2.0f);
        }
    }
    
    public void draw(Graphics2D g2d) {
        // Draw engine flames
        drawEngineFlames(g2d);
        
        // Apply damage flash
        if (damageFlash > 0) {
            g2d.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, 
                1.0f - damageFlash * 0.5f
            ));
            g2d.setColor(new Color(255, 0, 0, (int)(damageFlash * 100)));
            g2d.fillRect((int)x, (int)y, GameConfig.PLAYER_SIZE, GameConfig.PLAYER_SIZE);
        }
        
        // Draw ship
        if (spaceshipImage != null) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            g2d.drawImage(spaceshipImage, (int)x, (int)y, null);
        } else {
            // Fallback
            g2d.setColor(Color.CYAN);
            g2d.fillRect((int)x, (int)y, GameConfig.PLAYER_SIZE, GameConfig.PLAYER_SIZE);
        }
        
        // Draw shield if active
        if (shield > 0) {
            drawShield(g2d);
        }
        
        // Draw bullets
        for (Bullet bullet : bullets) {
            bullet.draw(g2d);
        }
    }
    
    private void drawEngineFlames(Graphics2D g2d) {
        if (engineFlames == null || engineFlames.length == 0) return;
        
        // Constant animation speed for smooth continuous flame
        int frame = (int)((System.currentTimeMillis() / 50) % engineFlames.length);
        BufferedImage flame = engineFlames[frame];
        
        // Calculate flame intensity based on movement
        float flameIntensity = 0.6f + engineGlow * 0.4f;
        
        // Add slight random flicker
        float flicker = 0.9f + (float)Math.random() * 0.1f;
        
        // Set composite for transparency
        Composite originalComposite = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(
            AlphaComposite.SRC_OVER, 
            flameIntensity * flicker
        ));
        
        // Engine positions
        int leftEngineX = (int)x + 8;
        int rightEngineX = (int)x + GameConfig.PLAYER_SIZE - 20;
        int engineY = (int)y + GameConfig.PLAYER_SIZE - 10;
        
        // Add vertical offset based on movement (flames trail when moving fast)
        int trailOffset = (int)(engineGlow * 5);
        
        // Draw outer glow for each engine
        g2d.setComposite(AlphaComposite.getInstance(
            AlphaComposite.SRC_OVER, 
            0.3f * flameIntensity
        ));
        g2d.setColor(new Color(255, 150, 50));
        g2d.fillOval(leftEngineX - 8, engineY - 5, 28, 35);
        g2d.fillOval(rightEngineX - 8, engineY - 5, 28, 35);
        
        // Draw main flames
        g2d.setComposite(AlphaComposite.getInstance(
            AlphaComposite.SRC_OVER, 
            flameIntensity * flicker
        ));
        
        // Left engine flame
        g2d.drawImage(flame, leftEngineX, engineY + trailOffset, null);
        
        // Right engine flame
        g2d.drawImage(flame, rightEngineX, engineY + trailOffset, null);
        
        // Restore original composite
        g2d.setComposite(originalComposite);
    }
    
    private void drawShield(Graphics2D g2d) {
        float shieldAlpha = shield / 100.0f;
        g2d.setColor(new Color(0, 200, 255, (int)(shieldAlpha * 100)));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval((int)x - 5, (int)y - 5, 
                    GameConfig.PLAYER_SIZE + 10, GameConfig.PLAYER_SIZE + 10);
    }
    
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        
        // Movement
        if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) {
            isMovingLeft = true;
        }
        if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) {
            isMovingRight = true;
        }
        if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) {
            isMovingUp = true;
        }
        if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) {
            isMovingDown = true;
        }
        
        // Firing
        if (key == KeyEvent.VK_SPACE) {
            isFiring = true;
            tryShoot();
        }
        
        // Pattern selection (number keys 1-8)
        if (key >= KeyEvent.VK_1 && key <= KeyEvent.VK_8) {
            int patternNum = key - KeyEvent.VK_1 + 1;
            
            // Check if pattern is unlocked
            if (patternNum <= maxUnlockedPattern) {
                setBulletPattern(patternNum);
                SoundManager.playSound(SoundManager.SoundType.UI_CLICK);
            } else {
                // Pattern is locked - play error sound
                SoundManager.playSound(SoundManager.SoundType.UI_ERROR);
            }
        }
    }
    
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        
        // Movement
        if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) {
            isMovingLeft = false;
        }
        if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) {
            isMovingRight = false;
        }
        if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) {
            isMovingUp = false;
        }
        if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) {
            isMovingDown = false;
        }
        
        // Firing
        if (key == KeyEvent.VK_SPACE) {
            isFiring = false;
        }
    }
    
    private void tryShoot() {
        if (System.currentTimeMillis() - lastShotTime < shotCooldown * 1000) {
            return;
        }
        
        // Adjust cooldown for rapid fire
        float currentCooldown = shotCooldown;
        if (rapidFireActive) {
            currentCooldown *= 0.5f;
        }
        
        if (System.currentTimeMillis() - lastShotTime < currentCooldown * 1000) {
            return;
        }
        
        // Determine pattern to use
        Bullet.Pattern patternToUse = currentPattern;
        if (spreadActive) {
            // Use spread pattern instead of current pattern
            patternToUse = Bullet.Pattern.HEXAGON; // Wide spread pattern
        }
        
        // Calculate bullet position (center of ship)
        int bulletX = (int)(x + GameConfig.PLAYER_SIZE / 2);
        int bulletY = (int)y;
        
        // Calculate damage per bullet
        int totalDamage = shotDamage * patternLevel;
        int damagePerBullet = Math.max(1, totalDamage / patternToUse.getBulletCount());
        
        // Create INDEPENDENT bullets for each position in the pattern
        List<Point> offsets = getPatternOffsets(patternToUse);
        for (Point offset : offsets) {
            // Each bullet is independent with its own position
            bullets.add(new Bullet(
                bulletX + offset.x,
                bulletY + offset.y,
                0,  // dx
                -GameConfig.BULLET_SPEED * 1.5f,  // dy
                Bullet.Pattern.SINGLE,  // Each is a single bullet
                damagePerBullet,  // Damage per individual bullet
                new Color(100, 200, 255)  // Cyan color
            ));
        }
        
        // Add muzzle flash effect
        ParticleSystem.getInstance().createMuzzleFlash(
            bulletX, bulletY, 
            0, -GameConfig.BULLET_SPEED * 1.5f,
            new Color(100, 200, 255)
        );
        
        // Play sound
        SoundManager.playSound(SoundManager.SoundType.LASER_BASIC, 
                             0.7f + patternLevel * 0.05f, 
                             1.0f);
        
        lastShotTime = System.currentTimeMillis();
    }
    
    // Helper method to get pattern offsets
    private List<Point> getPatternOffsets(Bullet.Pattern pattern) {
        List<Point> offsets = new ArrayList<>();
        int bulletSize = 8; // Match bullet size
        int bulletSpacing = bulletSize + 4; // Reduced spacing
        
        switch (pattern) {
            case SINGLE:
                offsets.add(new Point(0, 0));
                break;
                
            case TRIANGLE:
                offsets.add(new Point(0, -bulletSpacing));
                offsets.add(new Point(-bulletSpacing, 0));
                offsets.add(new Point(bulletSpacing, 0));
                break;
                
            case DIAMOND:
                offsets.add(new Point(0, -bulletSpacing * 2));
                offsets.add(new Point(-bulletSpacing, -bulletSpacing));
                offsets.add(new Point(bulletSpacing, -bulletSpacing));
                offsets.add(new Point(-bulletSpacing, 0));
                offsets.add(new Point(bulletSpacing, 0));
                break;
                
            case PENTAGON:
                offsets.add(new Point(0, -bulletSpacing * 2));
                offsets.add(new Point(-bulletSpacing, -bulletSpacing));
                offsets.add(new Point(bulletSpacing, -bulletSpacing));
                offsets.add(new Point(-bulletSpacing * 2, 0));
                offsets.add(new Point(bulletSpacing * 2, 0));
                break;
                
            case HEXAGON:
                offsets.add(new Point(0, -bulletSpacing * 3));
                offsets.add(new Point(-bulletSpacing, -bulletSpacing * 2));
                offsets.add(new Point(bulletSpacing, -bulletSpacing * 2));
                offsets.add(new Point(-bulletSpacing * 2, -bulletSpacing));
                offsets.add(new Point(0, -bulletSpacing));
                offsets.add(new Point(bulletSpacing * 2, -bulletSpacing));
                offsets.add(new Point(0, 0));
                break;
                
            case CROSS:
                offsets.add(new Point(0, 0));
                offsets.add(new Point(0, -bulletSpacing));
                offsets.add(new Point(bulletSpacing, 0));
                offsets.add(new Point(0, bulletSpacing));
                offsets.add(new Point(-bulletSpacing, 0));
                break;
                
            case ARROW:
                offsets.add(new Point(0, -bulletSpacing * 2));
                offsets.add(new Point(-bulletSpacing, -bulletSpacing));
                offsets.add(new Point(0, -bulletSpacing));
                offsets.add(new Point(bulletSpacing, -bulletSpacing));
                break;
                
            case INVERSE_TRIANGLE:
                offsets.add(new Point(-bulletSpacing, -bulletSpacing));
                offsets.add(new Point(0, -bulletSpacing));
                offsets.add(new Point(bulletSpacing, -bulletSpacing));
                offsets.add(new Point(-bulletSpacing/2, 0));
                offsets.add(new Point(bulletSpacing/2, 0));
                offsets.add(new Point(0, bulletSpacing));
                break;
        }
        
        return offsets;
    }
    
    public void setBulletPattern(int patternLevel) {
        // Clamp to unlocked patterns
        this.patternLevel = Math.max(1, Math.min(maxUnlockedPattern, patternLevel));
        updatePatternFromLevel();
    }
    
    public void updateUnlockedPatterns(int gameLevel) {
        // Unlock patterns based on game level (max 8)
        maxUnlockedPattern = Math.min(8, gameLevel);
        
        // If current pattern is now locked, revert to max unlocked
        if (patternLevel > maxUnlockedPattern) {
            setBulletPattern(maxUnlockedPattern);
        }
    }
    
    public void activateRapidFire(long durationMs) {
        rapidFireActive = true;
        rapidFireEndTime = System.currentTimeMillis() + durationMs;
        SoundManager.playSound(SoundManager.SoundType.PLAYER_POWERUP);
    }
    
    public void activateSpread(long durationMs) {
        spreadActive = true;
        spreadEndTime = System.currentTimeMillis() + durationMs;
        SoundManager.playSound(SoundManager.SoundType.PLAYER_POWERUP);
    }
    
    public void upgradePattern() {
        if (patternLevel < 8) {
            patternLevel++;
            updatePatternFromLevel();
            SoundManager.playSound(SoundManager.SoundType.LEVEL_UP);
            
            // Visual upgrade effect
            ParticleSystem.getInstance().createUpgradeEffect(
                x + GameConfig.PLAYER_SIZE / 2,
                y + GameConfig.PLAYER_SIZE / 2,
                Color.CYAN
            );
        }
    }
    
    public boolean takeDamage(int damage) {
        if (isInvulnerable) {
            return false;
        }
        
        // Apply to shield first
        if (shield > 0) {
            float shieldDamage = Math.min(damage, shield);
            shield -= shieldDamage;
            damage -= shieldDamage;
            
            if (shield <= 0) {
                SoundManager.playSound(SoundManager.SoundType.SHIELD_HIT);
            }
        }
        
        // Apply remaining damage to health
        if (damage > 0) {
            health -= damage;
            damageFlash = 1.0f;
            
            // Create hit particles
            ParticleSystem.getInstance().createExplosion(
                x + GameConfig.PLAYER_SIZE / 2,
                y + GameConfig.PLAYER_SIZE / 2,
                10,
                Color.RED
            );
            
            SoundManager.playSound(SoundManager.SoundType.PLAYER_HIT);
            
            // Check if dead
            if (health <= 0) {
                die();
                return true;
            }
            
            // Become invulnerable briefly
            setInvulnerable(1000);
        }
        
        return false;
    }
    
    private void die() {
        // Create death explosion
        ParticleSystem.getInstance().createLargeExplosion(
            x + GameConfig.PLAYER_SIZE / 2,
            y + GameConfig.PLAYER_SIZE / 2,
            Color.RED,
            Color.ORANGE
        );
        
        SoundManager.stopEngineHum();
        SoundManager.playSound(SoundManager.SoundType.PLAYER_DEATH);
    }
    
    public void heal(float amount) {
        health = Math.min(100.0f, health + amount);
        if (amount > 0) {
            ParticleSystem.getInstance().createHealEffect(
                x + GameConfig.PLAYER_SIZE / 2,
                y + GameConfig.PLAYER_SIZE / 2,
                Color.GREEN
            );
            
            SoundManager.playSound(SoundManager.SoundType.LEVEL_UP);
        }
    }
    
    public void addShield(float amount) {
        shield = Math.min(100.0f, shield + amount);
        SoundManager.playSound(SoundManager.SoundType.SHIELD_ACTIVATE);
    }
    
    public void setInvulnerable(long durationMs) {
        isInvulnerable = true;
        invulnerableEndTime = System.currentTimeMillis() + durationMs;
    }
    
    public void stopShooting() {
        isFiring = false;
    }

    // In Player.java, add these methods:

    public void addHeart() {
        // Add 33.33 health (one heart's worth)
        heal(33.33f);
        SoundManager.playSound(SoundManager.SoundType.PLAYER_POWERUP);
    }

    public void activateShield(long durationMs) {
        // Add 100 shield (full shield)
        addShield(100.0f);
        shieldActiveUntil = System.currentTimeMillis() + durationMs;
        SoundManager.playSound(SoundManager.SoundType.SHIELD_ACTIVATE);
    }
    
    // Getters
    public List<Bullet> getBullets() { return bullets; }
    public Rectangle getBounds() { 
        return new Rectangle((int)x, (int)y, GameConfig.PLAYER_SIZE, GameConfig.PLAYER_SIZE); 
    }
    public float getX() { return x; }
    public float getY() { return y; }
    public int getWidth() { return GameConfig.PLAYER_SIZE; }
    public int getHeight() { return GameConfig.PLAYER_SIZE; }
    public float getHealth() { return health; }
    public float getShield() { return shield; }
    public boolean isInvulnerable() { return isInvulnerable; }
    public int getPatternLevel() { return patternLevel; }
    public int getMaxUnlockedPattern() { return maxUnlockedPattern; }
    public Bullet.Pattern getCurrentPattern() { return currentPattern; }
    public boolean hasRapidFire() { return rapidFireActive; }
    public boolean hasSpread() { return spreadActive; }
    
    public int getFireMode() {
        return patternLevel - 1; // Convert to 0-based for UI
    }
    
    public String getFireModeLabel() {
        return currentPattern.name() + " (Lvl " + patternLevel + ")";
    }
}