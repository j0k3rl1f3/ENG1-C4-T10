package ENG1.Game.Entitys;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.MathUtils;

public abstract class Entity {
    protected Vector2 position;
    protected Vector2 velocity;
    private final Rectangle bound;

    public Entity(float x, float y, float width, float height){
        this.position = new Vector2(x,y);
        this.velocity = new Vector2(0,0);
        this.bound = new Rectangle(x, y, width, height);
    }

    public abstract void update(float alpha);

    public abstract void render(SpriteBatch batch);
    public Rectangle getBounds(){
        return bound;
    }

    public void dispose() { }

    /** Keeps this entity's collision bounds entirely within the map. */
    public void keepInsideMap(float mapWidth, float mapHeight) {
        position.x = MathUtils.clamp(position.x, 0, Math.max(0, mapWidth - bound.width));
        position.y = MathUtils.clamp(position.y, 0, Math.max(0, mapHeight - bound.height));
        bound.setPosition(position);
    }
}
