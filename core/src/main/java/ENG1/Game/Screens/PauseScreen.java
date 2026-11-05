package ENG1.Game.Screens;

import ENG1.Game.Main;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

/** A pause menu that returns to the exact {@link GameScreen} instance it interrupted. */
public class PauseScreen implements Screen {
    private final Main game;
    private final GameScreen pausedGame;
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = new BitmapFont();
    private final OrthographicCamera camera = new OrthographicCamera(320, 240);
    private final ExtendViewport viewport = new ExtendViewport(320, 240, camera);

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

        ScreenUtils.clear(Color.BLACK);
        viewport.apply();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        font.setColor(Color.WHITE);
        float centreX = viewport.getWorldWidth() / 2f;
        font.draw(batch, "PAUSED", centreX - 28, 150);
        font.draw(batch, "Press Enter or Escape to resume", centreX - 88, 110);
        batch.end();
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
    }
}
