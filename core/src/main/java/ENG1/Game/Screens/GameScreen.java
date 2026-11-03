package ENG1.Game.Screens;
import ENG1.Game.Entitys.Goose;
import ENG1.Game.Entitys.FoodCrumb;
import ENG1.Game.Entitys.FoodPickup;
import ENG1.Game.Entitys.Player;
import ENG1.Game.Managers.MapFeature;
import ENG1.Game.Managers.MapManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import static com.badlogic.gdx.Gdx.files;

/** Main gameplay screen: updates the world, renders the map, and draws the player HUD. */
public class GameScreen implements Screen {
    private static final float MAP_WIDTH = 2048f;
    private static final float MAP_HEIGHT = 2048f;
    private static final float GOOSE_SPAWN_INTERVAL = 5f;
    private static final float GOOSE_GRACE_PERIOD = 4f;
    private static final float LAKE_ACTIVATION_DISTANCE = 280f;
    private static final float PATH_WIDTH = 100f;
    private static final int MAX_GEESE = 30;
    private Player player;
    private Array<Goose> geese;
    private Array<FoodPickup> foodPickups;
    private Array<MapFeature> mapFeatures;
    private MapManager mapManager;
    private final Array<FoodCrumb> foodCrumbs = new Array<>();
    private final Rectangle cameraBounds = new Rectangle();
    private float gooseGraceTimer;
    final private SpriteBatch batch = new SpriteBatch();
    final private OrthographicCamera camera = new OrthographicCamera(320, 240);
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font = new BitmapFont();
    private final ScreenViewport uiViewport = new ScreenViewport();
    private final FitViewport viewport = new FitViewport(320, 240, camera);
    private final Texture inventoryTexture = new Texture(files.internal("original assets/inventory.png"));

    @Override
    /** Creates a fresh player, world features, and the starting food pickup. */
    public void show() {
        player = new Player(100,100, 32,32);
        geese = new com.badlogic.gdx.utils.Array<>();
        gooseGraceTimer = GOOSE_GRACE_PERIOD;
        foodCrumbs.clear();
        mapManager = new MapManager("Map/GooseMap.tmx");
        mapFeatures = mapManager.getWaterRegions();
        foodPickups = new Array<>();
        foodPickups.add(new FoodPickup(70, 170));
        camera.position.set(320, 240, 0);
        camera.update();
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        uiViewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
    }
    @Override
    /** Updates and renders a single gameplay frame. @param delta elapsed time in seconds */
    public void render(float delta) {
        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1);

        gooseGraceTimer = Math.max(0, gooseGraceTimer - delta);
        player.update(delta, gooseGraceTimer > 0 ? null : geese, mapFeatures, mapManager);
        player.keepInsideMap(MAP_WIDTH, MAP_HEIGHT);
        collectFood();
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
        for (FoodPickup food : foodPickups) {
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
        player.render(batch);
        for (Goose goose : geese) goose.render(batch);
        batch.end();
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
        batch.draw(inventoryTexture, barX - 360, barY - 60,196, 64);
        font.setColor(Color.WHITE);
        font.draw(batch, "Health: " + player.getHealth() + " / " + player.getMaxHealth(), barX, barY + barHeight + 20);

        batch.end();
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
        if (player != null) player.dispose();
        if (geese != null) for (Goose goose : geese) goose.dispose();
        if (mapManager != null) mapManager.dispose();
    }

    /** Gives the player any food pickup whose bounds they overlap. */
    private void collectFood() {
        for (int i = foodPickups.size - 1; i >= 0; i--) {
            FoodPickup food = foodPickups.get(i);
            if (player.getBounds().overlaps(food.getBounds())) {
                food.interact(player);
                if (food.isCollected()) foodPickups.removeIndex(i);
            }
        }
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

    /** Draws the primitive placeholder artwork for one map feature. @param feature feature to draw */
    public void renderFeature(MapFeature feature) {
        com.badlogic.gdx.math.Rectangle bounds = feature.getBounds();
        switch (feature.getType()) {
            case LAKE:
                shapes.setColor(Color.BLUE);
                shapes.ellipse(bounds.x, bounds.y, bounds.width, bounds.height);
                shapes.setColor(Color.CYAN);
                shapes.ellipse(bounds.x + bounds.width * 0.12f, bounds.y + bounds.height * 0.12f,
                    bounds.width * 0.76f, bounds.height * 0.76f);
                break;
            case TREE:
                shapes.setColor(Color.BROWN);
                shapes.rect(bounds.x + bounds.width * 0.4f, bounds.y, bounds.width * 0.2f, bounds.height * 0.55f);
                shapes.setColor(Color.FOREST);
                shapes.circle(bounds.x + bounds.width / 2f, bounds.y + bounds.height * 0.62f, bounds.width * 0.48f);
                break;
            case ROCK:
                shapes.setColor(Color.LIGHT_GRAY);
                shapes.ellipse(bounds.x, bounds.y, bounds.width, bounds.height);
                break;
            case BUSH:
                shapes.setColor(Color.OLIVE);
                shapes.ellipse(bounds.x, bounds.y, bounds.width, bounds.height);
                break;
            case FLOWERS:
                shapes.setColor(Color.PINK);
                shapes.circle(bounds.x + bounds.width * 0.25f, bounds.y + bounds.height * 0.5f, 5);
                shapes.circle(bounds.x + bounds.width * 0.5f, bounds.y + bounds.height * 0.7f, 5);
                shapes.circle(bounds.x + bounds.width * 0.75f, bounds.y + bounds.height * 0.4f, 5);
                shapes.setColor(Color.YELLOW);
                shapes.circle(bounds.x + bounds.width * 0.25f, bounds.y + bounds.height * 0.5f, 2);
                shapes.circle(bounds.x + bounds.width * 0.5f, bounds.y + bounds.height * 0.7f, 2);
                shapes.circle(bounds.x + bounds.width * 0.75f, bounds.y + bounds.height * 0.4f, 2);
                break;
        }
    }

    /** Draws the northbound trail that connects the four increasingly difficult map layers. */
    private void renderPath() {
        shapes.setColor(Color.TAN);
        shapes.rectLine(120, 120, 650, 120, PATH_WIDTH);
        shapes.rectLine(650, 120, 650, 430, PATH_WIDTH);
        shapes.rectLine(650, 430, 300, 430, PATH_WIDTH);
        shapes.rectLine(300, 430, 300, 830, PATH_WIDTH);
        shapes.rectLine(300, 830, 1100, 830, PATH_WIDTH);
        shapes.rectLine(1100, 830, 1100, 1250, PATH_WIDTH);
        shapes.rectLine(1100, 1250, 520, 1250, PATH_WIDTH);
        shapes.rectLine(520, 1250, 520, 1650, PATH_WIDTH);
        shapes.rectLine(520, 1650, 1500, 1650, PATH_WIDTH);
        shapes.rectLine(1500, 1650, 1500, 1930, PATH_WIDTH);
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

    private Array<MapFeature> createMapFeatures() {
        Array<MapFeature> features = new Array<>();

        // Layer 1: starting clearing, with a single lake.
        addLake(features, 1350, 220, 220, 180);
        addTreePatch(features, 60, 250, 4, 3);
        addTreePatch(features, 950, 70, 5, 3);

        // Layer 2: dense woodland, with three lakes and a gate at x = 220–400.
        addTreeWall(features, 480, 220, 400);
        addLake(features, 60, 620, 190, 150);
        addLake(features, 720, 570, 240, 180);
        addLake(features, 1500, 740, 230, 170);
        addTreePatch(features, 430, 600, 4, 4);
        addTreePatch(features, 1120, 510, 5, 3);
        addTreePatch(features, 1770, 560, 4, 4);

        // Layer 3: marsh maze, with five lakes and a gate at x = 1020–1180.
        addTreeWall(features, 960, 1020, 1180);
        addLake(features, 150, 1090, 210, 170);
        addLake(features, 600, 1080, 230, 180);
        addLake(features, 1300, 1030, 240, 190);
        addLake(features, 1530, 1200, 210, 170);
        addLake(features, 1420, 1320, 200, 150);
        addTreePatch(features, 250, 1010, 3, 4);
        addTreePatch(features, 880, 1360, 4, 3);
        addTreePatch(features, 1770, 1040, 4, 5);

        // Layer 4: flooded forest, with seven lakes and the final gate at x = 440–620.
        addTreeWall(features, 1440, 440, 620);
        addLake(features, 110, 1550, 200, 160);
        addLake(features, 820, 1730, 220, 180);
        addLake(features, 1150, 1780, 210, 160);
        addLake(features, 1700, 1550, 230, 180);
        addLake(features, 700, 1810, 220, 150);
        addLake(features, 1000, 1900, 220, 120);
        addLake(features, 1700, 1870, 200, 140);
        addTreePatch(features, 330, 1510, 2, 4);
        addTreePatch(features, 430, 1770, 4, 3);
        addTreePatch(features, 70, 1740, 4, 3);
        addTreePatch(features, 580, 1970, 4, 1);
        return features;
    }

    /** Adds a lake, which blocks the player, permits goose movement, and can spawn geese. */
    private void addLake(Array<MapFeature> features, float x, float y, float width, float height) {
        features.add(new MapFeature(MapFeature.Type.LAKE, x, y, width, height));
    }

    /** Adds a solid horizontal tree wall but leaves the supplied range open as a path gate. */
    private void addTreeWall(Array<MapFeature> features, float y, float gateStart, float gateEnd) {
        final float treeWidth = 54f;
        for (float x = 0; x < MAP_WIDTH; x += treeWidth) {
            if (x + treeWidth <= gateStart || x >= gateEnd) {
                features.add(new MapFeature(MapFeature.Type.TREE, x, y - 34, treeWidth, 68));
            }
        }
    }
    /** Adds a dense rectangular cluster of individual blocking trees. */
    private void addTreePatch(Array<MapFeature> features, float x, float y, int columns, int rows) {
        final float treeWidth = 54f;
        final float treeHeight = 68f;
        for (int column = 0; column < columns; column++) {
            for (int row = 0; row < rows; row++) {
                features.add(new MapFeature(MapFeature.Type.TREE,
                    x + column * treeWidth, y + row * treeHeight, treeWidth, treeHeight));
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
}
