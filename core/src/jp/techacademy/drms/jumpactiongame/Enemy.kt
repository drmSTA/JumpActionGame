package jp.techacademy.drms.jumpactiongame

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite

class Enemy(texture: Texture, srcX: Int, srcY: Int, srcWidth: Int, srcHeight: Int)
    : Sprite(texture, srcX, srcY, srcWidth, srcHeight) {

    companion object {
        // 横幅、高さ
        val ENEMY_WIDTH = 0.5f
        val ENEMY_HEIGHT = 0.5f

    }


    init {
        setSize(ENEMY_WIDTH, ENEMY_HEIGHT)
    }
}