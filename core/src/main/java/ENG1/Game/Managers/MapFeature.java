package ENG1.Game.Managers;
import com.badlogic.gdx.math.Rectangle;

/** A water region used as a goose spawning area. */
public class MapFeature {
    private final Rectangle bounds;
    private float spawnTimer;
    private boolean hasSpawned;

    /**
     * Creates a map feature with its world-space collision area.
     *
     * @param x left world coordinate
     * @param y bottom world coordinate
     * @param width collision width
     * @param height collision height
     */
    public MapFeature(float x, float y, float width, float height) {
        this.bounds = new Rectangle(x, y, width, height);
    }

    /** @return world-space collision bounds */
    public Rectangle getBounds() { return bounds; }

    /** The player cannot enter any feature, including lakes. */
    public boolean blocksPlayer() { return true; }

    /** Geese can swim through water regions. */
    public boolean blocksGoose() { return false; }

    /**
     * Returns true immediately when an active lake is first checked, then once per spawn interval.
     *
     * @param delta elapsed time in seconds
     * @param spawnInterval seconds between later goose spawns
     * @return whether this lake should spawn a goose this frame
     */
    public boolean updateSpawnTimer(float delta, float spawnInterval) {
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
