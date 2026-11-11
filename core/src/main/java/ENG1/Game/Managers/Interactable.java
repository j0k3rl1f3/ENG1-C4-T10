package ENG1.Game.Managers;
import ENG1.Game.Entities.Player;
import com.badlogic.gdx.math.Rectangle;

/** A world object the player can collect or otherwise interact with. */
public interface Interactable {
    /** Performs this object's interaction with the player. @param player player interacting with the object */
    void interact(Player player);
    /** @return whether this object should be removed from the world */
    boolean isCollected();
    /** @return world-space bounds used for interaction detection */
    Rectangle getBounds();
}
