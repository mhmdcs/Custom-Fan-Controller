package com.example.customfancontroller

import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.withStyledAttributes
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

//this enum represents the available fan speeds
//the enum is of type Int because the values are string resources rather than actual strings
private enum class FanSpeed(val label: Int) {
    OFF(R.string.fan_off),
    LOW(R.string.fan_low),
    MEDIUM(R.string.fan_medium),
    HIGH(R.string.fan_high);

    fun next() = when (this) {
        OFF -> LOW
        LOW -> MEDIUM
        MEDIUM -> HIGH
        HIGH -> OFF
    }
}

//constants for drawing the dial indicator(black dot) and its labels (off,1,2,3) radius (the radius of a circle is the distance from its center to its outside edge)
private const val RADIUS_OFFSET_LABEL = 40 //when it's positive it gets further from the center
private const val RADIUS_OFFSET_INDICATOR = -55 //when it's negative it gets closer to the center

//declare variables to cache the attribute values
private var fanSpeedLowColor = 0
private var fanSpeedMediumColor = 0
private var fanSeedMaxColor = 0

    //@JvmOverloads annotation instructs Kotlin's compiler to generate overloads for this constructor that substitute default parameter values
    //basically it allows us to add default parameters to constructors(or functions) which is a Kotlin exclusive feature, to code that will be compiled into Java, like most of Android's API including the View API
class DialView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var radius = 0.0f                   // Initial temp value for the radius of the circle, we will later properly initialize it with a real radius in onSizeChanged
    private var fanSpeed = FanSpeed.OFF      // The default active selection for the fan speed will be OFF
    // point position variable which will be used to draw the label RADIUS_OFFSET_LABEL and indicator RADIUS_OFFSET_INDICATOR circle positions
    private val pointPosition: PointF = PointF(0.0f, 0.0f) // PointF class holds two float coordinates. an x-y point that will be used for drawing elements on the screen
        //the three values above are created and initialized here instead of when the view is actually drawn in onDraw, to ensure that the actual drawing step runs as fast as possible

        //initialize a Paint object with a handful of basic styles, as with the three values above, the Paint object's styles are initialized here to help speed up the drawing step
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL //set the geometry and text to be filled, (try STROKE instead of FILL to see what it does)
        textAlign = Paint.Align.CENTER //align the text to center
        textSize = 55.0f //set the text size
        typeface = Typeface.create("", Typeface.BOLD) //set the typeface's family font to null to get the default font, and the font's style to bold.
    }

    init {
        isClickable = true //make the view as clickable as it's initialized by setting setClickable to true

        context.withStyledAttributes( attrs, R.styleable.DialView) {
            fanSpeedLowColor = getColor(R.styleable.DialView_fanColor1, Color.LTGRAY) // this will become a color attribute to be defined in the xml view
            fanSpeedMediumColor = getColor(R.styleable.DialView_fanColor2, Color.LTGRAY) // this will become a color attribute to be defined in the xml view
            fanSeedMaxColor = getColor(R.styleable.DialView_fanColor3, Color.LTGRAY) // this will become a color attribute to be defined in the xml view
        }
    }

        //override performClick() and call invalidate() to respond to a user click that changes how the view is drawn
        //thus forcing a call to onDraw() to redraw the view
    override fun performClick(): Boolean {
        if (super.performClick()) return true

        //move the fan speed dial based on user click via using enum's next() method you defined
        fanSpeed = fanSpeed.next()
        contentDescription = resources.getString(fanSpeed.label) //for accessibility support, change the content description for the fan speed based on user clicks
        Log.i("Test","I was clicked! ${fanSpeed}")
         // invalidate() to tell the Android system to call the onDraw() method to redraw the view
        invalidate()
        return true
    }

        //override onSizeChanged to calculate the view's size when it first appears and each time its size changes
        //the onSizeChanged is called every time the view's size changes, including the first time it's drawn when the layout is inflated
        //override onSizeChanged to calculate positions, dimensions, and any other values related to your custom view's size, instead of recalculating them every time you draw
        //in this case we'll override onSizeChanged to calculate the current radius of the dial circle
    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        radius = (min(width, height) / 2.0 * 0.8).toFloat()
    }

        //create extension function for the PointF class to calculate the current x-y position of the indicator & label on the dial
    private fun PointF.computeXYForSpeed(pos: FanSpeed, radius: Float) {
            //this helper method calculates the current x-y coordinates on the screen for the text label and indicator black dot
            //given the current fan speed position and the radius of the dial, this helper method will be used in onDraw()

        // Angles are in radians unit.
        val startAngle = Math.PI * (9 / 8.0)
        val angle = startAngle + pos.ordinal * (Math.PI / 4)
        x = (radius * cos(angle)).toFloat() + width / 2
        y = (radius * sin(angle)).toFloat() + height / 2
    }


        //override onDraw() to draw the custom view using a Canvas object styled by a Paint object
        //he onDraw() method is called every time the screen refreshes, which can be many times in a second
        //so for performance reasons, and to avoid visual glitches, you should do as little work as possible in onDraw()
        //in particular, don't place allocations in onDraw() because allocations may lead to garbage collection that may cause a visual stutter
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

            //the Canvas and Paint classes offer a number of useful drawing shortcuts
            //you can draw text using Canvas' drawText() method, set the typeface and text color using Paint's setTypeface() and setColor() methods
            //you can draw primitive shapes using Canvas' drawRect(), drawOval(), drawArc(), and change whether the shapes are filled, outlined or both by calling Paint's setStyle()
            //you can also draw bitmaps by using Canvas' drawBitmap()

        //set the dial color based on the current fan speed.
        paint.color = when (fanSpeed) {
            FanSpeed.OFF -> Color.GRAY
            FanSpeed.LOW -> fanSpeedLowColor
            FanSpeed.MEDIUM -> fanSpeedMediumColor
            FanSpeed.HIGH -> fanSeedMaxColor
        } as Int

        // Set dial background color to green if selection not off.
//               paint.color = if (fanSpeed == FanSpeed.OFF) Color.GRAY else Color.GREEN

        // the code to draw the circle for the dial with Canvas' drawCircle() method
        // this method uses the current view's width and height to find the center of the circle in its first two parameters, find the radius of the circle in its third parameter, and the current paint color in its 4th parameter
        //the width and height properties are members of the View super class and they indicate the current dimensions of the view
        canvas.drawCircle((width / 2).toFloat(), (height / 2).toFloat(), radius, paint)

        // Draw the small indicator black dot circle.
        val markerRadius = radius + RADIUS_OFFSET_INDICATOR
        pointPosition.computeXYForSpeed(fanSpeed, markerRadius) //use the computeXYForSpeed extension function to calculate the x-y coordinates for the indicator center based on the current fan speed
        paint.color = Color.BLACK //set the paint object's color to black for the indicator circle and label text colors
        canvas.drawCircle(pointPosition.x, pointPosition.y, radius / 12, paint) //draw the indicator's black dot circle using Canvas drawCircle()


        // Draw the text labels.
        val labelRadius = radius + RADIUS_OFFSET_LABEL
        for (i in FanSpeed.values()) {
            pointPosition.computeXYForSpeed(i, labelRadius) //reuse the same PointF pointPosition object you used earlier for the small indicator circle now again for the label text, reuse it each time to avoid allocations in onDraw() as we mentioned earlier
            val label = resources.getString(i.label)
            canvas.drawText(label, pointPosition.x, pointPosition.y, paint)
        }
    }

        //in apps that have a deep view hierarchy you can override onMeasure() method to accurately define how your custom views fit into the layout
        //that way, the parent layout can properly align the custom view, the onMeasure() method provides a set of measure specs that you can use to determine
        //your view's height and width

}