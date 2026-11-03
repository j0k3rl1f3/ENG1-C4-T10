package ENG1.Game.Managers;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

/** A map feature with a collision area. Lakes also act as goose spawn points. */
public class MapFeature {
    public enum Type { LAKE, TREE, ROCK, BUSH, FLOWERS }
    private final Type type;
    private final Rectangle bounds;
    private float spawnTimer;
    private final ShapeRenderer shapes = new ShapeRenderer();

    public MapFeature(Type type, float x, float y, float width, float height) {
        this.type = type;
        this.bounds = new Rectangle(x, y, width, height);
    }

    public Type getType() { return type; }
    public Rectangle getBounds() { return bounds; }

    /** The player cannot enter any feature, including lakes. */
    public boolean blocksPlayer() { return true; }

    /** Geese can swim through lakes but cannot pass through other features. */
    public boolean blocksGoose() { return type != Type.LAKE; }

    /** Returns true each time a lake's spawn interval elapses. */
    public boolean updateSpawnTimer(float delta, float spawnInterval) {
        if (type != Type.LAKE) return false;
        spawnTimer += delta;
        if (spawnTimer < spawnInterval) return false;
        spawnTimer -= spawnInterval;
        return true;
    }
}
