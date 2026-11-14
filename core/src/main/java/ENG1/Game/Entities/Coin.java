package ENG1.Game.Entities;

import ENG1.Game.Managers.Interactable;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

public class Coin implements Interactable{
    private final Rectangle bounds;
    private boolean collected;


    public Coin(float x, float y) {
        bounds = new Rectangle(x, y, 16, 16);
    }

    @Override
    public void interact(Player player) {
        if (!collected) {
            Player.collectCoin();
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

    /** Draws this coin with the shared coin texture owned by the game screen. */
    public void render(SpriteBatch batch, Texture coinIcon) {
        batch.draw(coinIcon, bounds.x, bounds.y, bounds.width, bounds.height);
    }

}
