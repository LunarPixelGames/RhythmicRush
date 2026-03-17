package io.github.msameer0.rhythmicrush.lwjgl3.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import io.github.msameer0.rhythmicrush.game.gameplay.blocks.BlockType;
import io.github.msameer0.rhythmicrush.game.level.LevelData;

import com.badlogic.gdx.utils.Array;
import java.util.EnumMap;
import java.util.Map;

public class EditorObjectRenderer {

    private final SpriteBatch   batch;
    private final ShapeRenderer shapes;
    private final OrthographicCamera camera;

    // ── Block atlas ───────────────────────────────────────────────────────────
    private TextureAtlas blocksAtlas;
    private final Map<BlockType, TextureRegion> blockRegions = new EnumMap<>(BlockType.class);

    private boolean atlasLoaded = false;

    private TextureAtlas spikesAtlas;
    private TextureRegion spikeRegion;

    // ─────────────────────────────────────────────────────────────────────────

    public EditorObjectRenderer(SpriteBatch batch, ShapeRenderer shapes, OrthographicCamera camera) {
        this.batch  = batch;
        this.shapes = shapes;
        this.camera = camera;
        tryLoadAtlas();
    }

    private void tryLoadAtlas() {
        try {
            blocksAtlas = new TextureAtlas(Gdx.files.internal("game/objects/blocks.atlas"));
            for (BlockType type : BlockType.values()) {
                TextureRegion region = blocksAtlas.findRegion(type.textureName);
                if (region != null) blockRegions.put(type, region);
                else System.err.println("[EditorRenderer] Missing region: " + type.textureName);
            }
            atlasLoaded = true;
        } catch (Exception e) {
            System.err.println("[EditorRenderer] Could not load blocks atlas, falling back to colors: " + e.getMessage());
            atlasLoaded = false;
        }

        try {
            spikesAtlas = new TextureAtlas(Gdx.files.internal("game/objects/spikes.atlas"));
            spikeRegion = spikesAtlas.findRegion("spike");
        } catch (Exception e) {
            System.err.println("[EditorRenderer] Could not load spikes atlas: " + e.getMessage());
        }

    }

    // ── Draw all placed objects ───────────────────────────────────────────────

    public void draw(Array<LevelData.ObjectEntry> placed) {
        drawTexturedObjects(placed);
        drawFallbackObjects(placed); // colored rects for non-textured types (portals, spikes)
    }

    private void drawTexturedObjects(Array<LevelData.ObjectEntry> placed) {
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (LevelData.ObjectEntry e : placed) {
            if (e.type.equals("block") && atlasLoaded) {
                BlockType bt = resolveBlockType(e.blockType);
                TextureRegion region = blockRegions.get(bt);
                if (region != null) {
                    batch.draw(region, e.x, e.y, e.size, e.size);
                }
            } else if (e.type.equals("spike") && spikeRegion != null) {
                batch.draw(spikeRegion,
                    e.x, e.y,
                    e.size / 2f, e.size / 2f,
                    e.size, e.size,
                    1f, 1f,
                    e.rotation);
            }
        }
        batch.end();
    }

    private void drawFallbackObjects(Array<LevelData.ObjectEntry> placed) {
        // separate pass for textured non-block objects
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (LevelData.ObjectEntry e : placed) {
            if (e.type.equals("block") && atlasLoaded) continue;
            if (e.type.equals("spike") && spikeRegion != null) {
                batch.draw(spikeRegion, e.x, e.y, e.size, e.size);
                continue;
            }
        }
        batch.end();

        // colored rect fallback for anything without a texture (portals etc.)
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (LevelData.ObjectEntry e : placed) {
            if (e.type.equals("block") && atlasLoaded) continue;
            if (e.type.equals("spike") && spikeRegion != null) continue;
            shapes.setColor(fallbackColor(e.type));
            shapes.rect(e.x, e.y, e.size, e.size);
        }
        shapes.end();
    }

    // ── Cursor preview ────────────────────────────────────────────────────────

    /**
     * Draws a semi-transparent preview at the cursor position.
     * For blocks, shows the actual texture tinted at 50% alpha if atlas is loaded.
     */
    public void drawCursorPreview(float wx, float wy, float size,
                                  String paletteType, BlockType blockType, float rotation) {
        TextureRegion previewRegion = null;

        if (paletteType.equals("block") && atlasLoaded) {
            previewRegion = blockRegions.get(blockType);
        } else if (paletteType.equals("spike") && spikeRegion != null) {
            previewRegion = spikeRegion;
        }

        if (previewRegion != null) {
            batch.setProjectionMatrix(camera.combined);
            batch.begin();
            batch.setColor(1f, 1f, 1f, 0.5f);
            batch.draw(previewRegion, wx, wy, size, size);
            batch.setColor(Color.WHITE);
            batch.end();

            shapes.setProjectionMatrix(camera.combined);
            shapes.begin(ShapeRenderer.ShapeType.Line);
            shapes.setColor(Color.WHITE);
            shapes.rect(wx, wy, size, size);
            shapes.end();
            return;
        }

        if (previewRegion != null) {
            batch.setProjectionMatrix(camera.combined);
            batch.begin();
            batch.setColor(1f, 1f, 1f, 0.5f);
            batch.draw(previewRegion,
                wx, wy,
                size / 2f, size / 2f,   // origin = center
                size, size,
                1f, 1f,
                rotation);              // ← apply rotation
            batch.setColor(Color.WHITE);
            batch.end();
            // outline...
            return;
        }

        // fallback colored preview
        Color c = new Color(fallbackColor(paletteType));
        c.a = 0.5f;
        shapes.setProjectionMatrix(camera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(c);
        shapes.rect(wx, wy, size, size);
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(Color.WHITE);
        shapes.rect(wx, wy, size, size);
        shapes.end();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private BlockType resolveBlockType(String textureName) {
        if (textureName == null) return BlockType.DEFAULT;
        for (BlockType t : BlockType.values()) {
            if (t.textureName.equals(textureName)) return t;
        }
        return BlockType.DEFAULT;
    }

    private Color fallbackColor(String type) {
        switch (type) {
            case "block":       return new Color(0.55f, 0.55f, 0.85f, 1f);
            case "spike":       return new Color(0.85f, 0.25f, 0.25f, 1f);
            case "cube_portal": return new Color(0.25f, 0.85f, 0.55f, 1f);
            case "ship_portal": return new Color(0.25f, 0.55f, 0.85f, 1f);
            default:            return Color.GRAY;
        }
    }

    public void dispose() {
        if (blocksAtlas != null) blocksAtlas.dispose();
        if (spikesAtlas != null) spikesAtlas.dispose();
    }
}
