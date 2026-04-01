package com.spaceshooterx.entities;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.spaceshooterx.audio.SoundManager;
import com.spaceshooterx.core.Difficulty;
import com.spaceshooterx.core.GameConfig;
import com.spaceshooterx.effects.ParticleSystem;
import com.spaceshooterx.util.ImageUtils;

public class Enemy {
    // Sprite
    private static BufferedImage enemySprite;
    
    // Basic properties
    protected float x, y;
    protected float dx, dy;
    protected float health;
    protected float maxHealth;
    protected int scoreValue = GameConfig.ENEMY_SCORE_VALUE;
    
    // Combat
    protected int attackDamage = GameConfig.ENEMY_BASE_DAMAGE;
    protected float attackSpeed = 1.0f;
    protected float bulletSpeed = GameConfig.ENEMY_BULLET_SPEED;
    protected long lastShotTime;
    protected long lastTargetTime;
    
    // Visual
    protected Color color = new Color(GameConfig.ENEMY_COLOR_R, GameConfig.ENEMY_COLOR_G, GameConfig.ENEMY_COLOR_B);
    protected Color targetColor = Color.RED;
    
    // State
    protected boolean isAlive = true;
    protected boolean isAttacking = false;
    protected boolean isTargeting = false;
    
    // World references
    protected List<EnemyBullet> worldBullets;
    protected List<Enemy> worldEnemies;
    
    // Drop chances
    protected float heartDropChance = GameConfig.HEART_DROP_CHANCE;
    protected float shieldDropChance = GameConfig.SHIELD_DROP_CHANCE;
    
    // Difficulty
    private Difficulty difficulty;
    
    // Movement properties - ENHANCED FOR BETTER BOUNDS CONTROL
    private float zigzagOffset = 0;
    private float zigzagSpeed = GameConfig.ENEMY_ZIGZAG_SPEED;
    private float zigzagAmplitude = GameConfig.ENEMY_ZIGZAG_AMPLITUDE;
    private float baseSpeed = GameConfig.ENEMY_BASE_SPEED;
    private float minY = GameConfig.ENEMY_MIN_Y;
    private float maxY = GameConfig.HEIGHT * GameConfig.ENEMY_MAX_Y_PERCENT;
    private float boundaryMargin = GameConfig.ENEMY_BOUNDARY_MARGIN;
    private int directionChangeCooldown = 0;
    private int directionChangeDelay = GameConfig.ENEMY_DIRECTION_CHANGE_DELAY;
    private int stateTimer = 0;
    private int stateDuration = GameConfig.ENEMY_STATE_DURATION;
    private int level = 1;
    
    public Enemy(float x, float y, Difficulty difficulty, int level) {
        this.x = x;
        this.y = y;
        this.difficulty = difficulty != null ? difficulty : Difficulty.NORMAL;
        this.level = Math.max(1, level);
        
        // Calculate level scaling using GameConfig
        float levelScale = 1.0f + ((this.level - 1) * GameConfig.LEVEL_STAT_INCREASE);
        
        // Apply difficulty multipliers to base stats with level scaling
        this.maxHealth = GameConfig.ENEMY_BASE_HEALTH * (float)this.difficulty.getEnemyHealthMultiplier() * levelScale;
        this.health = maxHealth;
        this.attackDamage = (int)(GameConfig.ENEMY_BASE_DAMAGE * this.difficulty.getEnemyDamageMultiplier() * levelScale);
        
        // Apply speed multiplier with level scaling
        double speedMult = this.difficulty.getSpeedMultiplier() * Math.min(GameConfig.LEVEL_SPEED_CAP, levelScale);
        baseSpeed = (1.2f + (float)Math.random() * 0.6f) * (float)speedMult;
        
        // Initial velocity - start with slight horizontal movement
        dx = (Math.random() < 0.5 ? 1 : -1) * baseSpeed;
        dy = 0.5f * (float)speedMult; // Very slow downward movement
        
        // Randomize movement parameters slightly
        zigzagSpeed = (0.04f + (float)Math.random() * 0.02f) * (float)speedMult;
        zigzagAmplitude = 2.0f + (float)Math.random() * 2.0f;
        
        // Random initial offset for varied movement patterns
        zigzagOffset = (float)(Math.random() * Math.PI * 2);
        
        // Load sprite if not already loaded
        if (enemySprite == null) {
            loadSprite();
        }
    }
    
    public Enemy(float x, float y, Difficulty difficulty) {
        this(x, y, difficulty, 1);
    }
    
    public Enemy(Difficulty difficulty, int level) {
        // Spawn at random x position near top with level
        this((float)(Math.random() * (GameConfig.WIDTH - 80) + 40), -30, difficulty, level);
    }
    
    public Enemy(Difficulty difficulty) {
        // Spawn at random x position near top
        this((float)(Math.random() * (GameConfig.WIDTH - 80) + 40), -30, difficulty, 1);
    }
    
    public Enemy() {
        // Default constructor for backward compatibility
        this(Difficulty.NORMAL);
    }
    
    private void loadSprite() {
        try {
            // Try to load enemy sprite
            enemySprite = ImageUtils.loadImage("/Inventory/spaceship.png");
            if (enemySprite != null) {
                enemySprite = ImageUtils.resizeImage(enemySprite, 40, 40);
            } else {
                // Try alternative path
                enemySprite = ImageUtils.loadImage("/Inventory/enemy.png");
                if (enemySprite != null) {
                    enemySprite = ImageUtils.resizeImage(enemySprite, 40, 40);
                }
            }
        } catch (Exception e) {
            System.err.println("Could not load enemy sprite: " + e.getMessage());
            enemySprite = null; // Will use fallback drawing
        }
    }
    
    public void update(float deltaTime, Player player, List<EnemyBullet> bulletList, List<Enemy> enemyList) {
        this.worldBullets = bulletList;
        this.worldEnemies = enemyList;
        
        if (!isAlive) return;
        
        // Update timers
        stateTimer++;
        if (directionChangeCooldown > 0) {
            directionChangeCooldown--;
        }
        
        // Update movement
        updateMovement(deltaTime, player);
        
        // Check if targeting player
        updateTargeting(player);
        
        // Update attack
        updateAttack(deltaTime, player);
        
        // Update position
        x += dx * deltaTime * 60;
        y += dy * deltaTime * 60;
        
        // Enforce boundaries with bounce-back
        enforceBoundaries();
        
        // Update health indicator color based on health
        updateVisuals();
    }
    
    private void updateMovement(float deltaTime, Player player) {
        // Update zigzag offset
        zigzagOffset += zigzagSpeed * deltaTime * 60;
        
        // Basic zigzag movement
        float horizontalMovement = (float)Math.sin(zigzagOffset) * zigzagAmplitude;
        
        // Apply different movement based on position
        if (y < minY) {
            // Too high - move down more
            dy = 1.5f;
            dx = horizontalMovement;
        } else if (y > maxY) {
            // Too low - move up or stay in place
            dy = -0.8f;
            dx = horizontalMovement * 0.7f;
        } else {
            // Normal movement zone - slight downward drift with zigzag
            dy = 0.5f;
            dx = horizontalMovement;
            
            // Add occasional direction changes
            if (directionChangeCooldown <= 0 && Math.random() < 0.02) {
                dx = -dx * 1.2f; // Reverse and speed up
                directionChangeCooldown = directionChangeDelay;
            }
        }
        
        // Avoid other enemies
        if (worldEnemies != null) {
            for (Enemy other : worldEnemies) {
                if (other == this || !other.isAlive()) continue;
                
                float distX = x - other.getX();
                float distY = y - other.getY();
                float distance = (float)Math.sqrt(distX * distX + distY * distY);
                
                // If too close, push away
                if (distance < 60 && distance > 0) {
                    float pushStrength = 0.5f;
                    dx += (distX / distance) * pushStrength;
                    dy += (distY / distance) * pushStrength * 0.5f;
                }
            }
        }
        
        // Simple player avoidance if too close
        if (player != null) {
            float distX = x - player.getX();
            float distY = y - player.getY();
            float distance = (float)Math.sqrt(distX * distX + distY * distY);
            
            if (distance < 100) {
                // Move away from player
                float avoidStrength = 0.8f;
                dx += (distX / distance) * avoidStrength;
                dy += (distY / distance) * avoidStrength * 0.5f;
            }
        }
        
        // Speed limits
        float maxSpeed = 3.0f;
        float currentSpeed = (float)Math.sqrt(dx * dx + dy * dy);
        if (currentSpeed > maxSpeed) {
            dx = (dx / currentSpeed) * maxSpeed;
            dy = (dy / currentSpeed) * maxSpeed;
        }
    }
    
    private void enforceBoundaries() {
        // Horizontal boundaries - bounce back from edges
        if (x < boundaryMargin) {
            x = boundaryMargin;
            dx = Math.abs(dx) * 0.8f; // Bounce back and slow down
        } else if (x > GameConfig.WIDTH - 40 - boundaryMargin) {
            x = GameConfig.WIDTH - 40 - boundaryMargin;
            dx = -Math.abs(dx) * 0.8f; // Bounce back and slow down
        }
        
        // Vertical boundaries - don't go too high or too low
        if (y < minY) {
            y = minY;
            dy = Math.abs(dy) * 0.5f; // Slow down upward movement
        } else if (y > maxY) {
            y = maxY;
            dy = -Math.abs(dy) * 0.5f; // Push upward
        }
        
        // Prevent getting stuck at top or bottom
        if (y < minY + 10 && dy < 0) {
            dy = 0.5f; // Force downward if stuck at top
        }
        if (y > maxY - 10 && dy > 0) {
            dy = -0.5f; // Force upward if stuck at bottom
        }
    }
    
    private void updateVisuals() {
        // Update color based on health
        float healthPercent = health / maxHealth;
        if (healthPercent > 0.6f) {
            color = new Color(255, 100, 100); // Healthy red
        } else if (healthPercent > 0.3f) {
            color = new Color(255, 150, 50); // Damaged orange
        } else {
            color = new Color(255, 50, 50); // Critical dark red
        }
    }
    
    private void updateTargeting(Player player) {
        if (player == null) return;
        
        float dx = Math.abs(player.getX() - x);
        float dy = player.getY() - y;
        
        // Calculate horizontal shooting range (150% of screen width)
        float horizontalRange = GameConfig.WIDTH * GameConfig.ENEMY_SHOOT_WIDTH_MULTIPLIER;
        
        // Check if player is below enemy and within vertical range
        boolean inHeightRange = dy > 0 && dy < GameConfig.ENEMY_SHOOT_HEIGHT_RANGE;
        
        // Check if within horizontal range
        boolean inWidthRange = dx < horizontalRange;
        
        // Start attacking if in both ranges
        isAttacking = inHeightRange && inWidthRange;
        
        // Show targeting indicator if close (200 pixels total distance)
        float distance = (float)Math.sqrt(dx * dx + dy * dy);
        isTargeting = distance < 200;
        
        if (isTargeting) {
            lastTargetTime = System.currentTimeMillis();
        }
    }
    
    private void updateAttack(float deltaTime, Player player) {
        if (player == null || !isAttacking) return;
        
        long currentTime = System.currentTimeMillis();
        // Use difficulty-based fire cooldown
        long shootInterval = difficulty.getEnemyFireCooldownMs();
        
        if (currentTime - lastShotTime > shootInterval) {
            attack(player);
            lastShotTime = currentTime;
        }
    }
    
    private void attack(Player player) {
        if (player == null) return;
        
        // Shoot slightly towards player
        float playerX = player.getX() + player.getWidth() / 2;
        float playerY = player.getY();
        
        float angleX = playerX - (x + 20);
        float angleY = playerY - (y + 20);
        float distance = (float)Math.sqrt(angleX * angleX + angleY * angleY);
        
        // Normalize and scale to bullet speed
        if (distance > 0) {
            angleX = (angleX / distance) * bulletSpeed * 0.3f; // Slight homing
            angleY = (angleY / distance) * bulletSpeed;
        }
        
        // Add slight spread
        float spread = (float)(Math.random() - 0.5) * 0.5f;
        
        EnemyBullet bullet = new EnemyBullet(
            x + 20 - 3, y + 20,
            angleX + spread, bulletSpeed + spread,
            attackDamage,
            color,
            "basic"
        );
        
        if (worldBullets != null) {
            worldBullets.add(bullet);
        }
        
        // Create muzzle flash
        ParticleSystem.getInstance().createMuzzleFlash(
            x + 20, y + 20,
            angleX, bulletSpeed,
            color
        );
        
        SoundManager.playSound(SoundManager.SoundType.ENEMY_SHOOT_SMALL, 0.6f, 1.0f);
    }
    
    public void draw(Graphics2D g2d) {
        if (!isAlive) return;
        
        // Draw targeting indicator if close to player
        if (isTargeting && System.currentTimeMillis() - lastTargetTime < 500) {
            drawTargetingIndicator(g2d);
        }
        
        // Draw enemy
        drawSpaceship(g2d);
        
        // Draw health bar
        drawHealthBar(g2d);
    }
    
    private void drawSpaceship(Graphics2D g2d) {
        if (enemySprite != null) {
            // Draw the sprite with rotation based on movement
            float angle = (float)Math.atan2(dy, dx);
            Graphics2D g = (Graphics2D) g2d.create();
            g.rotate(angle, x + 20, y + 20);
            g.drawImage(enemySprite, (int)x, (int)y, null);
            g.dispose();
        } else {
            // Fallback: Draw spaceship as a triangle
            int[] xPoints = {(int)x + 20, (int)x + 40, (int)x};
            int[] yPoints = {(int)y, (int)y + 40, (int)y + 40};
            
            g2d.setColor(color);
            g2d.fillPolygon(xPoints, yPoints, 3);
            
            // Add engine glow
            g2d.setColor(new Color(255, 200, 100, 150));
            g2d.fillRect((int)x + 15, (int)y + 40, 10, 8);
            
            g2d.setColor(color.darker());
            g2d.setStroke(new BasicStroke(2));
            g2d.drawPolygon(xPoints, yPoints, 3);
        }
    }
    
    private void drawTargetingIndicator(Graphics2D g2d) {
        // Draw a red circle around enemy when targeting
        int centerX = (int)x + 20;
        int centerY = (int)y + 20;
        
        // Pulsing effect
        float pulse = 0.5f + 0.5f * (float)Math.sin(System.currentTimeMillis() / 200.0);
        int alpha = (int)(pulse * 150);
        
        // Outer circle
        g2d.setColor(new Color(255, 0, 0, alpha));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval(centerX - 25, centerY - 25, 50, 50);
        
        // Inner targeting lines
        g2d.setColor(new Color(255, 100, 100, 200));
        g2d.drawLine(centerX - 15, centerY, centerX - 5, centerY);
        g2d.drawLine(centerX + 5, centerY, centerX + 15, centerY);
        g2d.drawLine(centerX, centerY - 15, centerX, centerY - 5);
        g2d.drawLine(centerX, centerY + 5, centerX, centerY + 15);
    }
    
    private void drawHealthBar(Graphics2D g2d) {
        int barWidth = 40;
        int barHeight = 5;
        int barX = (int)x;
        int barY = (int)y - 10;
        
        // Background
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(barX, barY, barWidth, barHeight);
        
        // Health
        float healthPercent = health / maxHealth;
        int fillWidth = (int)(barWidth * healthPercent);
        
        if (healthPercent > 0.6f) {
            g2d.setColor(Color.GREEN);
        } else if (healthPercent > 0.3f) {
            g2d.setColor(Color.YELLOW);
        } else {
            g2d.setColor(Color.RED);
        }
        
        g2d.fillRect(barX, barY, fillWidth, barHeight);
        
        // Border
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(1));
        g2d.drawRect(barX, barY, barWidth, barHeight);
    }
    
    public boolean takeDamage(float damage) {
        if (!isAlive) return false;
        
        health -= damage;
        
        // Create hit effect
        ParticleSystem.getInstance().createHitEffect(
            x + 20, y + 20,
            damage,
            color
        );
        
        SoundManager.playSound(SoundManager.SoundType.ENEMY_HIT, 0.5f, 1.0f);
        
        // Check if dead
        if (health <= 0) {
            die();
            return true;
        }
        
        // Flash white when hit
        ParticleSystem.getInstance().createExplosion(
            x + 20, y + 20,
            10,
            Color.WHITE
        );
        
        return false;
    }
    
    private void die() {
        isAlive = false;
        
        // Create explosion
        ParticleSystem.getInstance().createLargeExplosion(
            x + 20, y + 20,
            color,
            Color.ORANGE
        );
        
        SoundManager.playSound(SoundManager.SoundType.ENEMY_EXPLOSION_SMALL, 1.0f, 1.0f);
        
        // Drop items based on chance
        dropItems();
    }
    
    private void dropItems() {
        Random rand = new Random();
        
        // Check for heart drop
        if (rand.nextFloat() < heartDropChance) {
            dropHeart();
        }
        
        // Check for shield drop
        if (rand.nextFloat() < shieldDropChance) {
            dropShield();
        }
    }
    
    private void dropHeart() {
    // Currently: Sound only, no actual pickup creation
    SoundManager.playSound(SoundManager.SoundType.POWERUP_SPAWN, 0.3f, 1.0f);
    // Needs to create HeartPickup and add it to game world
    }

    private void dropShield() {
        // Same issue - just sound, no actual pickup
        SoundManager.playSound(SoundManager.SoundType.POWERUP_SPAWN, 0.3f, 1.0f);
    }
    
    // Getters
    public float getX() { return x; }
    public float getY() { return y; }
    public int getScoreValue() { return scoreValue; }
    public boolean isAlive() { return isAlive; }
    
    // Collision bounds
    public Rectangle getBounds() {
        return new Rectangle((int)x, (int)y, 40, 40);
    }
}