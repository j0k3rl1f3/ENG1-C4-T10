package ENG1.Game.Managers;
import ENG1.Game.Entitys.Goose;
import ENG1.Game.Entitys.Player;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.utils.Array;

public class GameManager {
    private Player player;
    private Array<Goose> geese;
    private MapManager mapManager;
    private AssetManager assetManager;

    public GameManager(AssetManager assetManager) {
        this.assetManager = assetManager;
        TiledMap map = assetManager.get("maps/level1.tmx", TiledMap.class);
        this.mapManager = new MapManager(map);
        this.player = new Player(100, 100, 32, 32);
        this.geese = new Array<>();
    }

    public void update(float alpha) {
        player.update(alpha);
        for (Goose goose : geese) {
            goose.update(alpha);
        }

    }
}
