package jp.techacademy.drms.jumpactiongame

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.viewport.FitViewport
import java.util.*
import kotlin.collections.ArrayList


class GameScreen (private val mGame : JumpActionGame) : ScreenAdapter(){
    companion object{
        val CAMERA_WIDTH  = 10f
        val CAMERA_HEIGHT = 15f

        val WORLD_WIDTH = CAMERA_WIDTH
        val WORLD_HEIGHT = CAMERA_HEIGHT * 20    // 20画面分登れば終了
        val GUI_WIDTH = 320f
        val GUI_HEIGHT = 480f


        val GAME_STATE_READY = 0
        val GAME_STATE_PLAYING = 1
        val GAME_STATE_GAMEOVER = 2

        // 重力
        val GRAVITY = -12
    }

    private val mBg : Sprite
    private val mCamera : OrthographicCamera
    private val mGuiCamera: OrthographicCamera
    private val mViewPort: FitViewport
    private val mGuiViewPort: FitViewport

    private var mRandom: Random
    private var mSteps: ArrayList<Step>
    private var mStars: ArrayList<Star>
    private var enemies : ArrayList<Enemy>
    private lateinit var mUfo: Ufo
    private lateinit var mPlayer: Player

    private var mGameState: Int
    private var mHeightSoFar: Float = 0f
    private var mTouchPoint: Vector3
    private var mFont: BitmapFont
    private var mScore: Int
    private var mHighScore: Int
    private var mPrefs: Preferences

    private var sound : Sound;

    init{
        val bgTexture = Texture("back.png")
        mBg = Sprite(TextureRegion(bgTexture, 0, 0, 540, 810))
        mBg.setSize(CAMERA_WIDTH, CAMERA_HEIGHT)
        mBg.setPosition(0f, 0f)

        mCamera = OrthographicCamera()
        mCamera.setToOrtho(false, CAMERA_WIDTH, CAMERA_HEIGHT)
        mViewPort = FitViewport(CAMERA_WIDTH, CAMERA_HEIGHT, mCamera)

        // GUI用のカメラを設定する
        mGuiCamera = OrthographicCamera()
        mGuiCamera.setToOrtho(false, GUI_WIDTH, GUI_HEIGHT)
        mGuiViewPort = FitViewport(GUI_WIDTH, GUI_HEIGHT, mGuiCamera)

        // プロパティの初期化
        mRandom = Random()
        mSteps = ArrayList<Step>()
        mStars = ArrayList<Star>()
        enemies = ArrayList<Enemy>()
        mGameState = GAME_STATE_READY
        mTouchPoint = Vector3()

        mFont = BitmapFont(Gdx.files.internal("font.fnt"), Gdx.files.internal("font.png"), false)
        mFont.data.setScale(0.8f)
        mScore = 0
        mHighScore = 0

        // ハイスコアをPreferencesから取得する
        mPrefs = Gdx.app.getPreferences("jp.techacademy.drms.jumpactiongame") // ←追加する
        mHighScore = mPrefs.getInteger("HIGHSCORE", 0)


        sound = Gdx.audio.newSound(Gdx.files.internal("chin.mp3"))

        createStage()
    }

    override fun render(delta: Float) {
        // それぞれの状態をアップデートする
        update(delta)

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)


        // カメラの中心を超えたらカメラを上に移動させる つまりキャラが画面の上半分には絶対に行かない
        if (mPlayer.y > mCamera.position.y) {
            mCamera.position.y = mPlayer.y
        }

        // カメラの座標をアップデート（計算）し、スプライトの表示に反映させる
        mCamera.update()
        mGame.batch.projectionMatrix = mCamera.combined

        mGame.batch.begin()

        // 背景
        // 原点は左下
        mBg.setPosition(mCamera.position.x - CAMERA_WIDTH / 2, mCamera.position.y - CAMERA_HEIGHT / 2)
        mBg.draw(mGame.batch)

        // Step
        for (i in 0 until mSteps.size) {
            mSteps[i].draw(mGame.batch)
        }

        // Star
        for (i in 0 until mStars.size) {
            mStars[i].draw(mGame.batch)
        }

        // UFO
        mUfo.draw(mGame.batch)

        //Player
        mPlayer.draw(mGame.batch)

        // Enemy
        for (enemy in enemies){
            enemy.draw(mGame.batch)
        }

        mGame.batch.end()

        // スコア表示
        mGuiCamera.update()
        mGame.batch.projectionMatrix = mGuiCamera.combined
        mGame.batch.begin()
        mFont.draw(mGame.batch, "HighScore: $mHighScore", 16f, GUI_HEIGHT - 15)
        mFont.draw(mGame.batch, "Score: $mScore", 16f, GUI_HEIGHT - 35)
        mGame.batch.end()

    }

    override fun resize(width: Int, height: Int) {
        mViewPort.update(width, height)
        mGuiViewPort.update(width, height)
    }

    // ステージを作成する
    private fun createStage() {

        // テクスチャの準備
        val stepTexture = Texture("step.png")
        val starTexture = Texture("star.png")
        val playerTexture = Texture("uma.png")
        val ufoTexture = Texture("ufo.png")

        // StepとStarをゴールの高さまで配置していく
        var y = 0f

        val maxJumpHeight = Player.PLAYER_JUMP_VELOCITY * Player.PLAYER_JUMP_VELOCITY / (2 * -GRAVITY)
        while (y < WORLD_HEIGHT - 5) {
            val type = if(mRandom.nextFloat() > 0.8f) Step.STEP_TYPE_MOVING else Step.STEP_TYPE_STATIC
            val x = mRandom.nextFloat() * (WORLD_WIDTH - Step.STEP_WIDTH)

            val step = Step(type, stepTexture, 0, 0, 144, 36)
            step.setPosition(x, y)
            mSteps.add(step)

            if (mRandom.nextFloat() > 0.6f) {
                val star = Star(starTexture, 0, 0, 72, 72)
                star.setPosition(step.x + mRandom.nextFloat(), step.y + Star.STAR_HEIGHT + mRandom.nextFloat() * 3)
                mStars.add(star)
            }

            y += (maxJumpHeight - 0.5f)
            y -= mRandom.nextFloat() * (maxJumpHeight / 3)
        }

        // Playerを配置
        mPlayer = Player(playerTexture, 0, 0, 72, 72)
        mPlayer.setPosition(WORLD_WIDTH / 2 - mPlayer.width / 2, Step.STEP_HEIGHT)

        // ゴールのUFOを配置
        mUfo = Ufo(ufoTexture, 0, 0, 120, 74)
        mUfo.setPosition(WORLD_WIDTH / 2 - Ufo.UFO_WIDTH / 2, y)

        // 敵を作成
        initializeEnemies()
    }

    private fun initializeEnemies(){
        // 敵はステップ毎に作成する（ステップ位置に乱数を加え敵の位置を決定する）
        val maxJumpHeight = Player.PLAYER_JUMP_VELOCITY * Player.PLAYER_JUMP_VELOCITY / (2 * -GRAVITY)
        var above : Float = 0f
        val enemyTexture = Texture("enemy.png")

        for(step in mSteps){
            val enemy = Enemy(enemyTexture, 0, 0, 72, 72)
            above = maxJumpHeight * mRandom.nextFloat() + 1
            enemy.setPosition(step.x, step.y + above)
            enemies.add(enemy)
        }
    }

    // それぞれのオブジェクトの状態をアップデートする
    private fun update(delta: Float) {
        when (mGameState) {
            GAME_STATE_READY    -> updateReady()
            GAME_STATE_PLAYING  -> updatePlaying(delta)
            GAME_STATE_GAMEOVER -> updateGameOver()
        }
    }

    private fun updateReady() {
        if (Gdx.input.justTouched()) {
            mGameState = GAME_STATE_PLAYING
        }
    }

    private fun updatePlaying(delta: Float) {
        var accel = 0f
        if (Gdx.input.isTouched) {
            mGuiViewPort.unproject(mTouchPoint.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f))

            //画面の左側半分であれば5.0f,　そうでなければ -5.0f
            val left = Rectangle(0f, 0f, GUI_WIDTH / 2, GUI_HEIGHT)
            accel = if(left.contains(mTouchPoint.x, mTouchPoint.y)) 5.0f else -5.0f
        }

        // Step
        for (i in 0 until mSteps.size) {
            mSteps[i].update(delta)
        }

        // Player
        if (mPlayer.y <= 0.5f) {
            mPlayer.hitStep()
        }
        mPlayer.update(delta, accel)
        mHeightSoFar = Math.max(mPlayer.y, mHeightSoFar)

        // 当たり判定を行う
        checkCollision()

        // ゲームオーバーか判断する
        checkGameOver()
    }

    private fun checkCollision() {
        // UFO(ゴールとの当たり判定)
        if (mPlayer.boundingRectangle.overlaps(mUfo.boundingRectangle)) {
            mGameState = GAME_STATE_GAMEOVER
            return
        }

        // Starとの当たり判定
        for (i in 0 until mStars.size) {
            val star = mStars[i]

            if (star.mState == Star.STAR_NONE) continue

            if (mPlayer.boundingRectangle.overlaps(star.boundingRectangle)) {
                star.get()
                mScore++
                mHighScore = if (mScore > mHighScore) mScore else mHighScore
                mPrefs.putInteger("HIGHSCORE", mHighScore)
                mPrefs.flush()
                break
            }
        }


        // Enemyとの当たり判定
        for (enemy in enemies) {
            if (mPlayer.boundingRectangle.overlaps(enemy.boundingRectangle)) {
                mGameState = GAME_STATE_GAMEOVER
                sound.play(1.0f)
                return
            }
        }

        // Stepとの当たり判定
        // 上昇中はStepとの当たり判定を確認しない
        if (mPlayer.velocity.y > 0) return

        for (i in 0 until mSteps.size) {
            val step = mSteps[i]

            if (step.mState == Step.STEP_STATE_VANISH) {
                continue
            }

            if (mPlayer.y > step.y) {
                if (mPlayer.boundingRectangle.overlaps(step.boundingRectangle)) {
                    mPlayer.hitStep()
                    if (mRandom.nextFloat() > 0.5f) {
                        step.vanish()
                    }
                    break
                }
            }
        }
    }

    private fun updateGameOver() {
        if (Gdx.input.justTouched()) {
            mGame.screen = ResultScreen(mGame, mScore)
        }
    }

    private fun checkGameOver() {
        if (mHeightSoFar - CAMERA_HEIGHT / 2 > mPlayer.y) {
            Gdx.app.log("JampActionGame", "GAMEOVER")
            mGameState = GAME_STATE_GAMEOVER
        }
    }
}