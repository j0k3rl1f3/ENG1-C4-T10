package ENG1.Game.Managers;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.utils.Array;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.GridPoint2;

/** Loads, renders, and queries collision data for a tiled map. */
public class MapManager {
    // Tiled culls by cell size, while some artwork extends well beyond its 32×32 cell.
    private static final float RENDER_CULLING_PADDING = 160f;
    private TiledMap map;
    private OrthogonalTiledMapRenderer renderer;
    private Array<Interactable> interactableArray;


    /** Loads a TMX map from the supplied internal asset path. @param mapPath path to the TMX file */
    public MapManager(String mapPath) {
        this(new TmxMapLoader().load(mapPath));
    }

    /** Creates a manager around an already-loaded tiled map. @param map tiled map to manage */
    public MapManager(TiledMap map) {
        this.map = map;
        renderer = new OrthogonalTiledMapRenderer(map);
        interactableArray = new Array<>();
    }
    /** Renders the map with enough culling margin for oversized tree and building artwork. @param camera world camera */
    public void render(OrthographicCamera camera){
        float viewWidth = camera.viewportWidth * camera.zoom;
        float viewHeight = camera.viewportHeight * camera.zoom;
        renderer.setView(camera.combined,
            camera.position.x - viewWidth / 2f - RENDER_CULLING_PADDING,
            camera.position.y - viewHeight / 2f - RENDER_CULLING_PADDING,
            viewWidth + RENDER_CULLING_PADDING * 2f,
            viewHeight + RENDER_CULLING_PADDING * 2f);
        renderer.render();
    }
    /** @return world interactables registered with this map */
    public Array<Interactable> getInteractableArray() {
        return interactableArray;
    }

    /**
     * Checks whether a rectangle overlaps a tile marked {@code blocked} on the collision layer.
     *
     * @param rect world-space area to test
     * @return true if the area overlaps a blocked tile
     */
    public boolean checkCollision(Rectangle rect){
        return overlapsLayer("Collisions", rect) || overlapsBuildingLayer(rect)
            || overlapsLayer("Water", rect) || overlapsTreeLayer(rect);
    }

    /** @return true if a goose overlaps a solid Tiled layer; water remains traversable to geese */
    public boolean checkGooseCollision(Rectangle rect) {
        return overlapsLayer("Collisions", rect) || overlapsBuildingLayer(rect)
            || overlapsTreeLayer(rect);
    }

    /** @return true if a rectangle overlaps at least one painted tile in the named layer */
    public boolean overlapsLayer(String layerName, Rectangle rect) {
        TiledMapTileLayer collisionLayer = getTileLayer(layerName);
        if (collisionLayer == null) return false;
        float tileWidth = collisionLayer.getTileWidth();
        float tileHeight = collisionLayer.getTileHeight();
        int minX = (int) Math.floor(rect.x / tileWidth);
        int maxX = (int) Math.floor((rect.x + rect.width - 0.001f) / tileWidth);
        int minY = (int) Math.floor(rect.y / tileHeight);
        int maxY = (int) Math.floor((rect.y + rect.height - 0.001f) / tileHeight);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                if (collisionLayer.getCell(x, y) != null) return true;
            }
        }
        return false;
    }

    /**
     * Tests the full visible footprint of tiles such as trees, whose sprites are larger than a map cell.
     *
     * @param layerName tile layer to inspect
     * @param rect entity collision bounds
     * @return true when the entity overlaps a painted tile's rendered area
     */
    public boolean overlapsVisualLayer(String layerName, Rectangle rect) {
        return overlapsVisualLayer(layerName, rect, Float.MAX_VALUE, Float.MAX_VALUE);
    }

    /** Checks tree sprites using a 64×64 collision area rather than their full 64×80 artwork. */
    private boolean overlapsTreeLayer(Rectangle rect) {
        return overlapsVisualLayer("Trees", rect, 64f, 64f);
    }

    /** Uses each building's full width but removes 80 pixels from its artwork height for collision. */
    private boolean overlapsBuildingLayer(Rectangle rect) {
        TiledMapTileLayer layer = getTileLayer("Buildings");
        if (layer == null) return false;

        float cellWidth = layer.getTileWidth();
        float cellHeight = layer.getTileHeight();
        for (int x = 0; x < layer.getWidth(); x++) {
            for (int y = 0; y < layer.getHeight(); y++) {
                TiledMapTileLayer.Cell cell = layer.getCell(x, y);
                if (cell == null) continue;
                TextureRegion region = cell.getTile().getTextureRegion();
                float collisionHeight = Math.max(1f, region.getRegionHeight() - 80f);
                Rectangle collisionBounds = new Rectangle(x * cellWidth, y * cellHeight,
                    region.getRegionWidth(), collisionHeight);
                if (rect.overlaps(collisionBounds)) return true;
            }
        }
        return false;
    }

    private boolean overlapsVisualLayer(String layerName, Rectangle rect, float maxWidth, float maxHeight) {
        TiledMapTileLayer layer = getTileLayer(layerName);
        if (layer == null) return false;

        float cellWidth = layer.getTileWidth();
        float cellHeight = layer.getTileHeight();
        for (int x = 0; x < layer.getWidth(); x++) {
            for (int y = 0; y < layer.getHeight(); y++) {
                TiledMapTileLayer.Cell cell = layer.getCell(x, y);
                if (cell == null) continue;
                TextureRegion region = cell.getTile().getTextureRegion();
                Rectangle visualBounds = new Rectangle(x * cellWidth, y * cellHeight,
                    Math.min(region.getRegionWidth(), maxWidth), Math.min(region.getRegionHeight(), maxHeight));
                if (rect.overlaps(visualBounds)) return true;
            }
        }
        return false;
    }

    /** @return true if the supplied bounds overlap the painted {@code Muddy} layer */
    public boolean isMuddy(Rectangle rect) {
        return overlapsLayer("Muddy", rect);
    }

    /** Draws red-outline equivalents of every map area that blocks the player. */
    public void renderCollisionDebug(ShapeRenderer shapes) {
        renderTileLayerBounds(shapes, "Collisions");
        renderTileLayerBounds(shapes, "Water");
        renderVisualLayerBounds(shapes, "Trees", 54f, 64f);
        renderBuildingCollisionBounds(shapes);
    }

    private void renderTileLayerBounds(ShapeRenderer shapes, String layerName) {
        TiledMapTileLayer layer = getTileLayer(layerName);
        if (layer == null) return;
        for (int x = 0; x < layer.getWidth(); x++) {
            for (int y = 0; y < layer.getHeight(); y++) {
                if (layer.getCell(x, y) != null) {
                    shapes.rect(x * layer.getTileWidth(), y * layer.getTileHeight(),
                        layer.getTileWidth(), layer.getTileHeight());
                }
            }
        }
    }

    private void renderVisualLayerBounds(ShapeRenderer shapes, String layerName, float maxWidth, float maxHeight) {
        TiledMapTileLayer layer = getTileLayer(layerName);
        if (layer == null) return;
        for (int x = 0; x < layer.getWidth(); x++) {
            for (int y = 0; y < layer.getHeight(); y++) {
                TiledMapTileLayer.Cell cell = layer.getCell(x, y);
                if (cell == null) continue;
                TextureRegion region = cell.getTile().getTextureRegion();
                shapes.rect(x * layer.getTileWidth(), y * layer.getTileHeight(),
                    Math.min(region.getRegionWidth(), maxWidth), Math.min(region.getRegionHeight(), maxHeight));
            }
        }
    }

    private void renderBuildingCollisionBounds(ShapeRenderer shapes) {
        TiledMapTileLayer layer = getTileLayer("Buildings");
        if (layer == null) return;
        for (int x = 0; x < layer.getWidth(); x++) {
            for (int y = 0; y < layer.getHeight(); y++) {
                TiledMapTileLayer.Cell cell = layer.getCell(x, y);
                if (cell == null) continue;
                TextureRegion region = cell.getTile().getTextureRegion();
                shapes.rect(x * layer.getTileWidth(), y * layer.getTileHeight(), region.getRegionWidth(),
                    Math.max(1f, region.getRegionHeight() - 80f));
            }
        }
    }

    /** Releases renderer and map resources. */
    public void dispose() { renderer.dispose(); map.dispose(); }

    /**
     * Converts each connected group of painted water tiles into one lake region for goose spawning.
     *
     * @return rectangular lake regions based on the Tiled {@code Water} layer
     */
    public Array<MapFeature> getWaterRegions() {
        Array<MapFeature> lakes = new Array<>();
        TiledMapTileLayer waterLayer = getTileLayer("Water");
        if (waterLayer == null) return lakes;

        int width = waterLayer.getWidth();
        int height = waterLayer.getHeight();
        boolean[][] visited = new boolean[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (visited[x][y] || waterLayer.getCell(x, y) == null) continue;
                lakes.add(createWaterRegion(waterLayer, visited, x, y));
            }
        }
        return lakes;
    }

    private MapFeature createWaterRegion(TiledMapTileLayer layer, boolean[][] visited, int startX, int startY) {
        Array<GridPoint2> open = new Array<>();
        open.add(new GridPoint2(startX, startY));
        visited[startX][startY] = true;
        int minX = startX, maxX = startX, minY = startY, maxY = startY;

        while (open.notEmpty()) {
            GridPoint2 cell = open.pop();
            minX = Math.min(minX, cell.x);
            maxX = Math.max(maxX, cell.x);
            minY = Math.min(minY, cell.y);
            maxY = Math.max(maxY, cell.y);
            addWaterNeighbour(layer, visited, open, cell.x + 1, cell.y);
            addWaterNeighbour(layer, visited, open, cell.x - 1, cell.y);
            addWaterNeighbour(layer, visited, open, cell.x, cell.y + 1);
            addWaterNeighbour(layer, visited, open, cell.x, cell.y - 1);
        }

        float tileWidth = layer.getTileWidth();
        float tileHeight = layer.getTileHeight();
        return new MapFeature(minX * tileWidth, minY * tileHeight,
            (maxX - minX + 1) * tileWidth, (maxY - minY + 1) * tileHeight);
    }

    private void addWaterNeighbour(TiledMapTileLayer layer, boolean[][] visited, Array<GridPoint2> open,
                                   int x, int y) {
        if (x < 0 || y < 0 || x >= layer.getWidth() || y >= layer.getHeight()
            || visited[x][y] || layer.getCell(x, y) == null) return;
        visited[x][y] = true;
        open.add(new GridPoint2(x, y));
    }

    private TiledMapTileLayer getTileLayer(String name) {
        return map.getLayers().get(name) instanceof TiledMapTileLayer
            ? (TiledMapTileLayer) map.getLayers().get(name) : null;
    }

}
