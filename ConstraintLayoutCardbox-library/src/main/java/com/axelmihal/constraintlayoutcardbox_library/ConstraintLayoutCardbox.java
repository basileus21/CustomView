package com.axelmihal.constraintlayoutcardbox_library;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.res.ResourcesCompat;

public class ConstraintLayoutCardbox extends ConstraintLayout implements View.OnTouchListener {

    public final static int MOVE_VERT = 1;
    public final static int MOVE_HORIZ = 2;
    public final static int MOVE_BOTH = 3;

    public final static int RESIZE_VERT = 1;
    public final static int RESIZE_HORIZ = 2;
    public final static int RESIZE_BOTH = 3;

    public final static int FRAME_RECT_ROUND = 1;
    public final static int FRAME_RECT = 2;
    public final static int NO_FRAME = 3;

    public final static int DEFAULT_DRAG_SPEED = 20;
    public final static int DRAG_NO_LIMIT = -1;

    private final int DRAG_ICON_SIZE = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics());

    private Paint paintStroke, paintFill;
    private View dragMove, dragResize;

    // Initial values used in move and resize
    private float initRawX = -1;
    private float initRawY = -1;
    private float prevRawX = -1;
    private float prevRawY = -1;
    private int initWidth = -1;
    private int initHeight = -1;
    private int initLeft = -1;
    private int initRight = -1;
    private int initTop = -1;
    private int initBottom = -1;

    // Directions in which move / resize apply
    private int moveDir = 0;
    private int resizeDir = 0;

    // Dragging speed limit
    private int dragSpeed = DEFAULT_DRAG_SPEED;

    // Minimal size
    private int widthMin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150, getResources().getDisplayMetrics());
    private int heightMin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 75, getResources().getDisplayMetrics());

    // Parent size
    private int parentWidth = -1;
    private int parentHeight = -1;

    // Frame color & shape
    private int frameColor = Color.BLUE;
    private int frameShape = FRAME_RECT_ROUND;


    // Position Listener
    private PositionListener positionListener;

    public interface PositionListener {
        void onPositionChanged(Rect rect, boolean released);
    }


    public ConstraintLayoutCardbox(Context context) {
        super(context);
        init(context);
    }

    public ConstraintLayoutCardbox(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ConstraintLayoutCardbox(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public ConstraintLayoutCardbox(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }


    public void setPositionListener(PositionListener positionListener) {
        this.positionListener = positionListener;
    }

    public void clearPositionListener() {
        positionListener = null;
    }


    private void init(Context context) {
        paintStroke = new Paint();
        paintStroke.setColor(frameColor);
        paintStroke.setStrokeWidth(7);
        paintStroke.setStyle(Paint.Style.STROKE);
    }

    // Override function drawing for Layout
    @Override
    protected void dispatchDraw(Canvas canvas) {
        int width = this.getWidth() - 1;
        int height = this.getHeight() - 1;
        switch (frameShape) {
            case FRAME_RECT_ROUND:
                int roundingLimit = 30;
                int rounding = (int) (Math.min(width, height) * 0.1f < roundingLimit ? Math.min(width, height) * 0.1f : roundingLimit);
                if (paintFill != null) {
                    canvas.drawRoundRect(0, 0, width, height, rounding, rounding, paintFill);
                }
                canvas.drawRoundRect(0, 0, width, height, rounding, rounding, paintStroke);
                break;
            case FRAME_RECT:
                if (paintFill != null) {
                    canvas.drawRect(0, 0, width, height, paintFill);
                }
                canvas.drawRect(0, 0, width, height, paintStroke);
                break;
            default:
                break;
        }
        super.dispatchDraw(canvas);
    }


    public void changeSize() {
        ViewGroup.LayoutParams params = this.getLayoutParams();

        int widthPx = this.getWidth();
        int heightPx = this.getHeight();

        widthPx = widthPx > initWidth ? widthPx / 2 : widthPx * 2;
        heightPx = heightPx > initHeight ? heightPx / 2 : heightPx * 2;

        params.width = widthPx;
        params.height = heightPx;

        this.setLayoutParams(params);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Start touch.
                // Save touch position
                initRawX = motionEvent.getRawX();
                initRawY = motionEvent.getRawY();
                prevRawX = initRawX;
                prevRawY = initRawY;

                // Get the size and margins
                initWidth = getWidth();
                initHeight = getHeight();
                initLeft = getLeft();
                initRight = getRight();
                initTop = getTop();
                initBottom = getBottom();

                if (parentWidth < 0 || parentHeight < 0) {
                    // Get the parent's size if not defined yet
                    View vParent = (View) (this.getParent());
                    parentWidth = vParent.getWidth();
                    parentHeight = vParent.getHeight();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float currRawX = motionEvent.getRawX();
                float currRawY = motionEvent.getRawY();

                // Prevent ScrollView (if exists) to intercept event
                if (view.getParent() != null) {
                    view.getParent().requestDisallowInterceptTouchEvent(true);
                }

                if (dragSpeed != DRAG_NO_LIMIT) {
                    // Checking dragging speed only if it is limited
                    if (Math.abs(currRawX - prevRawX) > dragSpeed || Math.abs(currRawY - prevRawY) > dragSpeed) {
                        // Dragging to fast -> drop the move
                        break;
                    } else {
                        prevRawX = currRawX;
                        prevRawY = currRawY;
                    }
                }

                if (initRawX >= 0 && initRawY >= 0) {
                    if (view == dragResize) {
                        resize(view, motionEvent);
                    } else if (view == dragMove) {
                        move(view, motionEvent);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                // End of the touch.
                // Clear initial position
                initRawX = prevRawX = -1;
                initRawY = prevRawY = -1;

                // Clear the size
                initWidth = -1;
                initHeight = -1;

                // Clear View location
                initLeft = -1;
                initRight = -1;
                initTop = -1;
                initBottom = -1;

                // Launch Position Listener if needed
                if (positionListener != null) {
                    positionListener.onPositionChanged(getCurrentPosition(view), true);
                }

                break;
            default:
                // Do nothing
        }
        return true;
    }


    private void resize(View view, MotionEvent motionEvent) {
        if (initRawX < 0 || initRawY < 0) {
            // Should not happen, but if no initial position is defined no move then
            return;
        }

        ViewGroup.LayoutParams params = this.getLayoutParams();

        int widthAdj = (int) (motionEvent.getRawX() - initRawX);
        int heightAdj = (int) (motionEvent.getRawY() - initRawY);
        int newWidth = initWidth + widthAdj;
        int newHeight = initHeight + heightAdj;

        if (newWidth >= widthMin && (initRight + widthAdj) < parentWidth && ((resizeDir & RESIZE_HORIZ) == RESIZE_HORIZ)) {
            // Store if new width is bigger then minimal and new window do not leap out of parent boundary
            params.width = newWidth;
        }
        if (newHeight >= heightMin && (initBottom + heightAdj) < parentHeight && ((resizeDir & RESIZE_VERT) == RESIZE_VERT)) {
            // Store if new width is bigger then minimal and new window do not leap out of parent boundary
            params.height = newHeight;
        }

        this.setLayoutParams(params);

        // Launch Position Listener if needed
        if (positionListener != null) {
            positionListener.onPositionChanged(getCurrentPosition(view), false);
        }
    }



    private void move(View view, MotionEvent motionEvent) {
        if (initRawX < 0 || initRawY < 0) {
            // Should not happen, but if no initial position is defined no move then
            return;
        }

        // Move adjustment
        int moveHorizontalAdj = (int) (motionEvent.getRawX() - initRawX);
        int moveVerticalAdj = (int) (motionEvent.getRawY() - initRawY);

        ConstraintLayout loutParent = (ConstraintLayout) (view.getParent().getParent());
        ConstraintSet set = new ConstraintSet();
        set.clone(loutParent);

        if (initLeft + moveHorizontalAdj >= 0 && initRight + moveHorizontalAdj < parentWidth && ((moveDir & MOVE_HORIZ) == MOVE_HORIZ)) {
            set.connect(((View)(view.getParent())).getId(), ConstraintSet.START, loutParent.getId(), ConstraintSet.START, initLeft + moveHorizontalAdj);
        }
        if (initTop + moveVerticalAdj >=0 && initBottom + moveVerticalAdj < parentHeight && ((moveDir & MOVE_VERT) == MOVE_VERT)) {
            set.connect(((View)(view.getParent())).getId(), ConstraintSet.TOP, loutParent.getId(), ConstraintSet.TOP, initTop + moveVerticalAdj);
        }

        set.applyTo(loutParent);

        // Launch Position Listener if needed
        if (positionListener != null) {
            positionListener.onPositionChanged(getCurrentPosition(view), false);
        }
    }



    /**
     * Setting Resize Drag icon
     * @return Result of dragging - success or not
     */
    public boolean setDragResize(int resizeDir) {
        Drawable drawable = ResourcesCompat.getDrawable(getResources(), android.R.drawable.ic_menu_crop, null);
        return setDragResize(drawable, ConstraintSet.BOTTOM, ConstraintSet.END, DRAG_ICON_SIZE, resizeDir);
    }

    public boolean setDragResize(int sizePx, int resizeDir) {
        Drawable drawable = ResourcesCompat.getDrawable(getResources(), android.R.drawable.ic_menu_crop, null);
        return setDragResize(drawable, ConstraintSet.BOTTOM, ConstraintSet.END, sizePx, resizeDir);
    }

    public boolean setDragResize(int verticalPos, int horizontalPos, int resizeDir) {
        Drawable drawable = ResourcesCompat.getDrawable(getResources(), android.R.drawable.ic_menu_crop, null);
        return setDragResize(drawable, verticalPos, horizontalPos, DRAG_ICON_SIZE, resizeDir);
    }

    public boolean setDragResize(int verticalPos, int horizontalPos, int sizePx, int resizeDir) {
        Drawable drawable = ResourcesCompat.getDrawable(getResources(), android.R.drawable.ic_menu_crop, null);
        return setDragResize(drawable, verticalPos, horizontalPos, sizePx, resizeDir);
    }

    public boolean setDragResize(Drawable drawable, int resizeDir) {
        return setDragResize(drawable, ConstraintSet.BOTTOM, ConstraintSet.END, DRAG_ICON_SIZE, resizeDir);
    }

    public boolean setDragResize(Drawable drawable, int sizePx, int resizeDir) {
        return setDragResize(drawable, ConstraintSet.BOTTOM, ConstraintSet.END, sizePx, resizeDir);
    }

    public boolean setDragResize(Drawable drawable, int verticalPos, int horizontalPos, int sizePx, int resizeDir) {
        if ((verticalPos != ConstraintSet.TOP && verticalPos != ConstraintSet.BOTTOM) ||
                (horizontalPos != ConstraintSet.START && horizontalPos!= ConstraintSet.END)) {
            // So far accepting only TOP, BOTTOM, START, END
            return false;
        }

        // Store resize direction
        this.resizeDir = resizeDir;

        ConstraintSet set = new ConstraintSet();
        set.clone(this);

        // Set drag views
        if (dragResize == null) {
            dragResize = new View(getContext());
            dragResize.setId(View.generateViewId());
            this.addView(dragResize);
            dragResize.setOnTouchListener(this);
        }
        dragResize.setBackground(drawable);
        set.connect(dragResize.getId(), verticalPos, this.getId(), verticalPos, 0);
        set.connect(dragResize.getId(), horizontalPos, this.getId(), horizontalPos, 0);
        set.constrainWidth(dragResize.getId(), sizePx);
        set.constrainHeight(dragResize.getId(), sizePx);
        set.applyTo(this);
        return true;
    }


    /**
     * Setting Move Drag icon
     * @return Result of dragging - success or not
     */

    public boolean setDragMove(int moveDir) {
        Drawable drawable = ResourcesCompat.getDrawable(getResources(), android.R.drawable.ic_menu_compass, null);
        return setDragMove(drawable, ConstraintSet.TOP, ConstraintSet.START, DRAG_ICON_SIZE, moveDir);
    }

    public boolean setDragMove(int sizePx, int moveDir) {
        Drawable drawable = ResourcesCompat.getDrawable(getResources(), android.R.drawable.ic_menu_compass, null);
        return setDragMove(drawable, ConstraintSet.TOP, ConstraintSet.START, sizePx, moveDir);
    }

    public boolean setDragMove(int verticalPos, int horizontalPos, int moveDir) {
        Drawable drawable = ResourcesCompat.getDrawable(getResources(), android.R.drawable.ic_menu_compass, null);
        return setDragMove(drawable, verticalPos, horizontalPos, DRAG_ICON_SIZE, moveDir);
    }

    public boolean setDragMove(int verticalPos, int horizontalPos, int sizePx, int moveDir) {
        Drawable drawable = ResourcesCompat.getDrawable(getResources(), android.R.drawable.ic_menu_compass, null);
        return setDragMove(drawable, verticalPos, horizontalPos, sizePx, moveDir);
    }

    public boolean setDragMove(Drawable drawable, int moveDir) {
        return setDragMove(drawable, ConstraintSet.TOP, ConstraintSet.START, DRAG_ICON_SIZE, moveDir);
    }

    public boolean setDragMove(Drawable drawable, int sizePx, int moveDir) {
        return setDragMove(drawable, ConstraintSet.TOP, ConstraintSet.START, sizePx, moveDir);
    }

    public boolean setDragMove(Drawable drawable, int verticalPos, int horizontalPos, int sizePx, int moveDir) {
        if ((verticalPos != ConstraintSet.TOP && verticalPos != ConstraintSet.BOTTOM) ||
                (horizontalPos != ConstraintSet.START && horizontalPos!= ConstraintSet.END)) {
            // So far accepting only TOP, BOTTOM, START, END
            return false;
        }


        // Store resize direction
        this.moveDir = moveDir;

        ConstraintSet set = new ConstraintSet();
        set.clone(this);

        set.clone(this);
        if (dragMove == null) {
            dragMove = new View(getContext());
            dragMove.setId(View.generateViewId());
            this.addView(dragMove);
            dragMove.setOnTouchListener(this);
        }
        dragMove.setBackground(drawable);
        set.connect(dragMove.getId(), verticalPos, this.getId(), verticalPos, 0);
        set.connect(dragMove.getId(), horizontalPos, this.getId(), horizontalPos, 0);
        set.constrainWidth(dragMove.getId(), sizePx);
        set.constrainHeight(dragMove.getId(), sizePx);
        set.applyTo(this);
        return true;
    }


    public void setSizeMin(int widthMin, int heightMin) {
        this.widthMin = widthMin;
        this.heightMin = heightMin;
        invalidate();
    }


    public void setSavedSize(Rect rect) {
        int xPos = rect.left;
        int yPos = rect.top;
        int width = ((rect.right < 0) || (rect.left) < 0 ? -1 : rect.right - rect.left);
        int height = ((rect.bottom < 0) || (rect.top) < 0 ? -1 : rect.bottom - rect.top);

        if (xPos >= 0 || yPos >= 0) {
            // Recover layout position
            ConstraintLayout loutParent = (ConstraintLayout) (this.getParent());
            ConstraintSet set = new ConstraintSet();
            set.clone(loutParent);

            if (xPos >= 0) {
                set.connect(this.getId(), ConstraintSet.START, loutParent.getId(), ConstraintSet.START, xPos);
            }
            if (yPos >= 0) {
                set.connect(this.getId(), ConstraintSet.TOP, loutParent.getId(), ConstraintSet.TOP, yPos);
            }
            set.applyTo(loutParent);
        }

        if (width >= 0 || height >= 0) {
            // Recover layout size
            ViewGroup.LayoutParams params = this.getLayoutParams();

            if (width >= 0) {
                params.width = width;
            }
            if (height >= 0) {
                params.height = height;
            }

            this.setLayoutParams(params);
        }
    }

    public void setFrameColor(int color) {
        paintStroke.setColor(color);
        invalidate();
    }


    public void setFillColor(int color) {
        paintFill = new Paint();
        paintFill.setColor(color);
        paintFill.setStyle(Paint.Style.FILL);
    }


    public void setFrameShape(int shape) {
        switch (shape) {
            case FRAME_RECT_ROUND:
            case FRAME_RECT:
            case NO_FRAME:
                frameShape = shape;
                break;
            default:
                break;
        }
        invalidate();
    }


    public void setDraggingSpeed(int speed) {
        if (speed < 0) {
            dragSpeed = DRAG_NO_LIMIT;
        } else {
            dragSpeed = speed;
        }
    }


    private Rect getCurrentPosition(View view) {
        int left = ((View) view.getParent()).getLeft();
        int right = ((View) view.getParent()).getRight();
        int top = ((View) view.getParent()).getTop();
        int bottom = ((View) view.getParent()).getBottom();
        return new Rect(left, top, right, bottom);
    }
}