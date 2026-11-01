package ENG1.Game.Managers;
import ENG1.Game.Entitys.Player;
import com.badlogic.gdx.math.Rectangle;

public interface Interactable {
    void interact(Player player);
    boolean isCollected();
    Rectangle getBounds();
}
