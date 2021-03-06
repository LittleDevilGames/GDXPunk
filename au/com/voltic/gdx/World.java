package au.com.voltic.gdx;

import java.util.ArrayList;

import au.com.voltic.gdx.ogmo.TileLayer;
import au.com.voltic.lidgdxtest.MyGame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

public class World {
    
    //TODO moveForward(Entity e)
    //TODO moveBack(Entity e)
    //TODO bringToFront(Entity e)
    //TODO sendToBack(Entity e)

    private ArrayList<Entity> entities = new ArrayList<Entity>();
    private ArrayList<Group> groups = new ArrayList<Group>();
    
    private BitmapFont font = new BitmapFont(Gdx.files.internal("data/font.fnt"),
                                             Gdx.files.internal("data/font.png"),
                                             true);
    
    private Array<Polygon> debugPolygons;
    private ShapeRenderer shapeRenderer = new ShapeRenderer();
    
    public Boolean drawDebug = false;
    
    //Rendering
    private ObjectMap<Integer, Entity> renderingLayers = new ObjectMap<Integer, Entity>();
    private ObjectMap<Integer, TileLayer> tileLayers = new ObjectMap<Integer, TileLayer>();
    
    public Camera camera;
    
    private String name = "DEFAULT_NAME";
    
    public World(String name) {
        this.name = name;
    }
    
    /**
     * Add an Entity
     * @param e The Entity
     */
    public void add(Entity e)
    {
        if (!entities.contains(e))
        {
            entities.add(e);
            e.world = this;
        }
        
        setUpRendering(e);
        e.added();
    }
    
    public void setUpRendering(Entity e)
    {
        if (e.world == null) return;
        
        if (!renderingLayers.containsKey(e.getLayer()))
        {
            renderingLayers.put(e.getLayer(), e);
        } else {
            Entity current = renderingLayers.get(e.getLayer());
            while (current.renderNext != null)
            {
                current = current.renderNext;
            }
            current.renderNext = e;
        }
    }
    
    public void removeEntityFromLayer(Entity e, int layer)
    {
        if (renderingLayers.get(layer) != e) return;
        
        if (renderingLayers.get(layer) == null) return;
        
        if (renderingLayers.get(layer).renderNext != null)
        {
            renderingLayers.put(layer, renderingLayers.get(layer).renderNext);
        } else {
            renderingLayers.remove(layer);
        }
    }
    
    /**
     * Add a list of Entities
     * @param list
     */
    public void add(Array<Entity> list)
    {
        for (Entity e : list)
        {
            if (e != null) add(e);
        }
    }
    
    public void add(Group g)
    {   
        groups.add(g);
        for (int i = 0; i < g.size(); i++)
        {
            entities.add(g.get(i));
            g.get(i).world = this;
            
            setUpRendering(g.get(i));
            g.get(i).added();
        }
    }
    
    /**
     * Add a TileLayer
     * @param t The TileLayer
     */
    public void add(TileLayer t)
    {
        t.world = this;
        tileLayers.put(t.layer, t);
    }
    
    /**
     * Remove an Entity from the container, if you want to destroy it use destroy() instead.
     * @param e The Entity
     */
    public void remove(Entity e)
    {
        entities.remove(e);
        e.world = null;
    }
    
    /**
     * Completely remove an Entity from memory.
     * 
     * @param e The Entity
     */
    public void destroy(Entity e)
    {
        entities.remove(e);
        e.destroy();
        e = null;
    }
    
    /**
     * Remove a TileLayer from the container
     * @param t The TileLayer
     */
    public void remove(TileLayer t)
    {
        tileLayers.remove(t.layer);
    }
    
    public void remove(Group g)
    {
        for (int i = 0; i < g.size(); i++)
        {
            g.get(i).world.remove(g.get(i));
        }
    }
    
    /**
     * Called when the world is created, override this.
     */
    public void create()
    {
        
    }

    /**
     * Called once a frame, updates all children. Override this if you want.
     */
    public void update()
    {
        for (Entity e : entities)
        {
            e.update(); 
        }
        
        for (Group g : groups)
        {
            g.update();
        }
        
        if (drawDebug) debugPolygons = getPolygons();
        
        setRenderBounds((int)camera.position.x - MyGame.VIRTUAL_WIDTH / 2 - MyGame.TILE_SIZE,
                (int)camera.position.y - MyGame.VIRTUAL_HEIGHT / 2 - + MyGame.TILE_SIZE,
                MyGame.VIRTUAL_WIDTH + (MyGame.TILE_SIZE * 2),
                MyGame.VIRTUAL_HEIGHT + (MyGame.TILE_SIZE * 2));
    }

    
    /**
     * Draw this World
     * @param batch
     */
    public void draw(SpriteBatch batch)
    {
        //TODO There are probably performance enhancements to be done
        Entity first;
        
        Array<Integer> keys = new Array<Integer>();
        
        for (int i : renderingLayers.keys()) keys.add(i);
        for (int i : tileLayers.keys()) if (!keys.contains(i, true)) keys.add(i);
        keys.sort();
        
        for (int i : keys)
        {
            if (tileLayers.get(i) != null)
            {
                tileLayers.get(i).draw(batch);
            }
            
            if (renderingLayers.get(i) != null)
            { 
                //log("Draw layer: " + i);
                first = renderingLayers.get(i);
                first.draw(batch);
                
                while (first.renderNext != null)
                {
                    first = first.renderNext;
                    first.draw(batch);
                }
            }
        }
        
       // log("Drew!");
        
        if (drawDebug)
        {
            batch.end();
            
            Gdx.gl.glEnable(GL10.GL_BLEND);
            Gdx.gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
            shapeRenderer.setProjectionMatrix(camera.combined);
                    
            shapeRenderer.begin(ShapeType.Line);
            for (Polygon r : debugPolygons)
            {
                shapeRenderer.setColor(0f, 1f, 0f, 0.5f);
                shapeRenderer.polygon(r.getTransformedVertices());
                shapeRenderer.setColor(1f, 0f, 1f, 0.5f);
                shapeRenderer.rect(r.getBoundingRectangle().x, r.getBoundingRectangle().y, r.getBoundingRectangle().width, r.getBoundingRectangle().height);
            }
            shapeRenderer.end();
            Gdx.gl.glDisable(GL10.GL_BLEND);
            
            batch.begin();
            
            font.draw(batch, "ENTS: " + this.objectCount(), 4, 30);
            font.draw(batch, "FPS: " + Gdx.graphics.getFramesPerSecond(), 4, 20);
        }
    }
    
    /**
     * Dispose
     */
    public void dispose()
    {
        for (Entity e : entities)
        {
            e.dispose();
        }
        
        for (TileLayer t : tileLayers.values())
        {
            t.dispose();
        }
    }
    
    protected void log(String str)
    {
        Gdx.app.log("World", str);
    }
    
    public String toString()
    {
        return "World: " + name;
    }
    
    /**
     * Update the RenderBounds of every TileLayer recursively.
     * @param x
     * @param y
     * @param width
     * @param height
     */
    public void setRenderBounds(int x, int y, int width, int height)
    {
        for (TileLayer t : tileLayers.values()){
            t.setRenderBounds(x, y, width, height);
        }
    }
    
    /**
     * Return a list of entities within a rectangle
     * @param r
     * @return List of entities
     */
    public ArrayList<Entity> getEntitiesWithinZone(Rectangle r)
    {
        return getEntitiesWithinZone(r.x, r.y, r.width, r.height);
    }
    
    /**
     * Retrieve all the entities within a certain rectangle of the world.
     * @param x
     * @param y
     * @param width
     * @param height
     * @return
     */
    public ArrayList<Entity> getEntitiesWithinZone(float x, float y, float width, float height)
    {
        ArrayList<Entity> ents = new ArrayList<Entity>();
        
        for (Entity e : entities)
        {
            if (e.x > x && e.x < width && e.y > y && e.y < height)
            {
                ents.add(e);
            }
        }

        return ents;
    }
    
    /**
     * Get a list of all collision polygons from every entity in the world.
     * @return List of polys
     */
    public Array<Polygon> getPolygons()
    {
        Array<Polygon> polys = new Array<Polygon>();
        
        for (Entity e : entities)
        {
            if (e.hitbox != null) polys.add(e.hitbox);
        }

        return polys;
    }

    public int objectCount()
    {
        return entities.size();
    }
}
