package com.spaceshooterx.core;

import java.awt.*;

public enum Difficulty {
    EASY("Easy", 0.006, 0.007, 5, 0.8, GameConfig.ENEMY_FIRE_COOLDOWN_EASY, 0.7, 0.6, 80, 30),
    NORMAL("Normal", 0.009, 0.010, 7, 1.0, GameConfig.ENEMY_FIRE_COOLDOWN_NORMAL, 1.0, 1.0, 100, 40),
    HARD("Hard", 0.013, 0.014, 10, 1.3, GameConfig.ENEMY_FIRE_COOLDOWN_HARD, 1.4, 1.4, 150, 50);

    private final String label;
    private final double enemySpawnChance;
    private final double rockSpawnChance;
    private final int maxEnemies;
    private final double speedMultiplier;
    private final int enemyFireCooldownMs;
    private final double enemyHealthMultiplier;
    private final double enemyDamageMultiplier;
    private final int baseScoreMultiplier;
    private final int powerUpDurationReduction;

    Difficulty(String label, double enemySpawnChance, double rockSpawnChance, int maxEnemies,
               double speedMultiplier, int enemyFireCooldownMs, double enemyHealthMultiplier,
               double enemyDamageMultiplier, int baseScoreMultiplier, int powerUpDurationReduction) {
        this.label = label;
        this.enemySpawnChance = enemySpawnChance;
        this.rockSpawnChance = rockSpawnChance;
        this.maxEnemies = maxEnemies;
        this.speedMultiplier = speedMultiplier;
        this.enemyFireCooldownMs = enemyFireCooldownMs;
        this.enemyHealthMultiplier = enemyHealthMultiplier;
        this.enemyDamageMultiplier = enemyDamageMultiplier;
        this.baseScoreMultiplier = baseScoreMultiplier;
        this.powerUpDurationReduction = powerUpDurationReduction;
    }

    public String getLabel() {
        return label;
    }

    public double getEnemySpawnChance() {
        return enemySpawnChance;
    }

    public double getRockSpawnChance() {
        return rockSpawnChance;
    }

    public int getMaxEnemies() {
        return maxEnemies;
    }

    public double getSpeedMultiplier() {
        return speedMultiplier;
    }

    public int getEnemyFireCooldownMs() {
        return enemyFireCooldownMs;
    }

    public double getEnemyHealthMultiplier() {
        return enemyHealthMultiplier;
    }

    public double getEnemyDamageMultiplier() {
        return enemyDamageMultiplier;
    }

    public int getBaseScoreMultiplier() {
        return baseScoreMultiplier;
    }

    public int getPowerUpDurationReduction() {
        return powerUpDurationReduction;
    }

    public Difficulty next() {
        Difficulty[] values = values();
        int next = (ordinal() + 1) % values.length;
        return values[next];
    }

    public Difficulty previous() {
        Difficulty[] values = values();
        int prev = (ordinal() - 1 + values.length) % values.length;
        return values[prev];
    }

    public String getDescription() {
        switch (this) {
            case EASY:
                return "Perfect for learning. Fewer enemies, slower attacks, less damage.";
            case NORMAL:
                return "Balanced challenge for most players. Standard enemy behavior.";
            case HARD:
                return "Increased difficulty with faster enemies and more frequent attacks.";
            default:
                return "";
        }
    }

    public String getDetailedStats() {
        return String.format(
            "Enemy Spawn: %.1f%%\n" +
            "Max Enemies: %d\n" +
            "Enemy Speed: %.1fx\n" +
            "Enemy Health: %.1fx\n" +
            "Enemy Damage: %.1fx\n" +
            "Fire Rate: %dms",
            enemySpawnChance * 100,
            maxEnemies,
            speedMultiplier,
            enemyHealthMultiplier,
            enemyDamageMultiplier,
            enemyFireCooldownMs
        );
    }

    public Color getColor() {
        switch (this) {
            case EASY:
                return new Color(0, 200, 120); // Green
            case NORMAL:
                return new Color(0, 180, 255); // Blue
            case HARD:
                return new Color(255, 140, 40); // Orange
            default:
                return Color.WHITE;
        }
    }

    public Color getGlowColor() {
        switch (this) {
            case EASY:
                return new Color(0, 255, 150, 50);
            case NORMAL:
                return new Color(0, 220, 255, 50);
            case HARD:
                return new Color(255, 180, 60, 50);
            default:
                return new Color(255, 255, 255, 30);
        }
    }

    public String getIcon() {
        switch (this) {
            case EASY:
                return "★";
            case NORMAL:
                return "★★";
            case HARD:
                return "★★★";
            default:
                return "?";
        }
    }

    public int getRecommendedPlayerLevel() {
        switch (this) {
            case EASY:
                return 1;
            case NORMAL:
                return 10;
            case HARD:
                return 25;
            default:
                return 0;
        }
    }

    public double getPlayerDamageMultiplier() {
        return 1.0; // Player damage remains same across difficulties
    }

    public double getPowerUpSpawnChance() {
        switch (this) {
            case EASY:
                return 0.25;
            case NORMAL:
                return 0.20;
            case HARD:
                return 0.15;
            default:
                return 0.20;
        }
    }

    public boolean allowsContinue() {
        return true; // All 3 levels allow continues
    }

    public int getContinueCost() {
        switch (this) {
            case EASY:
                return 0;
            case NORMAL:
                return 1000;
            case HARD:
                return 2500;
            default:
                return 1000;
        }
    }

    public static Difficulty fromLabel(String label) {
        for (Difficulty diff : values()) {
            if (diff.getLabel().equalsIgnoreCase(label)) {
                return diff;
            }
        }
        return NORMAL;
    }

    public static Difficulty fromIndex(int index) {
        Difficulty[] values = values();
        if (index >= 0 && index < values.length) {
            return values[index];
        }
        return NORMAL;
    }

    // Dynamic difficulty scaling based on player performance
    public Difficulty getDynamicVariant(double playerPerformance) {
        // playerPerformance: 0.0 (terrible) to 1.0 (perfect)
        if (playerPerformance > 0.8) {
            // Player doing great, increase difficulty
            Difficulty next = this.next();
            if (next != this) {
                return next;
            }
        } else if (playerPerformance < 0.3) {
            // Player struggling, decrease difficulty
            Difficulty prev = this.previous();
            if (prev != this) {
                return prev;
            }
        }
        return this;
    }
}