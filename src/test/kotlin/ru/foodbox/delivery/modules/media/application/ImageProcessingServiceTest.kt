package ru.foodbox.delivery.modules.media.application

import org.junit.jupiter.api.Test
import javax.imageio.ImageWriteParam
import kotlin.test.assertEquals

class ImageProcessingServiceTest {

    @Test
    fun `configureCompression sets compression type before quality`() {
        val param = TestImageWriteParam()

        configureCompression(param, 80)

        assertEquals(ImageWriteParam.MODE_EXPLICIT, param.compressionMode)
        assertEquals("Lossy", param.compressionType)
        assertEquals(0.8f, param.compressionQuality)
    }

    @Test
    fun `configureCompression clamps quality to supported range`() {
        val highQualityParam = TestImageWriteParam()
        configureCompression(highQualityParam, 150)

        val lowQualityParam = TestImageWriteParam()
        configureCompression(lowQualityParam, -10)

        assertEquals(1.0f, highQualityParam.compressionQuality)
        assertEquals(0.0f, lowQualityParam.compressionQuality)
    }

    private class TestImageWriteParam : ImageWriteParam() {
        init {
            canWriteCompressed = true
            compressionTypes = arrayOf("Lossy", "Lossless")
        }
    }
}
