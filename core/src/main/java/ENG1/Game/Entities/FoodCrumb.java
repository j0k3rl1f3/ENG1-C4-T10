package ENG1.Game.Entities;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/** A short-lived, small piece of food which distracts nearby geese. */
public class FoodCrumb {
    public static final float DISTRACTION_DURATION = 4f;
    public static final float SIZE = 2f;

    private final Vector2 position;
    private final Rectangle bounds;
    private float distractionTimer = DISTRACTION_DURATION;

    /** @param x left edge of this crumb in world units @param y bottom edge of this crumb in world units */
    public FoodCrumb(float x, float y) {
        position = new Vector2(x, y);
        bounds = new Rectangle(x, y, SIZE, SIZE);
    }

    /** Decreases the time for which this crumb remains attractive to geese. @param delta elapsed time in seconds */
    public void update(float delta) {
        distractionTimer = Math.max(0, distractionTimer - delta);
    }

    /** @return true while geese should still be distracted by this crumb */
    public boolean isActive() {
        return distractionTimer > 0;
    }

    /** @return world position of this crumb */
    public Vector2 getPosition() {
        return position;
    }

    /** @return render and collision bounds for this crumb */
    public Rectangle getBounds() {
        return bounds;
    }
}
