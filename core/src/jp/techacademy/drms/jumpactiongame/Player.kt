package jp.techacademy.drms.jumpactiongame

import com.badlogic.gdx.graphics.Texture

class Player (texture : Texture, srcX : Int, srcY : Int, srcWidth : Int, srcHeight : Int)
    : GameObject(texture, srcX, srcY, srcWidth, srcHeight){

    companion object{
        val PLAYER_WIDTH  = 1.0f
        val PLAYER_HEIGHT = 1.0f

        val PLAYER_STATE_JUMP = 0
        val PLAYER_STATE_FALL = 1

        val PLAYER_JUMP_VELOCITY = 11.0f
        val PLAYER_MOVE_VELOCITY = 20.0f
    }

    private var mState : Int

    init {
        setSize(PLAYER_WIDTH, PLAYER_HEIGHT)
        mState = PLAYER_STATE_FALL
    }

    fun update(delta : Float, accelX : Float){
        velocity.add(0f, GameScreen.GRAVITY * delta)
        velocity.x = -accelX / 10 * PLAYER_MOVE_VELOCITY
        setPosition(x + velocity.x * delta, y + velocity.y * delta)

        mState = if(velocity.y > 0) PLAYER_STATE_JUMP else PLAYER_STATE_FALL

        x = when{
            x + PLAYER_WIDTH / 2 < 0
                        -> GameScreen.WORLD_WIDTH - PLAYER_WIDTH / 2
            x + PLAYER_WIDTH / 2 > GameScreen.WORLD_WIDTH
                        -> 0f
            else        -> x
        }
    }

    fun hitStep(){
        velocity.y = PLAYER_JUMP_VELOCITY
        mState = PLAYER_STATE_JUMP
    }
}