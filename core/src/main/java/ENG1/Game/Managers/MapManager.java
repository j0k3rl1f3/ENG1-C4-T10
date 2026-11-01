package ENG1.Game.Managers;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.utils.Array;

import com.badlogic.gdx.math.Rectangle;

public class MapManager {
    private TiledMap map;
    private OrthogonalTiledMapRenderer renderer;
    private Array<Interactable> interactableArray;


    public MapManager(String mapPath) {
        this(new TmxMapLoader().load(mapPath));
    }

    public MapManager(TiledMap map) {
        this.map = map;
        renderer = new OrthogonalTiledMapRenderer(map);
        interactableArray = new Array<>();
    }
    public void render(OrthographicCamera camera){
        renderer.setView(camera);
        renderer.render();
    }
    public Array<Interactable> getInteractableArray() {
        return interactableArray;
    }

    public boolean checkCollision(Rectangle rect){
        TiledMapTileLayer collisionLayer = (TiledMapTileLayer) map.getLayers().get("CollisionLayer");
        if (collisionLayer == null) return false;
        float tileWidth = collisionLayer.getTileWidth();
        float tileHeight = collisionLayer.getTileHeight();
        int minX = (int) Math.floor(rect.x / tileWidth);
        int maxX = (int) Math.floor((rect.x + rect.width - 0.001f) / tileWidth);
        int minY = (int) Math.floor(rect.y / tileHeight);
        int maxY = (int) Math.floor((rect.y + rect.height - 0.001f) / tileHeight);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                if (isCellBlocked(collisionLayer, x, y)) return true;
            }
        }
        return false;
    }

    public void dispose() { renderer.dispose(); map.dispose(); }
    private boolean isCellBlocked(TiledMapTileLayer layer, int cellX, int cellY) {
        TiledMapTileLayer.Cell cell = layer.getCell(cellX, cellY);
        return cell != null && cell.getTile().getProperties().containsKey("blocked");
    }
}
