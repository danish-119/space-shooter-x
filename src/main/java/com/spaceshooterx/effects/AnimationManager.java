package com.spaceshooterx.effects;

import java.awt.*;
import java.awt.geom.*;

public final class AnimationManager {
    private static final AnimationManager INSTANCE = new AnimationManager();
    
    // Screen shake
    private float screenShakeIntensity = 0;
    private long screenShakeEndTime = 0;
    
    // Screen flash
    private Color screenFlashColor = null;
    private long screenFlashEndTime = 0;
    private float screenFlashAlpha = 0;
    
    // Camera
    private float cameraOffsetX = 0;
    private float cameraOffsetY = 0;
    
    private AnimationManager() {}
    
    public static AnimationManager getInstance() {
        return INSTANCE;
    }
    
    public void screenShake(float intensity, long durationMs) {
        this.screenShakeIntensity = intensity;
        this.screenShakeEndTime = System.currentTimeMillis() + durationMs;
    }
    
    public void screenFlash(Color color, float alpha, long durationMs) {
        this.screenFlashColor = color;
        this.screenFlashAlpha = alpha;
        this.screenFlashEndTime = System.currentTimeMillis() + durationMs;
    }
    
    public void applyCameraTransform(Graphics2D g2d, int screenWidth, int screenHeight) {
        long now = System.currentTimeMillis();
        
        // Update screen shake
        if (now < screenShakeEndTime) {
            cameraOffsetX = (float)(Math.random() - 0.5) * screenShakeIntensity * 20;
            cameraOffsetY = (float)(Math.random() - 0.5) * screenShakeIntensity * 20;
        } else {
            cameraOffsetX = 0;
            cameraOffsetY = 0;
        }
        
        // Apply camera transform
        g2d.translate(cameraOffsetX, cameraOffsetY);
    }
    
    public void applyPostProcessing(Graphics2D g2d, int screenWidth, int screenHeight) {
        // Vignette effect
        RadialGradientPaint vignette = new RadialGradientPaint(
            screenWidth / 2f, screenHeight / 2f,
            Math.max(screenWidth, screenHeight) * 0.8f,
            new float[]{0.0f, 1.0f},
            new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 100)}
        );
        g2d.setPaint(vignette);
        g2d.fillRect(0, 0, screenWidth, screenHeight);
    }
    
    public void drawOverlay(Graphics2D g2d, int screenWidth, int screenHeight) {
        long now = System.currentTimeMillis();
        
        // Screen flash
        if (now < screenFlashEndTime && screenFlashColor != null) {
            float progress = (float)(screenFlashEndTime - now) / (float)(screenFlashEndTime - (screenFlashEndTime - 1000));
            float alpha = screenFlashAlpha * progress;
            g2d.setColor(new Color(
                screenFlashColor.getRed(),
                screenFlashColor.getGreen(),
                screenFlashColor.getBlue(),
                (int)(alpha * 255)
            ));
            g2d.fillRect(0, 0, screenWidth, screenHeight);
        }
    }
}
