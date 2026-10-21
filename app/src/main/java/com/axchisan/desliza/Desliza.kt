package com.axchisan.desliza

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder

class Desliza : Application(), SingletonImageLoader.Factory {
    override fun newImageLoader(context: PlatformContext) = ImageLoader.Builder(context)
        .components { add(VideoFrameDecoder.Factory()) }
        .crossfade(120)
        .build()
}
