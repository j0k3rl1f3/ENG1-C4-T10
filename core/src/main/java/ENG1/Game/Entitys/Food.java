package ENG1.Game.Entitys;

import ENG1.Game.Managers.Interactable;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/** A collectible food item that can be thrown to distract a goose. */
public class Food implements Interactable {
    public static final float DISTRACTION_DURATION = 4f;
    public static final float THROW_DISTANCE = 120f;

    private final Vector2 position;
    private final Rectangle bounds;
    private boolean collected;
    private float distractionTimer;

    public Food(float x, float y) {
        position = new Vector2(x, y);
        bounds = new Rectangle(x, y, 16, 16);
    }

    @Override
    public void interact(Player player) {
        if (!collected && player.addItem(this)) collected = true;
    }

    @Override
    public boolean isCollected() { return collected; }

    @Override
    public Rectangle getBounds() { return bounds; }

    public Vector2 getPosition() { return position; }

    public void throwTo(float x, float y) {
        position.set(x, y);
        bounds.setPosition(position);
        distractionTimer = DISTRACTION_DURATION;
    }

    public void update(float delta) {
        distractionTimer = Math.max(0, distractionTimer - delta);
    }

    public boolean isDistracting() { return distractionTimer > 0; }
}
