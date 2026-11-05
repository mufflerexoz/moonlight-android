/**
 * Created by Karim Mreisi.
 */

package com.limelight.binding.input.virtual_controller;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;

public class DigitalPad extends VirtualControllerElement {
    public final static int DIGITAL_PAD_DIRECTION_NO_DIRECTION = 0;
    int direction = DIGITAL_PAD_DIRECTION_NO_DIRECTION;
    public final static int DIGITAL_PAD_DIRECTION_LEFT = 1;
    public final static int DIGITAL_PAD_DIRECTION_UP = 2;
    public final static int DIGITAL_PAD_DIRECTION_RIGHT = 4;
    public final static int DIGITAL_PAD_DIRECTION_DOWN = 8;
    List<DigitalPadListener> listeners = new ArrayList<>();

    private static final int DPAD_MARGIN = 5;

    private final Paint paint = new Paint();

    public DigitalPad(VirtualController controller, Context context) {
        super(controller, context, EID_DPAD);
    }

    public void addDigitalPadListener(DigitalPadListener listener) {
        listeners.add(listener);
    }

    private final Path crossPath = new Path();
    private final Path scratchPath = new Path();
    private final RectF scratchRect = new RectF();

    private void drawArm(Canvas canvas, float l, float t, float r, float b, float radius, boolean pressed) {
        if (!pressed) {
            return;
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(pressedColor);
        scratchRect.set(l, t, r, b);
        canvas.drawRoundRect(scratchRect, radius, radius, paint);
    }

    private void drawChevron(Canvas canvas, float cx, float cy, float size, int dir, boolean pressed) {
        scratchPath.reset();
        switch (dir) {
            case DIGITAL_PAD_DIRECTION_LEFT:
                scratchPath.moveTo(cx - size, cy);
                scratchPath.lineTo(cx + size * 0.6f, cy - size);
                scratchPath.lineTo(cx + size * 0.6f, cy + size);
                break;
            case DIGITAL_PAD_DIRECTION_RIGHT:
                scratchPath.moveTo(cx + size, cy);
                scratchPath.lineTo(cx - size * 0.6f, cy - size);
                scratchPath.lineTo(cx - size * 0.6f, cy + size);
                break;
            case DIGITAL_PAD_DIRECTION_UP:
                scratchPath.moveTo(cx, cy - size);
                scratchPath.lineTo(cx - size, cy + size * 0.6f);
                scratchPath.lineTo(cx + size, cy + size * 0.6f);
                break;
            case DIGITAL_PAD_DIRECTION_DOWN:
                scratchPath.moveTo(cx, cy + size);
                scratchPath.lineTo(cx - size, cy - size * 0.6f);
                scratchPath.lineTo(cx + size, cy - size * 0.6f);
                break;
        }
        scratchPath.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(pressed ? applyAlpha(Color.WHITE, getOpacityAlpha()) : getDefaultColor());
        canvas.drawPath(scratchPath, paint);
    }

    @Override
    protected void onElementDraw(Canvas canvas) {
        // set transparent background
        canvas.drawColor(Color.TRANSPARENT);

        final float w = getWidth();
        final float h = getHeight();
        final float stroke = getDefaultStrokeWidth();
        final float inset = stroke + DPAD_MARGIN;
        final float radius = Math.min(w, h) * 0.08f;

        final float x1 = getPercent(w, 33), x2 = getPercent(w, 66);
        final float y1 = getPercent(h, 33), y2 = getPercent(h, 66);

        paint.setAntiAlias(true);
        paint.setStrokeWidth(stroke);

        // Build the plus-shaped outline of the D-pad
        crossPath.reset();
        crossPath.addRoundRect(new RectF(inset, y1, w - inset, y2), radius, radius, Path.Direction.CW);
        scratchPath.reset();
        scratchPath.addRoundRect(new RectF(x1, inset, x2, h - inset), radius, radius, Path.Direction.CW);
        crossPath.op(scratchPath, Path.Op.UNION);

        // Translucent backdrop
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(getBackdropColor());
        canvas.drawPath(crossPath, paint);

        // Highlight pressed arms
        drawArm(canvas, inset, y1, x1 + radius, y2, radius, (direction & DIGITAL_PAD_DIRECTION_LEFT) > 0);
        drawArm(canvas, x2 - radius, y1, w - inset, y2, radius, (direction & DIGITAL_PAD_DIRECTION_RIGHT) > 0);
        drawArm(canvas, x1, inset, x2, y1 + radius, radius, (direction & DIGITAL_PAD_DIRECTION_UP) > 0);
        drawArm(canvas, x1, y2 - radius, x2, h - inset, radius, (direction & DIGITAL_PAD_DIRECTION_DOWN) > 0);

        // Outline
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(direction != DIGITAL_PAD_DIRECTION_NO_DIRECTION && isNormalMode() ?
                pressedColor : getDefaultColor());
        canvas.drawPath(crossPath, paint);

        // Direction chevrons
        float chevron = Math.min(w, h) * 0.06f;
        drawChevron(canvas, (inset + x1) / 2, h / 2, chevron, DIGITAL_PAD_DIRECTION_LEFT,
                (direction & DIGITAL_PAD_DIRECTION_LEFT) > 0);
        drawChevron(canvas, (x2 + w - inset) / 2, h / 2, chevron, DIGITAL_PAD_DIRECTION_RIGHT,
                (direction & DIGITAL_PAD_DIRECTION_RIGHT) > 0);
        drawChevron(canvas, w / 2, (inset + y1) / 2, chevron, DIGITAL_PAD_DIRECTION_UP,
                (direction & DIGITAL_PAD_DIRECTION_UP) > 0);
        drawChevron(canvas, w / 2, (y2 + h - inset) / 2, chevron, DIGITAL_PAD_DIRECTION_DOWN,
                (direction & DIGITAL_PAD_DIRECTION_DOWN) > 0);
    }

    private void newDirectionCallback(int direction) {
        _DBG("direction: " + direction);

        // notify listeners
        for (DigitalPadListener listener : listeners) {
            listener.onDirectionChange(direction);
        }
    }

    @Override
    public boolean onElementTouchEvent(MotionEvent event) {
        // get masked (not specific to a pointer) action
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE: {
                direction = 0;

                if (event.getX() < getPercent(getWidth(), 33)) {
                    direction |= DIGITAL_PAD_DIRECTION_LEFT;
                }
                if (event.getX() > getPercent(getWidth(), 66)) {
                    direction |= DIGITAL_PAD_DIRECTION_RIGHT;
                }
                if (event.getY() > getPercent(getHeight(), 66)) {
                    direction |= DIGITAL_PAD_DIRECTION_DOWN;
                }
                if (event.getY() < getPercent(getHeight(), 33)) {
                    direction |= DIGITAL_PAD_DIRECTION_UP;
                }
                newDirectionCallback(direction);
                invalidate();

                return true;
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                direction = 0;
                newDirectionCallback(direction);
                invalidate();

                return true;
            }
            default: {
            }
        }

        return true;
    }

    public interface DigitalPadListener {
        void onDirectionChange(int direction);
    }
}
