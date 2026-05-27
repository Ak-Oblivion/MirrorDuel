package com.mirrorduel.core;

import com.badlogic.gdx.graphics.Color;
import java.util.List;

/** Holds a player's pieces and metadata. */
public class Player {

    private final int     index;
    private final String  name;
    private final Color   color;
    private final Color   laserColor;

    private LaserEmitter  emitter;
    private Crystal       crystal;
    private List<Mirror>  mirrors;

    public static final Color P1_COLOR       = new Color(0.1f, 0.6f, 1.0f, 1f);  // electric blue
    public static final Color P1_LASER_COLOR = new Color(0.2f, 0.8f, 1.0f, 1f);
    public static final Color P2_COLOR       = new Color(1.0f, 0.15f, 0.15f, 1f); // crimson red
    public static final Color P2_LASER_COLOR = new Color(1.0f, 0.3f, 0.3f, 1f);

    public Player(int index, String name, Color color, Color laserColor) {
        this.index      = index;
        this.name       = name;
        this.color      = color;
        this.laserColor = laserColor;
    }

    public int          getIndex()       { return index; }
    public String       getName()        { return name; }
    public Color        getColor()       { return color; }
    public Color        getLaserColor()  { return laserColor; }
    public LaserEmitter getEmitter()     { return emitter; }
    public Crystal      getCrystal()     { return crystal; }
    public List<Mirror> getMirrors()     { return mirrors; }

    public void setEmitter(LaserEmitter e) { emitter = e; }
    public void setCrystal(Crystal c)      { crystal = c; }
    public void setMirrors(List<Mirror> m) { mirrors = m; }
}
