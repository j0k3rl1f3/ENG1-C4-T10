package ENG1.Game.Entities;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import ENG1.Game.Managers.Interactable;
import ENG1.Game.Managers.MapFeature;
import ENG1.Game.Managers.MapManager;
import com.badlogic.gdx.math.Vector2;

/** The user-controlled character, including movement, health, inventory, and food-sprinkler state. */
public class Player extends Entity {
    private static final int MAX_HEALTH = 100;
    private int health = MAX_HEALTH;
    private final Array<Interactable> inventory = new Array<>();
    private final Vector2 facing = new Vector2(1, 0);
    private static final float FOOD_COOLDOWN = 3f;
    private boolean hasFoodSprinkler;
    private float foodCooldown;
    private static float MOVE_SPEED = 64f;
    private static final float DRAW_SIZE = 64f;
    private enum Direction {DOWN, UP, RIGHT, LEFT}
    private static final float IDLE_FRAME_DURATION = 0.35f;
    private static final float WALK_FRAME_DURATION = 0.12f;
    private final Texture playerSheet;
    private final TextureRegion[] playerFrames;
    private final Animation<TextureRegion> idleDown;
    private final Animation<TextureRegion> idleUp;
    private final Animation<TextureRegion> idleRight;
    private final Animation<TextureRegion> idleLeft;
    private final Animation<TextureRegion> walkDown;
    private final Animation<TextureRegion> walkUp;
    private final Animation<TextureRegion> walkRight;
    private final Animation<TextureRegion> walkLeft;
    private Direction direction = Direction.RIGHT;
    private boolean moving;
    private float animationTime;

    /**
     * Creates a player at the supplied world position with the supplied collision size.
     */
    public Player(float x, float y, float width, float height) {
        super(x, y, width, height);
        playerSheet = new Texture(
            Gdx.files.internal("PlayerSprtepack/playerSprite-.png")
        );
        playerFrames = TextureRegion.split(playerSheet, 32, 32)[0];
        idleDown = makeAnimation(0, 3, IDLE_FRAME_DURATION);
        idleUp = makeAnimation(3, 3, IDLE_FRAME_DURATION);
        idleRight = makeAnimation(6, 3, IDLE_FRAME_DURATION);
        idleLeft = makeAnimation(9, 3, IDLE_FRAME_DURATION);
        walkDown = makeAnimation(12, 4, WALK_FRAME_DURATION);
        walkUp = makeAnimation(16, 4, WALK_FRAME_DURATION);
        walkRight = makeAnimation(20, 4, WALK_FRAME_DURATION);
        walkLeft = makeAnimation(24, 4, WALK_FRAME_DURATION);
    }

    /**
     * Draws the player at its current world position.
     */
    @Override
    public void render(SpriteBatch batch) {
        TextureRegion frame = getCurrentAnimation().getKeyFrame(animationTime, true);
        batch.draw(frame, position.x  - (0.25f * DRAW_SIZE), position.y  - (0.25f * DRAW_SIZE), DRAW_SIZE, DRAW_SIZE);
    }
    @Override
    public void dispose() {
        super.dispose();
        playerSheet.dispose();
    }

    /**
     * @return player's left world coordinate
     */
    public float getX() {
        return position.x;
    }

    /**
     * @return player's bottom world coordinate
     */
    public float getY() {
        return position.y;
    }


    @Override
    public void update(float delta) {
        update(delta, null, null);
    }

    /**
     * Reads movement input and moves the player while resolving collisions.
     *
     * @param delta       elapsed time in seconds
     * @param geese       geese which the player cannot move through
     * @param mapFeatures map features which the player cannot move through
     */
    public void update(float delta, Array<Goose> geese, Array<MapFeature> mapFeatures) {
        update(delta, geese, mapFeatures, null);
    }

    /**
     * Reads movement input and moves the player while resolving entity and Tiled-map collisions.
     *
     * @param delta elapsed time in seconds
     * @param geese geese which the player cannot move through
     * @param mapFeatures water regions used for goose behaviour
     * @param mapManager Tiled map queries for collisions and muddy terrain
     */
    public void update(float delta, Array<Goose> geese, Array<MapFeature> mapFeatures, MapManager mapManager) {
        foodCooldown = Math.max(0, foodCooldown - delta);
        if (health <= 0) return;
        velocity.set(0, 0);
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) velocity.x = -MOVE_SPEED;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) velocity.x = MOVE_SPEED;
        if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) velocity.y = MOVE_SPEED;
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) velocity.y = -MOVE_SPEED;
        animationTime += delta;
        moving = velocity.len2() > 0;
        if (moving) {
            updateDirection(velocity);
            facing.set(velocity).nor();
            float speedMultiplier = mapManager != null && mapManager.isMuddy(getBounds()) ? 0.45f : 1f;
            velocity.nor().scl(MOVE_SPEED * speedMultiplier);
        }
        float previousX = position.x;
        position.x += velocity.x * delta;
        getBounds().setX(position.x);
        if (overlapsSolid(geese, mapFeatures, mapManager)) {
            position.x = previousX;
            getBounds().setX(position.x);
        }

        float previousY = position.y;
        position.y += velocity.y * delta;
        getBounds().setY(position.y);
        if (overlapsSolid(geese, mapFeatures, mapManager)) {
            position.y = previousY;
            getBounds().setY(position.y);
        }
    }
    /** for future invis cloak
    public boolean isInvisible() {
        return false;
    } **/

    private boolean overlapsSolid(Array<Goose> geese, Array<MapFeature> mapFeatures, MapManager mapManager) {
        if (geese != null) {
            for (int i = 0; i < geese.size; i++) {
                Goose goose = geese.get(i);
                if (getBounds().overlaps(goose.getBounds())) return true;
            }
        }
        // With a Tiled map, use its exact painted collision cells. MapFeature lake rectangles
        // are retained only for goose spawning and may cover grass inside an irregular shoreline.
        if (mapFeatures != null && mapManager == null) {
            for (MapFeature feature : mapFeatures) {
                if (feature.blocksPlayer() && getBounds().overlaps(feature.getBounds())) return true;
            }
        }
        if (mapManager != null && mapManager.checkCollision(getBounds())) return true;
        return false;
    }

    public void takeDamage() {
        takeDamage(1);
    }

    /**
     * Applies positive damage without allowing health to fall below zero. @param damage health to remove
     */
    public void takeDamage(int damage) {
        if (damage > 0) health = Math.max(0, health - damage);
    }

    /**
     * Adds an interactable item to the player's inventory.
     */
    public boolean addItem(Interactable item) {
        if (item == null || inventory.contains(item, true)) return false;
        inventory.add(item);
        return true;
    }

    /**
     * Removes an item from the inventory.
     */
    public boolean removeItem(Interactable item) {
        return inventory.removeValue(item, true);
    }

    /**
     * Returns whether the given item is in the inventory.
     */
    public boolean hasItem(Interactable item) {
        return inventory.contains(item, true);
    }

    /**
     * Returns a copy so callers cannot change the inventory without using its methods.
     */
    public Array<Interactable> getInventory() {
        return new Array<>(inventory);
    }

    public int getInventorySize() {
        return inventory.size;
    }

    /** Unlocks unlimited food sprinkles for the remainder of this game. */
    /**
     * Enables unlimited food sprinkling for the rest of the current game.
     */
    public void unlockFoodSprinkler() {
        hasFoodSprinkler = true;
    }

    public boolean hasFoodSprinkler() {
        return hasFoodSprinkler;
    }

    /**
     * @return whether the ability is unlocked, off cooldown, and the player is alive
     */
    public boolean canSprinkleFood() {
        return hasFoodSprinkler && foodCooldown <= 0 && isAlive();
    }

    public static void collectBike() {
        MOVE_SPEED += 5f;
    }
    /**
     * Starts the three-second cooldown after a food sprinkle is created.
     */
    public void startFoodCooldown() {
        foodCooldown = FOOD_COOLDOWN;
    }

    public float getFoodCooldown() {
        return foodCooldown;
    }

    public Vector2 getFacing() {
        return facing;
    }

    public int getHealth() {
        return health;
    }

    /**
     * Sets health after constraining it to the valid range. @param health requested health value
     */
    public void setHealth(int health) {
        this.health = Math.max(0, Math.min(MAX_HEALTH, health));
    }

    public int getMaxHealth() {
        return MAX_HEALTH;
    }

    public boolean isAlive() {
        return health > 0;
    }

    private Animation<TextureRegion> makeAnimation(
        int firstFrame, int frameCount, float frameDuration
    ) {
        TextureRegion[] frames = new TextureRegion[frameCount];
        for (int i = 0; i < frameCount; i++) {
            frames[i] = playerFrames[firstFrame + i];
        }
        return new Animation<>(frameDuration, frames);
    }

    private void updateDirection(Vector2 movement) {
        if (Math.abs(movement.x) > Math.abs(movement.y)) {
            direction = movement.x > 0 ? Direction.RIGHT : Direction.LEFT;
        } else {
            direction = movement.y > 0 ? Direction.UP : Direction.DOWN;
        }
    }

    private Animation<TextureRegion> getCurrentAnimation() {
        if (moving) {
            switch (direction) {
                case UP:
                    return walkUp;
                case RIGHT:
                    return walkRight;
                case LEFT:
                    return walkLeft;
                default:
                    return walkDown;
            }
        }

        switch (direction) {
            case UP:
                return idleUp;
            case RIGHT:
                return idleRight;
            case LEFT:
                return idleLeft;
            default:
                return idleDown;
        }
    }
}
