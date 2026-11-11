package ENG1.Game.Managers;
import ENG1.Game.Entities.Goose;
import ENG1.Game.Entities.Player;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.utils.Array;

/** Coordinates game entities and the tiled map for game modes that use a {@link MapManager}. */
public class GameManager {
    private Player player;
    private Array<Goose> geese;
    private MapManager mapManager;
    private AssetManager assetManager;

    /**
     * Creates the map and initial player state using an asset manager that has loaded level one.
     *
     * @param assetManager asset manager containing {@code maps/level1.tmx}
     */
    public GameManager(AssetManager assetManager) {
        this.assetManager = assetManager;
        TiledMap map = assetManager.get("maps/level1.tmx", TiledMap.class);
        this.mapManager = new MapManager(map);
        this.player = new Player(100, 100, 32, 32);
        this.geese = new Array<>();
    }

    /** Updates the player and every managed goose. @param alpha elapsed time in seconds */
    public void update(float alpha) {
        player.update(alpha);
        for (Goose goose : geese) {
            goose.update(alpha);
        }

    }
}
