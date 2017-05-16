package com.sample.makhrov.mapssampleak

import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.gms.maps.model.PolylineOptions

/**
 * Sorry for my eng :D
 */
class MapsActivity : FragmentActivity(), OnMapReadyCallback {
  //Earth radius, const. No
  val EARTH_RADIUS = 6378100.0

  //Radius from polyline to polygon
  val POINT_AREA_RADIUS = 300.0

  //  Координаты маршрута
  //  50.441104, 30.488949 - Вокзальная
  //  50.442539, 30.501115 - Парк университет, вход
  //  50.444317, 30.506115 - Парк университет выход
  //  50.444488, 30.508593 - Владимирский
  //  50.442676, 30.520084 - Бессарабка
  val coordinatesArray = arrayListOf(LatLng(50.441104, 30.488949), LatLng(50.442539, 30.501115), LatLng(50.444317, 30.506115),
    LatLng(50.444488, 30.508593), LatLng(50.442676, 30.520084))

  private var googleMap: GoogleMap? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_maps)
    val mapFragment = supportFragmentManager
      .findFragmentById(R.id.map) as SupportMapFragment
    mapFragment.getMapAsync(this)
  }

  //This method is invoked when maps API synchronizes map. In that method map is ready to use.
  override fun onMapReady(googleMap: GoogleMap) {
    this.googleMap = googleMap

    googleMap.isMyLocationEnabled = true
    googleMap.uiSettings.isMyLocationButtonEnabled = true
    googleMap.uiSettings.isCompassEnabled = true
    googleMap.uiSettings.isMapToolbarEnabled = true

    val strokeWidth = 2f

    /**
     * Polyline options:
     * stroke joint
     * polyline points
     * width of polyline's stroke
     * polyline color
     */
    val polylineOptions = PolylineOptions()
      .jointType(JointType.ROUND)
      .addAll(coordinatesArray)
      .width(strokeWidth)
      .color(Color.parseColor("#000000"))

    /**
     * Polygon options:
     * stroke joint
     * polygon points
     * width of stroke around polygon
     * polygon fill color
     * stroke color
     */
    val polygonOptions = PolygonOptions()
      .strokeJointType(JointType.ROUND)
      .addAll(getPolygonPoints(coordinatesArray))
      .strokeWidth(strokeWidth)
      .fillColor(Color.parseColor("#993F51B5"))
      .strokeColor(Color.parseColor("#000000"))

    googleMap.addPolygon(polygonOptions)

    googleMap.addPolyline(polylineOptions)
  }

  /**
   * Method for polygon points calculation.
   */

  fun getPolygonPoints(polyline: List<LatLng>): ArrayList<LatLng> {
    //Points at the top of polyline
    val pointsAbove = arrayListOf<LatLng>()

    //Points below polyline
    val pointsBelow = arrayListOf<LatLng>()

    polyline.forEachIndexed { index, latLng ->
      if (index > 0 && index < polyline.size - 1) {
        val points = findBiss(polyline[index - 1], polyline[index], polyline[index + 1], POINT_AREA_RADIUS)
        pointsAbove.add(points.first)
        pointsBelow.add(points.second)
      }
    }

    val firstPoints = getVerticalVectorsForFirstPoint(polyline[0], polyline[1], POINT_AREA_RADIUS)
    val destPoints = getVerticalVectorsForLastPolylinePoint(polyline[polyline.size - 2], polyline[polyline.size - 1], POINT_AREA_RADIUS)

    val firstCircle = startCircle(polyline[0], firstPoints.first.minus(polyline[0]), polyline[1].minus(polyline[0]).ordinary().mul((POINT_AREA_RADIUS / EARTH_RADIUS) * 180 / Math.PI))
    val secondCircle = startCircle(polyline[polyline.size-1], destPoints.first.minus(polyline[polyline.size-1]), polyline[polyline.size-2].minus(polyline[polyline.size-1]).ordinary().mul((POINT_AREA_RADIUS / EARTH_RADIUS) * 180 / Math.PI))

    val resultPoints = arrayListOf<LatLng>()

    //Careful with that: you will need to make sure polygon points are starting with right one.
    //reverse() method used to reverse points in circle array and points above to add them in right order


    //Adding first circle to polygon
    resultPoints.addAll(firstCircle)
    //Adding point at the bottom of first polyline point
    resultPoints.add(firstPoints.second)
    //Adding all points below
    resultPoints.addAll(pointsBelow)
    //Adding points at the bottom of last polyline point
    resultPoints.add(destPoints.second)
    //reversing circle, because method startCircle creates circle from top to bottom
    secondCircle.reverse()
    //Adding circle points as polygon points
    resultPoints.addAll(secondCircle)
    //Adding point at the top of last polyline point
    resultPoints.add(destPoints.first)
    //reversing outer points so they will be added in right order
    pointsAbove.reverse()
    //adding outer points to polygon
    resultPoints.addAll(pointsAbove)
    //adding point at the top of first polyline point
    resultPoints.add(firstPoints.first)

    return resultPoints
  }

  /**
   * Method that takes three dots as paramether and returns pair of dots.
   *
   * @return - dots that will be on biss, at radius of @param radius
   * pair.first - outer dot
   * pair.second - inner dot
   */
  fun findBiss(first: LatLng, second: LatLng, third: LatLng, radius: Double): Pair<LatLng, LatLng> {
    val meters = (radius / EARTH_RADIUS) * 180 / Math.PI

    val firstMinusSecond = first.minus(second)
    val thirdMinusSecond = third.minus(second)

    val firstMinusSecondOrdinary = firstMinusSecond.ordinary()
    val thirdMinusSecondOrdinary = thirdMinusSecond.ordinary()

    val sum = firstMinusSecondOrdinary.plus(thirdMinusSecondOrdinary)

    val sumOrdinary = sum.ordinary()
    val anotherSumOrdinary = sumOrdinary.mul(meters)

    val secondPlusSum = second.plus(anotherSumOrdinary)
    val secondMinusSum = second.minus(anotherSumOrdinary)

    val firstPoint = secondPlusSum
    val secondPoint = secondMinusSum

    val vectorMul = firstMinusSecond.vectorMul(thirdMinusSecond)

    return if (vectorMul >= 0) firstPoint to secondPoint else secondPoint to firstPoint
  }

  fun startCircle(center: LatLng, vertical: LatLng, sideOrdinal: LatLng): ArrayList<LatLng> {
    val result = arrayListOf<LatLng>()

    var t = Math.PI / 2
    while (t < 3 * Math.PI / 2) {
      result.add(vertical.mul(Math.sin(t)).plus(sideOrdinal.mul(Math.cos(t))).plus(center))
      t += 0.1
    }

    return result
  }

  /**
   @param start - first point of polyline
   @param dest - second point of polyline
   @param radius - distance between @param first and dots that will be returned
   @return Pair<LatLng, LatLng> pair of dots;
    pair.first - dot above
    pair.second - dot below
   */
  private fun getVerticalVectorsForFirstPoint(start: LatLng, dest: LatLng, radius: Double): Pair<LatLng, LatLng> {
    // Convert start to radians.
    val startLat = start.latitude
    val startLon = start.longitude

    val destLat = dest.latitude
    val destLon = dest.longitude

    val latDiff = destLat - startLat
    val lngDiff = destLon - startLon

    val y = lngDiff
    val x = -latDiff

    val meters = (radius / EARTH_RADIUS) * 180 / Math.PI
    val startPoint1 = LatLng(
      start.latitude - y / Math.sqrt((latDiff) * (latDiff) + (lngDiff) * (lngDiff)) * meters,
      start.longitude - x / Math.sqrt((latDiff) * (latDiff) + (lngDiff) * (lngDiff)) * meters
    )

    val startPoint2 = LatLng(
      start.latitude + y / Math.sqrt((latDiff) * (latDiff) + (lngDiff) * (lngDiff)) * meters,
      start.longitude + x / Math.sqrt((latDiff) * (latDiff) + (lngDiff) * (lngDiff)) * meters
    )

    return startPoint2 to startPoint1
  }

  /**
   * see doc for getVerticalVectorsForFirstPoint
   */
  fun getVerticalVectorsForLastPolylinePoint(start: LatLng, dest: LatLng, radius: Double): Pair<LatLng, LatLng> {
    val startLat = start.latitude
    val startLon = start.longitude

    val destLat = dest.latitude
    val destLon = dest.longitude

    val latDiff = destLat - startLat
    val lngDiff = destLon - startLon

    val y = lngDiff
    val x = -latDiff
    val meters = (radius / EARTH_RADIUS) * 180 / Math.PI

    val destPoint1 = LatLng(
      dest.latitude - y / Math.sqrt((latDiff) * (latDiff) + (lngDiff) * (lngDiff)) * meters,
      dest.longitude - x / Math.sqrt((latDiff) * (latDiff) + (lngDiff) * (lngDiff)) * meters
    )

    val destPoint2 = LatLng(
      dest.latitude + y / Math.sqrt((latDiff) * (latDiff) + (lngDiff) * (lngDiff)) * meters,
      dest.longitude + x / Math.sqrt((latDiff) * (latDiff) + (lngDiff) * (lngDiff)) * meters
    )

    return destPoint2 to destPoint1
  }

  /**
   * Methods below are simple vector operations
   */

  fun LatLng.minus(another: LatLng): LatLng {
    return LatLng(this.latitude - another.latitude, this.longitude - another.longitude)
  }

  fun LatLng.plus(another: LatLng): LatLng {
    return LatLng(this.latitude + another.latitude, this.longitude + another.longitude)
  }

  fun LatLng.ordinary() = LatLng(
    this.latitude / Math.sqrt((this.longitude) * (this.longitude) + (this.latitude) * (this.latitude)),
    this.longitude / Math.sqrt((this.longitude) * (this.longitude) + (this.latitude) * (this.latitude))
  )

  fun LatLng.mul(value: Double) = LatLng(
    this.latitude * value,
    this.longitude * value)

  fun LatLng.vectorMul(value: LatLng) =
    this.latitude * value.longitude -
      value.latitude * this.longitude
}
