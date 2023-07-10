package com.ruoq.customview.surfaceviewcharge

class BubbleBean(var x: Float, var y: Float, randomY: Float, index: Int) {
    private var randomY = 3f
    var index: Int
        private set

    operator fun set(x: Float, y: Float, randomY: Float, index: Int) {
        this.x = x
        this.y = y
        this.randomY = randomY
        this.index = index
    }

    fun setMove(screenHeight: Int, maxDistance: Int) {
        if (y - maxDistance < 110) {
            y -= 2f
            return
        }
        if (maxDistance <= y && screenHeight - y > 110) {
            y -= randomY
        } else {
            //气泡刚开始移动的时候速度比较慢，形成粘性气泡
            y -= 0.6f
        }
        if (index == 0) {
            x -= 0.4f
        } else if (index == 2) {
            x += 0.4f
        }
    }

    init {
        this.randomY = randomY
        this.index = index
    }
}