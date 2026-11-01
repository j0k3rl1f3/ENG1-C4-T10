package ENG1.Game.Screens;
import ENG1.Game.Entitys.Goose;
import ENG1.Game.Entitys.Food;
import ENG1.Game.Entitys.Player;
import ENG1.Game.Managers.MapFeature;
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
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import static com.badlogic.gdx.Gdx.files;

public class GameScreen implements Screen {
    private static final float MAP_WIDTH = 2048f;
    private static final float MAP_HEIGHT = 2048f;
    private static final float GOOSE_SPAWN_INTERVAL = 12f;
    private static final int MAX_GEESE = 9;

    private Player player;
    private Array<Goose> geese;
    private Array<Food> foodPickups;
    private Array<MapFeature> mapFeatures;
    private final Array<Food> thrownFoods = new Array<>();
    final private SpriteBatch batch = new SpriteBatch();
    final private OrthographicCamera camera = new OrthographicCamera(320, 240);
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final BitmapFont font = new BitmapFont();
    private final ScreenViewport uiViewport = new ScreenViewport();
    private final FitViewport viewport = new FitViewport(320, 240, camera);
    private final Texture backgroundTexture = new Texture(files.internal("largeBackground.png"));

    @Override
    public void show() {
        player = new Player(0, 0, 32, 32);
        geese = new com.badlogic.gdx.utils.Array<>();
        mapFeatures = createMapFeatures();
        foodPickups = new Array<>();
        foodPickups.add(new Food(70, 170));
        camera.position.set(320, 240, 0);
        camera.update();
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
        uiViewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
    }
    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1);

        player.update(delta, geese, mapFeatures);
        player.keepInsideMap(MAP_WIDTH, MAP_HEIGHT);
        collectFood();
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            Food food = player.throwFood();
            if (food != null) thrownFoods.add(food);
        }
        for (int i = thrownFoods.size - 1; i >= 0; i--) {
            Food food = thrownFoods.get(i);
            food.update(delta);
            if (!food.isDistracting()) thrownFoods.removeIndex(i);
        }
        Food bait = thrownFoods.size == 0 ? null : thrownFoods.peek();
        spawnGeese(delta);
        for (int i = 0; i < geese.size; i++) {
            Goose goose = geese.get(i);
            goose.update(delta, player, bait, geese, mapFeatures);
            goose.keepInsideMap(MAP_WIDTH, MAP_HEIGHT);
        }
        float cameraHalfWidth = camera.viewportWidth / 2f;
        float cameraHalfHeight = camera.viewportHeight / 2f;
        camera.position.set(
            clampCameraPosition(player.getX() + 16, cameraHalfWidth, MAP_WIDTH - cameraHalfWidth, MAP_WIDTH / 2f),
            clampCameraPosition(player.getY() + 16, cameraHalfHeight, MAP_HEIGHT - cameraHalfHeight, MAP_HEIGHT / 2f),
            0
        );
        camera.update();
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(backgroundTexture, 0, 0, MAP_WIDTH, MAP_HEIGHT);
        batch.end();

        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (MapFeature feature : mapFeatures) renderFeature(feature);
        shapes.setColor(Color.GOLD);
        for (Food food : foodPickups) {
            com.badlogic.gdx.math.Rectangle bounds = food.getBounds();
            shapes.rect(bounds.x, bounds.y, bounds.width, bounds.height);
        }
        shapes.setColor(Color.YELLOW);
        for (Food food : thrownFoods) {
            com.badlogic.gdx.math.Rectangle bounds = food.getBounds();
            shapes.rect(bounds.x, bounds.y, bounds.width, bounds.height);
        }
        shapes.end();

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
        font.setColor(Color.WHITE);
        font.draw(batch, "Health: " + player.getHealth() + " / " + player.getMaxHealth(), barX, barY + barHeight + 20);
        if (!player.isAlive()) font.draw(batch, "You were chased down!", uiWidth / 2 - 75, uiHeight / 2);
        batch.end();
    }

    @Override
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
    public void dispose() {
        batch.dispose();
        shapes.dispose();
        font.dispose();
        if (player != null) player.dispose();
        if (geese != null) for (Goose goose : geese) goose.dispose();
        backgroundTexture.dispose();
    }

    private void collectFood() {
        for (int i = foodPickups.size - 1; i >= 0; i--) {
            Food food = foodPickups.get(i);
            if (player.getBounds().overlaps(food.getBounds())) {
                food.interact(player);
                if (food.isCollected()) foodPickups.removeIndex(i);
            }
        }
    }

    private float clampCameraPosition(float target, float minimum, float maximum, float mapCenter) {
        if (minimum > maximum) return mapCenter;
        return MathUtils.clamp(target, minimum, maximum);
    }

    private Array<MapFeature> createMapFeatures() {
        Array<MapFeature> features = new Array<>();
        // Lakes sit over the pale placeholder areas in the background; positions use bottom-left map coordinates.
        features.add(new MapFeature(MapFeature.Type.LAKE, 500, 1570, 220, 190));
        features.add(new MapFeature(MapFeature.Type.LAKE, 300, 620, 180, 150));
        features.add(new MapFeature(MapFeature.Type.LAKE, 960, 680, 280, 210));

        // Trees and smaller decorations are placed in green areas away from the main paths.
        features.add(new MapFeature(MapFeature.Type.TREE, 170, 520, 54, 66));
        features.add(new MapFeature(MapFeature.Type.TREE, 520, 1050, 58, 70));
        features.add(new MapFeature(MapFeature.Type.TREE, 820, 1750, 56, 68));
        features.add(new MapFeature(MapFeature.Type.TREE, 1370, 1220, 60, 72));
        features.add(new MapFeature(MapFeature.Type.TREE, 1680, 430, 56, 68));
        features.add(new MapFeature(MapFeature.Type.TREE, 1840, 1050, 60, 72));
        features.add(new MapFeature(MapFeature.Type.ROCK, 350, 1120, 42, 30));
        features.add(new MapFeature(MapFeature.Type.ROCK, 1480, 1740, 46, 32));
        features.add(new MapFeature(MapFeature.Type.BUSH, 660, 520, 42, 34));
        features.add(new MapFeature(MapFeature.Type.BUSH, 1240, 1540, 46, 36));
        features.add(new MapFeature(MapFeature.Type.FLOWERS, 1600, 1380, 44, 34));
        features.add(new MapFeature(MapFeature.Type.FLOWERS, 760, 900, 40, 32));
        return features;
    }

    private void spawnGeese(float delta) {
        if (geese.size >= MAX_GEESE) return;
        for (MapFeature feature : mapFeatures) {
            if (geese.size >= MAX_GEESE) break;
            if (feature.updateSpawnTimer(delta, GOOSE_SPAWN_INTERVAL)) {
                com.badlogic.gdx.math.Rectangle lake = feature.getBounds();
                float angle = geese.size * 2.39996323f;
                float spawnRadius = 42f;
                geese.add(new Goose(lake.x + lake.width / 2f - 16 + MathUtils.cos(angle) * spawnRadius,
                    lake.y + lake.height / 2f - 16 + MathUtils.sin(angle) * spawnRadius, 32, 32));
            }
        }
    }

    private void renderFeature(MapFeature feature) {
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

}
