package ENG1.Game.Entities;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.MathUtils;

/** Base class for movable objects with a world position, velocity, and collision bounds. */
public abstract class Entity {
    protected Vector2 position;
    protected Vector2 velocity;
    private final Rectangle bound;
    private final float width;
    private final float height;

    /**
     * Creates an entity at the supplied world position.
     *
     * @param x left edge of the entity in world units
     * @param y bottom edge of the entity in world units
     * @param width collision width in world units
     * @param height collision height in world units
     */
    public Entity(float x, float y, float width, float height) {
        this.position = new Vector2(x,y);
        this.velocity = new Vector2(0,0);
        this.bound = new Rectangle(x, y, width, height);
        this.width = width;
        this.height = height;
    }

    /** Updates this entity for one rendered frame. @param alpha elapsed time in seconds */
    public abstract void update(float alpha);

    /** Draws this entity. @param batch active sprite batch used for rendering */
    public abstract void render(SpriteBatch batch);

    /** @return the collision rectangle, kept in sync with the entity position */
    public Rectangle getBounds(){
        return bound;
    }

    /** @return the rectangles width */
    public float getWidth() { return width; }

    /** @return the rectangles height */
    public float getHeight() { return height; }

    /** Releases resources owned by this entity. Subclasses override when needed. */
    public void dispose() { }

    /** Keeps this entity's collision bounds entirely within the map. */
    public void keepInsideMap(float mapWidth, float mapHeight) {
        position.x = MathUtils.clamp(position.x, 0, Math.max(0, mapWidth - bound.width));
        position.y = MathUtils.clamp(position.y, 0, Math.max(0, mapHeight - bound.height));
        bound.setPosition(position);
    }
}
