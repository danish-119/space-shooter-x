package com.spaceshooterx.core;

import com.spaceshooterx.audio.SoundManager;
import com.spaceshooterx.effects.AnimationManager;
import com.spaceshooterx.effects.ParticleSystem;
import com.spaceshooterx.entities.*;
import com.spaceshooterx.util.ImageUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ShootingGame extends JPanel implements ActionListener {
    // Enhanced game systems
    private javax.swing.Timer timer;
    private Player player;
    private List<Rock> rocks;
    private List<Enemy> enemies;
    private List<EnemyBullet> enemyBullets;
    private List<HeartPickup> heartPickups;
    private List<ShieldPickup> shieldPickups;
    private List<PowerUpPickup> powerUpPickups; // NEW: For pattern upgrades
    private List<ParticleEffect> particles;
    private List<Star> stars;
    private List<Comet> comets;
    
    // Game state
    private int score;
    private int highScore;
    private int lives;
    private int combo;
    private long lastComboTime;
    private int comboMultiplier;
    private GameState gameState;
    private int level;
    private long lastLevelUpTime; // Track time for level progression
    private Difficulty difficulty;
    private GameStats stats;
    
    // UI state
    private int menuIndex;
    private float menuPulse;
    private int menuPulseDir = 1;
    private float uiScale = 1.0f;
    private float screenShakeIntensity = 0;
    private long screenShakeEndTime = 0;
    private boolean showDamageIndicator = false;
    private long damageIndicatorEndTime = 0;
    private int killStreak = 0;
    private long lastKillTime = 0;
    private String streakMessage = "";
    private long streakMessageEndTime = 0;
    
    // Animation values
    private float titleGlow = 0.0f;
    private int titleGlowDir = 1;
    private float buttonHoverAlpha = 0.0f;
    private float loadingProgress = 0.0f;
    private long loadingStartTime;
    private float transitionAlpha = 0.0f;
    private boolean transitioning = false;
    private GameState transitionTarget;
    private boolean ambientActive = false;
    private boolean alarmActive = false;
    private JFrame hostFrame;
    private boolean isMaximized = false;
    
    // UI Colors
    private final Color[] NEON_COLORS = {
        new Color(0, 200, 255),    // Blue
        new Color(255, 0, 100),    // Pink
        new Color(0, 255, 150),    // Green
        new Color(255, 200, 0)     // Yellow
    };
    
    // Gradients
    private GradientPaint spaceGradient;
    private GradientPaint uiGradient;
    
    // Fonts
    private Font titleFont;
    private Font largeFont;
    private Font mediumFont;
    private Font smallFont;
    private Font digitalFont;
    
    // Resources
    private BufferedImage backgroundImage;
    private int backgroundOffsetY = 0;
    private int backgroundScrollSpeed = GameConfig.BACKGROUND_SCROLL_SPEED;
    private BufferedImage heartFull;
    private BufferedImage heartHalf;
    private BufferedImage heartEmpty;
    private BufferedImage[] patternIcons; // CHANGED: Now shows pattern icons
    
    // Note: Constants moved to GameConfig for centralized configuration
    
    // Power-up types
    public enum PowerUpType {
        RAPID_FIRE,
        SPREAD_SHOT,
        PATTERN_UPGRADE
    }
    
    public ShootingGame() {
        setPreferredSize(new Dimension(GameConfig.WIDTH, GameConfig.HEIGHT));
        setBackground(new Color(GameConfig.BG_COLOR_R, GameConfig.BG_COLOR_G, GameConfig.BG_COLOR_B));
        setFocusable(true);
        setDoubleBuffered(true);
        
        // Initialize collections
        rocks = new CopyOnWriteArrayList<>();
        enemies = new CopyOnWriteArrayList<>();
        enemyBullets = new CopyOnWriteArrayList<>();
        heartPickups = new CopyOnWriteArrayList<>();
        shieldPickups = new CopyOnWriteArrayList<>();
        powerUpPickups = new CopyOnWriteArrayList<>(); // NEW
        particles = new CopyOnWriteArrayList<>();
        stars = new CopyOnWriteArrayList<>();
        comets = new CopyOnWriteArrayList<>();
        
        // Initialize game state
        player = new Player();
        timer = new javax.swing.Timer(GameConfig.FRAME_DELAY_MS, this);
        lives = GameConfig.PLAYER_LIVES;
        gameState = GameState.LOADING;
        loadingStartTime = System.currentTimeMillis();
        difficulty = Difficulty.NORMAL;
        menuIndex = 0;
        highScore = loadHighScore();
        stats = new GameStats();

        SoundManager.setEnabled(true);
        SoundManager.setMusicVolume(GameConfig.MUSIC_VOLUME);
        SoundManager.setSFXVolume(GameConfig.SFX_VOLUME);
        
        // Create stars
        for (int i = 0; i < GameConfig.STAR_COUNT; i++) {
            stars.add(new Star());
        }
        
        // Initialize fonts and resources
        initializeResources();
        
        // Setup key listener
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F11) {
                    toggleMaximize();
                    return;
                }
                handleKeyPressed(e);
            }
            
            @Override
            public void keyReleased(KeyEvent e) {
                if (gameState == GameState.RUNNING) {
                    player.keyReleased(e);
                }
            }
        });
        
        // Add mouse support for menu
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (gameState == GameState.MENU || gameState == GameState.GAME_OVER) {
                    handleMouseClick(e.getX(), e.getY());
                }
            }
        });
        
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (gameState == GameState.MENU) {
                    handleMouseMove(e.getX(), e.getY());
                }
            }
        });
        
        timer.start();
    }

    public void setHostFrame(JFrame frame) {
        this.hostFrame = frame;
    }

    private void toggleMaximize() {
        if (hostFrame == null) {
            return;
        }
        isMaximized = !isMaximized;
        if (isMaximized) {
            hostFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        } else {
            hostFrame.setExtendedState(JFrame.NORMAL);
            hostFrame.pack();
            hostFrame.setLocationRelativeTo(null);
        }
    }
    
    private void initializeResources() {
        try {
            // Create fonts
            titleFont = new Font("Arial", Font.BOLD, GameConfig.FONT_SIZE_TITLE);
            largeFont = new Font("Arial", Font.BOLD, GameConfig.FONT_SIZE_LARGE);
            mediumFont = new Font("Arial", Font.PLAIN, GameConfig.FONT_SIZE_MEDIUM);
            smallFont = new Font("Arial", Font.PLAIN, GameConfig.FONT_SIZE_SMALL);
            digitalFont = new Font("Courier New", Font.BOLD, GameConfig.FONT_SIZE_DIGITAL);
            
            // Load images
            backgroundImage = ImageUtils.loadImage("/Inventory/outerspace.png");
            if (backgroundImage == null) {
                backgroundImage = createSpaceBackground();
            }
            createHeartIcons();
            createPatternIcons(); // CHANGED: Now creates pattern icons
            
        } catch (Exception e) {
            System.err.println("Error loading resources: " + e.getMessage());
        }
    }
    
    private BufferedImage createSpaceBackground() {
        BufferedImage img = new BufferedImage(1200, 800, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        
        // Create nebula effect
        for (int i = 0; i < 50; i++) {
            int x = (int)(Math.random() * 1200);
            int y = (int)(Math.random() * 800);
            int size = (int)(Math.random() * 200 + 100);
            float alpha = (float)(Math.random() * 0.1 + 0.05);
            
            GradientPaint gradient = new GradientPaint(
                x, y, new Color(100, 50, 200, (int)(alpha * 255)),
                x + size, y + size, new Color(50, 100, 255, 0)
            );
            g2d.setPaint(gradient);
            g2d.fillOval(x - size/2, y - size/2, size, size);
        }
        
        g2d.dispose();
        return img;
    }
    
    private void createHeartIcons() {
        heartFull = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        heartHalf = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        heartEmpty = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        
        Graphics2D g2d;
        
        // Full heart
        g2d = heartFull.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(new Color(255, 50, 50));
        drawHeart(g2d, 16, 16, 14);
        g2d.dispose();
        
        // Half heart
        g2d = heartHalf.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(new Color(255, 150, 50));
        drawHeart(g2d, 16, 16, 14);
        g2d.dispose();
        
        // Empty heart
        g2d = heartEmpty.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(new Color(100, 100, 100, 100));
        drawHeart(g2d, 16, 16, 14);
        g2d.dispose();
    }
    
    private void drawHeart(Graphics2D g2d, int x, int y, int size) {
        Polygon heart = new Polygon();
        for (int i = 0; i < 360; i++) {
            double rad = Math.toRadians(i);
            double px = 16 * Math.pow(Math.sin(rad), 3);
            double py = -(13 * Math.cos(rad) - 5 * Math.cos(2 * rad) - 2 * Math.cos(3 * rad) - Math.cos(4 * rad));
            heart.addPoint(x + (int)(px * size / 16), y + (int)(py * size / 16));
        }
        g2d.fill(heart);
    }
    
    private void createPatternIcons() {
        // Create icons for pattern levels 1-8
        patternIcons = new BufferedImage[8];
        for (int level = 0; level < 8; level++) {
            patternIcons[level] = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = patternIcons[level].createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Use different colors for different levels
            Color iconColor = NEON_COLORS[level % NEON_COLORS.length];
            g2d.setColor(iconColor);
            
            // Draw pattern representation
            int bulletCount = level + 1;
            int radius = 10;
            
            // Draw bullets in a circle pattern
            for (int i = 0; i < bulletCount; i++) {
                double angle = 2 * Math.PI * i / bulletCount;
                int x = 16 + (int)(Math.cos(angle) * radius);
                int y = 16 + (int)(Math.sin(angle) * radius);
                g2d.fillOval(x - 2, y - 2, 4, 4);
            }
            
            g2d.dispose();
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Graphics2D g2d = (Graphics2D) g;
        setupRenderingHints(g2d);

        AnimationManager.getInstance().applyCameraTransform(g2d, getWidth(), getHeight());
        
        // Apply screen shake if active
        if (screenShakeIntensity > 0 && System.currentTimeMillis() < screenShakeEndTime) {
            int shakeX = (int)((Math.random() - 0.5) * screenShakeIntensity * 10);
            int shakeY = (int)((Math.random() - 0.5) * screenShakeIntensity * 10);
            g2d.translate(shakeX, shakeY);
        }
        
        // Draw background
        drawBackground(g2d);
        
        // Draw game elements based on state
        switch (gameState) {
            case LOADING:
                drawLoadingScreen(g2d);
                break;
            case MENU:
                drawMenuScreen(g2d);
                break;
            case RUNNING:
                drawGame(g2d);
                break;
            case PAUSED:
                drawGame(g2d);
                drawPauseScreen(g2d);
                break;
            case GAME_OVER:
                drawGame(g2d);
                drawGameOverScreen(g2d);
                break;
        }

        AnimationManager.getInstance().applyPostProcessing(g2d, getWidth(), getHeight());
        AnimationManager.getInstance().drawOverlay(g2d, getWidth(), getHeight());
        
        // Apply damage indicator if active
        if (showDamageIndicator && System.currentTimeMillis() < damageIndicatorEndTime) {
            g2d.setColor(new Color(255, 50, 50, 100));
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
        
        // Apply transition fade if active
        if (transitioning) {
            g2d.setColor(new Color(0, 0, 0, (int)(transitionAlpha * 255)));
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
    }
    
    private void setupRenderingHints(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }
    
    private void drawBackground(Graphics2D g2d) {
        // Draw space background
        if (backgroundImage != null) {
            int imgW = backgroundImage.getWidth();
            int imgH = backgroundImage.getHeight();
            int startY = -backgroundOffsetY;
            for (int y = startY; y < getHeight(); y += imgH) {
                for (int x = 0; x < getWidth(); x += imgW) {
                    g2d.drawImage(backgroundImage, x, y, null);
                }
            }
        }
        
        // Draw stars
        for (Star star : stars) {
            star.draw(g2d);
        }
        
        // Draw comets
        for (Comet comet : comets) {
            comet.draw(g2d);
        }
        
        // Draw particles
        for (ParticleEffect particle : new ArrayList<>(particles)) {
            if (particle.isAlive()) {
                particle.draw(g2d);
            } else {
                particles.remove(particle);
            }
        }
    }
    
    private void drawLoadingScreen(Graphics2D g2d) {
        float progress = (System.currentTimeMillis() - loadingStartTime) / (float) GameConfig.LOADING_DURATION_MS;
        loadingProgress = Math.min(progress, 1.0f);
        
        // Draw animated loading screen
        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.fillRect(0, 0, getWidth(), getHeight());
        
        // Draw title
        g2d.setFont(titleFont);
        String title = "SPACE SHOOTER X";
        drawTextWithGlow(g2d, title, getWidth() / 2, getHeight() / 2 - 100, 
                        NEON_COLORS[0], Color.BLACK, 3);
        
        // Draw loading bar container
        int barWidth = 400;
        int barHeight = 20;
        int x = (getWidth() - barWidth) / 2;
        int y = getHeight() / 2 + 20;
        
        // Bar background
        g2d.setColor(new Color(50, 50, 100, 150));
        g2d.fillRoundRect(x - 5, y - 5, barWidth + 10, barHeight + 10, 15, 15);
        
        // Bar border
        g2d.setColor(NEON_COLORS[0]);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(x - 5, y - 5, barWidth + 10, barHeight + 10, 15, 15);
        
        // Progress bar
        int fillWidth = (int)(barWidth * loadingProgress);
        GradientPaint progressGradient = new GradientPaint(
            x, y, NEON_COLORS[0],
            x + fillWidth, y, NEON_COLORS[1]
        );
        g2d.setPaint(progressGradient);
        g2d.fillRoundRect(x, y, fillWidth, barHeight, 10, 10);
        
        // Progress text
        g2d.setFont(mediumFont);
        g2d.setColor(Color.WHITE);
        String progressText = String.format("LOADING... %.0f%%", loadingProgress * 100);
        drawCenteredText(g2d, progressText, getWidth() / 2, y + barHeight + 30);
        
        // Loading hints
        g2d.setFont(smallFont);
        g2d.setColor(new Color(200, 200, 255, 150));
        String[] hints = {
            "Tip: Press 1-8 keys to switch bullet patterns",
            "Tip: Collect power-ups for special abilities",
            "Tip: Chain kills for combo multipliers"
        };
        
        int hintIndex = (int)((System.currentTimeMillis() / 2000) % hints.length);
        drawCenteredText(g2d, hints[hintIndex], getWidth() / 2, y + barHeight + 60);
    }
    
    private void drawMenuScreen(Graphics2D g2d) {
        // Update animations
        menuPulse += menuPulseDir * 0.05f;
        if (menuPulse > 1.0f) {
            menuPulse = 1.0f;
            menuPulseDir = -1;
        } else if (menuPulse < 0.0f) {
            menuPulse = 0.0f;
            menuPulseDir = 1;
        }
        
        titleGlow += titleGlowDir * 0.02f;
        if (titleGlow > 1.0f) {
            titleGlow = 1.0f;
            titleGlowDir = -1;
        } else if (titleGlow < 0.3f) {
            titleGlow = 0.3f;
            titleGlowDir = 1;
        }
        
        // Draw menu backdrop
        drawMenuBackground(g2d);
        
        // Draw title
        g2d.setFont(titleFont);
        String title = "SPACE SHOOTER X";
        
        // Title glow effect
        for (int i = 5; i > 0; i--) {
            float alpha = titleGlow * (0.2f - i * 0.02f);
            g2d.setColor(new Color(NEON_COLORS[0].getRed(), 
                                 NEON_COLORS[0].getGreen(), 
                                 NEON_COLORS[0].getBlue(), 
                                 (int)(alpha * 255)));
            drawCenteredText(g2d, title, getWidth() / 2 + i, getHeight() / 2 - 150 + i);
        }
        
        // Main title
        g2d.setColor(NEON_COLORS[0]);
        drawCenteredText(g2d, title, getWidth() / 2, getHeight() / 2 - 150);
        
        // Subtitle
        g2d.setFont(mediumFont);
        g2d.setColor(new Color(200, 220, 255));
        drawCenteredText(g2d, "GALACTIC DEFENSE PROTOCOL v2.0", getWidth() / 2, getHeight() / 2 - 110);
        
        // Draw menu options
        drawMenuOptions(g2d);
        
        // Draw high score
        g2d.setFont(mediumFont);
        g2d.setColor(new Color(255, 215, 0));
        String highScoreText = String.format("HIGH SCORE: %,d", highScore);
        drawCenteredText(g2d, highScoreText, getWidth() / 2, getHeight() / 2 + 180);
        
        // Draw controls help
        g2d.setFont(smallFont);
        g2d.setColor(new Color(180, 200, 255, 180));
        drawCenteredText(g2d, "↑↓ Navigate | ENTER Select | ESC Exit", getWidth() / 2, getHeight() - 50);
        drawCenteredText(g2d, "Mouse also supported", getWidth() / 2, getHeight() - 30);
    }
    
    private void drawMenuBackground(Graphics2D g2d) {
        // Dark overlay
        g2d.setColor(new Color(0, 0, 20, 200));
        g2d.fillRect(0, 0, getWidth(), getHeight());
        
        // Animated grid lines
        g2d.setStroke(new BasicStroke(1));
        g2d.setColor(new Color(0, 100, 255, 30));
        
        int gridSize = 40;
        int offset = (int)(System.currentTimeMillis() / 50) % gridSize;
        
        for (int x = -offset; x < getWidth(); x += gridSize) {
            g2d.drawLine(x, 0, x, getHeight());
        }
        for (int y = -offset; y < getHeight(); y += gridSize) {
            g2d.drawLine(0, y, getWidth(), y);
        }
        
        // Pulsing scan line
        int scanY = (int)(getHeight() * (0.3 + menuPulse * 0.4));
        g2d.setColor(new Color(0, 200, 255, 50));
        g2d.fillRect(0, scanY, getWidth(), 2);
        
        // Glowing scan line effect
        GradientPaint scanGradient = new GradientPaint(
            0, scanY, new Color(0, 200, 255, 100),
            0, scanY + 10, new Color(0, 200, 255, 0)
        );
        g2d.setPaint(scanGradient);
        g2d.fillRect(0, scanY - 5, getWidth(), 20);
    }
    
    private void drawMenuOptions(Graphics2D g2d) {
        String[] options = {
            "START MISSION",
            "DIFFICULTY: " + difficulty.getLabel(),
            "SOUND: " + (SoundManager.isEnabled() ? "ON" : "OFF"),
            "CONTROLS",
            "QUIT TO DESKTOP"
        };
        
        int baseY = getHeight() / 2 - 40;
        
        for (int i = 0; i < options.length; i++) {
            int y = baseY + (i * 50);
            boolean selected = i == menuIndex;
            
            // Button background
            if (selected) {
                // Glowing selection effect
                for (int j = 0; j < 3; j++) {
                    float alpha = 0.3f - j * 0.1f;
                    g2d.setColor(new Color(NEON_COLORS[0].getRed(),
                                         NEON_COLORS[0].getGreen(),
                                         NEON_COLORS[0].getBlue(),
                                         (int)(alpha * 255)));
                    g2d.fillRoundRect(getWidth() / 2 - 160 - j * 5, 
                                     y - 25 - j * 5, 
                                     320 + j * 10, 
                                     40 + j * 10, 
                                     20, 20);
                }
                
                // Main button
                GradientPaint buttonGradient = new GradientPaint(
                    getWidth() / 2 - 150, y - 20, NEON_COLORS[0],
                    getWidth() / 2 + 150, y + 20, NEON_COLORS[1]
                );
                g2d.setPaint(buttonGradient);
                g2d.fillRoundRect(getWidth() / 2 - 150, y - 20, 300, 40, 20, 20);
                
                // Button border glow
                g2d.setColor(Color.WHITE);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRoundRect(getWidth() / 2 - 150, y - 20, 300, 40, 20, 20);
                
                // Selection indicator
                g2d.fillPolygon(
                    new int[]{getWidth() / 2 - 170, getWidth() / 2 - 160, getWidth() / 2 - 160},
                    new int[]{y, y - 10, y + 10},
                    3
                );
                g2d.fillPolygon(
                    new int[]{getWidth() / 2 + 170, getWidth() / 2 + 160, getWidth() / 2 + 160},
                    new int[]{y, y - 10, y + 10},
                    3
                );
                
                g2d.setFont(largeFont);
                g2d.setColor(Color.BLACK);
            } else {
                // Inactive button
                g2d.setColor(new Color(255, 255, 255, 30));
                g2d.fillRoundRect(getWidth() / 2 - 150, y - 20, 300, 40, 20, 20);
                
                g2d.setColor(new Color(255, 255, 255, 100));
                g2d.setStroke(new BasicStroke(1));
                g2d.drawRoundRect(getWidth() / 2 - 150, y - 20, 300, 40, 20, 20);
                
                g2d.setFont(mediumFont);
                g2d.setColor(new Color(200, 220, 255));
            }
            
            // Draw option text
            drawCenteredText(g2d, options[i], getWidth() / 2, y + 5);
            
            // Draw difficulty indicator
            if (i == 1 && selected) {
                g2d.setFont(smallFont);
                g2d.setColor(new Color(200, 200, 255, 150));
                String difficultyDesc = difficulty.getDescription();
                drawCenteredText(g2d, difficultyDesc, getWidth() / 2, y + 25);
            }
        }
    }
    
    private void drawGame(Graphics2D g2d) {
        // Draw game objects
        for (Rock rock : rocks) {
            rock.draw(g2d);
        }
        
        for (Enemy enemy : enemies) {
            enemy.draw(g2d);
        }
        
        for (Bullet bullet : player.getBullets()) {
            bullet.draw(g2d);
        }
        
        for (EnemyBullet bullet : enemyBullets) {
            bullet.draw(g2d);
        }
        
        // Draw pickups
        for (HeartPickup pickup : heartPickups) {
            pickup.draw(g2d);
        }
        
        for (ShieldPickup pickup : shieldPickups) {
            pickup.draw(g2d);
        }
        
        // Draw power-up pickups
        for (PowerUpPickup pickup : powerUpPickups) {
            pickup.draw(g2d);
        }
        
        player.draw(g2d);

        ParticleSystem.getInstance().renderImmediate(g2d);
        
        // Draw enhanced HUD
        drawEnhancedHUD(g2d);
        
        // Draw combo streak
        if (System.currentTimeMillis() - lastKillTime < GameConfig.STREAK_TIMEOUT_MS && killStreak > 0) {
            drawComboStreak(g2d);
        }
        
        // Draw combo multiplier
        if (System.currentTimeMillis() - lastComboTime < GameConfig.COMBO_TIMEOUT_MS && combo > 0) {
            drawComboDisplay(g2d);
        }
    }
    
    private void drawEnhancedHUD(Graphics2D g2d) {
        int hudWidth = 300;
        int hudHeight = 180;
        int hudX = 20;
        int hudY = 20;
        
        // HUD background with transparency
        g2d.setColor(new Color(0, 10, 30, 180));
        g2d.fillRoundRect(hudX, hudY, hudWidth, hudHeight, 15, 15);
        
        // HUD border
        g2d.setColor(NEON_COLORS[0]);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(hudX, hudY, hudWidth, hudHeight, 15, 15);
        
        // Score display
        g2d.setFont(digitalFont);
        g2d.setColor(Color.WHITE);
        g2d.drawString("SCORE", hudX + 15, hudY + 35);
        
        g2d.setFont(new Font("Courier New", Font.BOLD, 32));
        g2d.setColor(NEON_COLORS[0]);
        g2d.drawString(String.format("%,d", score), hudX + 15, hudY + 70);
        
        // Level display
        g2d.setFont(mediumFont);
        g2d.setColor(new Color(200, 220, 255));
        g2d.drawString("LEVEL " + level, hudX + 15, hudY + 95);
        
        // Difficulty indicator
        g2d.setColor(difficulty.getColor());
        g2d.drawString(difficulty.getLabel(), hudX + 15, hudY + 120);
        
        // Lives display
        drawLivesDisplay(g2d, hudX + 15, hudY + 145);
        
        // Weapon status
        drawPatternStatus(g2d, hudX + hudWidth + 10, hudY);
        
        // Mini-map or radar (simplified)
        drawRadar(g2d, getWidth() - 150, hudY);
    }
    
    private void drawLivesDisplay(Graphics2D g2d, int x, int y) {
        g2d.setFont(smallFont);
        g2d.setColor(new Color(200, 220, 255));
        g2d.drawString("HEALTH:", x, y);
        
        // Calculate hearts based on player health (0-100)
        float health = player.getHealth();
        int fullHearts = (int)(health / 33.33f);
        boolean halfHeart = (health % 33.33f) > 16.66f;
        
        for (int i = 0; i < GameConfig.PLAYER_LIVES; i++) {
            BufferedImage heart;
            if (i < fullHearts) {
                heart = heartFull;
            } else if (i == fullHearts && halfHeart) {
                heart = heartHalf;
            } else {
                heart = heartEmpty;
            }
            g2d.drawImage(heart, x + 70 + i * 35, y - 25, null);
        }
    }
    
    private void drawPatternStatus(Graphics2D g2d, int x, int y) {
        int statusWidth = 180;
        
        g2d.setColor(new Color(0, 10, 30, 180));
        g2d.fillRoundRect(x, y, statusWidth, 100, 10, 10);
        
        g2d.setColor(NEON_COLORS[2]);
        g2d.setStroke(new BasicStroke(1));
        g2d.drawRoundRect(x, y, statusWidth, 100, 10, 10);
        
        g2d.setFont(mediumFont);
        g2d.setColor(Color.WHITE);
        g2d.drawString("PATTERN SYSTEM", x + 15, y + 25);
        
        // Current pattern icon
        int patternLevel = player.getPatternLevel() - 1; // Convert to 0-based index
        if (patternLevel >= 0 && patternLevel < patternIcons.length) {
            g2d.drawImage(patternIcons[patternLevel], x + 15, y + 35, null);
        }
        
        g2d.setFont(smallFont);
        g2d.setColor(new Color(200, 220, 255));
        g2d.drawString("PATTERN: " + player.getFireModeLabel(), x + 60, y + 55);
        
        // Show unlocked patterns
        g2d.setColor(new Color(150, 200, 150));
        g2d.drawString("Unlocked: 1-" + player.getMaxUnlockedPattern(), x + 60, y + 70);
        
        // Power-up status
        if (player.hasRapidFire()) {
            g2d.setColor(new Color(255, 200, 0));
            g2d.drawString("RAPID FIRE ACTIVE", x + 15, y + 85);
        }
        
        if (player.hasSpread()) {
            g2d.setColor(new Color(0, 255, 150));
            g2d.drawString("SPREAD ACTIVE", x + 15, player.hasRapidFire() ? y + 100 : y + 85);
        }
    }
    
    private void drawRadar(Graphics2D g2d, int x, int y) {
        int radarSize = 120;
        
        // Radar background
        g2d.setColor(new Color(0, 30, 60, 180));
        g2d.fillOval(x, y, radarSize, radarSize);
        
        // Radar rings
        g2d.setColor(new Color(0, 100, 200, 80));
        g2d.setStroke(new BasicStroke(1));
        for (int i = 1; i <= 3; i++) {
            g2d.drawOval(x + i * radarSize/6, y + i * radarSize/6, 
                         radarSize - i * radarSize/3, radarSize - i * radarSize/3);
        }
        
        // Radar sweep
        double sweepAngle = (System.currentTimeMillis() % 2000) / 2000.0 * Math.PI * 2;
        g2d.setColor(new Color(0, 255, 100, 100));
        g2d.fillArc(x, y, radarSize, radarSize, 
                   (int)Math.toDegrees(sweepAngle - 0.1), 20);
        
        // Center dot (player)
        g2d.setColor(Color.GREEN);
        g2d.fillOval(x + radarSize/2 - 2, y + radarSize/2 - 2, 4, 4);
        
        // Enemy blips
        g2d.setColor(Color.RED);
        for (Enemy enemy : enemies) {
            double relX = (enemy.getX() - player.getX()) / (double)getWidth();
            double relY = (enemy.getY() - player.getY()) / (double)getHeight();
            
            int blipX = (int)(x + radarSize/2 + relX * radarSize/2);
            int blipY = (int)(y + radarSize/2 + relY * radarSize/2);
            
            if (blipX >= x && blipX <= x + radarSize && 
                blipY >= y && blipY <= y + radarSize) {
                g2d.fillOval(blipX - 2, blipY - 2, 4, 4);
            }
        }
        
        // Radar label
        g2d.setFont(smallFont);
        g2d.setColor(new Color(200, 220, 255));
        g2d.drawString("RADAR", x + radarSize/2 - 15, y + radarSize + 15);
    }
    
    private void drawComboDisplay(Graphics2D g2d) {
        int comboX = getWidth() / 2;
        int comboY = 100;
        
        // Combo background
        float comboScale = 1.0f + (float)Math.sin(System.currentTimeMillis() / 100.0) * 0.1f;
        g2d.setFont(new Font("Arial", Font.BOLD, (int)(48 * comboScale)));
        
        // Combo text with glow
        String comboText = combo + "x COMBO!";
        drawTextWithGlow(g2d, comboText, comboX, comboY, 
                        NEON_COLORS[1], Color.BLACK, 5);
        
        // Combo multiplier
        if (comboMultiplier > 1) {
            g2d.setFont(new Font("Arial", Font.BOLD, 24));
            String multiplierText = "x" + comboMultiplier + " MULTIPLIER";
            drawTextWithGlow(g2d, multiplierText, comboX, comboY + 40, 
                            NEON_COLORS[3], Color.BLACK, 3);
        }
    }
    
    private void drawComboStreak(Graphics2D g2d) {
        int streakX = getWidth() / 2;
        int streakY = getHeight() - 150;
        
        // Streak background
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(streakX - 120, streakY - 30, 240, 60, 20, 20);
        
        // Streak border
        g2d.setColor(NEON_COLORS[2]);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRoundRect(streakX - 120, streakY - 30, 240, 60, 20, 20);
        
        // Streak text
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.setColor(Color.WHITE);
        String streakText = "KILL STREAK: " + killStreak;
        drawCenteredText(g2d, streakText, streakX, streakY);
        
        // Streak message
        if (System.currentTimeMillis() < streakMessageEndTime) {
            g2d.setFont(new Font("Arial", Font.BOLD, 28));
            g2d.setColor(NEON_COLORS[1]);
            drawCenteredText(g2d, streakMessage, streakX, streakY - 50);
        }
    }
    
    private void drawPauseScreen(Graphics2D g2d) {
        // Dark overlay
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, getWidth(), getHeight());
        
        // Pause title
        g2d.setFont(titleFont);
        drawTextWithGlow(g2d, "GAME PAUSED", getWidth() / 2, getHeight() / 2 - 100,
                        NEON_COLORS[0], Color.BLACK, 4);
        
        // Stats panel
        int panelWidth = 400;
        int panelHeight = 200;
        int panelX = (getWidth() - panelWidth) / 2;
        int panelY = getHeight() / 2 - 50;
        
        g2d.setColor(new Color(20, 30, 50, 220));
        g2d.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 20, 20);
        
        g2d.setColor(NEON_COLORS[0]);
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 20, 20);
        
        // Stats
        g2d.setFont(largeFont);
        g2d.setColor(Color.WHITE);
        
        int textY = panelY + 50;
        g2d.drawString("SCORE: " + String.format("%,d", score), panelX + 40, textY);
        g2d.drawString("LEVEL: " + level, panelX + 40, textY + 40);
        g2d.drawString("LIVES: " + lives, panelX + 40, textY + 80);
        g2d.drawString("PATTERN: " + player.getPatternLevel(), panelX + 40, textY + 120);
        g2d.drawString("DIFFICULTY: " + difficulty.getLabel(), panelX + 40, textY + 160);
        
        // Continue prompt
        g2d.setFont(mediumFont);
        g2d.setColor(new Color(200, 220, 255));
        drawCenteredText(g2d, "Press P to Resume | ESC for Menu", getWidth() / 2, panelY + panelHeight + 50);
    }
    
    private void drawGameOverScreen(Graphics2D g2d) {
        // Dark overlay with stars
        g2d.setColor(new Color(0, 0, 0, 220));
        g2d.fillRect(0, 0, getWidth(), getHeight());
        
        // Game over title
        g2d.setFont(new Font("Arial", Font.BOLD, 56));
        drawTextWithGlow(g2d, "MISSION FAILED", getWidth() / 2, getHeight() / 2 - 150,
                        NEON_COLORS[1], Color.BLACK, 6);
        
        // Stats panel
        int panelWidth = 500;
        int panelHeight = 300;
        int panelX = (getWidth() - panelWidth) / 2;
        int panelY = getHeight() / 2 - 100;
        
        // Panel with gradient
        GradientPaint panelGradient = new GradientPaint(
            panelX, panelY, new Color(50, 0, 0, 200),
            panelX + panelWidth, panelY + panelHeight, new Color(20, 0, 30, 200)
        );
        g2d.setPaint(panelGradient);
        g2d.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 30, 30);
        
        // Panel border
        g2d.setColor(NEON_COLORS[1]);
        g2d.setStroke(new BasicStroke(4));
        g2d.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 30, 30);
        
        // Stats
        g2d.setFont(largeFont);
        g2d.setColor(Color.WHITE);
        
        int textY = panelY + 60;
        int col1 = panelX + 50;
        int col2 = panelX + 300;
        
        g2d.drawString("FINAL SCORE:", col1, textY);
        g2d.setColor(NEON_COLORS[0]);
        g2d.drawString(String.format("%,d", score), col2, textY);
        
        g2d.setColor(Color.WHITE);
        g2d.drawString("HIGH SCORE:", col1, textY + 50);
        g2d.setColor(new Color(255, 215, 0));
        g2d.drawString(String.format("%,d", Math.max(score, highScore)), col2, textY + 50);
        
        g2d.setColor(Color.WHITE);
        g2d.drawString("LEVEL REACHED:", col1, textY + 100);
        g2d.setColor(NEON_COLORS[2]);
        g2d.drawString(String.valueOf(level), col2, textY + 100);
        
        g2d.drawString("ENEMIES DEFEATED:", col1, textY + 150);
        g2d.setColor(NEON_COLORS[3]);
        g2d.drawString(String.valueOf(stats.enemiesKilled), col2, textY + 150);
        
        g2d.drawString("MAX COMBO:", col1, textY + 200);
        g2d.setColor(NEON_COLORS[1]);
        g2d.drawString(stats.maxCombo + "x", col2, textY + 200);
        
        g2d.drawString("MAX PATTERN:", col1, textY + 250);
        g2d.setColor(NEON_COLORS[0]);
        g2d.drawString("Lvl " + player.getPatternLevel(), col2, textY + 250);
        
        // Buttons
        drawButton(g2d, "RETRY MISSION", getWidth() / 2 - 120, panelY + panelHeight + 50, 
                  240, 50, menuIndex == 0, NEON_COLORS[0]);
        drawButton(g2d, "MAIN MENU", getWidth() / 2 - 120, panelY + panelHeight + 120, 
                  240, 50, menuIndex == 1, NEON_COLORS[2]);
        
        // Prompts
        g2d.setFont(smallFont);
        g2d.setColor(new Color(200, 200, 255, 150));
        drawCenteredText(g2d, "Press ENTER to select | Use mouse to click buttons", 
                        getWidth() / 2, getHeight() - 30);
    }
    
    private void drawButton(Graphics2D g2d, String text, int x, int y, int width, int height, 
                           boolean selected, Color baseColor) {
        if (selected) {
            // Glow effect
            for (int i = 0; i < 3; i++) {
                float alpha = 0.3f - i * 0.1f;
                g2d.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), 
                                     baseColor.getBlue(), (int)(alpha * 255)));
                g2d.fillRoundRect(x - i * 3, y - i * 3, width + i * 6, height + i * 6, 25, 25);
            }
            
            // Main button
            GradientPaint gradient = new GradientPaint(
                x, y, baseColor,
                x, y + height, baseColor.darker()
            );
            g2d.setPaint(gradient);
        } else {
            g2d.setColor(new Color(baseColor.getRed(), baseColor.getGreen(), 
                                 baseColor.getBlue(), 100));
        }
        
        g2d.fillRoundRect(x, y, width, height, 25, 25);
        
        // Border
        g2d.setColor(selected ? Color.WHITE : new Color(255, 255, 255, 150));
        g2d.setStroke(new BasicStroke(selected ? 3 : 1));
        g2d.drawRoundRect(x, y, width, height, 25, 25);
        
        // Text
        g2d.setFont(mediumFont);
        g2d.setColor(selected ? Color.BLACK : new Color(220, 240, 255));
        FontMetrics fm = g2d.getFontMetrics();
        int textX = x + (width - fm.stringWidth(text)) / 2;
        int textY = y + (height + fm.getAscent()) / 2 - 5;
        g2d.drawString(text, textX, textY);
    }
    
    private void drawTextWithGlow(Graphics2D g2d, String text, int x, int y, 
                                 Color textColor, Color glowColor, int glowSize) {
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textX = x - textWidth / 2;
        int textY = y;
        
        // Draw glow
        for (int i = glowSize; i > 0; i--) {
            float alpha = 0.5f * (i / (float)glowSize);
            g2d.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), 
                                 glowColor.getBlue(), (int)(alpha * 255)));
            g2d.drawString(text, textX + i, textY + i);
            g2d.drawString(text, textX - i, textY - i);
            g2d.drawString(text, textX + i, textY - i);
            g2d.drawString(text, textX - i, textY + i);
        }
        
        // Draw main text
        g2d.setColor(textColor);
        g2d.drawString(text, textX, textY);
    }
    
    private void drawCenteredText(Graphics2D g2d, String text, int x, int y) {
        FontMetrics fm = g2d.getFontMetrics();
        int textX = x - fm.stringWidth(text) / 2;
        int textY = y;
        g2d.drawString(text, textX, textY);
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        // Update animations
        updateAnimations();

        if (gameState == GameState.RUNNING && !ambientActive) {
            SoundManager.playSound(SoundManager.SoundType.ENGINE_HUM);
            SoundManager.playSound(SoundManager.SoundType.SPACE_AMBIENT);
            ambientActive = true;
        } else if (gameState != GameState.RUNNING && ambientActive) {
            SoundManager.stopSoundType(SoundManager.SoundType.ENGINE_HUM);
            SoundManager.stopSoundType(SoundManager.SoundType.SPACE_AMBIENT);
            ambientActive = false;
        }
        
        if (gameState == GameState.LOADING) {
            loadingProgress = Math.min(1.0f, 
                (System.currentTimeMillis() - loadingStartTime) / (float)GameConfig.LOADING_DURATION_MS);
            if (loadingProgress >= 1.0f) {
                gameState = GameState.MENU;
            }
            repaint();
            return;
        }
        
        if (gameState != GameState.RUNNING) {
            repaint();
            return;
        }
        
        // Update game logic
        updateGameLogic();
        repaint();
    }
    
    private void updateAnimations() {
        if (backgroundImage != null) {
            int imgH = backgroundImage.getHeight();
            backgroundOffsetY = (backgroundOffsetY + backgroundScrollSpeed) % imgH;
        }

        // Update stars
        for (Star star : stars) {
            star.update();
        }
        
        // Update particles
        for (ParticleEffect particle : particles) {
            particle.update();
        }
        
        // Spawn comets occasionally
        if (Math.random() < 0.001) {
            comets.add(new Comet(getWidth(), getHeight()));
        }
        
        // Update comets
        for (Comet comet : new ArrayList<>(comets)) {
            comet.update();
            if (comet.isOffscreen()) {
                comets.remove(comet);
            }
        }
        
        // Update screen shake
        if (System.currentTimeMillis() >= screenShakeEndTime) {
            screenShakeIntensity = Math.max(0, screenShakeIntensity - 0.1f);
        }
    }
    
    private void updateGameLogic() {
        // Update player
        player.update(getWidth(), getHeight());
        
        // Update bullets
        updateBullets();
        updateEnemyBullets();
        
        // Update enemies
        updateEnemies();
        
        // Update rocks
        updateRocks();
        
        // Update pickups
        updatePickups();
        
        // Check collisions
        checkBulletCollision();
        checkPlayerCollisions();
        checkPickupCollisions();
        checkPowerUpCollisions(); // NEW
        
        // Spawn new objects
        spawnRock();
        spawnEnemy();
        maybeEnemyShoot();
        
        // Update combo
        updateCombo();
        
        // Update level based on time
        updateLevel();
    }
    
    private void updateCombo() {
        if (System.currentTimeMillis() - lastComboTime > GameConfig.COMBO_TIMEOUT_MS) {
            if (combo > 0) {
                stats.maxCombo = Math.max(stats.maxCombo, combo);
                SoundManager.playSound(SoundManager.SoundType.COMBO_BREAK);
            }
            combo = 0;
            comboMultiplier = 1;
        }
    }

    private void updateBullets() {
        for (Bullet bullet : new ArrayList<>(player.getBullets())) {
            bullet.update(1.0f / 60.0f);
            if (!bullet.isVisible()) {
                player.getBullets().remove(bullet);
            }
        }
    }

    private void updateEnemyBullets() {
        for (EnemyBullet bullet : new ArrayList<>(enemyBullets)) {
            bullet.update(1.0f / 60.0f);
            if (!bullet.isActive()) {
                enemyBullets.remove(bullet);
            }
        }
    }

    private void updateEnemies() {
        ArrayList<Enemy> snapshot = new ArrayList<>(enemies);
        for (Enemy enemy : snapshot) {
            enemy.update(1.0f / 60.0f, player, enemyBullets, enemies);
            if (enemy.getBounds().y > getHeight() + GameConfig.ENEMY_SIZE) {
                enemies.remove(enemy);
            }
        }

        // Simple separation to avoid enemy collisions
        for (int i = 0; i < snapshot.size(); i++) {
            Enemy a = snapshot.get(i);
            for (int j = i + 1; j < snapshot.size(); j++) {
                Enemy b = snapshot.get(j);
                Rectangle ra = a.getBounds();
                Rectangle rb = b.getBounds();
                if (ra.intersects(rb)) {
                    // Enemies now handle their own collision avoidance in update()
                }
            }
        }
    }

    private void updateRocks() {
        for (Rock rock : new ArrayList<>(rocks)) {
            rock.update();
            if (rock.getBounds().y > getHeight() + GameConfig.ROCK_SIZE) {
                rocks.remove(rock);
            }
        }
    }
    
    private void updatePickups() {
        // Update heart pickups
        for (HeartPickup pickup : new ArrayList<>(heartPickups)) {
            pickup.update(1.0f / 60.0f);
            if (pickup.isCollected()) {
                heartPickups.remove(pickup);
            }
        }
        
        // Update shield pickups
        for (ShieldPickup pickup : new ArrayList<>(shieldPickups)) {
            pickup.update(1.0f / 60.0f);
            if (pickup.isCollected()) {
                shieldPickups.remove(pickup);
            }
        }
        
        // Update power-up pickups
        for (PowerUpPickup pickup : new ArrayList<>(powerUpPickups)) {
            pickup.update(1.0f / 60.0f);
            if (pickup.isCollected()) {
                powerUpPickups.remove(pickup);
            }
        }
    }
    
    private void checkPickupCollisions() {
        // Check heart pickups
        for (HeartPickup pickup : new ArrayList<>(heartPickups)) {
            if (pickup.checkCollision(player)) {
                heartPickups.remove(pickup);
            }
        }
        
        // Check shield pickups
        for (ShieldPickup pickup : new ArrayList<>(shieldPickups)) {
            if (pickup.checkCollision(player)) {
                shieldPickups.remove(pickup);
            }
        }
    }
    
    private void checkPowerUpCollisions() {
        // Check power-up pickups
        for (PowerUpPickup pickup : new ArrayList<>(powerUpPickups)) {
            if (pickup.checkCollision(player)) {
                applyPowerUp(pickup.getType());
                powerUpPickups.remove(pickup);
            }
        }
    }
    
    private void applyPowerUp(PowerUpType type) {
        // Apply difficulty-based power-up duration reduction
        int durationReduction = difficulty.getPowerUpDurationReduction();
        int baseDuration = GameConfig.POWERUP_DURATION_MS;
        
        switch (type) {
            case RAPID_FIRE:
                player.activateRapidFire(baseDuration - durationReduction);
                SoundManager.playSound(SoundManager.SoundType.PLAYER_POWERUP);
                break;
            case SPREAD_SHOT:
                player.activateSpread(baseDuration - durationReduction);
                SoundManager.playSound(SoundManager.SoundType.PLAYER_POWERUP);
                break;
            case PATTERN_UPGRADE:
                player.upgradePattern();
                break;
        }
        
        // Visual effect
        ParticleSystem.getInstance().createUpgradeEffect(
            player.getX() + player.getWidth() / 2,
            player.getY() + player.getHeight() / 2,
            Color.CYAN
        );
    }

    private void checkPlayerCollisions() {
        if (!player.isInvulnerable()) {
            for (Rock rock : new ArrayList<>(rocks)) {
                if (player.getBounds().intersects(rock.getBounds())) {
                    if (player.takeDamage(33)) { // 33 damage (1 heart)
                        handlePlayerDeath();
                    } else {
                        handlePlayerHit();
                    }
                    return;
                }
            }

            for (Enemy enemy : new ArrayList<>(enemies)) {
                if (player.getBounds().intersects(enemy.getBounds())) {
                    enemies.remove(enemy);
                    if (player.takeDamage(33)) { // 33 damage (1 heart)
                        handlePlayerDeath();
                    } else {
                        handlePlayerHit();
                    }
                    return;
                }
            }

            for (EnemyBullet bullet : new ArrayList<>(enemyBullets)) {
                if (player.getBounds().intersects(bullet.getBounds())) {
                    enemyBullets.remove(bullet);
                    if (player.takeDamage(bullet.getDamage())) { // Use actual bullet damage (12)
                        handlePlayerDeath();
                    } else {
                        handlePlayerHit();
                    }
                    return;
                }
            }
        }
    }

    private void spawnRock() {
        // Progressive difficulty: increase spawn rate with level
        double levelBonus = (level * 0.0015);
        double chance = difficulty.getRockSpawnChance() + levelBonus;
        if (Math.random() < chance) {
            // Rocks get faster with level (using GameConfig)
            double levelSpeedBonus = 1.0 + (level * (GameConfig.LEVEL_ROCK_SPEED_INCREASE / 100.0));
            rocks.add(new Rock(difficulty.getSpeedMultiplier() * levelSpeedBonus));
        }
    }

    private void spawnEnemy() {
        // Progressive difficulty: more enemies and higher spawn rate with level
        int maxEnemies = Math.min(GameConfig.ENEMY_MAX, difficulty.getMaxEnemies() + (level / 2));
        double levelBonus = (level * 0.0015);
        double chance = difficulty.getEnemySpawnChance() + levelBonus;
        if (enemies.size() < maxEnemies && Math.random() < chance) {
            enemies.add(spawnEnemyByType());
            SoundManager.playSound(SoundManager.SoundType.ENEMY_SPAWN);
        }
    }

    private Enemy spawnEnemyByType() {
        // Spawn enemy with current difficulty and level for progressive scaling
        Enemy enemy = new Enemy(difficulty, level);
        return enemy;
    }

    private void maybeEnemyShoot() {
        // Enemy shooting is now handled internally in Enemy.update()
    }

    private void updateLevel() {
        long currentTime = System.currentTimeMillis();
        long elapsedSeconds = (currentTime - lastLevelUpTime) / 1000;
        
        // Calculate new level based on time (every LEVEL_INTERVAL_SECONDS)
        int newLevel = 1 + (int)(elapsedSeconds / GameConfig.LEVEL_INTERVAL_SECONDS);
        
        if (newLevel > level) {
            SoundManager.playSound(SoundManager.SoundType.LEVEL_UP);
            
            // Auto-increase difficulty tier at configured levels
            if (newLevel % GameConfig.DIFFICULTY_UPGRADE_LEVEL_1 == 0 && newLevel > level) {
                if (difficulty == Difficulty.EASY && newLevel >= GameConfig.DIFFICULTY_UPGRADE_LEVEL_1) {
                    difficulty = Difficulty.NORMAL;
                    showDifficultyMessage("Difficulty increased to NORMAL!");
                } else if (difficulty == Difficulty.NORMAL && newLevel >= GameConfig.DIFFICULTY_UPGRADE_LEVEL_2) {
                    difficulty = Difficulty.HARD;
                    showDifficultyMessage("Difficulty increased to HARD!");
                }
            }
            
            // Unlock new bullet patterns based on level
            player.updateUnlockedPatterns(newLevel);
            
            level = newLevel;
        }
    }
    
    private void showDifficultyMessage(String message) {
        streakMessage = message;
        streakMessageEndTime = System.currentTimeMillis() + GameConfig.DIFFICULTY_MESSAGE_DURATION_MS;
        SoundManager.playSound(SoundManager.SoundType.LEVEL_UP);
    }
    
    private void handleKeyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        
        if (gameState == GameState.LOADING) {
            SoundManager.playSound(SoundManager.SoundType.UI_ERROR);
            return;
        }
        
        if (gameState == GameState.MENU) {
            handleMenuInput(key);
            return;
        }
        
        if (gameState == GameState.GAME_OVER) {
            handleGameOverInput(key);
            return;
        }
        
        if (key == KeyEvent.VK_ESCAPE) {
            if (gameState == GameState.PAUSED) {
                gameState = GameState.MENU;
                SoundManager.playMenu();
            } else if (gameState == GameState.RUNNING) {
                gameState = GameState.PAUSED;
                player.stopShooting();
                SoundManager.playPause();
            }
            return;
        }
        
        if (key == KeyEvent.VK_P) {
            if (gameState == GameState.PAUSED) {
                gameState = GameState.RUNNING;
                SoundManager.playResume();
            } else if (gameState == GameState.RUNNING) {
                gameState = GameState.PAUSED;
                player.stopShooting();
                SoundManager.playPause();
            }
            return;
        }
        
        if (gameState == GameState.RUNNING) {
            player.keyPressed(e);
        }
    }
    
    private void handleMenuInput(int key) {
        switch (key) {
            case KeyEvent.VK_UP:
                menuIndex = (menuIndex - 1 + 5) % 5;
                SoundManager.playMenu();
                break;
            case KeyEvent.VK_DOWN:
                menuIndex = (menuIndex + 1) % 5;
                SoundManager.playMenu();
                break;
            case KeyEvent.VK_LEFT:
                if (menuIndex == 1) {
                    difficulty = difficulty.previous();
                    SoundManager.playMenu();
                } else if (menuIndex == 2) {
                    SoundManager.setEnabled(!SoundManager.isEnabled());
                    SoundManager.playMenu();
                }
                break;
            case KeyEvent.VK_RIGHT:
                if (menuIndex == 1) {
                    difficulty = difficulty.next();
                    SoundManager.playMenu();
                } else if (menuIndex == 2) {
                    SoundManager.setEnabled(!SoundManager.isEnabled());
                    SoundManager.playMenu();
                }
                break;
            case KeyEvent.VK_ENTER:
                handleMenuSelection();
                break;
        }
        repaint();
    }
    
    private void handleGameOverInput(int key) {
        switch (key) {
            case KeyEvent.VK_UP:
                menuIndex = (menuIndex - 1 + 2) % 2;
                SoundManager.playMenu();
                break;
            case KeyEvent.VK_DOWN:
                menuIndex = (menuIndex + 1) % 2;
                SoundManager.playMenu();
                break;
            case KeyEvent.VK_ENTER:
                if (menuIndex == 0) {
                    resetGame();
                    gameState = GameState.RUNNING;
                    SoundManager.playStart();
                } else {
                    gameState = GameState.MENU;
                    menuIndex = 0;
                    SoundManager.playMenu();
                }
                break;
        }
        repaint();
    }
    
    private void handleMouseClick(int x, int y) {
        if (gameState == GameState.MENU) {
            handleMenuMouseClick(x, y);
        } else if (gameState == GameState.GAME_OVER) {
            handleGameOverMouseClick(x, y);
        }
    }
    
    private void handleMenuMouseClick(int x, int y) {
        int baseY = getHeight() / 2 - 40;
        
        for (int i = 0; i < 5; i++) {
            int buttonY = baseY + (i * 50);
            int buttonX = getWidth() / 2 - 150;
            
            if (x >= buttonX && x <= buttonX + 300 && 
                y >= buttonY - 20 && y <= buttonY + 20) {
                menuIndex = i;
                SoundManager.playUIClick();
                if (i == 0 || i == 4) {
                    handleMenuSelection();
                }
                repaint();
                break;
            }
        }
    }
    
    private void handleGameOverMouseClick(int x, int y) {
        int buttonY = getHeight() / 2 + 150;
        int buttonX = getWidth() / 2 - 120;
        
        // Retry button
        if (x >= buttonX && x <= buttonX + 240 && 
            y >= buttonY && y <= buttonY + 50) {
            resetGame();
            gameState = GameState.RUNNING;
            SoundManager.playStart();
            return;
        }
        
        // Menu button
        buttonY += 70;
        if (x >= buttonX && x <= buttonX + 240 && 
            y >= buttonY && y <= buttonY + 50) {
            gameState = GameState.MENU;
            menuIndex = 0;
            SoundManager.playMenu();
        }
    }
    
    private void handleMouseMove(int x, int y) {
        int baseY = getHeight() / 2 - 40;
        
        for (int i = 0; i < 5; i++) {
            int buttonY = baseY + (i * 50);
            int buttonX = getWidth() / 2 - 150;
            
            if (x >= buttonX && x <= buttonX + 300 && 
                y >= buttonY - 20 && y <= buttonY + 20) {
                if (menuIndex != i) {
                    menuIndex = i;
                    SoundManager.playUIHover();
                    repaint();
                }
                return;
            }
        }
    }
    
    private void handleMenuSelection() {
        SoundManager.playUIClick();
        switch (menuIndex) {
            case 0: // Start Mission
                resetGame();
                gameState = GameState.RUNNING;
                SoundManager.playStart();
                break;
            case 3: // Controls
                showControlsScreen();
                break;
            case 4: // Quit
                System.exit(0);
                break;
        }
    }
    
    private void showControlsScreen() {
        JOptionPane.showMessageDialog(this,
            "USER MANUAL\n\n" +
            "MOVEMENT:\n" +
            "- Arrow Keys or WASD: Move the ship\n\n" +
            "SHOOTING:\n" +
            "- SPACE: Fire bullets\n" +
            "- 1-8: Switch bullet patterns (1=Single, 8=Max)\n\n" +
            "BULLET PATTERNS:\n" +
            "- Each pattern has same total damage\n" +
            "- More bullets = wider coverage\n" +
            "- Press number keys to switch patterns\n\n" +
            "POWER-UPS:\n" +
            "- Heart: Restore health\n" +
            "- Shield: Temporary protection\n" +
            "- RF: Rapid Fire (faster shooting)\n" +
            "- SP: Spread Shot (wider pattern)\n" +
            "- PU: Pattern Upgrade (increase level)\n\n" +
            "GAME STATE & MENUS:\n" +
            "- P: Pause/Resume\n" +
            "- ESC: Back to menu from pause\n" +
            "- Menu: Arrow Keys + ENTER (or mouse)\n\n" +
            "TIPS:\n" +
            "- Chain kills quickly for combo multipliers\n" +
            "- Use wider patterns for groups of enemies\n" +
            "- Higher levels spawn faster, tougher enemies",
            "Controls & Tips",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void resetGame() {
        player = new Player();
        rocks.clear();
        enemies.clear();
        enemyBullets.clear();
        heartPickups.clear();
        shieldPickups.clear();
        powerUpPickups.clear();
        particles.clear();
        score = 0;
        lives = GameConfig.PLAYER_LIVES;
        combo = 0;
        killStreak = 0;
        level = 1;
        lastLevelUpTime = System.currentTimeMillis(); // Initialize level timer
        stats.reset();
        
        // Initialize player with level 1 patterns
        player.updateUnlockedPatterns(level);
        
        SoundManager.stopAlarm();
        alarmActive = false;
    }
    
    private void startScreenShake(float intensity, long duration) {
        screenShakeIntensity = intensity;
        screenShakeEndTime = System.currentTimeMillis() + duration;
    }
    
    private void showDamageIndicator(long duration) {
        showDamageIndicator = true;
        damageIndicatorEndTime = System.currentTimeMillis() + duration;
    }
    
    private void updateKillStreak() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastKillTime > GameConfig.STREAK_TIMEOUT_MS) {
            killStreak = 0;
        }
        
        killStreak++;
        lastKillTime = currentTime;
        
        // Set streak message based on streak count
        if (killStreak >= 20) {
            streakMessage = "UNSTOPPABLE!";
            SoundManager.playSound(SoundManager.SoundType.RAILGUN);
        } else if (killStreak >= 15) {
            streakMessage = "DOMINATING!";
            SoundManager.playSound(SoundManager.SoundType.ROCKET);
        } else if (killStreak >= 10) {
            streakMessage = "RAMPAGE!";
            SoundManager.playSound(SoundManager.SoundType.ROCKET);
        } else if (killStreak >= 5) {
            streakMessage = "KILLING SPREE!";
            SoundManager.playSound(SoundManager.SoundType.ROCKET);
        }
        
        streakMessageEndTime = currentTime + 1000;
    }
    
    private int loadHighScore() {
        // In a real game, this would load from a file
        return 0;
    }
    
    private void saveHighScore() {
        if (score > highScore) {
            highScore = score;
            // In a real game, this would save to a file
        }
    }
    
    private void checkBulletCollision() {
        List<Bullet> bullets = player.getBullets();
        for (Bullet bullet : new ArrayList<>(bullets)) {
            // Check rock collisions
            for (Rock rock : new ArrayList<>(rocks)) {
                if (bullet.checkCollision(rock.getBounds())) {
                    // Create impact effects for each bullet in pattern
                    for (Rectangle hitbox : bullet.getAllHitboxes()) {
                        if (hitbox.intersects(rock.getBounds())) {
                            ParticleSystem.getInstance().createBulletImpact(
                                (float)hitbox.getCenterX(),
                                (float)hitbox.getCenterY(),
                                bullet.getColor()
                            );
                        }
                    }
                    
                    bullets.remove(bullet);
                    SoundManager.playSound(SoundManager.SoundType.ENEMY_HIT);
                    break;
                }
            }
            
            if (!bullet.isVisible()) continue;
            
            // Check enemy collisions
            for (Enemy enemy : new ArrayList<>(enemies)) {
                if (bullet.checkCollision(enemy.getBounds())) {
                    // Create impact effects for each bullet that hits
                    for (Rectangle hitbox : bullet.getAllHitboxes()) {
                        if (hitbox.intersects(enemy.getBounds())) {
                            ParticleSystem.getInstance().createBulletImpact(
                                (float)hitbox.getCenterX(),
                                (float)hitbox.getCenterY(),
                                bullet.getColor()
                            );
                        }
                    }
                    
                    SoundManager.playSound(SoundManager.SoundType.ENEMY_HIT);
                    
                    // Apply damage - each bullet in pattern does damage
                    // bullet.getDamage() returns damage per bullet
                    boolean died = enemy.takeDamage(bullet.getDamage() * bullet.getBulletCount());
                    
                    bullet.setVisible(false);
                    bullets.remove(bullet);
                    
                    if (!enemy.isAlive()) {
                        // Create explosion
                        createExplosion((int)(enemy.getX() + GameConfig.ENEMY_SIZE/2), 
                                      (int)(enemy.getY() + GameConfig.ENEMY_SIZE/2));
                        
                        enemies.remove(enemy);
                        // Apply difficulty-based score multiplier
                        int baseScore = GameConfig.BASE_ENEMY_SCORE;
                        double difficultyMultiplier = difficulty.getBaseScoreMultiplier() / 100.0;
                        score += (int)(baseScore * comboMultiplier * difficultyMultiplier);
                        
                        // Update combo
                        combo++;
                        lastComboTime = System.currentTimeMillis();
                        comboMultiplier = 1 + combo / GameConfig.COMBO_MULTIPLIER_STEP;
                        if (combo % GameConfig.COMBO_MULTIPLIER_STEP == 0) {
                            SoundManager.playSound(SoundManager.SoundType.COMBO_MULTIPLIER);
                        }
                        
                        // Update streak
                        updateKillStreak();
                        stats.enemiesKilled++;
                        
                        startScreenShake(GameConfig.SCREEN_SHAKE_INTENSITY, GameConfig.SCREEN_SHAKE_DURATION_MS);
                        SoundManager.playSound(SoundManager.SoundType.ENEMY_EXPLOSION_SMALL);
                        
                        // Chance to spawn power-up
                        spawnPowerUp(enemy.getX(), enemy.getY());
                    }
                    break;
                }
            }
        }
    }
    
    private void spawnPowerUp(float x, float y) {
        // Use difficulty-based power-up spawn chance
        double chance = difficulty.getPowerUpSpawnChance();
        
        if (Math.random() < chance) {
            double rand = Math.random();
            PowerUpType type;
            
            if (rand < 0.4) {
                type = PowerUpType.RAPID_FIRE;
            } else if (rand < 0.7) {
                type = PowerUpType.SPREAD_SHOT;
            } else {
                type = PowerUpType.PATTERN_UPGRADE;
            }
            
            powerUpPickups.add(new PowerUpPickup((int)x, (int)y, type));
        }
    }
    
    private void createExplosion(int x, int y) {
        ParticleSystem.getInstance().createLargeExplosion(
            (float)x, (float)y,
            new Color(255, 140, 40),
            new Color(255, 80, 20)
        );
        AnimationManager.getInstance().screenFlash(new Color(255, 120, 50), 0.2f, 120);
        for (int i = 0; i < 20; i++) {
            particles.add(new ParticleEffect(x, y, 
                (float)(Math.random() * 360), 
                (float)(Math.random() * 3 + 1),
                new Color(255, (int)(Math.random() * 200), 0),
                (int)(Math.random() * 30 + 20)));
        }
    }
    
    private void handlePlayerHit() {
        // Don't decrement lives - that's handled by player's health system
        player.setInvulnerable(GameConfig.INVULNERABLE_MS);
        
        // Reset combo on hit
        combo = 0;
        comboMultiplier = 1;
        killStreak = 0;
        
        // Visual effects
        startScreenShake(0.5f, 300);
        showDamageIndicator(300);
        
        // Create hit particles
        for (int i = 0; i < 15; i++) {
            particles.add(new ParticleEffect(player.getX() + 25, player.getY() + 25,
                (float)(Math.random() * 360),
                (float)(Math.random() * 2 + 1),
                new Color(255, 100, 100),
                (int)(Math.random() * 40 + 20)));
        }

        ParticleSystem.getInstance().createHitEffect(player.getX() + 25, player.getY() + 25, 20, new Color(255, 80, 80));
        AnimationManager.getInstance().screenFlash(new Color(255, 60, 60), 0.15f, 100);
        
        SoundManager.playHit();
        SoundManager.playSound(SoundManager.SoundType.SHIELD_HIT);

        // Check if health is low
        if (player.getHealth() <= 33.33f && !alarmActive) {
            SoundManager.playSound(SoundManager.SoundType.WARNING);
            SoundManager.playAlarm();
            alarmActive = true;
        }
    }
    
    private void handlePlayerDeath() {
        // Handle visual effects first
        handlePlayerHit();
        
        // Game over
        gameState = GameState.GAME_OVER;
        player.stopShooting();
        saveHighScore();
        SoundManager.playSound(SoundManager.SoundType.PLAYER_DEATH);
        SoundManager.stopAlarm();
        alarmActive = false;
    }
    
    // Supporting inner classes
    class Star {
        float x, y;
        float speed;
        float size;
        float brightness;
        
        Star() {
            x = (float)(Math.random() * getWidth());
            y = (float)(Math.random() * getHeight());
            speed = (float)(Math.random() * 0.5 + 0.1);
            size = (float)(Math.random() * 2 + 1);
            brightness = (float)(Math.random() * 0.5 + 0.5);
        }
        
        void update() {
            y += speed;
            if (y > getHeight()) {
                y = 0;
                x = (float)(Math.random() * getWidth());
            }
        }
        
        void draw(Graphics2D g2d) {
            int alpha = (int)(brightness * 255);
            g2d.setColor(new Color(255, 255, 255, alpha));
            g2d.fillOval((int)x, (int)y, (int)size, (int)size);
        }
    }
    
    class Comet {
        float x, y;
        float speedX, speedY;
        float size;
        List<ParticleEffect> trail;
        
        Comet(int screenWidth, int screenHeight) {
            x = (float)(Math.random() * screenWidth);
            y = -20;
            speedX = (float)(Math.random() * 2 - 1);
            speedY = (float)(Math.random() * 3 + 2);
            size = (float)(Math.random() * 4 + 2);
            trail = new ArrayList<>();
        }
        
        void update() {
            x += speedX;
            y += speedY;
            
            // Add trail particles
            if (Math.random() < 0.7) {
                trail.add(new ParticleEffect(x, y,
                    (float)(Math.random() * 360),
                    (float)(Math.random() * 0.5),
                    new Color(200, 230, 255, 150),
                    (int)(Math.random() * 20 + 10)));
            }
            
            // Update trail
            Iterator<ParticleEffect> iter = trail.iterator();
            while (iter.hasNext()) {
                ParticleEffect p = iter.next();
                p.update();
                if (!p.isAlive()) {
                    iter.remove();
                }
            }
        }
        
        void draw(Graphics2D g2d) {
            // Draw trail
            for (ParticleEffect p : trail) {
                p.draw(g2d);
            }
            
            // Draw comet
            g2d.setColor(Color.WHITE);
            g2d.fillOval((int)x, (int)y, (int)size, (int)size);
            
            // Glow effect
            g2d.setColor(new Color(200, 230, 255, 100));
            g2d.fillOval((int)x - 2, (int)y - 2, (int)size + 4, (int)size + 4);
        }
        
        boolean isOffscreen() {
            return y > getHeight() + 20 || x < -20 || x > getWidth() + 20;
        }
    }
    
    class ParticleEffect {
        float x, y;
        float angle;
        float speed;
        Color color;
        int life;
        int maxLife;
        float size;
        
        ParticleEffect(float x, float y, float angle, float speed, Color color, int life) {
            this.x = x;
            this.y = y;
            this.angle = angle;
            this.speed = speed;
            this.color = color;
            this.life = life;
            this.maxLife = life;
            this.size = (float)(Math.random() * 4 + 2);
        }
        
        void update() {
            float rad = (float)Math.toRadians(angle);
            x += Math.cos(rad) * speed;
            y += Math.sin(rad) * speed;
            
            // Slow down
            speed *= 0.95f;
            life--;
        }
        
        void draw(Graphics2D g2d) {
            float alpha = (float)life / maxLife;
            int drawSize = (int)(size * alpha);
            
            if (drawSize > 0) {
                g2d.setColor(new Color(
                    color.getRed(),
                    color.getGreen(),
                    color.getBlue(),
                    (int)(alpha * 255)
                ));
                g2d.fillOval((int)x - drawSize/2, (int)y - drawSize/2, drawSize, drawSize);
            }
        }
        
        boolean isAlive() {
            return life > 0;
        }
    }
    
    class GameStats {
        int enemiesKilled = 0;
        int maxCombo = 0;
        int powerUpsCollected = 0;
        long playTime = 0;
        
        void reset() {
            enemiesKilled = 0;
            maxCombo = 0;
            powerUpsCollected = 0;
            playTime = 0;
        }
        
        void update(double deltaTime) {
            playTime += (long)(deltaTime * 1000);
        }
    }
}

// New PowerUpPickup class (add this as a separate file or inner class)
class PowerUpPickup {
    private float x, y;
    private float dy = GameConfig.POWERUP_FALL_SPEED;
    private boolean collected = false;
    private ShootingGame.PowerUpType type;
    private Color color;
    
    public PowerUpPickup(int x, int y, ShootingGame.PowerUpType type) {
        this.x = x;
        this.y = y;
        this.type = type;
        
        // Set color based on type
        switch (type) {
            case RAPID_FIRE:
                color = Color.YELLOW;
                break;
            case SPREAD_SHOT:
                color = Color.GREEN;
                break;
            case PATTERN_UPGRADE:
                color = Color.CYAN;
                break;
        }
    }
    
    public void update(float deltaTime) {
        y += dy * deltaTime * GameConfig.FPS;
        if (y > GameConfig.HEIGHT + GameConfig.POWERUP_SIZE) {
            collected = true;
        }
    }
    
    public void draw(Graphics2D g2d) {
        g2d.setColor(color);
        g2d.fillRect((int)x, (int)y, GameConfig.POWERUP_SIZE, GameConfig.POWERUP_SIZE);
        
        // Draw letter based on type
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, GameConfig.FONT_SIZE_SMALL));
        
        String letter = "";
        switch (type) {
            case RAPID_FIRE: letter = "RF"; break;
            case SPREAD_SHOT: letter = "SP"; break;
            case PATTERN_UPGRADE: letter = "PU"; break;
        }
        
        FontMetrics fm = g2d.getFontMetrics();
        int textX = (int)x + (GameConfig.POWERUP_SIZE - fm.stringWidth(letter)) / 2;
        int textY = (int)y + (GameConfig.POWERUP_SIZE + fm.getAscent()) / 2 - 2;
        g2d.drawString(letter, textX, textY);
    }
    
    public boolean checkCollision(Player player) {
        if (collected) return false;
        
        Rectangle pickupBounds = new Rectangle((int)x, (int)y, GameConfig.POWERUP_SIZE, GameConfig.POWERUP_SIZE);
        if (pickupBounds.intersects(player.getBounds())) {
            collected = true;
            return true;
        }
        return false;
    }
    
    public boolean isCollected() {
        return collected;
    }
    
    public ShootingGame.PowerUpType getType() {
        return type;
    }
}