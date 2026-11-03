package ENG1.Game.Managers;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

/** A map feature with a collision area. Lakes also act as goose spawn points. */
public class MapFeature {
    /** Categories of scenery supported by the current map. */
    public enum Type { LAKE, TREE, ROCK, BUSH, FLOWERS }
    private final Type type;
    private final Rectangle bounds;
    private float spawnTimer;
    private boolean hasSpawned;
    private final ShapeRenderer shapes = new ShapeRenderer();

    /**
     * Creates a map feature with its world-space collision area.
     *
     * @param type feature category
     * @param x left world coordinate
     * @param y bottom world coordinate
     * @param width collision width
     * @param height collision height
     */
    public MapFeature(Type type, float x, float y, float width, float height) {
        this.type = type;
        this.bounds = new Rectangle(x, y, width, height);
    }

    /** @return this feature's category */
    public Type getType() { return type; }
    /** @return world-space collision bounds */
    public Rectangle getBounds() { return bounds; }

    /** The player cannot enter any feature, including lakes. */
    public boolean blocksPlayer() { return true; }

    /** Geese can swim through lakes but cannot pass through other features. */
    public boolean blocksGoose() { return type != Type.LAKE; }

    /**
     * Returns true immediately when an active lake is first checked, then once per spawn interval.
     *
     * @param delta elapsed time in seconds
     * @param spawnInterval seconds between later goose spawns
     * @return whether this lake should spawn a goose this frame
     */
    public boolean updateSpawnTimer(float delta, float spawnInterval) {
        if (type != Type.LAKE) return false;
        if (!hasSpawned) {
            hasSpawned = true;
            return true;
        }
        spawnTimer += delta;
        if (spawnTimer < spawnInterval) return false;
        spawnTimer -= spawnInterval;
        return true;
    }
}
