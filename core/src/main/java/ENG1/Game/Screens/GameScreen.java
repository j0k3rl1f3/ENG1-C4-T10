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
    private static final float GOOSE_GRACE_PERIOD = 4f;
    private static final float GAME_DURATION_SECONDS = 5 * 60f;
    private static final float LAKE_ACTIVATION_DISTANCE = 280f;
    private static final int MAX_GEESE = 50;
    private static final boolean SHOW_COLLISION_DEBUG = false;
    private final Main game;
    private WinTrigger winTrigger;;
    private Player player;
    private Bike bike;
    private Array<Goose> geese;
    private Array<FoodItem> Pickups;
    private Array<MapFeature> mapFeatures;
    private MapManager mapManager;
    private final Array<FoodCrumb> foodCrumbs = new Array<>();
    private final Rectangle cameraBounds = new Rectangle();
    private float gooseGraceTimer;
    public static float timeRemaining;
    public static int points;
    final private SpriteBatch batch = new SpriteBatch();
    final private OrthographicCamera camera = new OrthographicCamera(320, 240);
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font = new BitmapFont();
    private final ExtendViewport uiViewport = new ExtendViewport(640, 480);
    private final ExtendViewport viewport = new ExtendViewport(320, 240, camera);
    private final Texture inventoryTexture = new Texture(files.internal("original assets/inventory(1).png"));
    private final TextureRegion inventoryBar = new TextureRegion(inventoryTexture, 0,0,632,100);
    private final Texture bikeIcon = new Texture(files.internal("assets/bikeIcon.png"));
    private final Texture foodIcon = new Texture(files.internal("assets/foodIcon.png"));

    public GameScreen(Main game) {
        this.game = game;
    }

    @Override
    /** Creates a fresh player, world features, and the starting food pickup. */
    public void show() {
        if (player != null) return;
        player = new Player(100,100, 32,32);
        FoodItem startingFood = new FoodItem(0, 0);
        player.addItem(startingFood);
        player.unlockFoodSprinkler();
        points = 0;
        geese = new com.badlogic.gdx.utils.Array<>();
        gooseGraceTimer = GOOSE_GRACE_PERIOD;
        timeRemaining = GAME_DURATION_SECONDS;
        foodCrumbs.clear();
        mapManager = new MapManager("Map/GooseMap.tmx");
        mapFeatures = mapManager.getWaterRegions();
        Pickups = new Array<>();
        bike = new Bike(200, 100);
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
        if (bike != null && !bike.isCollected()) {
            Rectangle bounds = bike.getBounds();
            shapes.setColor(Color.CYAN);
            shapes.rect(bounds.x, bounds.y, bounds.width, bounds.height);
        }
        shapes.end();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        winTrigger.render(batch);
        player.render(batch);
        for (Goose goose : geese) goose.render(batch);
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
        shapes.setProjectionMatrix(uiViewport.getCamera().combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(Color.DARK_GRAY);
        shapes.rect(barX, barY, barWidth, barHeight);
        shapes.setColor(healthRatio > 0.5f ? Color.GREEN : healthRatio > 0.25f ? Color.ORANGE : Color.RED);
        shapes.rect(barX, barY, barWidth * healthRatio, barHeight);
        shapes.end();
        batch.setProjectionMatrix(uiViewport.getCamera().combined);
        batch.begin();
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
        if (player.getNearMiss() != 0) {
            font.draw(batch, "near miss streak:" + player.getNearMiss(), barX, 60);
        }
        batch.end();
    }

    private Texture getInventoryIcon(Interactable interactable) {{
            if (interactable instanceof Bike) return bikeIcon;
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
    public void collectBike() {

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
    public static String formatTimeRemaining() {
        int totalSeconds = (int) Math.ceil(timeRemaining);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
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
                    lake.y + lake.height / 2f - 8 + MathUtils.sin(angle) * spawnRadius, 16, 16));
            }
        }
    }

    public static void goosePoint(){
        points += 10;
    }
}
