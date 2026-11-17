package com.workout.timer.ui.component

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline

/**
 * Custom Compose Wrapper untuk OSMDroid MapView.
 * Mendukung rendering rute polyline real-time, user location indicator, dan offline tiles.
 */
@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    currentLocation: Pair<Double, Double>? = null,
    routePoints: List<Pair<Double, Double>> = emptyList(),
    isFollowUser: Boolean = true,
    zoomLevel: Double = 16.5,
    onMapReady: (MapView) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var polylineOverlay by remember { mutableStateOf<Polyline?>(null) }

    // Lifecycle observer untuk pause/resume tile decoding saat activity background
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

                // Overlay rute lari (polyline putih modern)
                val polyline = Polyline().apply {
                    outlinePaint.color = android.graphics.Color.WHITE
                    outlinePaint.strokeWidth = 10f
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                    outlinePaint.strokeJoin = Paint.Join.ROUND
                    outlinePaint.isAntiAlias = true
                }
                polylineOverlay = polyline
                overlays.add(polyline)

                // Overlay titik lokasi pengguna (dot putih bernyawa dengan outer ring)
                val userLocationOverlay = object : Overlay() {
                    private val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.WHITE
                        style = Paint.Style.FILL
                    }
                    private val outerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.argb(80, 255, 255, 255)
                        style = Paint.Style.FILL
                    }
                    private val screenPoint = Point()

                    override fun draw(pCanvas: Canvas?, pMapView: MapView?, pShadow: Boolean) {
                        if (pShadow || pCanvas == null || pMapView == null) return
                        val currentLoc = currentLocation ?: return
                        val geoPoint = GeoPoint(currentLoc.first, currentLoc.second)
                        val pj = pMapView.projection
                        pj.toPixels(geoPoint, screenPoint)

                        // Gambar halo luar
                        pCanvas.drawCircle(screenPoint.x.toFloat(), screenPoint.y.toFloat(), 24f, outerPaint)
                        // Gambar titik tengah
                        pCanvas.drawCircle(screenPoint.x.toFloat(), screenPoint.y.toFloat(), 12f, innerPaint)
                    }
                }
                overlays.add(userLocationOverlay)

                mapViewRef = this
                onMapReady(this)
            }
        },
        update = { mapView ->
            mapViewRef = mapView

            // Update titik polyline
            polylineOverlay?.let { polyline ->
                val geoPoints = routePoints.map { GeoPoint(it.first, it.second) }
                polyline.setPoints(geoPoints)
            }

            // Update posisi kamera jika follow user aktif
            if (isFollowUser && currentLocation != null) {
                val center = GeoPoint(currentLocation.first, currentLocation.second)
                mapView.controller.animateTo(center)
            }

            mapView.invalidate()
        }
    )
}
