/**
 * Created by Karim Mreisi.
 */

package com.limelight.binding.input.virtual_controller;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a digital button on screen element. It is used to get click and double click user input.
 */
public class DigitalButton extends VirtualControllerElement {

    /**
     * Listener interface to update registered observers.
     */
    public interface DigitalButtonListener {

        /**
         * onClick event will be fired on button click.
         */
        void onClick();

        /**
         * onLongClick event will be fired on button long click.
         */
        void onLongClick();

        /**
         * onRelease event will be fired on button unpress.
         */
        void onRelease();
    }

    private List<DigitalButtonListener> listeners = new ArrayList<>();
    private String text = "";
    private int icon = -1;
    private long timerLongClickTimeout = 3000;
    private final Runnable longClickRunnable = new Runnable() {
        @Override
        public void run() {
            onLongClickCallback();
        }
    };

    private final Paint paint = new Paint();
    private final RectF rect = new RectF();

    private int layer;
    private DigitalButton movingButton = null;

    boolean inRange(float x, float y) {
        return (this.getX() < x && this.getX() + this.getWidth() > x) &&
                (this.getY() < y && this.getY() + this.getHeight() > y);
    }

    public boolean checkMovement(float x, float y, DigitalButton movingButton) {
        // check if the movement happened in the same layer
        if (movingButton.layer != this.layer) {
            return false;
        }

        // save current pressed state
        boolean wasPressed = isPressed();

        // check if the movement directly happened on the button
        if ((this.movingButton == null || movingButton == this.movingButton)
                && this.inRange(x, y)) {
            // set button pressed state depending on moving button pressed state
            if (this.isPressed() != movingButton.isPressed()) {
                this.setPressed(movingButton.isPressed());
            }
        }
        // check if the movement is outside of the range and the movement button
        // is the saved moving button
        else if (movingButton == this.movingButton) {
            this.setPressed(false);
        }

        // check if a change occurred
        if (wasPressed != isPressed()) {
            if (isPressed()) {
                // is pressed set moving button and emit click event
                this.movingButton = movingButton;
                onClickCallback();
            } else {
                // no longer pressed reset moving button and emit release event
                this.movingButton = null;
                onReleaseCallback();
            }

            invalidate();

            return true;
        }

        return false;
    }

    private void checkMovementForAllButtons(float x, float y) {
        for (VirtualControllerElement element : virtualController.getElements()) {
            if (element != this && element instanceof DigitalButton) {
                ((DigitalButton) element).checkMovement(x, y, this);
            }
        }
    }

    public DigitalButton(VirtualController controller, int elementId, int layer, Context context) {
        super(controller, context, elementId);
        this.layer = layer;
    }

    public void addDigitalButtonListener(DigitalButtonListener listener) {
        listeners.add(listener);
    }

    public void setText(String text) {
        this.text = text;
        invalidate();
    }

    public void setIcon(int id) {
        this.icon = id;
        invalidate();
    }

    /**
     * Xbox-style accent colors for the face buttons. Other buttons use a neutral accent.
     */
    private static int getAccentColor(String text) {
        switch (text) {
            case "A":
                return 0xFF55B83B;
            case "B":
                return 0xFFE8333A;
            case "X":
                return 0xFF2C8DE8;
            case "Y":
                return 0xFFF5B800;
            default:
                return 0;
        }
    }

    private static String getDisplayText(String text) {
        // Match the Xbox controller naming for the center buttons
        switch (text) {
            case "START":
                return "MENU";
            case "BACK":
                return "VIEW";
            case "GUIDE":
                return "\u2716"; // Heavy X glyph, close to the Xbox button
            default:
                return text;
        }
    }

    @Override
    protected void onElementDraw(Canvas canvas) {
        // set transparent background
        canvas.drawColor(Color.TRANSPARENT);

        final int width = getWidth();
        final int height = getHeight();
        final int alpha = getOpacityAlpha();
        final int accent = getAccentColor(text);
        final boolean isFaceButton = accent != 0;
        final boolean pressed = isPressed();
        final float stroke = getDefaultStrokeWidth();

        paint.setAntiAlias(true);
        paint.setStrokeWidth(stroke);

        rect.left = rect.top = stroke;
        rect.right = width - stroke;
        rect.bottom = height - stroke;

        // Square-ish elements are drawn as circles, everything else as rounded pills
        boolean circular = Math.abs(width - height) < Math.max(width, height) * 0.15f;
        float cornerRadius = Math.min(rect.width(), rect.height()) / 2;

        // Background fill
        int fillColor;
        if (pressed) {
            fillColor = isFaceButton ? applyAlpha(accent, alpha) : pressedColor;
        }
        else {
            fillColor = getBackdropColor();
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(fillColor);
        if (circular) {
            canvas.drawOval(rect, paint);
        }
        else {
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint);
        }

        // Outline ring
        int ringColor;
        if (!isNormalMode() || !isFaceButton) {
            ringColor = pressed ? pressedColor : getDefaultColor();
        }
        else {
            ringColor = applyAlpha(accent, alpha);
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(ringColor);
        if (circular) {
            canvas.drawOval(rect, paint);
        }
        else {
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint);
        }

        if (icon != -1) {
            Drawable d = getResources().getDrawable(icon);
            d.setBounds(5, 5, width - 5, height - 5);
            d.draw(canvas);
        } else {
            String label = getDisplayText(text);

            paint.setStyle(Paint.Style.FILL);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(circular ? width * 0.45f : height * 0.42f);

            // Shrink long labels until they fit inside the element
            float maxTextWidth = rect.width() * 0.8f;
            float measured = paint.measureText(label);
            if (measured > maxTextWidth && measured > 0) {
                paint.setTextSize(paint.getTextSize() * maxTextWidth / measured);
            }

            if (pressed) {
                paint.setColor(applyAlpha(Color.WHITE, alpha));
            }
            else if (isFaceButton && isNormalMode()) {
                paint.setColor(applyAlpha(accent, alpha));
            }
            else {
                paint.setColor(getDefaultColor());
            }

            Paint.FontMetrics fm = paint.getFontMetrics();
            float baseline = height / 2f - (fm.ascent + fm.descent) / 2f;
            canvas.drawText(label, width / 2f, baseline, paint);
        }
    }

    private void onClickCallback() {
        _DBG("clicked");
        // notify listeners
        for (DigitalButtonListener listener : listeners) {
            listener.onClick();
        }

        virtualController.getHandler().removeCallbacks(longClickRunnable);
        virtualController.getHandler().postDelayed(longClickRunnable, timerLongClickTimeout);
    }

    private void onLongClickCallback() {
        _DBG("long click");
        // notify listeners
        for (DigitalButtonListener listener : listeners) {
            listener.onLongClick();
        }
    }

    private void onReleaseCallback() {
        _DBG("released");
        // notify listeners
        for (DigitalButtonListener listener : listeners) {
            listener.onRelease();
        }

        // We may be called for a release without a prior click
        virtualController.getHandler().removeCallbacks(longClickRunnable);
    }

    @Override
    public boolean onElementTouchEvent(MotionEvent event) {
        // get masked (not specific to a pointer) action
        float x = getX() + event.getX();
        float y = getY() + event.getY();
        int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                movingButton = null;
                setPressed(true);
                onClickCallback();

                invalidate();

                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                checkMovementForAllButtons(x, y);

                return true;
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                setPressed(false);
                onReleaseCallback();

                checkMovementForAllButtons(x, y);

                invalidate();

                return true;
            }
            default: {
            }
        }
        return true;
    }
}
