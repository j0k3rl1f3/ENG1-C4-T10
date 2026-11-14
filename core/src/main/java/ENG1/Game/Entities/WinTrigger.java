package ENG1.Game.Entities;

import ENG1.Game.Managers.Interactable;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;

public class WinTrigger implements Interactable{
    private final Rectangle bounds;
    private boolean collected;
    private final Texture texture =
        new Texture(Gdx.files.internal("original assets/winTrigger.png"));

    public void render(SpriteBatch batch) {
        batch.draw(texture, bounds.x, bounds.y, 32, 32);

    }public WinTrigger(float x, float y) {
        bounds = new Rectangle(x, y, 32, 32);
    }

    public void dispose() {
        texture.dispose();
    }
    @Override
    public void interact(Player player) {
        if (!collected) {
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
