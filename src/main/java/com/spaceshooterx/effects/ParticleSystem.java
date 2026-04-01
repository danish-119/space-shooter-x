package com.spaceshooterx.effects;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.*;
import java.util.List;

public final class ParticleSystem {
    private static final ParticleSystem INSTANCE = new ParticleSystem();
    
    private final List<Particle> activeParticles = new ArrayList<>();
    private static final int MAX_PARTICLES = 3000;
    
    private enum ParticleType {
        CIRCLE, SQUARE, TRIANGLE, STAR, TRAIL, SPARK, GLOW
    }
    
    private static class Particle {
        float x, y, dx, dy;
        float startX, startY; // For trail calculations
        float size, startSize, endSize;
        float alpha, alphaDelta;
        Color color;
        float life, maxLife;
        boolean active;
        ParticleType type;
        float rotation, rotationSpeed;
        float gravity = 0.2f;
        float drag = 0.98f;
        Color endColor;
        float velocityVariance;
    }
    
    private ParticleSystem() {}
    
    public static ParticleSystem getInstance() {
        return INSTANCE;
    }
    
    public void update(float deltaTime) {
        float frameDelta = deltaTime * 60f; // Normalize to 60fps
        activeParticles.removeIf(p -> {
            if (!p.active) return true;
            
            // Apply physics
            p.x += p.dx * frameDelta;
            p.y += p.dy * frameDelta;
            p.dy += p.gravity * frameDelta;
            
            // Apply drag
            p.dx *= Math.pow(p.drag, frameDelta);
            p.dy *= Math.pow(p.drag, frameDelta);
            
            // Update rotation
            p.rotation += p.rotationSpeed * frameDelta;
            
            // Update size interpolation
            float lifeRatio = 1 - (p.life / p.maxLife);
            p.size = p.startSize + (p.endSize - p.startSize) * lifeRatio;
            
            // Update color interpolation
            if (p.endColor != null) {
                float r = p.color.getRed() + (p.endColor.getRed() - p.color.getRed()) * lifeRatio;
                float g = p.color.getGreen() + (p.endColor.getGreen() - p.color.getGreen()) * lifeRatio;
                float b = p.color.getBlue() + (p.endColor.getBlue() - p.color.getBlue()) * lifeRatio;
                p.color = new Color((int)r, (int)g, (int)b);
            }
            
            // Update alpha
            p.alpha -= p.alphaDelta * frameDelta;
            p.life -= frameDelta;
            
            // Fade out at end of life
            if (p.life < p.maxLife * 0.3f) {
                p.alphaDelta = Math.max(p.alphaDelta, 0.2f);
            }
            
            return p.life <= 0 || p.alpha <= 0.02f || p.size <= 0.5f;
        });
    }
    
    public void renderImmediate(Graphics2D g2d) {
        Composite old = g2d.getComposite();
        RenderingHints oldHints = g2d.getRenderingHints();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        for (Particle p : activeParticles) {
            if (!p.active || p.alpha <= 0.02f || p.size <= 0.1f) continue;
            
            float effectiveAlpha = Math.max(0, Math.min(1, p.alpha));
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, effectiveAlpha));
            g2d.setColor(p.color);
            
            AffineTransform oldTransform = null;
            if (p.rotation != 0) {
                oldTransform = g2d.getTransform();
                g2d.rotate(Math.toRadians(p.rotation), p.x, p.y);
            }
            
            switch (p.type) {
                case CIRCLE:
                    g2d.fillOval((int)(p.x - p.size/2), (int)(p.y - p.size/2), 
                                 (int)p.size, (int)p.size);
                    // Add highlight for circles
                    if (p.size > 3) {
                        g2d.setColor(new Color(255, 255, 255, (int)(effectiveAlpha * 100)));
                        g2d.fillOval((int)(p.x - p.size/4), (int)(p.y - p.size/4), 
                                    (int)(p.size/2), (int)(p.size/2));
                    }
                    break;
                    
                case SQUARE:
                    g2d.fillRect((int)(p.x - p.size/2), (int)(p.y - p.size/2), 
                                (int)p.size, (int)p.size);
                    break;
                    
                case TRIANGLE:
                    int[] xPoints = {(int)p.x, (int)(p.x - p.size/2), (int)(p.x + p.size/2)};
                    int[] yPoints = {(int)(p.y - p.size/2), (int)(p.y + p.size/2), (int)(p.y + p.size/2)};
                    g2d.fillPolygon(xPoints, yPoints, 3);
                    break;
                    
                case STAR:
                    drawStar(g2d, (int)p.x, (int)p.y, (int)p.size, 5);
                    break;
                    
                case TRAIL:
                    // Draw line from start position to current position
                    g2d.setStroke(new BasicStroke(p.size, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2d.drawLine((int)p.startX, (int)p.startY, (int)p.x, (int)p.y);
                    break;
                    
                case SPARK:
                    // Spark particles are thin lines
                    g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    double angle = Math.atan2(p.dy, p.dx);
                    int endX = (int)(p.x + Math.cos(angle) * p.size * 2);
                    int endY = (int)(p.y + Math.sin(angle) * p.size * 2);
                    g2d.drawLine((int)p.x, (int)p.y, endX, endY);
                    break;
                    
                case GLOW:
                    // Glow particles with gradient
                    RadialGradientPaint gradient = new RadialGradientPaint(
                        p.x, p.y, p.size,
                        new float[]{0.0f, 1.0f},
                        new Color[]{p.color, new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), 0)}
                    );
                    g2d.setPaint(gradient);
                    g2d.fillOval((int)(p.x - p.size), (int)(p.y - p.size), 
                                (int)(p.size * 2), (int)(p.size * 2));
                    break;
            }
            
            if (oldTransform != null) {
                g2d.setTransform(oldTransform);
            }
        }
        
        g2d.setComposite(old);
        g2d.setRenderingHints(oldHints);
    }
    
    private void drawStar(Graphics2D g2d, int x, int y, int radius, int points) {
        double outerRadius = radius;
        double innerRadius = radius * 0.4;
        
        int[] xPoints = new int[points * 2];
        int[] yPoints = new int[points * 2];
        
        for (int i = 0; i < points * 2; i++) {
            double radius2 = (i % 2 == 0) ? outerRadius : innerRadius;
            double angle = Math.PI * i / points;
            xPoints[i] = (int)(x + Math.cos(angle) * radius2);
            yPoints[i] = (int)(y + Math.sin(angle) * radius2);
        }
        
        g2d.fillPolygon(xPoints, yPoints, points * 2);
    }
    
    public void createExplosion(float x, float y, int count, Color color) {
        if (activeParticles.size() + count > MAX_PARTICLES) return;
        
        Random rand = new Random();
        for (int i = 0; i < count; i++) {
            Particle p = createBaseParticle();
            p.x = x;
            p.y = y;
            
            // Random direction with some variations
            double angle = rand.nextDouble() * Math.PI * 2;
            float speed = 1 + rand.nextFloat() * 6;
            p.dx = (float)Math.cos(angle) * speed;
            p.dy = (float)Math.sin(angle) * speed;
            
            // Randomize particle properties
            p.startSize = 2 + rand.nextFloat() * 6;
            p.endSize = p.startSize * 0.3f;
            p.alpha = 0.9f + rand.nextFloat() * 0.1f;
            p.alphaDelta = 0.08f + rand.nextFloat() * 0.06f;
            p.color = color;
            p.endColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 50);
            p.life = p.maxLife = 20 + rand.nextFloat() * 25;
            p.type = rand.nextBoolean() ? ParticleType.CIRCLE : ParticleType.SQUARE;
            p.rotationSpeed = (rand.nextFloat() - 0.5f) * 10;
            p.gravity = 0.05f + rand.nextFloat() * 0.15f;
            p.drag = 0.96f + rand.nextFloat() * 0.03f;
            
            activeParticles.add(p);
        }
    }
    
    public void createBulletImpact(float x, float y, Color color) {
        if (activeParticles.size() + 20 > MAX_PARTICLES) return;
        
        Random rand = new Random();
        // Main impact
        createExplosion(x, y, 12, color);
        
        // Add sparks
        for (int i = 0; i < 8; i++) {
            Particle p = createBaseParticle();
            p.x = x;
            p.y = y;
            p.type = ParticleType.SPARK;
            
            double angle = rand.nextDouble() * Math.PI * 2;
            float speed = 4 + rand.nextFloat() * 8;
            p.dx = (float)Math.cos(angle) * speed;
            p.dy = (float)Math.sin(angle) * speed;
            
            p.startSize = p.endSize = 1 + rand.nextFloat() * 2;
            p.alpha = 1.0f;
            p.alphaDelta = 0.15f;
            p.color = new Color(255, 255, 200);
            p.life = p.maxLife = 10 + rand.nextFloat() * 10;
            p.gravity = 0.3f;
            
            activeParticles.add(p);
        }
    }
    
    public void createMuzzleFlash(float x, float y, float dx, float dy, Color color) {
        if (activeParticles.size() + 15 > MAX_PARTICLES) return;
        
        Random rand = new Random();
        
        // Flash core
        Particle flash = createBaseParticle();
        flash.x = x;
        flash.y = y;
        flash.type = ParticleType.GLOW;
        flash.startSize = 8 + rand.nextFloat() * 4;
        flash.endSize = 0;
        flash.alpha = 0.8f;
        flash.alphaDelta = 0.25f;
        flash.color = color.brighter();
        flash.life = flash.maxLife = 5;
        activeParticles.add(flash);
        
        // Particles
        for (int i = 0; i < 10; i++) {
            Particle p = createBaseParticle();
            p.x = x;
            p.y = y;
            p.type = ParticleType.CIRCLE;
            
            // Direction based on bullet direction with some spread
            double angle = Math.atan2(dy, dx) + (rand.nextFloat() - 0.5f) * 0.5;
            float speed = 3 + rand.nextFloat() * 4;
            p.dx = (float)Math.cos(angle) * speed;
            p.dy = (float)Math.sin(angle) * speed;
            
            p.startSize = 1 + rand.nextFloat() * 3;
            p.endSize = 0;
            p.alpha = 0.9f;
            p.alphaDelta = 0.12f;
            p.color = color;
            p.life = p.maxLife = 8 + rand.nextFloat() * 5;
            p.gravity = 0.1f;
            
            activeParticles.add(p);
        }
    }
    
    public void createTrail(float x, float y, float dx, float dy, Color color) {
        if (activeParticles.size() + 2 > MAX_PARTICLES) return;
        
        Random rand = new Random();
        
        for (int i = 0; i < 2; i++) {
            Particle p = createBaseParticle();
            p.type = ParticleType.TRAIL;
            p.startX = x - dx * (i + 1);
            p.startY = y - dy * (i + 1);
            p.x = x;
            p.y = y;
            p.dx = dx * 0.1f;
            p.dy = dy * 0.1f;
            p.size = 1 + rand.nextFloat() * 2;
            p.alpha = 0.4f;
            p.alphaDelta = 0.2f;
            p.color = color;
            p.life = p.maxLife = 6;
            activeParticles.add(p);
        }
    }
    
    public void createHealEffect(float x, float y, Color color) {
        if (activeParticles.size() + 25 > MAX_PARTICLES) return;
        
        Random rand = new Random();
        Color brightColor = color.brighter();
        
        for (int i = 0; i < 25; i++) {
            Particle p = createBaseParticle();
            p.x = x + (rand.nextFloat() - 0.5f) * 20;
            p.y = y + (rand.nextFloat() - 0.5f) * 20;
            
            // Rising particles
            double angle = Math.PI / 2 + (rand.nextFloat() - 0.5f) * 0.5;
            float speed = 0.5f + rand.nextFloat() * 2;
            p.dx = (float)Math.cos(angle) * speed * 0.3f;
            p.dy = (float)Math.sin(angle) * speed;
            
            p.startSize = 1.5f + rand.nextFloat() * 3;
            p.endSize = 0;
            p.alpha = 0.7f;
            p.alphaDelta = 0.04f;
            p.color = brightColor;
            p.life = p.maxLife = 40 + rand.nextFloat() * 20;
            p.type = rand.nextBoolean() ? ParticleType.CIRCLE : ParticleType.STAR;
            p.gravity = -0.02f; // Negative gravity makes them float up
            p.drag = 0.99f;
            p.rotationSpeed = (rand.nextFloat() - 0.5f) * 3;
            
            activeParticles.add(p);
        }
    }
    
    public void createShieldEffect(float x, float y, float radius, Color color) {
        if (activeParticles.size() + 16 > MAX_PARTICLES) return;
        
        Random rand = new Random();
        for (int i = 0; i < 16; i++) {
            Particle p = createBaseParticle();
            double angle = (i / 16.0) * Math.PI * 2;
            p.x = x + (float)Math.cos(angle) * radius;
            p.y = y + (float)Math.sin(angle) * radius;
            
            p.dx = (float)Math.cos(angle + Math.PI/2) * 0.5f;
            p.dy = (float)Math.sin(angle + Math.PI/2) * 0.5f;
            
            p.startSize = 3 + rand.nextFloat() * 2;
            p.endSize = 0;
            p.alpha = 0.6f;
            p.alphaDelta = 0.05f;
            p.color = color;
            p.life = p.maxLife = 30;
            p.type = ParticleType.CIRCLE;
            
            activeParticles.add(p);
        }
    }
    
    public void createPowerUpEffect(float x, float y, Color color) {
        if (activeParticles.size() + 30 > MAX_PARTICLES) return;
        
        Random rand = new Random();
        
        // Central glow
        Particle glow = createBaseParticle();
        glow.x = x;
        glow.y = y;
        glow.type = ParticleType.GLOW;
        glow.startSize = 15;
        glow.endSize = 0;
        glow.alpha = 0.7f;
        glow.alphaDelta = 0.1f;
        glow.color = color;
        glow.life = glow.maxLife = 20;
        activeParticles.add(glow);
        
        // Orbiting particles
        for (int i = 0; i < 20; i++) {
            Particle p = createBaseParticle();
            double angle = rand.nextDouble() * Math.PI * 2;
            float distance = 10 + rand.nextFloat() * 20;
            p.x = x + (float)Math.cos(angle) * distance;
            p.y = y + (float)Math.sin(angle) * distance;
            
            // Orbit velocity
            p.dx = (float)Math.cos(angle + Math.PI/2) * 2;
            p.dy = (float)Math.sin(angle + Math.PI/2) * 2;
            
            p.startSize = 1.5f + rand.nextFloat() * 2;
            p.endSize = 0;
            p.alpha = 0.8f;
            p.alphaDelta = 0.03f;
            p.color = color.brighter();
            p.life = p.maxLife = 40 + rand.nextFloat() * 20;
            p.type = rand.nextBoolean() ? ParticleType.CIRCLE : ParticleType.STAR;
            p.gravity = 0;
            
            activeParticles.add(p);
        }
    }
    
    private Particle createBaseParticle() {
        Particle p = new Particle();
        p.active = true;
        p.rotation = 0;
        p.rotationSpeed = 0;
        p.gravity = 0.2f;
        p.drag = 0.98f;
        p.endColor = null;
        p.type = ParticleType.CIRCLE;
        return p;
    }
    
    public void clear() {
        activeParticles.clear();
    }
    
    public int getActiveParticleCount() {
        return activeParticles.size();
    }
    
    public void createLargeExplosion(float x, float y, Color primary, Color secondary) {
        createExplosion(x, y, 40, primary);
        createExplosion(x, y, 20, secondary);
        createExplosion(x, y + 10, 15, primary.brighter());
    }
    
    public void createHitEffect(float x, float y, float damage, Color color) {
        int count = (int)(5 + damage / 2);
        createExplosion(x, y, Math.min(count, 15), color);
    }
    
    public void createUpgradeEffect(float x, float y, Color color) {
        createPowerUpEffect(x, y, color);
    }
    
    public void createDashEffect(float x, float y, float dx, float dy) {
        if (activeParticles.size() + 5 > MAX_PARTICLES) return;
        
        Random rand = new Random();
        for (int i = 0; i < 5; i++) {
            Particle p = createBaseParticle();
            p.x = x + (rand.nextFloat() - 0.5f) * 10;
            p.y = y + (rand.nextFloat() - 0.5f) * 10;
            p.type = ParticleType.TRAIL;
            
            p.dx = -dx * 0.3f;
            p.dy = -dy * 0.3f;
            p.startX = p.x - dx * 3;
            p.startY = p.y - dy * 3;
            
            p.size = 2 + rand.nextFloat() * 2;
            p.alpha = 0.6f;
            p.alphaDelta = 0.1f;
            p.color = new Color(100, 150, 255);
            p.life = p.maxLife = 15;
            p.gravity = 0;
            
            activeParticles.add(p);
        }
    }
    
    public void createEngineTrail(float x, float y, Color color) {
        if (activeParticles.size() + 3 > MAX_PARTICLES) return;
        
        Random rand = new Random();
        for (int i = 0; i < 3; i++) {
            Particle p = createBaseParticle();
            p.x = x + (rand.nextFloat() - 0.5f) * 6;
            p.y = y;
            p.type = ParticleType.GLOW;
            
            p.dx = (rand.nextFloat() - 0.5f) * 0.8f;
            p.dy = 1.0f + rand.nextFloat() * 0.5f;
            
            p.startSize = 3 + rand.nextFloat() * 3;
            p.endSize = 0.5f;
            p.size = p.startSize;
            p.alpha = 0.7f;
            p.alphaDelta = 0.03f;
            p.color = color;
            p.endColor = new Color(255, 50, 0);
            p.life = p.maxLife = 20 + rand.nextFloat() * 15;
            p.gravity = -0.1f;
            p.drag = 0.95f;
            
            activeParticles.add(p);
        }
    }
}