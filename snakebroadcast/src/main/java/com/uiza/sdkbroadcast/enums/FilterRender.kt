package com.uiza.sdkbroadcast.enums

import android.graphics.PointF
import android.view.View
import com.pedro.encoder.input.gl.render.filters.*

enum class FilterRender(val filterRender: BaseFilterRender) {

    None(NoFilterRender()),
    AnalogTV(AnalogTVFilterRender()),
    AndroidView(AndroidViewFilterRender()),
    BasicDeformation(BasicDeformationFilterRender()),
    Beauty(BeautyFilterRender()),
    Black(BlackFilterRender()),
    Blur(BlurFilterRender()),
    Brightness(BrightnessFilterRender()),
    Cartoon(CartoonFilterRender()),
    Circle(CircleFilterRender()),
    Color(ColorFilterRender()),
    Contrast(ContrastFilterRender()),
    Duotone(DuotoneFilterRender()),
    EarlyBird(EarlyBirdFilterRender()),
    EdgeDetection(EdgeDetectionFilterRender()),
    Exposure(ExposureFilterRender()),
    Fire(FireFilterRender()),
    Gamma(GammaFilterRender()),
    Glitch(GlitchFilterRender()),
    GreyScale(GreyScaleFilterRender()),
    HalftoneLines(HalftoneLinesFilterRender()),
    Image70s(Image70sFilterRender()),
    Lamoish(LamoishFilterRender()),
    Money(MoneyFilterRender()),
    Negative(NegativeFilterRender()),
    Pixelated(PixelatedFilterRender()),
    Polygonization(PolygonizationFilterRender()),
    Rainbow(RainbowFilterRender()),
    RGBSaturation(RGBSaturationFilterRender()),
    Ripple(RippleFilterRender()),
    Rotation(RotationFilterRender()),
    Saturation(SaturationFilterRender()),
    Sepia(SepiaFilterRender()),
    Sharpness(SharpnessFilterRender()),
    Snow(SnowFilterRender()),
    Swirl(SwirlFilterRender()),
    Temperature(TemperatureFilterRender()),
    Zebra(ZebraFilterRender());

    /**
     * for [RotationFilterRender] or [AndroidViewFilterRender]
     * @param rotation of filter
     */
    fun setRotation(rotation: Int) {
        if (filterRender is RotationFilterRender) {
            filterRender.rotation = rotation
        } else if (filterRender is AndroidViewFilterRender) {
            filterRender.setRotation(rotation)
        }
    }

    /**
     * [RGBSaturationFilterRender] only
     * Saturate red, green and blue colors 0% to 100% (0.0f to 1.0f)
     */
    fun setRGBSaturation(r: Float, g: Float, b: Float) {
        if (filterRender is RGBSaturationFilterRender) {
            filterRender.setRGBSaturation(r, g, b)
        }
    }

    /**
     * For AndroidView Filter
     * @param view View
     */
    fun setView(view: View) {
        if (filterRender is AndroidViewFilterRender) {
            filterRender.view = view
        }
    }

    /**
     * [AndroidViewFilterRender] only
     * @param position of View
     */
    fun setPosition(position: Translate) {
        if (filterRender is AndroidViewFilterRender) {
            filterRender.setPosition(position.translateTo)
        }
    }

    /**
     * [AndroidViewFilterRender] only
     * @param scale of View
     */
    fun setScale(scale: PointF) {
        if (filterRender is AndroidViewFilterRender) {
            filterRender.setScale(scale.x, scale.y)
        }
    }
}
