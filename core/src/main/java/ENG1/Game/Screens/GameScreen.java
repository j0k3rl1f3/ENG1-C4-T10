package ENG1.Game.Screens;
import ENG1.Game.Entities.*;
import ENG1.Game.Main;
import ENG1.Game.Managers.Interactable;
import ENG1.Game.Managers.MapFeature;
import ENG1.Game.Managers.MapManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import static com.badlogic.gdx.Gdx.files;

/** Main gameplay screen: updates the world, renders the map, and draws the player HUD. */
public class GameScreen implements Screen {
    private static final float MAP_WIDTH = 2048f;
    private static final float MAP_HEIGHT = 2048f;
    private static final float GOOSE_SPAWN_INTERVAL = 5f;
    private static final float COIN_SPAWN_INTERVAL = 2.5f;
    private static final float GOOSE_GRACE_PERIOD = 4f;
    private static final float GAME_DURATION_SECONDS = 5 * 60f;
    private static final float LAKE_ACTIVATION_DISTANCE = 280f;
    private static final int MAX_GEESE = 50;
    private static final int MAX_COINS = 18;
    private static final float COIN_SIZE = 16f;
    public static boolean SHOW_COLLISION_DEBUG = false;
    public static boolean SHOW_CONTROL_ICONS = true;
    private final Main game;
    private WinTrigger winTrigger;;
    private Player player;
    private Bike bike;
    private Heal heal;
    private Array<Goose> geese;
    private Array<FoodItem> Pickups;
    private Array<MapFeature> mapFeatures;
    private MapManager mapManager;
    private final Array<FoodCrumb> foodCrumbs = new Array<>();
    private final Array<Coin> coins = new Array<>();
    private final Rectangle cameraBounds = new Rectangle();
    private float gooseGraceTimer;
    private float coinSpawnTimer;
    private float timeRemaining;
    public static int points;
    final private SpriteBatch batch = new SpriteBatch();
    final private OrthographicCamera camera = new OrthographicCamera(320, 240);
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font = new BitmapFont();
    private final ExtendViewport uiViewport = new ExtendViewport(640, 480);
    private final ExtendViewport viewport = new ExtendViewport(320, 240, camera);
    private final Texture inventoryTexture = new Texture(files.internal("original assets/inventory(1).png"));
    private final TextureRegion inventoryBar = new TextureRegion(inventoryTexture, 0,0,632,100);
    private final Texture foodIcon = new Texture(files.internal("assets/foodIcon.png"));
    private final Texture walkIcon = new Texture(files.internal("assets/wasd.png"));
    private final Texture pauseIcon = new Texture(files.internal("assets/esc.png"));
    private final Texture throwIcon = new Texture(files.internal("assets/space.png"));
    private final Texture coinIcon = new Texture(files.internal(
        "Chronicles of the Realm - Coins Pack (16x16)/16x16/Coin_Gold_0.png"));

    public GameScreen(Main game) {
        this.game = game;
    }

    @Override
    /** Creates a fresh player, world features, and the starting food pickup. */
    public void show() {
        if (player != null) return;
        player = new Player(100,100, 24,24);
        FoodItem startingFood = new FoodItem(0, 0);
        player.addItem(startingFood);
        player.unlockFoodSprinkler();
        points = 0;
        geese = new com.badlogic.gdx.utils.Array<>();
        gooseGraceTimer = GOOSE_GRACE_PERIOD;
        coins.clear();
        coinSpawnTimer = COIN_SPAWN_INTERVAL;
        timeRemaining = GAME_DURATION_SECONDS;
        foodCrumbs.clear();
        mapManager = new MapManager("Map/GooseMap.tmx");
        mapFeatures = mapManager.getWaterRegions();
        Pickups = new Array<>();
        bike = new Bike(20, 100);
        heal = new Heal(1900, 1000);
        winTrigger = new WinTrigger(58 * 32, 57 * 32);
        camera.position.set(320, 240, 0);
        camera.update();
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        uiViewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
    }
    @Override
    /** Updates and renders a single gameplay frame. @param delta elapsed time in seconds */
    public void render(float delta) {
        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1);

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.showPauseMenu(this);
            return;
        }
        gooseGraceTimer = Math.max(0, gooseGraceTimer - delta);
        player.update(delta, gooseGraceTimer > 0 ? null : geese, mapFeatures, mapManager);
        if (!player.isAlive()) {
            game.showChasedHomeScreen(this);
            return;
        }
        timeRemaining = Math.max(0, timeRemaining - delta);
        if (timeRemaining <= 0) {
            game.showTimeExpiredScreen(this);
            return;
        }
        if (player.getBounds().overlaps(winTrigger.getBounds())) {
            game.showWinScreen(this);
            return;
        }
        if (bike != null && !bike.isCollected()
            && player.getBounds().overlaps(bike.getBounds())) {
            bike.interact(player);
            player.addItem(bike);
        }
        if (heal != null && !heal.isCollected()
            && player.getBounds().overlaps(heal.getBounds())) {
            heal.interact(player);
        }
        collectCoins();
        player.keepInsideMap(MAP_WIDTH, MAP_HEIGHT);
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) && player.canSprinkleFood()) {
            sprinkleFood();
            player.startFoodCooldown();
        }
        for (int i = foodCrumbs.size - 1; i >= 0; i--) {
            FoodCrumb crumb = foodCrumbs.get(i);
            crumb.update(delta);
            if (!crumb.isActive()) foodCrumbs.removeIndex(i);
        }
        spawnGeese(delta);
        spawnCoins(delta);
        if (gooseGraceTimer <= 0) {
            for (int i = 0; i < geese.size; i++) {
                Goose goose = geese.get(i);
                goose.update(delta, player, foodCrumbs, geese, mapFeatures, mapManager);
                goose.keepInsideMap(MAP_WIDTH, MAP_HEIGHT);
            }
        }
        float cameraHalfWidth = camera.viewportWidth / 2f;
        float cameraHalfHeight = camera.viewportHeight / 2f;
        camera.position.set(
            clampCameraPosition(player.getX() + 16, cameraHalfWidth, MAP_WIDTH - cameraHalfWidth, MAP_WIDTH / 2f),
            clampCameraPosition(player.getY() + 16, cameraHalfHeight, MAP_HEIGHT - cameraHalfHeight, MAP_HEIGHT / 2f),
            0
        );
        camera.update();
        removeOffCameraGeese(delta);
        viewport.apply();
        mapManager.render(camera);

        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(Color.GOLD);
        for (FoodItem food : Pickups) {
            com.badlogic.gdx.math.Rectangle bounds = food.getBounds();
            shapes.rect(bounds.x, bounds.y, bounds.width, bounds.height);
        }
        shapes.setColor(Color.YELLOW);
        for (FoodCrumb food : foodCrumbs) {
            com.badlogic.gdx.math.Rectangle bounds = food.getBounds();
            shapes.rect(bounds.x, bounds.y, bounds.width, bounds.height);
        }
        shapes.end();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        if (bike != null && !bike.isCollected()) {
            Rectangle bounds = bike.getBounds();
            batch.draw(bike.bikeIcon, bounds.x - 8, bounds.y - 8, 32, 32);
        }
        if (heal != null && !heal.isCollected()) {
            Rectangle bounds = heal.getBounds();
            batch.draw(heal.healIcon, bounds.x - 8, bounds.y - 8, 16, 16);
        }
        for (Coin coin : coins) coin.render(batch, coinIcon);
        winTrigger.render(batch);
        for (Goose goose : geese) goose.render(batch);
        player.render(batch);
        batch.end();
        renderCollisionDebug();
        float healthRatio = (float) player.getHealth() / player.getMaxHealth();

        // Draw the HUD in screen coordinates so it stays fixed while the world camera follows the player.
        uiViewport.apply();
        float uiWidth = uiViewport.getWorldWidth();
        float uiHeight = uiViewport.getWorldHeight();
        float barWidth = 200;
        float barHeight = 16;
        float barX = uiWidth - barWidth - 20;
        float barY = uiHeight - 44;
        float tuticonSize = 64f;
        float gap = 8f;
        float stackHeight = 3 * tuticonSize + 2 * gap;
        float leftX = 16f;
        float bottomY = (uiHeight - stackHeight) / 2f;
        shapes.setProjectionMatrix(uiViewport.getCamera().combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(Color.DARK_GRAY);
        shapes.rect(barX, barY, barWidth, barHeight);
        shapes.setColor(healthRatio > 0.5f ? Color.GREEN : healthRatio > 0.25f ? Color.ORANGE : Color.RED);
        shapes.rect(barX, barY, barWidth * healthRatio, barHeight);
        shapes.end();
        batch.setProjectionMatrix(uiViewport.getCamera().combined);
        batch.begin();
        if (SHOW_CONTROL_ICONS) {
            batch.draw(throwIcon, leftX, bottomY, tuticonSize, tuticonSize);
            batch.draw(pauseIcon, leftX, bottomY + tuticonSize + gap, tuticonSize, tuticonSize);
            batch.draw(walkIcon, leftX, bottomY + 2 * (tuticonSize + gap),
                tuticonSize, tuticonSize);
        }
        batch.draw(inventoryTexture, 20, barY - 16, 316, 50);
        float inventoryX = 20;
        float inventoryY = barY - 16;
        float slotSpacing = 30;
        float iconSize = 20;
        Array<Interactable> items = player.getInventory();
        for (int i = 0; i < Math.min(items.size, 10); i++) {
            Texture icon = getInventoryIcon(items.get(i));
            if (icon != null) {
                float iconX = inventoryX + 14 + i * slotSpacing;
                float iconY = inventoryY + 10;
                batch.draw(icon, iconX, iconY, iconSize, iconSize);
            }
        }
        font.setColor(Color.WHITE);
        font.draw(batch, "Health: " + player.getHealth() + " / " + player.getMaxHealth(), barX, barY + barHeight + 20);
        font.draw(batch, "Time: " + formatTimeRemaining(), 20, 20);
        font.draw(batch, "points: " + points, barX, 20);
        batch.end();
    }

    private Texture getInventoryIcon(Interactable interactable) {{
            if (interactable instanceof Bike) return bike.bikeIcon;
            if (interactable instanceof FoodItem) return foodIcon;
            return null;
        }
    }

    @Override
    /** Updates world and UI viewports after the window size changes. */
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        uiViewport.update(width, height, true);
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    /** Releases all rendering resources owned by this screen. */
    public void dispose() {
        batch.dispose();
        shapes.dispose();
        font.dispose();
        bike.bikeIcon.dispose();
        heal.healIcon.dispose();
        coinIcon.dispose();
        if (player != null) player.dispose();
        if (geese != null) for (Goose goose : geese) goose.dispose();
        if (mapManager != null) mapManager.dispose();
        if (winTrigger != null) winTrigger.dispose();
    }


    /** Draws the active collision rectangles above the gameplay artwork for alignment checks. */
    private void renderCollisionDebug() {
        if (!SHOW_COLLISION_DEBUG) return;
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(Color.RED);
        mapManager.renderCollisionDebug(shapes);
        Rectangle playerBounds = player.getBounds();
        shapes.rect(playerBounds.x, playerBounds.y, playerBounds.width, playerBounds.height * 0.5f);
        for (Goose goose : geese) {
            Rectangle gooseBounds = goose.getBounds();
            shapes.rect(gooseBounds.x, gooseBounds.y, gooseBounds.width, gooseBounds.height);
        }
        shapes.end();
    }

    /** Formats the remaining countdown as minutes and two-digit seconds. */
    private String formatTimeRemaining() {
        int totalSeconds = (int) Math.ceil(timeRemaining);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
    }

    public float getTimeRemaining() {
        return timeRemaining;
    }

    public String getFormattedTimeRemaining() {
        return formatTimeRemaining();
    }

    /** Creates a small scatter of food in front of the player. */
    private void sprinkleFood() {
        float centreX = player.getX() + player.getFacing().x * 80f;
        float centreY = player.getY() + player.getFacing().y * 80f;
        for (int i = 0; i < 6; i++) {
            foodCrumbs.add(new FoodCrumb(
                centreX + MathUtils.random(-18f, 18f),
                centreY + MathUtils.random(-18f, 18f)
            ));
        }
    }

    private float clampCameraPosition(float target, float minimum, float maximum, float mapCenter) {
        if (minimum > maximum) return mapCenter;
        return MathUtils.clamp(target, minimum, maximum);
    }

    /** Removes geese that have spent ten continuous seconds outside the active camera view. */
    private void removeOffCameraGeese(float delta) {
        cameraBounds.set(camera.position.x - camera.viewportWidth / 2f,
            camera.position.y - camera.viewportHeight / 2f, camera.viewportWidth, camera.viewportHeight);
        for (int i = geese.size - 1; i >= 0; i--) {
            Goose goose = geese.get(i);
            if (goose.hasBeenOffCameraTooLong(delta, cameraBounds)) {
                geese.removeIndex(i);
                goose.dispose();
            }
        }
    }

    /** Advances lake spawn timers and adds geese until the population cap is reached. */
    private void spawnGeese(float delta) {
        if (geese.size >= MAX_GEESE) return;
        for (MapFeature feature : mapFeatures) {
            if (geese.size >= MAX_GEESE) break;
            if (feature.getBounds().y > player.getY() + LAKE_ACTIVATION_DISTANCE) continue;
            if (feature.updateSpawnTimer(delta, GOOSE_SPAWN_INTERVAL)) {
                com.badlogic.gdx.math.Rectangle lake = feature.getBounds();
                float angle = geese.size * 2.39996323f;
                float spawnRadius = 42f;
                geese.add(new Goose(lake.x + lake.width / 2f - 8 + MathUtils.cos(angle) * spawnRadius,
                    lake.y + lake.height / 2f - 8 + MathUtils.sin(angle) * spawnRadius, 16, 8));
            }
        }
    }

    /** Adds a coin at a random clear tile on the Tiled path every few seconds. */
    private void spawnCoins(float delta) {
        if (coins.size >= MAX_COINS) return;
        coinSpawnTimer += delta;
        if (coinSpawnTimer < COIN_SPAWN_INTERVAL) return;
        coinSpawnTimer -= COIN_SPAWN_INTERVAL;

        for (int attempt = 0; attempt < 20; attempt++) {
            float searchWidth = camera.viewportWidth + 128f;
            float searchHeight = camera.viewportHeight + 128f;
            Rectangle nearbyPaths = new Rectangle(
                player.getX() + 16f - searchWidth / 2f,
                player.getY() + 16f - searchHeight / 2f,
                searchWidth,
                searchHeight
            );
            com.badlogic.gdx.math.Vector2 position = mapManager.getRandomPathPosition(
                COIN_SIZE, COIN_SIZE, nearbyPaths);
            if (position == null) position = mapManager.getRandomPathPosition(COIN_SIZE, COIN_SIZE);
            if (position == null) return;

            Rectangle candidate = new Rectangle(position.x, position.y, COIN_SIZE, COIN_SIZE);
            boolean tooCloseToExistingCoin = false;
            for (Coin existing : coins) {
                Rectangle bounds = existing.getBounds();
                if (Math.abs(candidate.x - bounds.x) < 48 && Math.abs(candidate.y - bounds.y) < 48) {
                    tooCloseToExistingCoin = true;
                    break;
                }
            }
            if (!tooCloseToExistingCoin) {
                coins.add(new Coin(position.x, position.y));
                return;
            }
        }
    }

    /** Collects coins on contact and removes them from the active world list. */
    private void collectCoins() {
        for (int i = coins.size - 1; i >= 0; i--) {
            Coin coin = coins.get(i);
            if (player.getBounds().overlaps(coin.getBounds())) {
                coin.interact(player);
                if (coin.isCollected()) coins.removeIndex(i);
            }
        }
    }

    public static void goosePoint(){
        points += 10;
    }

    public static void addCoin() {
        points += 100;
    }
}
