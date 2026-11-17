package com.workout.timer.ui.component

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline

/**
 * Custom Compose Wrapper untuk OSMDroid MapView.
 * Menampilkan:
 * 1. Garis rute lari dengan kontras tinggi (Strava Vivid Orange + dark shadow outline).
 * 2. Titik GPS Puck dinamis (titik lokasi saat ini dengan halo dan ring putih).
 * 3. Titik Start (Hijau) dan End (Merah/Oranye).
 * 4. Mendukung auto-center ke user atau auto-fit bounding box untuk hasil akhir lari.
 */
@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    currentLocation: Pair<Double, Double>? = null,
    routePoints: List<Pair<Double, Double>> = emptyList(),
    isFollowUser: Boolean = true,
    fitBounds: Boolean = false,
    zoomLevel: Double = 16.5,
    onMapReady: (MapView) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    // Layer-layer overlay OSMDroid
    val routeShadowPolyline = remember {
        Polyline().apply {
            outlinePaint.color = Color.argb(160, 20, 20, 20)
            outlinePaint.strokeWidth = 18f
            outlinePaint.strokeCap = Paint.Cap.ROUND
            outlinePaint.strokeJoin = Paint.Join.ROUND
            outlinePaint.isAntiAlias = true
        }
    }

    val routeMainPolyline = remember {
        Polyline().apply {
            // Strava Vibrant Electric Coral / Orange
            outlinePaint.color = Color.parseColor("#FC5200")
            outlinePaint.strokeWidth = 12f
            outlinePaint.strokeCap = Paint.Cap.ROUND
            outlinePaint.strokeJoin = Paint.Join.ROUND
            outlinePaint.isAntiAlias = true
        }
    }

    // Overlay titik lokasi pengguna (GPS Puck) & Pin Start/End
    val trackerOverlay = remember {
        object : Overlay() {
            var userLocation: Pair<Double, Double>? = null
            var pointsList: List<Pair<Double, Double>> = emptyList()

            private val screenPoint = Point()

            // Paint untuk User GPS Puck
            private val puckHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(60, 252, 82, 0)
                style = Paint.Style.FILL
            }
            private val puckWhiteBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            private val puckCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FC5200")
                style = Paint.Style.FILL
            }

            // Paint untuk Titik Mulai (Start - Emerald Green)
            private val startBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            private val startCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#00C853")
                style = Paint.Style.FILL
            }

            // Paint untuk Titik Akhir (Finish - Crimson Red)
            private val finishBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            private val finishCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#D50000")
                style = Paint.Style.FILL
            }

            override fun draw(pCanvas: Canvas?, pMapView: MapView?, pShadow: Boolean) {
                if (pShadow || pCanvas == null || pMapView == null) return
                val projection = pMapView.projection

                // 1. Gambar Pin Start jika rute sudah punya minimal 1 titik
                if (pointsList.isNotEmpty()) {
                    val startCoord = pointsList.first()
                    projection.toPixels(GeoPoint(startCoord.first, startCoord.second), screenPoint)
                    // White border ring
                    pCanvas.drawCircle(screenPoint.x.toFloat(), screenPoint.y.toFloat(), 14f, startBorderPaint)
                    // Green center
                    pCanvas.drawCircle(screenPoint.x.toFloat(), screenPoint.y.toFloat(), 10f, startCorePaint)
                }

                // 2. Gambar Pin Finish jika rute selesai (lebih dari 1 titik dan bukan sedang tracking)
                if (pointsList.size >= 2 && userLocation == null) {
                    val finishCoord = pointsList.last()
                    projection.toPixels(GeoPoint(finishCoord.first, finishCoord.second), screenPoint)
                    pCanvas.drawCircle(screenPoint.x.toFloat(), screenPoint.y.toFloat(), 14f, finishBorderPaint)
                    pCanvas.drawCircle(screenPoint.x.toFloat(), screenPoint.y.toFloat(), 10f, finishCorePaint)
                }

                // 3. Gambar User GPS Puck (posisi aktif lari saat ini)
                val current = userLocation
                if (current != null) {
                    projection.toPixels(GeoPoint(current.first, current.second), screenPoint)
                    val cx = screenPoint.x.toFloat()
                    val cy = screenPoint.y.toFloat()

                    // Outer pulsing halo
                    pCanvas.drawCircle(cx, cy, 28f, puckHaloPaint)
                    // White outer ring
                    pCanvas.drawCircle(cx, cy, 14f, puckWhiteBorderPaint)
                    // Vibrant orange center
                    pCanvas.drawCircle(cx, cy, 9f, puckCorePaint)
                }
            }
        }
    }

    // Lifecycle observer untuk pause/resume tile decoding
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapViewRef?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapViewRef?.onPause()
                Lifecycle.Event.ON_DESTROY -> mapViewRef?.onDetach()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapViewRef?.onDetach()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                isTilesScaledToDpi = true
                controller.setZoom(zoomLevel)

                // Tambahkan layer shadow & main polyline
                overlays.add(routeShadowPolyline)
                overlays.add(routeMainPolyline)
                overlays.add(trackerOverlay)

                mapViewRef = this
                onMapReady(this)
            }
        },
        update = { mapView ->
            mapViewRef = mapView

            // 1. Update titik-titik rute polyline
            val geoPoints = routePoints.map { GeoPoint(it.first, it.second) }
            routeShadowPolyline.setPoints(geoPoints)
            routeMainPolyline.setPoints(geoPoints)

            // 2. Update koordinat tracker overlay secara dinamis
            trackerOverlay.userLocation = currentLocation
            trackerOverlay.pointsList = routePoints

            // 3. Jika fitBounds aktif (misal pada layar ringkasan / summary), auto-frame seluruh rute
            if (fitBounds && routePoints.isNotEmpty()) {
                if (routePoints.size == 1) {
                    val single = GeoPoint(routePoints[0].first, routePoints[0].second)
                    mapView.controller.setCenter(single)
                    mapView.controller.setZoom(16.5)
                } else {
                    var minLat = 90.0
                    var maxLat = -90.0
                    var minLng = 180.0
                    var maxLng = -180.0
                    for (p in routePoints) {
                        if (p.first < minLat) minLat = p.first
                        if (p.first > maxLat) maxLat = p.first
                        if (p.second < minLng) minLng = p.second
                        if (p.second > maxLng) maxLng = p.second
                    }
                    val latPadding = ((maxLat - minLat) * 0.25).coerceAtLeast(0.002)
                    val lngPadding = ((maxLng - minLng) * 0.25).coerceAtLeast(0.002)
                    val boundingBox = BoundingBox(
                        maxLat + latPadding,
                        maxLng + lngPadding,
                        minLat - latPadding,
                        minLng - lngPadding
                    )
                    mapView.post {
                        try {
                            mapView.zoomToBoundingBox(boundingBox, true, 80)
                        } catch (_: Exception) {
                            mapView.controller.setCenter(boundingBox.centerWithDateLine)
                        }
                    }
                }
            } else if (isFollowUser && currentLocation != null) {
                // Ikuti pergerakan user
                val center = GeoPoint(currentLocation.first, currentLocation.second)
                mapView.controller.animateTo(center)
            }

            mapView.invalidate()
        }
    )
}
