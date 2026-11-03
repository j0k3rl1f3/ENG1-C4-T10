package ENG1.Game;
import ENG1.Game.Screens.GameScreen;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

/** libGDX application entry point which creates and owns the active game screen. */
public class Main extends Game {
    private SpriteBatch batch;
    private Texture image;

    @Override
    /** Initialises shared rendering resources and opens the main game screen. */
    public void create() {
        batch = new SpriteBatch();
        image = new Texture("original assets/libgdx.png");
        setScreen(new GameScreen());
    }

    @Override
    /** Delegates rendering to the currently active libGDX screen. */
    public void render() {
        super.render();
    }

    @Override
    /** Releases resources owned by the application. */
    public void dispose() {
        super.dispose();
        batch.dispose();
        image.dispose();
    }
}
