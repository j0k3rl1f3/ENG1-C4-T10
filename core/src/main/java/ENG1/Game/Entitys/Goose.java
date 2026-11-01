package ENG1.Game.Entitys;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import ENG1.Game.Managers.MapFeature;

public class Goose extends Entity{
    private static final float SPEED = 85f;
    private static final float ATTACK_COOLDOWN = 1f;
    private static final int DAMAGE = 1;
    private final Texture gooseSprite = new Texture(Gdx.files.internal("gooseSprite.png"));
    private float attackTimer;
    private final Vector2 movement = new Vector2();

    public Goose(float x, float y, float width, float height) {
        super(x, y, width, height);
    }

    @Override
    public void render(SpriteBatch batch) {
        batch.draw(gooseSprite, position.x, position.y);
    }

    @Override public void dispose() {
        super.dispose();
        gooseSprite.dispose();
    }

    public void update(float delta, Player player) {
        update(delta, player, null, null, null);
    }

    public void update(float delta, Player player, Food bait) {
        update(delta, player, bait, null, null);
    }

    public void update(float delta, Player player, Food bait, Array<Goose> geese,
                       Array<MapFeature> mapFeatures) {
        if (player == null || !player.isAlive()) return;

        boolean distracted = bait != null && bait.isDistracting();
        Vector2 target = distracted ? bait.getPosition() : player.position;
        movement.set(target).sub(position);
        float distance = movement.len();
        boolean touchingPlayer = getBounds().overlaps(player.getBounds());
        if (distance > 1f) {
            movement.scl(Math.min(SPEED * delta, distance) / distance);
            touchingPlayer |= moveAxis(movement.x, true, player, geese, mapFeatures);
            touchingPlayer |= moveAxis(movement.y, false, player, geese, mapFeatures);
        }

        if (distracted) return;
        attackTimer -= delta;
        if (attackTimer <= 0 && touchingPlayer) {
            player.takeDamage(DAMAGE);
            attackTimer = ATTACK_COOLDOWN;
        }
    }

    private boolean moveAxis(float amount, boolean horizontal, Player player, Array<Goose> geese,
                             Array<MapFeature> mapFeatures) {
        if (amount == 0) return false;
        float previous = horizontal ? position.x : position.y;
        if (horizontal) {
            position.x += amount;
            getBounds().setX(position.x);
        } else {
            position.y += amount;
            getBounds().setY(position.y);
        }

        boolean touchingPlayer = getBounds().overlaps(player.getBounds());
        if (touchingPlayer || overlapsOtherGoose(geese) || overlapsSolidFeature(mapFeatures)) {
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

    @Override public void update(float delta) { }

}
