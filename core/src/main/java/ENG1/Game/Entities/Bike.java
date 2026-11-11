package ENG1.Game.Entities;

import ENG1.Game.Managers.Interactable;
import com.badlogic.gdx.math.Rectangle;

public class Bike implements Interactable{
    private final Rectangle bounds;
    private boolean collected;

    public Bike(float x, float y) {
        bounds = new Rectangle(x, y, 16, 16);
    }

    @Override
    public void interact(Player player) {
        if (!collected) {
            Player.collectBike();
            collected = true;
        }
    }
    /** @return collision bounds used to detect collection */
    @Override
    public Rectangle getBounds() {
        return bounds;
    }

    /** @return whether this pickup has already been collected */
    @Override
    public boolean isCollected() {
        return collected;
    }

}

