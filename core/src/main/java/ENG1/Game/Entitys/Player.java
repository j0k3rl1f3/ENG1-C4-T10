package ENG1.Game.Entitys;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import ENG1.Game.Managers.Interactable;
import ENG1.Game.Managers.MapFeature;
import com.badlogic.gdx.math.Vector2;

public class Player extends Entity {
    private static final int MAX_HEALTH = 100;
    private int health = MAX_HEALTH;
    private final Array<Interactable> inventory = new Array<>();
    private final Vector2 facing = new Vector2(1, 0);
    private static final float THROW_COOLDOWN = 1.5f;
    private float throwCooldown;
    private static final float MOVE_SPEED = 180f;
    private final Texture texturePlayer = new Texture(Gdx.files.internal("playerdraft.png"));

    public Player(float x, float y, float width, float height) {
        super(x, y, width, height);
    }
    public void render(SpriteBatch batch) {
        batch.draw(texturePlayer, position.x, position.y);
    }

    @Override public void dispose() {
        super.dispose();
        texturePlayer.dispose();
    }
    public float getX() {
        return position.x;
    }
    public float getY() {
        return position.y;
    }


    @Override public void update(float delta) {
        update(delta, null, null);
    }

    /** Moves the player while keeping them outside geese and solid map features. */
    public void update(float delta, Array<Goose> geese, Array<MapFeature> mapFeatures) {
        throwCooldown = Math.max(0, throwCooldown - delta);
        if (health <= 0) return;
        velocity.set(0, 0);
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) velocity.x = -MOVE_SPEED;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) velocity.x = MOVE_SPEED;
        if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) velocity.y = MOVE_SPEED;
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) velocity.y = -MOVE_SPEED;
        if (velocity.len2() > 0) {
            facing.set(velocity).nor();
            velocity.nor().scl(MOVE_SPEED);
        }
        float previousX = position.x;
        position.x += velocity.x * delta;
        getBounds().setX(position.x);
        if (overlapsSolid(geese, mapFeatures)) {
            position.x = previousX;
            getBounds().setX(position.x);
        }

        float previousY = position.y;
        position.y += velocity.y * delta;
        getBounds().setY(position.y);
        if (overlapsSolid(geese, mapFeatures)) {
            position.y = previousY;
            getBounds().setY(position.y);
        }
    }

    public boolean isInvisible() { return false; }

    private boolean overlapsSolid(Array<Goose> geese, Array<MapFeature> mapFeatures) {
        if (geese != null) {
            for (int i = 0; i < geese.size; i++) {
                Goose goose = geese.get(i);
                if (getBounds().overlaps(goose.getBounds())) return true;
            }
        }
        if (mapFeatures != null) {
            for (MapFeature feature : mapFeatures) {
                if (feature.blocksPlayer() && getBounds().overlaps(feature.getBounds())) return true;
            }
        }
        return false;
    }

    public void takeDamage() { takeDamage(1); }

    public void takeDamage(int damage) {
        if (damage > 0) health = Math.max(0, health - damage);
    }

    /** Adds an interactable item to the player's inventory. */
    public boolean addItem(Interactable item) {
        if (item == null || inventory.contains(item, true)) return false;
        inventory.add(item);
        return true;
    }

    /** Removes an item from the inventory. */
    public boolean removeItem(Interactable item) {
        return inventory.removeValue(item, true);
    }

    /** Returns whether the given item is in the inventory. */
    public boolean hasItem(Interactable item) {
        return inventory.contains(item, true);
    }

    /** Returns a copy so callers cannot change the inventory without using its methods. */
    public Array<Interactable> getInventory() {
        return new Array<>(inventory);
    }

    public int getInventorySize() {
        return inventory.size;
    }

    public int getFoodCount() {
        int count = 0;
        for (Interactable item : inventory) if (item instanceof Food) count++;
        return count;
    }

    /** Throws the next food item if one is available and the cooldown has elapsed. */
    public Food throwFood() {
        if (throwCooldown > 0 || !isAlive()) return null;
        for (int i = 0; i < inventory.size; i++) {
            Interactable item = inventory.get(i);
            if (item instanceof Food food) {
                inventory.removeIndex(i);
                food.throwTo(position.x + facing.x * Food.THROW_DISTANCE,
                    position.y + facing.y * Food.THROW_DISTANCE);
                throwCooldown = THROW_COOLDOWN;
                return food;
            }
        }
        return null;
    }

    public float getThrowCooldown() { return throwCooldown; }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = Math.max(0, Math.min(MAX_HEALTH, health));
    }

    public int getMaxHealth() { return MAX_HEALTH; }
    public boolean isAlive() { return health > 0; }
}
