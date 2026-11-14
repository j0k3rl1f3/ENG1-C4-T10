package ENG1.Game.Screens;

import ENG1.Game.Main;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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

/** A pause menu that returns to the exact {@link GameScreen} instance it interrupted. */
public class PauseScreen implements Screen {
    private final Main game;
    private final GameScreen pausedGame;
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = new BitmapFont();
    private final GlyphLayout labelLayout = new GlyphLayout();
    private final OrthographicCamera camera = new OrthographicCamera(320, 240);
    private final FitViewport viewport = new FitViewport(320, 240, camera);
    private final Texture uiTexture = new Texture("Spritesheet_UI_Flat.png");
    private final TextureRegion buttonArt = new TextureRegion(uiTexture, 32, 33, 95, 61);
    private final TextureRegion closeArt = new TextureRegion(uiTexture, 392, 104, 16, 16);
    private final Rectangle resumeBounds = new Rectangle(92, 112, 136, 44);
    private final Rectangle mainMenuBounds = new Rectangle(92, 56, 136, 44);
    private final Rectangle closeBounds = new Rectangle(284, 204, 24, 24);
    private final Vector2 clickPosition = new Vector2();

    public PauseScreen(Main game, GameScreen pausedGame) {
        this.game = game;
        this.pausedGame = pausedGame;
    }

    @Override
    public void show() { }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
            || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            game.resumeGame(pausedGame);
            return;
        }
        if (handleClick()) return;

        ScreenUtils.clear(Color.valueOf("1f2933"));
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        font.setColor(Color.WHITE);
        labelLayout.setText(font, "PAUSED");
        font.draw(batch, "PAUSED", (320 - labelLayout.width) / 2f, 196);
        drawButton(resumeBounds, "RESUME");
        drawButton(mainMenuBounds, "MAIN MENU");
        batch.draw(closeArt, closeBounds.x, closeBounds.y, closeBounds.width, closeBounds.height);
        batch.end();
    }

    /** @return true when an action switches away from the pause screen. */
    private boolean handleClick() {
        if (!Gdx.input.justTouched()) return false;
        clickPosition.set(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(clickPosition);
        if (resumeBounds.contains(clickPosition)) {
            game.resumeGame(pausedGame);
            return true;
        }
        if (mainMenuBounds.contains(clickPosition)) {
            game.showMainMenu();
            return true;
        }
        if (closeBounds.contains(clickPosition)) {
            Gdx.app.exit();
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
