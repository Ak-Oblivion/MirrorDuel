package com.mirrorduel.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Disposable;

/**
 * AudioManager stub.
 *
 * Since no audio assets are bundled, all methods are no-ops.
 * Replace the stub methods with real Sound/Music loading once you add
 * .ogg/.wav files to assets/sounds/.
 *
 * To add sounds:
 *   Sound laserHum = Gdx.audio.newSound(Gdx.files.internal("sounds/laser.ogg"));
 */
public class AudioManager implements Disposable {

    private boolean enabled = false; // set to true once real assets exist

    public AudioManager() {
        // Could attempt to load sounds here with a try/catch
    }

    public void playLaserFire()      { /* stub */ }
    public void playReflect()        { /* stub */ }
    public void playCrystalHit()     { /* stub */ }
    public void playVictory()        { /* stub */ }
    public void playMenuClick()      { /* stub */ }
    public void playMirrorRotate()   { /* stub */ }
    public void playMirrorMove()     { /* stub */ }

    public void setEnabled(boolean b) { enabled = b; }
    public boolean isEnabled()        { return enabled; }

    @Override public void dispose() { /* nothing to dispose */ }
}
