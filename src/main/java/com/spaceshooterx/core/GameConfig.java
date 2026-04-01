package com.spaceshooterx.core;

public final class GameConfig {
    // ==================== SCREEN ====================
    public static final int WIDTH = 1200;
    public static final int HEIGHT = 800;
    public static final int FPS = 60;
    public static final int FRAME_DELAY_MS = 1000 / FPS; // ~16ms for 60 FPS

    // ==================== PLAYER ====================
    public static final int PLAYER_SIZE = 80;
    public static final int PLAYER_SPEED = 10;
    public static final int PLAYER_LIVES = 3;
    public static final float PLAYER_HEALTH = 100.0f;
    public static final float PLAYER_INITIAL_SHIELD = 0.0f;
    public static final int PLAYER_SHOT_DAMAGE = 10;
    public static final float PLAYER_SHOT_COOLDOWN = 0.15f; // seconds
    public static final int INVULNERABLE_MS = 1000;

    // ==================== BULLETS ====================
    public static final int BULLET_SPEED = 15;
    public static final int BULLET_SIZE = 45;

    // ==================== ENEMY ====================
    public static final int ENEMY_SIZE = 80;
    public static final int ENEMY_MAX = 6;
    public static final float ENEMY_BASE_HEALTH = 30.0f;
    public static final int ENEMY_BASE_DAMAGE = 10;
    public static final int ENEMY_SCORE_VALUE = 50;
    public static final float ENEMY_BASE_SPEED = 2.0f;
    public static final float ENEMY_BULLET_SPEED = 15.0f;
    public static final float ENEMY_ZIGZAG_SPEED = 0.05f;
    public static final float ENEMY_ZIGZAG_AMPLITUDE = 2.5f;
    public static final float ENEMY_MIN_Y = 50;
    public static final float ENEMY_MAX_Y_PERCENT = 0.5f; // 50% of screen height
    public static final float ENEMY_BOUNDARY_MARGIN = 40;
    public static final int ENEMY_DIRECTION_CHANGE_DELAY = 60; // frames
    public static final int ENEMY_STATE_DURATION = 120; // frames
    
    // Enemy Shooting Range
    public static final float ENEMY_SHOOT_HEIGHT_RANGE = 6000; // Always shoot if within this Y distance
    public static final float ENEMY_SHOOT_WIDTH_MULTIPLIER = 0.5f; // Shoot if within 50% of screen width
    
    // Enemy Fire Frequency (milliseconds between shots)
    public static final int ENEMY_FIRE_COOLDOWN_EASY = 5000; // 5 seconds
    public static final int ENEMY_FIRE_COOLDOWN_NORMAL = 2500; // 2.5 seconds
    public static final int ENEMY_FIRE_COOLDOWN_HARD = 1500; // 1.5 seconds

    // ==================== ROCK ====================
    public static final int ROCK_SIZE = 60;
    public static final int ROCK_SPEED = 5;

    // ==================== DIFFICULTY PROGRESSION ====================
    public static final int LEVEL_INTERVAL_SECONDS = 30; // Time between level increases
    public static final float LEVEL_STAT_INCREASE = 0.05f; // 5% increase per level
    public static final float LEVEL_SPEED_CAP = 1.5f; // Maximum speed multiplier from levels
    public static final int LEVEL_ROCK_SPEED_INCREASE = 2; // 2% per level
    public static final int DIFFICULTY_UPGRADE_LEVEL_1 = 10; // EASY -> NORMAL
    public static final int DIFFICULTY_UPGRADE_LEVEL_2 = 20; // NORMAL -> HARD

    // ==================== PICKUPS & DROPS ====================
    public static final float HEART_DROP_CHANCE = 0.3f; // 30%
    public static final float SHIELD_DROP_CHANCE = 0.2f; // 20%
    public static final int PICKUP_SIZE = 32;
    public static final int PICKUP_FALL_SPEED = 3;

    // ==================== POWER-UPS ====================
    public static final int POWERUP_DURATION_MS = 8000; // 8 seconds
    public static final int POWERUP_SIZE = 35;
    public static final int POWERUP_FALL_SPEED = 10;
    
    // ==================== SCORE & COMBO ====================
    public static final int BASE_ENEMY_SCORE = 25;
    public static final int COMBO_TIMEOUT_MS = 2000;
    public static final int COMBO_MULTIPLIER_STEP = 5; // Every 5 combo increases multiplier
    public static final int STREAK_TIMEOUT_MS = 3000;
    
    // ==================== VISUAL EFFECTS ====================
    public static final int STAR_COUNT = 200;
    public static final int BACKGROUND_SCROLL_SPEED = 2;
    public static final float SCREEN_SHAKE_INTENSITY = 0.2f;
    public static final int SCREEN_SHAKE_DURATION_MS = 150;
    public static final int DAMAGE_INDICATOR_DURATION_MS = 200;
    public static final int STREAK_MESSAGE_DURATION_MS = 3000;
    public static final int DIFFICULTY_MESSAGE_DURATION_MS = 3000;
    
    // ==================== UI & MENU ====================
    public static final int LOADING_DURATION_MS = 2000;
    public static final int FONT_SIZE_TITLE = 48;
    public static final int FONT_SIZE_LARGE = 24;
    public static final int FONT_SIZE_MEDIUM = 18;
    public static final int FONT_SIZE_SMALL = 14;
    public static final int FONT_SIZE_DIGITAL = 28;
    
    // ==================== AUDIO ====================
    public static final float MUSIC_VOLUME = 0.6f;
    public static final float SFX_VOLUME = 0.9f;
    public static final float ENGINE_HUM_VOLUME = 0.3f;
    
    // ==================== COLORS ====================
    // Background
    public static final int BG_COLOR_R = 5;
    public static final int BG_COLOR_G = 5;
    public static final int BG_COLOR_B = 20;
    
    // Player
    public static final int PLAYER_COLOR_R = 0;
    public static final int PLAYER_COLOR_G = 200;
    public static final int PLAYER_COLOR_B = 255;
    
    // Enemy
    public static final int ENEMY_COLOR_R = 255;
    public static final int ENEMY_COLOR_G = 100;
    public static final int ENEMY_COLOR_B = 100;
    
    // ==================== DEBUG ====================
    public static final boolean DEBUG_MODE = false;

    private GameConfig() {
        // Prevent instantiation
    }
}
