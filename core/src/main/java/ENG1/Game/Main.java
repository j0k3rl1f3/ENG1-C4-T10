package ENG1.Game;
import ENG1.Game.Screens.GameScreen;
import ENG1.Game.Screens.MainMenuScreen;
import ENG1.Game.Screens.PauseScreen;
import ENG1.Game.Screens.WinScreen;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/** libGDX application entry point which creates and owns the active game screen. */
public class Main extends Game {
    private SpriteBatch batch;
    private Texture image;
    private GameScreen pausedGame;

    @Override
    /** Initialises shared rendering resources and opens the main menu. */
    public void create() {
        batch = new SpriteBatch();
        image = new Texture("original assets/libgdx.png");
        setScreen(new MainMenuScreen(this));
    }

    @Override
    /** Delegates rendering to the currently active libGDX screen. */
    public void render() {
        super.render();
    }

    /** Opens the menu without disposing the supplied game, so it can later resume unchanged. */
    public void showPauseMenu(GameScreen pausedGame) {
        this.pausedGame = pausedGame;
        setScreen(new PauseScreen(this, pausedGame));
    }

    /** Restores a paused game and releases the menu which was displaying above it. */
    public void resumeGame(GameScreen gameScreen) {
        Screen pauseMenu = getScreen();
        pausedGame = null;
        setScreen(gameScreen);
        pauseMenu.dispose();
    }
    public void showWinScreen(GameScreen completedGame) {
        showEndScreen(completedGame, "YOU WON!");
    }

    /** Shows the end screen when the player has used all available time. */
    public void showTimeExpiredScreen(GameScreen completedGame) {
        showEndScreen(completedGame, "YOU RAN OUT OF TIME");
    }

    /** Shows the end screen when the geese have depleted the player's health. */
    public void showChasedHomeScreen(GameScreen completedGame) {
        showEndScreen(completedGame, "THE GEESE CHASED YOU BACK HOME");
    }

    private void showEndScreen(GameScreen completedGame, String message) {
        setScreen(new WinScreen(this, message, GameScreen.points, GameScreen.timeRemaining, GameScreen.formatTimeRemaining()));
        completedGame.dispose();
    }
    /** Starts a fresh game and releases the menu that launched it. */
    public void startNewGame() {
        Screen mainMenu = getScreen();
        setScreen(new GameScreen(this));
        mainMenu.dispose();
    }

    public void showMainMenu() {
        Screen winScreen = getScreen();
        setScreen(new MainMenuScreen(this));
        winScreen.dispose();
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
