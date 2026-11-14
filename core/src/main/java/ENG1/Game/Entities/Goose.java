package ENG1.Game.Entities;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import ENG1.Game.Managers.MapFeature;
import ENG1.Game.Managers.MapManager;

/** An enemy goose that chases the player, or the nearest active food crumb. */
public class Goose extends Entity{
    private static final float SPEED = 45f;
    private static final float DRAW_SIZE = 64f;
    private static final float ATTACK_COOLDOWN = 1f;
    private static final int DAMAGE = 5;
    private final Texture idleSheet = new Texture(Gdx.files.internal("Goose/Idle.png"));
    private final Texture runSheet = new Texture(Gdx.files.internal("Goose/Run.png"));
    private final Texture flapSheet = new Texture(Gdx.files.internal("Goose/Flap.png"));
    private final Animation<TextureRegion> idleAnimation =
        new Animation<>(0.4f, TextureRegion.split(idleSheet, 64, 64)[0]);
    private final Animation<TextureRegion> runAnimation =
        new Animation<>(0.12f, TextureRegion.split(runSheet, 64, 64)[0]);
    private final Animation<TextureRegion> flapAnimation =
        new Animation<>(0.08f, TextureRegion.split(flapSheet, 64, 64)[0]);
    private float animationTime;
    private float flapTime;
    private float attackTimer;
    private float offCameraTimer;
    private final Vector2 movement = new Vector2();


    /** Creates a goose and its collision bounds. */
    public Goose(float x, float y, float width, float height) {
        super(x, y, width, height);
    }

    public boolean isTouchingPlayer(Player player) {
        return getBounds().overlaps(player.getPlayerHitBox());
    }

    /** Draws the appropriate idle, running, or attack animation frame. */
    @Override
    public void render(SpriteBatch batch) {
        Animation<TextureRegion> current;
        if (flapTime > 0) {
            current = flapAnimation;
        } else if (movement.len2() > 0.01f) {
            current = runAnimation;
        } else {
            current = idleAnimation;
        }
        TextureRegion frame = current.getKeyFrame(
            animationTime,
            current != flapAnimation
        );
        batch.draw(frame, position.x - (0.5f * DRAW_SIZE), position.y  - (0.5f * DRAW_SIZE), DRAW_SIZE, DRAW_SIZE);
    }

    /** Releases the textures used by this goose's animations. */
    @Override public void dispose() {
        super.dispose();
        idleSheet.dispose();
        runSheet.dispose();
        flapSheet.dispose();
    }

    /** Updates a goose with no food crumbs or world collision data. */
    public void update(float delta, Player player) {
        update(delta, player, null, null, null);
    }

    /**
     * Moves and attacks the player, unless a nearby food crumb takes priority.
     *
     * @param delta elapsed time in seconds
     * @param player player to chase and attack
     * @param crumbs active distractions in the world
     * @param geese all geese, used to prevent overlap
     * @param mapFeatures solid world features which block movement
     */
    public void update(float delta, Player player, Array<FoodCrumb> crumbs, Array<Goose> geese,
                       Array<MapFeature> mapFeatures) {
        update(delta, player, crumbs, geese, mapFeatures, null);
    }

    /**
     * Moves and attacks while resolving collisions against entities and the Tiled map.
     *
     * @param mapManager Tiled map collision queries; water is intentionally passable to geese
     */
    public void update(float delta, Player player, Array<FoodCrumb> crumbs, Array<Goose> geese,
                       Array<MapFeature> mapFeatures, MapManager mapManager) {
        animationTime += delta;
        flapTime = Math.max(0, flapTime - delta);
        if (player == null || !player.isAlive()) return;

        FoodCrumb crumb = findClosestCrumb(crumbs);
        boolean distracted = crumb != null;
        Vector2 target = new Vector2();
        if (distracted) {
            target = crumb.getPosition();
        } else {
            // Make sure goose is targeted to the centre of the player
            target.x = Math.round(player.position.x + (0.5f * player.getWidth()));
            target.y = Math.round(player.position.y + (0.5f * player.getHeight()));
        }

        movement.set(target).sub(position);
        float distance = movement.len();
        boolean touchingPlayer = isTouchingPlayer(player);

        if (distance > 1f) {
            movement.scl(Math.min(SPEED * delta, distance) / distance);
            touchingPlayer |= moveAxis(movement.x, true, player, geese, mapFeatures, mapManager);
            touchingPlayer |= moveAxis(movement.y, false, player, geese, mapFeatures, mapManager);
        }

        if (distracted) {
            return;
        }

        attackTimer -= delta;
        if (attackTimer <= 0 && touchingPlayer) {
            attackTimer = ATTACK_COOLDOWN;
            flapTime = flapAnimation.getAnimationDuration();
            if (player.getInvulnerabilityTime() == 0) {
                player.setInvulnerabilityTime(1);
                player.takeDamage(DAMAGE);
            }
        }
    }

    /** Returns the nearest active crumb so separate geese can choose separate targets. */
    private FoodCrumb findClosestCrumb(Array<FoodCrumb> crumbs) {
        if (crumbs == null) return null;

        FoodCrumb closest = null;
        float closestDistance2 = Float.MAX_VALUE;
        for (FoodCrumb crumb : crumbs) {
            if (!crumb.isActive()) continue;
            float distance2 = position.dst2(crumb.getPosition());
            if (distance2 < closestDistance2) {
                closest = crumb;
                closestDistance2 = distance2;
            }
        }
        return closest;
    }

    private boolean moveAxis(float amount, boolean horizontal, Player player, Array<Goose> geese,
                             Array<MapFeature> mapFeatures, MapManager mapManager) {
        if (amount == 0) return false;
        float previous = horizontal ? position.x : position.y;
        if (horizontal) {
            position.x += amount;
            getBounds().setX(position.x);
        } else {
            position.y += amount;
            getBounds().setY(position.y);
        }

        boolean touchingPlayer = isTouchingPlayer(player);
        if (overlapsOtherGoose(geese) || overlapsSolidFeature(mapFeatures)
            || (mapManager != null && mapManager.checkGooseCollision(getBounds()))) {
            if (horizontal) {
                position.x = previous;
                getBounds().setX(position.x);
            } else {
                position.y = previous;
                getBounds().setY(position.y);
            }
        }
        return touchingPlayer;
    }

    private boolean overlapsOtherGoose(Array<Goose> geese) {
        if (geese == null) return false;
        for (int i = 0; i < geese.size; i++) {
            Goose other = geese.get(i);
            if (other != this && getBounds().overlaps(other.getBounds())) return true;
        }
        return false;
    }

    private boolean overlapsSolidFeature(Array<MapFeature> mapFeatures) {
        if (mapFeatures == null) return false;
        for (MapFeature feature : mapFeatures) {
            if (feature.blocksGoose() && getBounds().overlaps(feature.getBounds())) return true;
        }
        return false;
    }

    /**
     * Tracks continuous time outside the active world camera.
     *
     * @param delta elapsed time in seconds
     * @param cameraBounds visible world-space area
     * @return true after this goose has remained outside the camera for ten seconds
     */
    public boolean hasBeenOffCameraTooLong(float delta, com.badlogic.gdx.math.Rectangle cameraBounds) {
        if (getBounds().overlaps(cameraBounds)) {
            offCameraTimer = 0;
            return false;
        }
        offCameraTimer += delta;
        return offCameraTimer >= 10f;
    }

    @Override public void update(float delta) {
    }
}
