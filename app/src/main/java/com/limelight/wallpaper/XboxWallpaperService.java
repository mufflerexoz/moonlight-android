package com.limelight.wallpaper;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

/**
 * Animated Xbox dashboard style live wallpaper: a dark gradient with slowly drifting
 * green glows, flowing light ribbons, rising particles and a green pulse where you tap.
 * Rendering only runs while the wallpaper is visible and is capped at ~30 FPS.
 */
public class XboxWallpaperService extends WallpaperService {
    @Override
    public Engine onCreateEngine() {
        return new DashboardEngine();
    }

    private static final long FRAME_DELAY_MS = 33;
    private static final int PARTICLE_COUNT = 36;
    private static final float PULSE_DURATION_S = 1.2f;

    private static final int BG_TOP = 0xFF1C211D;
    private static final int BG_BOTTOM = 0xFF060706;
    private static final int GREEN = 0x107C10;
    private static final int GREEN_BRIGHT = 0x2DA82D;
    private static final int[] RIBBON_COLORS = {0x2DA82D, 0x107C10, 0x9BE89B};
    private static final int[] RIBBON_ALPHAS = {0x50, 0x40, 0x28};

    private static final class Glow {
        final float baseX, baseY, ampX, ampY, speed, phase, depth, radiusScale;
        final int color;
        RadialGradient shader;
        float radius;

        Glow(float baseX, float baseY, float ampX, float ampY, float speed, float phase,
             float depth, float radiusScale, int color) {
            this.baseX = baseX; this.baseY = baseY; this.ampX = ampX; this.ampY = ampY;
            this.speed = speed; this.phase = phase; this.depth = depth;
            this.radiusScale = radiusScale; this.color = color;
        }
    }

    private static final class Pulse {
        final float x, y, start;
        Pulse(float x, float y, float start) { this.x = x; this.y = y; this.start = start; }
    }

    private class DashboardEngine extends Engine {
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final Runnable drawRunnable = new Runnable() {
            @Override
            public void run() {
                drawFrame();
            }
        };

        private final Paint backgroundPaint = new Paint();
        private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ribbonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint pulsePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Matrix shaderMatrix = new Matrix();
        private final Path ribbonPath = new Path();
        private final Shader[] ribbonShaders = new Shader[3];

        private final Glow[] glows = {
                new Glow(0.15f, 0.10f, 0.10f, 0.06f, 0.11f, 0.0f, 0.6f, 0.75f, GREEN),
                new Glow(0.85f, 0.65f, 0.08f, 0.10f, 0.08f, 2.1f, 1.0f, 0.60f, GREEN_BRIGHT),
                new Glow(0.45f, 1.05f, 0.15f, 0.04f, 0.06f, 4.2f, 0.4f, 0.80f, GREEN),
        };

        // x, y, speed, size, phase for each particle
        private final float[] particles = new float[PARTICLE_COUNT * 5];
        private final ArrayList<Pulse> pulses = new ArrayList<>();
        private final Random random = new Random();

        private int width, height;
        private float xOffset = 0.5f;
        private boolean visible;
        private final long startTime = SystemClock.uptimeMillis();

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            setTouchEventsEnabled(true);
            setOffsetNotificationsEnabled(true);

            ribbonPaint.setStyle(Paint.Style.STROKE);
            ribbonPaint.setStrokeCap(Paint.Cap.ROUND);
            particlePaint.setStyle(Paint.Style.FILL);
            pulsePaint.setStyle(Paint.Style.STROKE);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            handler.removeCallbacks(drawRunnable);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            this.visible = visible;
            if (visible) {
                drawFrame();
            }
            else {
                handler.removeCallbacks(drawRunnable);
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            this.width = width;
            this.height = height;

            backgroundPaint.setShader(new LinearGradient(0, 0, 0, height, BG_TOP, BG_BOTTOM,
                    Shader.TileMode.CLAMP));

            float max = Math.max(width, height);
            for (Glow glow : glows) {
                glow.radius = max * glow.radiusScale;
                glow.shader = new RadialGradient(0, 0, glow.radius,
                        new int[] {0x66000000 | glow.color, 0x22000000 | glow.color, 0x00000000 | glow.color},
                        new float[] {0f, 0.45f, 1f}, Shader.TileMode.CLAMP);
            }

            for (int i = 0; i < ribbonShaders.length; i++) {
                int color = RIBBON_COLORS[i];
                ribbonShaders[i] = new LinearGradient(0, 0, width, 0,
                        new int[] {color, (RIBBON_ALPHAS[i] << 24) | color, color},
                        new float[] {0f, 0.5f, 1f}, Shader.TileMode.CLAMP);
            }

            for (int i = 0; i < PARTICLE_COUNT; i++) {
                resetParticle(i, true);
            }

            drawFrame();
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            visible = false;
            handler.removeCallbacks(drawRunnable);
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset, float xStep, float yStep,
                                     int xPixels, int yPixels) {
            // Gentle parallax when a launcher scrolls between pages
            this.xOffset = xOffset;
        }

        @Override
        public void onTouchEvent(MotionEvent event) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN && pulses.size() < 8) {
                pulses.add(new Pulse(event.getX(), event.getY(), now()));
            }
            super.onTouchEvent(event);
        }

        private float now() {
            return (SystemClock.uptimeMillis() - startTime) / 1000f;
        }

        private void resetParticle(int i, boolean anywhere) {
            int o = i * 5;
            particles[o] = random.nextFloat() * width;
            particles[o + 1] = anywhere ? random.nextFloat() * height : height + 10;
            particles[o + 2] = 8 + random.nextFloat() * 22;              // px per second
            particles[o + 3] = 1.2f + random.nextFloat() * 2.8f;         // radius
            particles[o + 4] = random.nextFloat() * (float) (Math.PI * 2); // twinkle phase
        }

        private void drawFrame() {
            handler.removeCallbacks(drawRunnable);
            if (width == 0 || height == 0) {
                return;
            }

            SurfaceHolder holder = getSurfaceHolder();
            Canvas canvas = null;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    canvas = holder.lockHardwareCanvas();
                }
                else {
                    canvas = holder.lockCanvas();
                }
                if (canvas != null) {
                    render(canvas, now());
                }
            } catch (IllegalStateException | IllegalArgumentException e) {
                // Surface went away between checks
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas);
                    } catch (IllegalStateException | IllegalArgumentException ignored) {
                    }
                }
            }

            if (visible) {
                handler.postDelayed(drawRunnable, FRAME_DELAY_MS);
            }
        }

        private void render(Canvas canvas, float t) {
            final float parallax = (xOffset - 0.5f) * -width * 0.2f;

            canvas.drawRect(0, 0, width, height, backgroundPaint);

            // Drifting glows
            for (Glow glow : glows) {
                float cx = width * (glow.baseX + glow.ampX * (float) Math.sin(t * glow.speed + glow.phase))
                        + parallax * glow.depth;
                float cy = height * (glow.baseY + glow.ampY * (float) Math.cos(t * glow.speed * 1.3f + glow.phase));
                shaderMatrix.setTranslate(cx, cy);
                glow.shader.setLocalMatrix(shaderMatrix);
                glowPaint.setShader(glow.shader);
                canvas.drawCircle(cx, cy, glow.radius, glowPaint);
            }

            // Flowing light ribbons
            drawRibbon(canvas, t, 0.62f, 0.07f, 0.35f, 0.9f, ribbonShaders[0], height * 0.05f, parallax * 0.5f);
            drawRibbon(canvas, t, 0.70f, 0.05f, 0.27f, 1.4f, ribbonShaders[1], height * 0.09f, parallax * 0.7f);
            drawRibbon(canvas, t, 0.66f, 0.09f, 0.21f, 2.6f, ribbonShaders[2], height * 0.012f, parallax * 0.9f);

            // Rising particles
            float dt = FRAME_DELAY_MS / 1000f;
            for (int i = 0; i < PARTICLE_COUNT; i++) {
                int o = i * 5;
                particles[o + 1] -= particles[o + 2] * dt;
                if (particles[o + 1] < -10) {
                    resetParticle(i, false);
                }
                float twinkle = 0.5f + 0.5f * (float) Math.sin(t * 1.7f + particles[o + 4]);
                particlePaint.setColor(((int) (40 + 120 * twinkle) << 24) | 0xB8F5B8);
                canvas.drawCircle(particles[o] + parallax * 0.3f, particles[o + 1], particles[o + 3], particlePaint);
            }

            // Touch pulses
            Iterator<Pulse> it = pulses.iterator();
            while (it.hasNext()) {
                Pulse p = it.next();
                float progress = (t - p.start) / PULSE_DURATION_S;
                if (progress >= 1f) {
                    it.remove();
                    continue;
                }
                float radius = Math.min(width, height) * 0.35f * progress;
                pulsePaint.setStrokeWidth(Math.max(2f, 14f * (1f - progress)));
                pulsePaint.setColor(((int) (200 * (1f - progress)) << 24) | GREEN_BRIGHT);
                canvas.drawCircle(p.x, p.y, radius, pulsePaint);
            }
        }

        private void drawRibbon(Canvas canvas, float t, float baseY, float amp, float speed,
                                float phase, Shader shader, float thickness, float shift) {
            ribbonPath.reset();
            final int steps = 24;
            for (int i = 0; i <= steps; i++) {
                float fx = i / (float) steps;
                float x = -width * 0.1f + fx * width * 1.2f + shift;
                float y = height * (baseY
                        + amp * (float) Math.sin(fx * 3.2f + t * speed + phase)
                        + amp * 0.5f * (float) Math.sin(fx * 7.1f - t * speed * 0.7f + phase * 2));
                if (i == 0) {
                    ribbonPath.moveTo(x, y);
                }
                else {
                    ribbonPath.lineTo(x, y);
                }
            }
            ribbonPaint.setStrokeWidth(thickness);
            ribbonPaint.setShader(shader);
            canvas.drawPath(ribbonPath, ribbonPaint);
        }
    }
}
