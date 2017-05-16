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
import com.google.maps.android.SphericalUtil


class MapsActivity : FragmentActivity(), OnMapReadyCallback {
  val EARTH_RADIUS = 6378100.0

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
    // Obtain the SupportMapFragment and get notified when the map is ready to be used.
    val mapFragment = supportFragmentManager
      .findFragmentById(R.id.map) as SupportMapFragment
    mapFragment.getMapAsync(this)
  }

  override fun onMapReady(googleMap: GoogleMap) {
    this.googleMap = googleMap

    googleMap.isMyLocationEnabled = true
    googleMap.uiSettings.isMyLocationButtonEnabled = true
    googleMap.uiSettings.isCompassEnabled = true
    googleMap.uiSettings.isMapToolbarEnabled = true

    val strokeWidth = 2f

    val polylineOptions = PolylineOptions()
      .jointType(JointType.ROUND)
      .addAll(coordinatesArray)
      .width(strokeWidth)
      .color(Color.parseColor("#000000"))

    val polygonOptions = PolygonOptions()
      .strokeJointType(JointType.ROUND)
      .addAll(getPolygonPoints(coordinatesArray))
      .strokeWidth(strokeWidth)
      .fillColor(Color.parseColor("#993F51B5"))
      .strokeColor(Color.parseColor("#000000"))

    googleMap.addPolygon(polygonOptions)

    googleMap.addPolyline(polylineOptions)
    //Marker sample
//    val sydney = LatLng(-34.0, 151.0)
//    googleMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
//    googleMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
  }

  fun getPolygonPoints(polyline: List<LatLng>): ArrayList<LatLng> {
    val pointsAbove = arrayListOf<LatLng>()
    val pointsBelow = arrayListOf<LatLng>()

    polyline.forEachIndexed { index, latLng ->
      if (index > 0 && index < polyline.size - 1) {
        val points = findBiss(polyline[index - 1], polyline[index], polyline[index + 1], POINT_AREA_RADIUS)
        pointsAbove.add(points.first)
        pointsBelow.add(points.second)
      }
    }

    val firstPoints = getPointsForFirstPoint(polyline[0], polyline[1], POINT_AREA_RADIUS)
    val destPoints = getPointsForLast(polyline[polyline.size - 2], polyline[polyline.size - 1], POINT_AREA_RADIUS)
    val firstCircle = startCircle(polyline[0], firstPoints.first.minus(polyline[0]), polyline[1].minus(polyline[0]).ordinary().mul((POINT_AREA_RADIUS / EARTH_RADIUS) * 180 / Math.PI))
    val secondCircle = startCircle(polyline[polyline.size-1], destPoints.first.minus(polyline[polyline.size-1]), polyline[polyline.size-2].minus(polyline[polyline.size-1]).ordinary().mul((POINT_AREA_RADIUS / EARTH_RADIUS) * 180 / Math.PI))

    val resultPoints = arrayListOf<LatLng>()
    resultPoints.addAll(firstCircle)
    resultPoints.add(firstPoints.second)
    resultPoints.addAll(pointsBelow)
    resultPoints.add(destPoints.second)
    secondCircle.reverse()
    resultPoints.addAll(secondCircle)
    resultPoints.add(destPoints.first)
    pointsAbove.reverse()
    resultPoints.addAll(pointsAbove)
    resultPoints.add(firstPoints.first)
    return resultPoints
  }

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

  private fun getPointsForFirstPoint(start: LatLng, dest: LatLng, radius: Double): Pair<LatLng, LatLng> {
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

  fun getPointsForLast(start: LatLng, dest: LatLng, radius: Double): Pair<LatLng, LatLng> {
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

  fun getDistanceBetweenPoints(start: LatLng, destination: LatLng): Double {
    val sourceLat = start.latitude
    val sourceLng = start.longitude
    val destinationLat = destination.latitude
    val destinationLng = destination.longitude
    return SphericalUtil.computeDistanceBetween(LatLng(sourceLat, sourceLng), LatLng(destinationLat, destinationLng))
  }

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
