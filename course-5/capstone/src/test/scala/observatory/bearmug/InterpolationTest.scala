package observatory.bearmug

import com.sksamuel.scrimage.Pixel
import observatory.bearmug.Interpolation.plain
import observatory.{Color, Location}
import org.scalatest.FunSuite
import org.scalatest.Matchers._

class InterpolationTest extends FunSuite {

  test("distance calculated correctly") {
    assert(Interpolation.distance(Location(55.870488, 37.411361), Location(55.872008, 37.420192)) < 0.6)
  }

  test("nearest point temperature chosen if distance less than 1.0 km") {
    assert(
      plain.predictTemperature(List((Location(10, 10), 11)), Location(10.0001, 10.00001)) == 11
    )

    assert(
      plain.predictTemperature(List((Location(55.870488, 37.411361), 12), (Location(55.890488, 37.471361), 120)),
        Location(55.872008, 37.420192)) == 12
    )

    assert(
      plain.predictTemperature(List((Location(55.864175, 37.402059), 12)), Location(55.875008, 37.423492)) == 12
    )
  }

  test("avg temperature chosen for two avg point") {
    assert(
      plain.predictTemperature(List((Location(10, 10), 0), (Location(12, 12), 20)), Location(11, 11)) === 10.0 +- .05
    )
  }

  test("avg temperature chosen for four avg point") {
    assert(
      plain.predictTemperature(List(
        (Location(10, 10), 0),
        (Location(10, 12), 5),
        (Location(12, 10), 15),
        (Location(12, 12), 20)
      ),
        Location(11, 11)) === 10.0 +- .05
    )
  }

  val colors = List(
    (32.0, Color(255, 0, 0)),
    (0.0, Color(0, 255, 255)),
    (60.0, Color(255, 255, 255)),
    (12.0, Color(255, 255, 0)),
    (-50.0, Color(33, 0, 107)),
    (-15.0, Color(0, 0, 255)),
    (-27.0, Color(255, 0, 255)),
    (-60.0, Color(0, 0, 0))
  )

  test("color interpolated to black for very low temperature") {
    assert(plain.interpolateColor(colors, -700) == Color(0, 0, 0))
    assert(plain.interpolateColor(colors, -60.6) == Color(0, 0, 0))
  }

  test("color interpolated to white for very high temperature") {
    assert(plain.interpolateColor(colors, 700) == Color(255, 255, 255))
    assert(plain.interpolateColor(colors, 60.5) == Color(255, 255, 255))
  }

  test("color interpolated well for medium values") {
    assert(plain.interpolateColor(colors, 33.0) == Color(255, 9, 9))
    assert(plain.interpolateColor(colors, 59.0) == Color(255, 246, 246))
    assert(plain.interpolateColor(colors, 1.0) == Color(21, 255, 234))
    assert(plain.interpolateColor(colors, 11.0) == Color(234, 255, 21))
  }

  test("color interpolated for two temperatures") {
    assert(plain.interpolateColor(List((-1.0, Color(255, 0, 0)), (0.0, Color(0, 0, 255))), -0.5) == Color(128, 0, 128))
  }

  test("color picked from list for matches values") {
    assert(plain.interpolateColor(colors, 32.0) == Color(255, 0, 0))
    assert(plain.interpolateColor(colors, 60.0) == Color(255, 255, 255))
    assert(plain.interpolateColor(colors, -27.0) == Color(255, 0, 255))
    assert(plain.interpolateColor(colors, -60.0) == Color(0, 0, 0))
  }

  val temperatures = List(
    (Location(40, 10), -30.0),
    (Location(10, -12), 5.0),
    (Location(12, 60), 15.0),
    (Location(82, -42), 40.0)
  )
  test("visualize works fine for a set of points") {
    val image = plain.visualize(temperatures, colors)
    assert(image.forall((x, y, pixel: Pixel) => {
      val lon = x - 180
      val lat = 90 - y
      val temp = plain.predictTemperature(temperatures, Location(lat, lon))
      val color = plain.interpolateColor(colors, temp)
      color.red == pixel.red && color.green == pixel.green && color.blue == pixel.blue
    }))
  }

  test("tileLocation works fine for initial zoom level") {
    assert(plain.tileLocation(0, 1, 1) == Location(-85.05112877980659, 180.0))
    assert(plain.tileLocation(0, 0, 0) == Location(85.05112877980659,-180.0))
  }

  test("tileLocation works fine for 10th zoom level") {
    assert(plain.tileLocation(10, 10, 10) == Location(84.7383871209534,-176.484375))
    assert(plain.tileLocation(10, 130, 310) == Location(57.704147234341924,-134.296875))
  }

  test("tile generated OK for 5th zoom level") {
    plain.tile(temperatures, colors, 5, 34, 22)
  }
}
