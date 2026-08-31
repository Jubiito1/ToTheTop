package com.TfPSR.ToTheTop.asset;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;

public class AudioManager {
    private float musicVolume = 1.0f;
    private float soundVolume = 1.0f;
    private Music currentMusic = null;

    public void setMusicVolume(float musicVolume){
        this.musicVolume = MathUtils.clamp(musicVolume, 0f, 1f);
        if(this.currentMusic != null){
            currentMusic.setVolume(this.musicVolume);
        }
    }

    public void setSoundVolume(float soundVolume){
        this.soundVolume = MathUtils.clamp(soundVolume, 0f, 1f);
    }

    public void playMusic(Music music){
        if(this.currentMusic == music && this.currentMusic.isPlaying()){
            return;
        }
        if(this.currentMusic != null){
            stopMusic();
        }

        this.currentMusic = music;
        this.currentMusic.setVolume(musicVolume);
        this.currentMusic.setLooping(true);
        this.currentMusic.play();
    }

    public void stopMusic(){
        if(currentMusic != null){
        currentMusic.stop();
        this.currentMusic =  null;
        }
    }

    public void playSound(Sound sound){
        sound.play(soundVolume);
    }

    public float getMusicVolume() {
        return musicVolume;
    }

    public float getSoundVolume() {
        return soundVolume;
    }
}
