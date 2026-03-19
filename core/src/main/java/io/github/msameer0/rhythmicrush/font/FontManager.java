package io.github.msameer0.rhythmicrush.font;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

import io.github.msameer0.rhythmicrush.RhythmicRushGame;

/**
 * Generates and owns all BitmapFonts used across the game.
 * Created once in {@link RhythmicRushGame#create()} so no screen ever
 * blocks the main thread waiting for FreeType to rasterize a font.
 * <p>
 * Screens should call {@link #get(int)} with one of the SIZE_* constants.
 * Do NOT dispose fonts obtained from here — FontManager owns their lifecycle.
 */
public class FontManager {

    public static final int SIZE_SMALL = 22;
    public static final int SIZE_MEDIUM = 28;
    public static final int SIZE_LARGE = 32;
    public static final int SIZE_XLARGE = 48;

    private final BitmapFont[] fonts;
    private static final int[] SIZES = {SIZE_SMALL, SIZE_MEDIUM, SIZE_LARGE, SIZE_XLARGE};

    /**
     * Constructs a new FontManager and initializes all predefined font sizes.
     * <p>
     * This constructor loads the "zendots-regular.ttf" font file and generates
     * {@link BitmapFont} instances for each size defined in {@code SIZES}.
     * It applies linear filtering and generates mipmaps for better scaling quality.
     * If the font file cannot be loaded, it falls back to the default LibGDX {@link BitmapFont}.
     */
    public FontManager() {
        Gdx.app.log("FontManager", "Initializing fonts...");
        fonts = new BitmapFont[SIZES.length];
        FreeTypeFontGenerator gen = null;
        try {
            gen = new FreeTypeFontGenerator(Gdx.files.internal("fonts/zendots-regular.ttf"));
            FreeTypeFontGenerator.FreeTypeFontParameter p =
                new FreeTypeFontGenerator.FreeTypeFontParameter();
            p.magFilter = Texture.TextureFilter.Linear;
            p.minFilter = Texture.TextureFilter.MipMapLinearLinear;
            p.genMipMaps = true;
            for (int i = 0; i < SIZES.length; i++) {
                p.size = SIZES[i];
                fonts[i] = gen.generateFont(p);
            }
            Gdx.app.log("FontManager", "Fonts initialized successfully.");
        } catch (Exception e) {
            Gdx.app.error("FontManager", "Could not load font: " + e.getMessage());
            for (int i = 0; i < SIZES.length; i++) {
                if (fonts[i] == null) fonts[i] = new BitmapFont();
            }
        } finally {
            if (gen != null) gen.dispose();
        }
    }

    public void dispose() {
        Gdx.app.log("FontManager", "Disposing fonts...");
        for (BitmapFont f : fonts) if (f != null) f.dispose();
        Gdx.app.log("FontManager", "Fonts disposed.");
    }
    public BitmapFont get(int size) {
        int best = 0;
        int bestDiff = Math.abs(SIZES[0] - size);
        for (int i = 1; i < SIZES.length; i++) {
            int diff = Math.abs(SIZES[i] - size);
            if (diff < bestDiff) {
                bestDiff = diff;
                best = i;
            }
        }
        return fonts[best];
    }
}
