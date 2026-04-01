package com.spaceshooterx.audio;

import javax.sound.sampled.*;
import java.util.*;
import java.util.concurrent.*;

public final class SoundManager {
    // Audio Configuration
    private static final int SAMPLE_RATE = 44100;
    private static final int MAX_CONCURRENT_SOUNDS = 32;
    private static final int SOUND_POOL_SIZE = 64;
    private static final float DEFAULT_MASTER_VOLUME = 0.8f;
    private static final float DEFAULT_MUSIC_VOLUME = 0.6f;
    private static final float DEFAULT_SFX_VOLUME = 0.9f;
    
    // Audio Engine
    private static final ExecutorService audioThreadPool;
    private static final ScheduledExecutorService scheduler;
    private static final Map<SoundType, List<Clip>> soundPools = new HashMap<>();
    private static final Map<Integer, Clip> activeTones = new ConcurrentHashMap<>();
    
    // Volume Controls
    private static float masterVolume = DEFAULT_MASTER_VOLUME;
    private static float musicVolume = DEFAULT_MUSIC_VOLUME;
    private static float sfxVolume = DEFAULT_SFX_VOLUME;
    private static boolean muted = false;
    private static boolean musicEnabled = true;
    private static boolean sfxEnabled = true;
    
    // Sound Types (all procedurally generated)
    public enum SoundType {
        // Player Actions
        PLAYER_SHOOT,
        PLAYER_DOUBLE_SHOOT,
        PLAYER_TRIPLE_SHOOT,
        PLAYER_DEATH,
        PLAYER_HIT,
        PLAYER_POWERUP,
        
        // Weapons
        LASER_BASIC,
        LASER_ENHANCED,
        PLASMA,
        ROCKET,
        RAILGUN,
        BEAM_LASER,
        
        // Enemies
        ENEMY_SHOOT_SMALL,
        ENEMY_SHOOT_MEDIUM,
        ENEMY_SHOOT_LARGE,
        ENEMY_EXPLOSION_SMALL,
        ENEMY_EXPLOSION_MEDIUM,
        ENEMY_EXPLOSION_LARGE,
        ENEMY_SPAWN,
        ENEMY_HIT,
        
        // UI
        UI_CLICK,
        UI_HOVER,
        UI_SELECT,
        UI_BACK,
        UI_ERROR,
        
        // Game Events
        LEVEL_UP,
        COMBO_BREAK,
        COMBO_MULTIPLIER,
        POWERUP_SPAWN,
        SHIELD_HIT,
        SHIELD_ACTIVATE,
        
        // Ambient
        ENGINE_HUM,
        SPACE_AMBIENT,
        ALARM,
        WARNING
    }
    
    // Waveform Types
    private enum Waveform {
        SINE,
        SQUARE,
        SAWTOOTH,
        TRIANGLE,
        NOISE,
        PULSE
    }
    
    // Sound Parameters
    private static class SoundParams {
        int baseFreq;
        int duration;
        float volume;
        Waveform waveform;
        float attack;
        float decay;
        float sustain;
        float release;
        int harmonics;
        float detune;
        float vibratoDepth;
        float vibratoRate;
        int sweepStart;
        int sweepEnd;
        boolean hasSweep;
        
        SoundParams(int freq, int dur, float vol, Waveform wave) {
            this.baseFreq = freq;
            this.duration = dur;
            this.volume = vol;
            this.waveform = wave;
            this.attack = 0.01f;
            this.decay = 0.1f;
            this.sustain = 0.7f;
            this.release = 0.2f;
            this.harmonics = 1;
            this.detune = 0.0f;
            this.vibratoDepth = 0.0f;
            this.vibratoRate = 0.0f;
            this.sweepStart = freq;
            this.sweepEnd = freq;
            this.hasSweep = false;
        }

        SoundParams setADSR(float a, float d, float s, float r) {
            this.attack = a;
            this.decay = d;
            this.sustain = s;
            this.release = r;
            return this;
        }

        SoundParams setHarmonics(int h) {
            this.harmonics = h;
            return this;
        }

        SoundParams setDetune(float d) {
            this.detune = d;
            return this;
        }

        SoundParams setVibrato(float depth, float rate) {
            this.vibratoDepth = depth;
            this.vibratoRate = rate;
            return this;
        }

        SoundParams setFrequencySweep(int start, int end) {
            this.sweepStart = start;
            this.sweepEnd = end;
            this.hasSweep = true;
            return this;
        }
    }
    
    static {
        // Initialize thread pools
        audioThreadPool = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "audio-procedural");
            t.setPriority(Thread.MAX_PRIORITY);
            return t;
        });
        
        scheduler = Executors.newScheduledThreadPool(2);
        
        // Initialize sound pools
        initializeSoundPools();
    }
    
    private static void initializeSoundPools() {
        for (SoundType type : SoundType.values()) {
            List<Clip> pool = new ArrayList<>();
            for (int i = 0; i < 3; i++) { // Create 3 instances of each sound type
                Clip clip = generateSound(type);
                if (clip != null) {
                    pool.add(clip);
                }
            }
            soundPools.put(type, pool);
        }
        
        // Start background music thread
        if (musicEnabled) {
            startBackgroundMusic();
        }
    }
    
    private static Clip generateSound(SoundType type) {
        SoundParams params = getSoundParams(type);
        return createProceduralClip(params);
    }
    
    private static SoundParams getSoundParams(SoundType type) {
        Random rand = new Random();
        
        switch (type) {
            // Player Sounds
            case PLAYER_SHOOT:
                return new SoundParams(880 + rand.nextInt(40), 60, 0.8f, Waveform.SQUARE)
                    .setADSR(0.001f, 0.05f, 0.0f, 0.01f)
                    .setHarmonics(3)
                    .setDetune(0.02f);
                    
            case PLAYER_DOUBLE_SHOOT:
                return new SoundParams(660 + rand.nextInt(30), 70, 0.9f, Waveform.SQUARE)
                    .setADSR(0.001f, 0.1f, 0.0f, 0.05f)
                    .setHarmonics(2);
                    
            case PLAYER_TRIPLE_SHOOT:
                return new SoundParams(440 + rand.nextInt(20), 80, 1.0f, Waveform.SQUARE)
                    .setADSR(0.001f, 0.15f, 0.0f, 0.1f);
                    
            case PLAYER_DEATH:
                return new SoundParams(220, 800, 1.0f, Waveform.SAWTOOTH)
                    .setADSR(0.1f, 0.3f, 0.5f, 0.5f)
                    .setFrequencySweep(220, 55);
                    
            case PLAYER_HIT:
                return new SoundParams(320 + rand.nextInt(40), 100, 0.9f, Waveform.TRIANGLE)
                    .setADSR(0.001f, 0.2f, 0.0f, 0.1f)
                    .setHarmonics(2);
                    
            case PLAYER_POWERUP:
                return new SoundParams(400, 500, 1.0f, Waveform.SINE)
                    .setADSR(0.1f, 0.2f, 0.5f, 0.2f)
                    .setFrequencySweep(400, 1200)
                    .setVibrato(0.1f, 5.0f);
                    
            // Weapon Sounds
            case LASER_BASIC:
                return new SoundParams(1760 + rand.nextInt(80), 120, 0.7f, Waveform.PULSE)
                    .setADSR(0.001f, 0.2f, 0.3f, 0.1f)
                    .setHarmonics(5);
                    
            case LASER_ENHANCED:
                return new SoundParams(1320 + rand.nextInt(60), 150, 0.8f, Waveform.PULSE)
                    .setADSR(0.001f, 0.3f, 0.4f, 0.2f)
                    .setHarmonics(4)
                    .setDetune(0.05f);
                    
            case PLASMA:
                return new SoundParams(990, 200, 0.9f, Waveform.SAWTOOTH)
                    .setADSR(0.05f, 0.4f, 0.5f, 0.3f)
                    .setFrequencySweep(990, 495);
                    
            case ROCKET:
                return new SoundParams(110, 600, 1.0f, Waveform.NOISE)
                    .setADSR(0.2f, 0.5f, 0.6f, 1.0f)
                    .setFrequencySweep(110, 55);
                    
            case RAILGUN:
                return new SoundParams(55, 300, 1.2f, Waveform.SQUARE)
                    .setADSR(0.001f, 0.1f, 0.0f, 0.5f)
                    .setHarmonics(8);

            case BEAM_LASER:
                return new SoundParams(520, 250, 0.8f, Waveform.SINE)
                    .setADSR(0.01f, 0.2f, 0.6f, 0.2f)
                    .setHarmonics(2)
                    .setVibrato(0.05f, 6.0f);
                    
            // Enemy Sounds
            case ENEMY_SHOOT_SMALL:
                return new SoundParams(1320 + rand.nextInt(100), 80, 0.6f, Waveform.SQUARE)
                    .setADSR(0.001f, 0.1f, 0.0f, 0.05f);
                    
            case ENEMY_SHOOT_MEDIUM:
                return new SoundParams(880 + rand.nextInt(80), 100, 0.7f, Waveform.SAWTOOTH)
                    .setADSR(0.001f, 0.15f, 0.0f, 0.1f);
                    
            case ENEMY_SHOOT_LARGE:
                return new SoundParams(440 + rand.nextInt(40), 150, 0.9f, Waveform.TRIANGLE)
                    .setADSR(0.001f, 0.2f, 0.0f, 0.15f);
                    
            case ENEMY_EXPLOSION_SMALL:
                return createExplosionParams(0.5f);
                    
            case ENEMY_EXPLOSION_MEDIUM:
                return createExplosionParams(0.8f);
                    
            case ENEMY_EXPLOSION_LARGE:
                return createExplosionParams(1.2f);
                
            case ENEMY_SPAWN:
                return new SoundParams(660, 300, 0.7f, Waveform.SINE)
                    .setADSR(0.2f, 0.3f, 0.4f, 0.1f)
                    .setFrequencySweep(1320, 330)
                    .setVibrato(0.2f, 3.0f);
                    
            case ENEMY_HIT:
                return new SoundParams(220 + rand.nextInt(40), 60, 0.8f, Waveform.TRIANGLE)
                    .setADSR(0.001f, 0.05f, 0.0f, 0.02f);
                    
            // UI Sounds
            case UI_CLICK:
                return new SoundParams(800 + rand.nextInt(100), 40, 0.5f, Waveform.SINE)
                    .setADSR(0.001f, 0.05f, 0.0f, 0.02f);
                    
            case UI_HOVER:
                return new SoundParams(1200 + rand.nextInt(200), 30, 0.3f, Waveform.SINE)
                    .setADSR(0.01f, 0.1f, 0.0f, 0.05f);
                    
            case UI_SELECT:
                return new SoundParams(600, 70, 0.7f, Waveform.SINE)
                    .setADSR(0.001f, 0.1f, 0.0f, 0.05f);
                    
            case UI_BACK:
                return new SoundParams(420, 80, 0.6f, Waveform.SINE)
                    .setADSR(0.001f, 0.1f, 0.0f, 0.05f);
                    
            case UI_ERROR:
                return new SoundParams(200, 150, 0.8f, Waveform.SQUARE)
                    .setADSR(0.001f, 0.05f, 0.5f, 0.1f)
                    .setHarmonics(3);
                    
            // Game Events
            case LEVEL_UP:
                return createLevelUpSound();
                
            case COMBO_BREAK:
                return new SoundParams(980, 120, 1.0f, Waveform.SAWTOOTH)
                    .setADSR(0.001f, 0.1f, 0.0f, 0.05f)
                    .setHarmonics(4);
                    
            case COMBO_MULTIPLIER:
                return createComboSound();
                
            case POWERUP_SPAWN:
                return new SoundParams(1200, 120, 1.0f, Waveform.SINE)
                    .setADSR(0.05f, 0.2f, 0.5f, 0.3f)
                    .setFrequencySweep(1200, 600)
                    .setVibrato(0.15f, 8.0f);
                    
            case SHIELD_HIT:
                return new SoundParams(440, 200, 0.9f, Waveform.TRIANGLE)
                    .setADSR(0.001f, 0.3f, 0.5f, 0.4f)
                    .setHarmonics(3);
                    
            case SHIELD_ACTIVATE:
                return new SoundParams(660, 400, 1.0f, Waveform.SINE)
                    .setADSR(0.1f, 0.4f, 0.6f, 0.5f)
                    .setFrequencySweep(1320, 330)
                    .setVibrato(0.1f, 4.0f);
                    
            // Ambient Sounds
            case ENGINE_HUM:
                return new SoundParams(110, 10000, 0.1f, Waveform.SINE)
                    .setADSR(1.0f, 0.5f, 1.0f, 1.0f)
                    .setHarmonics(2)
                    .setVibrato(0.05f, 0.5f);
                    
            case SPACE_AMBIENT:
                return new SoundParams(55, 10000, 0.05f, Waveform.NOISE)
                    .setADSR(2.0f, 1.0f, 1.0f, 2.0f);
                    
            case ALARM:
                return createAlarmSound();
                
            case WARNING:
                return new SoundParams(330, 300, 0.9f, Waveform.SQUARE)
                    .setADSR(0.001f, 0.1f, 0.8f, 0.1f)
                    .setHarmonics(2);
                    
            default:
                return new SoundParams(440, 100, 0.5f, Waveform.SINE);
        }
    }
    
    private static SoundParams createExplosionParams(float size) {
        Random rand = new Random();
        SoundParams params = new SoundParams(
            (int)(80 / size), 
            (int)(200 * size), 
            1.0f * size, 
            Waveform.NOISE
        );
        params.attack = 0.001f;
        params.decay = 0.1f * size;
        params.sustain = 0.3f;
        params.release = 0.6f * size;
        params.harmonics = 3;
        return params;
    }
    
    private static SoundParams createLevelUpSound() {
        SoundParams params = new SoundParams(440, 500, 1.0f, Waveform.SINE);
        params.attack = 0.1f;
        params.decay = 0.2f;
        params.sustain = 0.5f;
        params.release = 0.2f;
        params.harmonics = 5;
        params.detune = 0.1f;
        params.vibratoDepth = 0.2f;
        params.vibratoRate = 6.0f;
        return params.setFrequencySweep(440, 880);
    }
    
    private static SoundParams createComboSound() {
        SoundParams params = new SoundParams(660, 200, 1.2f, Waveform.SAWTOOTH);
        params.attack = 0.001f;
        params.decay = 0.1f;
        params.sustain = 0.0f;
        params.release = 0.1f;
        params.harmonics = 4;
        params.detune = 0.15f;
        return params;
    }
    
    private static SoundParams createAlarmSound() {
        SoundParams params = new SoundParams(880, 2000, 1.0f, Waveform.SQUARE);
        params.attack = 0.05f;
        params.decay = 0.1f;
        params.sustain = 0.9f;
        params.release = 0.05f;
        params.harmonics = 3;
        params.vibratoDepth = 0.3f;
        params.vibratoRate = 10.0f;
        return params;
    }
    
    private static Clip createProceduralClip(SoundParams params) {
        try {
            int numSamples = (int)(SAMPLE_RATE * params.duration / 1000.0);
            int frameSize = 4; // 16-bit stereo
            byte[] buffer = new byte[numSamples * frameSize];
            
            double phase = 0;
            Random rand = new Random();
            
            for (int i = 0; i < numSamples; i++) {
                double time = i / (double)SAMPLE_RATE;
                
                // ADSR envelope
                double env = calculateADSR(time, params);
                
                // Base frequency with vibrato
                double baseFreq = params.baseFreq;
                if (params.hasSweep) {
                    double sweepProgress = time / (params.duration / 1000.0);
                    baseFreq = params.sweepStart + (params.sweepEnd - params.sweepStart) * sweepProgress;
                }
                double freq = baseFreq * (1.0 + params.vibratoDepth * 
                    Math.sin(2 * Math.PI * params.vibratoRate * time));
                
                // Generate waveform with harmonics
                double sample = 0;
                for (int h = 1; h <= params.harmonics; h++) {
                    double harmonicFreq = freq * h * (1.0 + (h - 1) * params.detune);
                    phase += 2 * Math.PI * harmonicFreq / SAMPLE_RATE;
                    
                    double harmonicSample = generateWaveform(phase, params.waveform);
                    sample += harmonicSample / h; // Higher harmonics are quieter
                }
                
                // Apply envelope and volume
                sample *= env * params.volume * masterVolume * sfxVolume;
                
                // Convert to 16-bit
                short sampleValue = (short)(sample * 32767);
                int idx = i * frameSize;
                buffer[idx] = (byte)(sampleValue & 0xFF);
                buffer[idx + 1] = (byte)((sampleValue >> 8) & 0xFF);
                buffer[idx + 2] = buffer[idx];
                buffer[idx + 3] = buffer[idx + 1];
                
                // Add some noise for certain waveforms
                if (params.waveform == Waveform.NOISE) {
                    buffer[i * 2] += (byte)(rand.nextInt(100) - 50);
                }
            }
            
            // Create audio format
            AudioFormat format = new AudioFormat(
                SAMPLE_RATE,
                16,
                2, // Stereo
                true,
                false
            );
            
            // Create clip
            Clip clip = AudioSystem.getClip();
            clip.open(format, buffer, 0, buffer.length);
            
            // Add listener to reset clip when done
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    clip.setFramePosition(0);
                }
            });
            
            return clip;
            
        } catch (Exception e) {
            System.err.println("Error generating procedural sound: " + e.getMessage());
            return null;
        }
    }
    
    private static double calculateADSR(double time, SoundParams params) {
        double totalDuration = params.duration / 1000.0;
        double attackTime = params.attack;
        double decayTime = params.decay;
        double releaseTime = params.release;
        double sustainLevel = params.sustain;
        
        if (time < attackTime) {
            // Attack phase
            return time / attackTime;
        } else if (time < attackTime + decayTime) {
            // Decay phase
            double decayProgress = (time - attackTime) / decayTime;
            return 1.0 - (1.0 - sustainLevel) * decayProgress;
        } else if (time < totalDuration - releaseTime) {
            // Sustain phase
            return sustainLevel;
        } else {
            // Release phase
            double releaseProgress = (time - (totalDuration - releaseTime)) / releaseTime;
            return sustainLevel * (1.0 - releaseProgress);
        }
    }
    
    private static double generateWaveform(double phase, Waveform waveform) {
        switch (waveform) {
            case SINE:
                return Math.sin(phase);
                
            case SQUARE:
                return Math.sin(phase) > 0 ? 0.8 : -0.8;
                
            case SAWTOOTH:
                return 2 * (phase / (2 * Math.PI) - Math.floor(phase / (2 * Math.PI) + 0.5));
                
            case TRIANGLE:
                return 2 * Math.abs(2 * (phase / (2 * Math.PI) - Math.floor(phase / (2 * Math.PI) + 0.5))) - 1;
                
            case PULSE:
                double pulseWidth = 0.25;
                return (phase % (2 * Math.PI)) < (2 * Math.PI * pulseWidth) ? 0.9 : -0.9;
                
            case NOISE:
                return Math.random() * 2 - 1;
                
            default:
                return Math.sin(phase);
        }
    }
    
    // SoundParams builder methods
    private static SoundParams setADSR(SoundParams params, float a, float d, float s, float r) {
        params.attack = a;
        params.decay = d;
        params.sustain = s;
        params.release = r;
        return params;
    }
    
    private static SoundParams setHarmonics(SoundParams params, int h) {
        params.harmonics = h;
        return params;
    }
    
    private static SoundParams setDetune(SoundParams params, float d) {
        params.detune = d;
        return params;
    }
    
    private static SoundParams setVibrato(SoundParams params, float depth, float rate) {
        params.vibratoDepth = depth;
        params.vibratoRate = rate;
        return params;
    }
    
    private static SoundParams setFrequencySweep(SoundParams params, int start, int end) {
        // This would be implemented in the sound generation
        return params;
    }
    
    // Public API Methods
    public static void setEnabled(boolean value) {
        muted = !value;
        if (muted) {
            stopAllSounds();
            stopBackgroundMusic();
        } else if (musicEnabled) {
            startBackgroundMusic();
        }
    }
    
    public static boolean isEnabled() {
        return !muted;
    }
    
    public static void playSound(SoundType type) {
        if (muted || !sfxEnabled) return;
        
        audioThreadPool.submit(() -> {
            try {
                List<Clip> pool = soundPools.get(type);
                if (pool == null || pool.isEmpty()) return;
                
                // Find available clip
                Clip clip = null;
                for (Clip c : pool) {
                    if (!c.isRunning()) {
                        clip = c;
                        break;
                    }
                }
                
                if (clip == null) {
                    // All clips busy, create new one
                    clip = generateSound(type);
                    if (clip != null) {
                        pool.add(clip);
                    }
                }
                
                if (clip != null) {
                    clip.setFramePosition(0);
                    
                    // Set volume
                    if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                        FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                        float db = 20f * (float) Math.log10(masterVolume * sfxVolume);
                        gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db)));
                    }
                    
                    clip.start();
                }
            } catch (Exception e) {
                System.err.println("Error playing sound: " + e.getMessage());
            }
        });
    }

    public static void playSound(SoundType type, float volumeMultiplier, float pitchMultiplier) {
        playSound(type);
    }
    
    // Legacy compatibility methods
    public static void playShoot() {
        playSound(SoundType.PLAYER_SHOOT);
    }
    
    public static void playExplosion() {
        playSound(SoundType.ENEMY_EXPLOSION_MEDIUM);
    }
    
    public static void playPowerUp() {
        playSound(SoundType.PLAYER_POWERUP);
    }
    
    public static void playHit() {
        playSound(SoundType.PLAYER_HIT);
    }
    
    public static void playMenu() {
        playSound(SoundType.UI_SELECT);
    }
    
    public static void playPause() {
        playSound(SoundType.UI_BACK);
    }
    
    public static void playResume() {
        playSound(SoundType.UI_SELECT);
    }
    
    public static void playStart() {
        playSound(SoundType.LEVEL_UP);
    }
    
    public static void playLaserShoot() {
        playSound(SoundType.LASER_BASIC);
    }
    
    public static void playPlasmaShoot() {
        playSound(SoundType.PLASMA);
    }
    
    public static void playRocketShoot() {
        playSound(SoundType.ROCKET);
    }
    
    public static void playRailgunShoot() {
        playSound(SoundType.RAILGUN);
    }
    
    public static void playUIClick() {
        playSound(SoundType.UI_CLICK);
    }
    
    public static void playUIHover() {
        playSound(SoundType.UI_HOVER);
    }
    
    public static void playComboBreak() {
        playSound(SoundType.COMBO_BREAK);
    }
    
    public static void playAlarm() {
        playSound(SoundType.ALARM);
    }
    
    public static void stopAlarm() {
        stopSoundType(SoundType.ALARM);
    }
    
    public static void playEngineHum(float intensity) {
        playSound(SoundType.ENGINE_HUM);
    }
    
    public static void stopEngineHum() {
        stopSoundType(SoundType.ENGINE_HUM);
    }
    
    public static void stopSoundType(SoundType type) {
        List<Clip> pool = soundPools.get(type);
        if (pool != null) {
            for (Clip clip : pool) {
                if (clip.isRunning()) {
                    clip.stop();
                    clip.setFramePosition(0);
                }
            }
        }
    }
    
    public static void stopAllSounds() {
        for (List<Clip> pool : soundPools.values()) {
            for (Clip clip : pool) {
                if (clip.isRunning()) {
                    clip.stop();
                    clip.setFramePosition(0);
                }
            }
        }
    }
    
    // Background Music (procedurally generated)
    private static Clip bgMusicClip;
    private static boolean bgMusicPlaying = false;
    private static boolean bgMusicFailed = false;
    
    private static void startBackgroundMusic() {
        if (!musicEnabled || muted || bgMusicPlaying || bgMusicFailed) return;
        
        audioThreadPool.submit(() -> {
            try {
                bgMusicClip = generateBackgroundMusic();
                if (bgMusicClip == null) return;
                
                bgMusicClip.loop(Clip.LOOP_CONTINUOUSLY);
                
                // Set music volume
                if (bgMusicClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gain = (FloatControl) bgMusicClip.getControl(FloatControl.Type.MASTER_GAIN);
                    float db = 20f * (float) Math.log10(masterVolume * musicVolume);
                    gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db)));
                }
                
                bgMusicClip.start();
                bgMusicPlaying = true;
                
            } catch (Exception e) {
                System.err.println("Error starting background music: " + e.getMessage());
            }
        });
    }
    
    private static Clip generateBackgroundMusic() {
        try {
            // Generate ambient space music
            int duration = 15000; // 15 seconds (will loop)
            int numSamples = (int)(SAMPLE_RATE * duration / 1000.0);
            int frameSize = 4;
            byte[] buffer = new byte[numSamples * frameSize];
            
            double bassPhase = 0;
            double padPhase = 0;
            double leadPhase = 0;
            Random rand = new Random();
            
            // Chord progression for space music
            int[][] chords = {
                {55, 69, 82},  // A minor
                {65, 82, 98},  // F major
                {62, 78, 93},  // D minor
                {58, 73, 87}   // B diminished
            };
            
            int chordIndex = 0;
            int chordDuration = duration / chords.length;
            
            for (int i = 0; i < numSamples; i++) {
                double time = i / (double)SAMPLE_RATE;
                
                // Change chord every chordDuration
                if (i % (SAMPLE_RATE * chordDuration / 1000) == 0) {
                    chordIndex = (chordIndex + 1) % chords.length;
                }
                
                int[] chord = chords[chordIndex];
                
                // Bass (triangle wave)
                bassPhase += 2 * Math.PI * chord[0] / SAMPLE_RATE;
                double bass = Math.abs(2 * (bassPhase / (2 * Math.PI) - 
                    Math.floor(bassPhase / (2 * Math.PI) + 0.5))) - 1;
                bass *= 0.3;
                
                // Pad (sawtooth with filter)
                padPhase += 2 * Math.PI * chord[1] / SAMPLE_RATE;
                double pad = 2 * (padPhase / (2 * Math.PI) - 
                    Math.floor(padPhase / (2 * Math.PI) + 0.5));
                pad *= 0.2;
                
                // Lead (sine with occasional arpeggio)
                double leadFreq = chord[2];
                if (rand.nextDouble() < 0.01) { // Random arpeggio
                    leadFreq = chord[rand.nextInt(3)];
                }
                
                leadPhase += 2 * Math.PI * leadFreq / SAMPLE_RATE;
                double lead = Math.sin(leadPhase) * 0.1;
                
                // Combine with envelope
                double env = Math.sin(time * 0.5) * 0.5 + 0.5; // Slow pulsing
                double sample = (bass + pad + lead) * env * 0.7;
                
                // Add some space ambiance (noise)
                sample += (rand.nextDouble() * 2 - 1) * 0.02;
                
                // Convert to 16-bit
                short sampleValue = (short)(sample * 32767);
                int idx = i * frameSize;
                buffer[idx] = (byte)(sampleValue & 0xFF);
                buffer[idx + 1] = (byte)((sampleValue >> 8) & 0xFF);
                buffer[idx + 2] = buffer[idx];
                buffer[idx + 3] = buffer[idx + 1];
            }
            
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 2, true, false);
            DataLine.Info info = new DataLine.Info(Clip.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                bgMusicFailed = true;
                return null;
            }
            Clip clip = AudioSystem.getClip();
            clip.open(format, buffer, 0, buffer.length);
            
            return clip;
            
        } catch (Exception e) {
            System.err.println("Error generating background music: " + e.getMessage());
            bgMusicFailed = true;
            return null;
        }
    }
    
    private static void stopBackgroundMusic() {
        if (bgMusicClip != null && bgMusicPlaying) {
            bgMusicClip.stop();
            bgMusicClip.setFramePosition(0);
            bgMusicPlaying = false;
        }
    }
    
    public static void setMasterVolume(float volume) {
        masterVolume = Math.max(0, Math.min(1, volume));
        updateAllVolumes();
    }
    
    public static void setMusicVolume(float volume) {
        musicVolume = Math.max(0, Math.min(1, volume));
        updateMusicVolume();
    }
    
    public static void setSFXVolume(float volume) {
        sfxVolume = Math.max(0, Math.min(1, volume));
        updateSFXVolumes();
    }
    
    private static void updateAllVolumes() {
        updateMusicVolume();
        updateSFXVolumes();
    }
    
    private static void updateMusicVolume() {
        if (bgMusicClip != null && bgMusicClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gain = (FloatControl) bgMusicClip.getControl(FloatControl.Type.MASTER_GAIN);
            float db = 20f * (float) Math.log10(masterVolume * musicVolume);
            gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db)));
        }
    }
    
    private static void updateSFXVolumes() {
        for (List<Clip> pool : soundPools.values()) {
            for (Clip clip : pool) {
                if (clip.isRunning() && clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    float db = 20f * (float) Math.log10(masterVolume * sfxVolume);
                    gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db)));
                }
            }
        }
    }
    
    // Utility method for simple tone generation (backup)
    public static void playSimpleTone(int hz, int msecs, float volume) {
        if (muted || !sfxEnabled) return;
        
        audioThreadPool.submit(() -> {
            try {
                int numSamples = msecs * SAMPLE_RATE / 1000;
                byte[] buffer = new byte[numSamples * 2];
                
                for (int i = 0; i < numSamples; i++) {
                    double time = i / (double)SAMPLE_RATE;
                    double angle = 2.0 * Math.PI * hz * time;
                    short sample = (short)(Math.sin(angle) * 32767 * volume * masterVolume * sfxVolume);
                    
                    buffer[i * 2] = (byte)(sample & 0xFF);
                    buffer[i * 2 + 1] = (byte)((sample >> 8) & 0xFF);
                }
                
                AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
                Clip clip = AudioSystem.getClip();
                clip.open(format, buffer, 0, buffer.length);
                clip.start();
                
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                    }
                });
                
            } catch (Exception e) {
                System.err.println("Error playing simple tone: " + e.getMessage());
            }
        });
    }
    
    // Clean shutdown
    public static void shutdown() {
        muted = true;
        stopAllSounds();
        stopBackgroundMusic();
        
        for (List<Clip> pool : soundPools.values()) {
            for (Clip clip : pool) {
                clip.close();
            }
        }
        
        if (bgMusicClip != null) {
            bgMusicClip.close();
        }
        
        audioThreadPool.shutdown();
        scheduler.shutdown();
        
        try {
            if (!audioThreadPool.awaitTermination(1, TimeUnit.SECONDS)) {
                audioThreadPool.shutdownNow();
            }
            if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            audioThreadPool.shutdownNow();
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}