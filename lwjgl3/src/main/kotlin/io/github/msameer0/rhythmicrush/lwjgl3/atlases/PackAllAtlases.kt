package io.github.msameer0.rhythmicrush.lwjgl3.atlases

object PackAllAtlases {
    @JvmStatic
    fun main(args: Array<String>) {
        PackMenuAtlas.main(args)
        PackBlocksAtlas.main(args)
        PackGamemodesAtlas.main(args)
        PackLevelSelectAtlas.main(args)
        PackOrbsAtlas.main(args)
        PackPadsAtlas.main(args)
        PackPortalsAtlas.main(args)
        PackSpikesAtlas.main(args)

        println("All atlases packed for all qualities!")
    }
}
