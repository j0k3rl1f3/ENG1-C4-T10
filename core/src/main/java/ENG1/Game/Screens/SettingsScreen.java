package ENG1.Game.Screens;

import ENG1.Game.Main;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

/** The game's win screen, with actions for a new game, settings, and exit. */
public class SettingsScreen implements Screen {
    private static final float WORLD_WIDTH = 320f;
    private static final float WORLD_HEIGHT = 240f;
    private static final float BUTTON_WIDTH = 136f;
    private static final float BUTTON_HEIGHT = 44f;
    private static final float BUTTON_X = (WORLD_WIDTH - BUTTON_WIDTH) / 2f;
    private static final float TOGGLE_WIDTH = 88f;
    private static final float TOGGLE_HEIGHT = 36f;
    private static final float TOGGLE_X = WORLD_WIDTH - TOGGLE_WIDTH - 24f;
    private static final float COLLISION_TOGGLE_Y = 142f;
    private static final float ICONS_TOGGLE_Y = 92f;
    private static final float MAIN_MENU_Y = 24f;
    private static final float CLOSE_SIZE = 24f;
    private static final float CLOSE_X = WORLD_WIDTH - CLOSE_SIZE - 12f;
    private static final float CLOSE_Y = WORLD_HEIGHT - CLOSE_SIZE - 12f;
    private final Main game;
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = new BitmapFont();
    private final GlyphLayout labelLayout = new GlyphLayout();
    private final OrthographicCamera camera = new OrthographicCamera(WORLD_WIDTH, WORLD_HEIGHT);
    // A fixed virtual canvas keeps every menu element scaled proportionally on resize.
    private final FitViewport viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
    private final Texture uiTexture = new Texture("Spritesheet_UI_Flat.png");
    private final TextureRegion buttonArt = new TextureRegion(uiTexture, 32, 33, 95, 61);
    private final TextureRegion closeArt = new TextureRegion(uiTexture, 392, 104, 16, 16);
    private final Rectangle collisionToggleBounds = new Rectangle(
        TOGGLE_X, COLLISION_TOGGLE_Y, TOGGLE_WIDTH, TOGGLE_HEIGHT);
    private final Rectangle iconsToggleBounds = new Rectangle(
        TOGGLE_X, ICONS_TOGGLE_Y, TOGGLE_WIDTH, TOGGLE_HEIGHT);
    private final Rectangle mainMenuBounds = new Rectangle(BUTTON_X, MAIN_MENU_Y, BUTTON_WIDTH, BUTTON_HEIGHT);
    private final Rectangle closeBounds = new Rectangle(CLOSE_X, CLOSE_Y, CLOSE_SIZE, CLOSE_SIZE);
    private final Vector2 clickPosition = new Vector2();


    public SettingsScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void render(float delta) {
        if (handleClick()) return;
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.showMainMenu();
            return;
        }

        ScreenUtils.clear(Color.valueOf("1f2933"));
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        font.setColor(Color.WHITE);
        labelLayout.setText(font, "Settings");
        font.draw(batch, "Settings", (WORLD_WIDTH - labelLayout.width) / 2f, 224);
        font.draw(batch, "Show collision borders", 24, COLLISION_TOGGLE_Y + 13);
        font.draw(batch, "Show control icons", 24, ICONS_TOGGLE_Y + 13);
        drawButton(collisionToggleBounds, GameScreen.SHOW_COLLISION_DEBUG ? "ON" : "OFF");
        drawButton(iconsToggleBounds, GameScreen.SHOW_CONTROL_ICONS ? "ON" : "OFF");
        drawButton(mainMenuBounds, "MAIN MENU");
        batch.draw(closeArt, CLOSE_X, CLOSE_Y, CLOSE_SIZE, CLOSE_SIZE);
        batch.end();
    }

    /** @return true when the click switched away from this screen. */
    private boolean handleClick() {
        if (!Gdx.input.justTouched()) return false;

        clickPosition.set(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(clickPosition);
        if (closeBounds.contains(clickPosition)) {
            Gdx.app.exit();
            return true;
        } else if (collisionToggleBounds.contains(clickPosition)) {
            GameScreen.SHOW_COLLISION_DEBUG = !GameScreen.SHOW_COLLISION_DEBUG;
            return false;
        } else if (iconsToggleBounds.contains(clickPosition)) {
            GameScreen.SHOW_CONTROL_ICONS = !GameScreen.SHOW_CONTROL_ICONS;
            return false;
        } else if (mainMenuBounds.contains(clickPosition)) {
            game.showMainMenu();
            return true;
        }
        return false;
    }

    private void drawButton(Rectangle bounds, String label) {
        batch.draw(buttonArt, bounds.x, bounds.y, bounds.width, bounds.height);
        labelLayout.setText(font, label);
        float labelX = bounds.x + (bounds.width - labelLayout.width) / 2f;
        font.draw(batch, label, labelX, bounds.y + (bounds.height + labelLayout.height) / 2f);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        uiTexture.dispose();
    }
}
