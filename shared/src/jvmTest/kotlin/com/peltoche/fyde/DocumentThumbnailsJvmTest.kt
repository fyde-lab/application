package com.peltoche.fyde

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking

// On the JVM, since decoding the SDK's PNGs takes the platform's codec.
class DocumentThumbnailsJvmTest {
    private fun png(width: Int, height: Int): ByteArray {
        val out = ByteArrayOutputStream()
        ImageIO.write(BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "png", out)
        return out.toByteArray()
    }

    private fun key(id: String, size: Long = 1) = DocumentThumbnails.Key(id, size = size)

    @Test
    fun drawsEachDocumentOnceThenKeepsItsImage() = runBlocking<Unit> {
        val calls = mutableListOf<String>()
        val thumbnails = DocumentThumbnails({ id ->
            calls += id
            Result.success(FydeThumbnail(png(30, 40), 30, 40))
        })

        val first = thumbnails.load(key("a"))
        val second = thumbnails.load(key("a"))

        assertNotNull(first)
        assertEquals(30 to 40, first.width to first.height)
        assertEquals(first, second)
        assertEquals(listOf("a"), calls)
        assertEquals(first, thumbnails.peek(key("a"))?.image)
    }

    @Test
    fun drawsADocumentAgainOnceItsFileChanges() = runBlocking<Unit> {
        var calls = 0
        val thumbnails = DocumentThumbnails({ _ ->
            calls++
            Result.success(FydeThumbnail(png(30, 40), 30, 40))
        })

        thumbnails.load(key("a", size = 1))
        thumbnails.load(key("a", size = 1))
        thumbnails.load(key("a", size = 2))

        assertEquals(2, calls)
    }

    @Test
    fun aDocumentNotCachedHereIsAskedForAgain() = runBlocking<Unit> {
        var calls = 0
        val thumbnails = DocumentThumbnails({ _ ->
            calls++
            Result.success(null)
        })

        assertNull(thumbnails.load(key("a")))
        assertNull(thumbnails.peek(key("a")))
        thumbnails.load(key("a"))

        assertEquals(2, calls)
    }

    @Test
    fun aDocumentTheSdkCannotDrawIsNotTriedAgain() = runBlocking<Unit> {
        var calls = 0
        val thumbnails = DocumentThumbnails({ _ ->
            calls++
            Result.failure(IllegalStateException("unreadable PDF"))
        })

        assertNull(thumbnails.load(key("a")))
        assertNull(thumbnails.load(key("a")))

        assertEquals(1, calls)
        assertNotNull(thumbnails.peek(key("a")))
    }

    @Test
    fun dropsTheLeastRecentlyUsedPastItsBudget() = runBlocking<Unit> {
        // Room for two 10 x 10 images (400 bytes each).
        val thumbnails = DocumentThumbnails({ _ -> Result.success(FydeThumbnail(png(10, 10), 10, 10)) }, budgetBytes = 800)

        thumbnails.load(key("a"))
        thumbnails.load(key("b"))
        thumbnails.peek(key("a")) // a is now the most recent
        thumbnails.load(key("c"))

        assertNotNull(thumbnails.peek(key("a")))
        assertNull(thumbnails.peek(key("b")))
        assertNotNull(thumbnails.peek(key("c")))
    }
}
