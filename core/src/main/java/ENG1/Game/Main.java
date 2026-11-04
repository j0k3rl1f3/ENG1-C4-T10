package ENG1.Game;
import ENG1.Game.Screens.GameScreen;
import ENG1.Game.Screens.MainMenuScreen;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

/** libGDX application entry point which creates and owns the active game screen. */
public class Main extends Game {
    private SpriteBatch batch;
    private Texture image;
    private GameScreen pausedGame;

    @Override
    /** Initialises shared rendering resources and opens the main game screen. */
    public void create() {
        batch = new SpriteBatch();
        image = new Texture("original assets/libgdx.png");
        setScreen(new GameScreen(this));
    }

    @Override
    /** Delegates rendering to the currently active libGDX screen. */
    public void render() {
        super.render();
    }

    /** Opens the menu without disposing the supplied game, so it can later resume unchanged. */
    public void showPauseMenu(GameScreen pausedGame) {
        this.pausedGame = pausedGame;
        setScreen(new MainMenuScreen(this, pausedGame));
    }

    /** Restores a paused game and releases the menu which was displaying above it. */
    public void resumeGame(GameScreen gameScreen) {
        Screen pauseMenu = getScreen();
        pausedGame = null;
        setScreen(gameScreen);
        pauseMenu.dispose();
    }

    @Override
    /** Releases resources owned by the application. */
    public void dispose() {
        if (pausedGame != null) pausedGame.dispose();
        super.dispose();
        batch.dispose();
        image.dispose();
    }
}
