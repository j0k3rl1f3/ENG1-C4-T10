package ENG1.Game.Entitys;

import ENG1.Game.Managers.Interactable;
import com.badlogic.gdx.math.Rectangle;

/** A map pickup that permanently unlocks the player's food-sprinkler ability. */
public class FoodPickup implements Interactable {
    private final Rectangle bounds;
    private boolean collected;

    /** @param x left edge of the pickup in world units @param y bottom edge of the pickup in world units */
    public FoodPickup(float x, float y) {
        bounds = new Rectangle(x, y, 16, 16);
    }

    /** Unlocks the player's food-sprinkler ability the first time it is collected. */
    @Override
    public void interact(Player player) {
        if (!collected) {
            player.unlockFoodSprinkler();
            collected = true;
        }
    }

    /** @return whether this pickup has already been collected */
    @Override
    public boolean isCollected() {
        return collected;
    }

    /** @return collision bounds used to detect collection */
    @Override
    public Rectangle getBounds() {
        return bounds;
    }
}
