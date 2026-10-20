package com.loregate.app.feature.map.maplibre

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.loregate.app.core.designsystem.OrangePrimary
import com.loregate.app.domain.model.GeoPoint
import com.loregate.app.domain.model.HistoricalSite
import com.loregate.app.domain.model.SiteType
import com.loregate.app.domain.model.Spear
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.expressions.Expression.concat
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.expressions.Expression.literal
import org.maplibre.android.style.expressions.Expression.match
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon

object MapStyleRenderer {

    const val SITES_LAYER_ID = "sites-layer"
    const val SPEARS_ICON_LAYER_ID = "spears-icon-layer"
    const val SPEARS_PULSE_LAYER_ID = "spears-pulse-layer"
    const val SPEARS_LABEL_LAYER_ID = "spears-label-layer"

    fun updateSitesLayer(
        map: MapLibreMap,
        visibleSites: List<HistoricalSite>,
        selectedSiteId: String?
    ) {
        map.getStyle { style ->
            val features = visibleSites.map { site ->
                val point = Point.fromLngLat(site.longitude, site.latitude)
                val properties = JsonObject().apply {
                    add("id", JsonPrimitive(site.id))
                    add("nama", JsonPrimitive(site.nama))
                    add("type", JsonPrimitive(site.type.name))
                }
                Feature.fromGeometry(point, properties, site.id)
            }
            val featureCollection = FeatureCollection.fromFeatures(features)
            val sourceId = "sites-source"

            val source = style.getSourceAs<GeoJsonSource>(sourceId)
            if (source == null) {
                style.addSource(GeoJsonSource(sourceId, featureCollection))
            } else {
                source.setGeoJson(featureCollection)
            }

            SiteType.entries.forEach { type ->
                val normalIconId = "icon-${type.name}-normal"
                val selectedIconId = "icon-${type.name}-selected"
                if (style.getImage(normalIconId) == null) {
                    style.addImage(
                        normalIconId,
                        MapMarkerBitmapGenerator.createSiteMarkerBitmap(type, false)
                    )
                }
                if (style.getImage(selectedIconId) == null) {
                    style.addImage(
                        selectedIconId,
                        MapMarkerBitmapGenerator.createSiteMarkerBitmap(type, true)
                    )
                }
            }

            if (style.getLayer(SITES_LAYER_ID) == null) {
                val symbolLayer = SymbolLayer(SITES_LAYER_ID, sourceId)
                    .withProperties(
                        PropertyFactory.iconImage(
                            match(
                                get("id"),
                                literal(selectedSiteId ?: ""),
                                concat(literal("icon-"), get("type"), literal("-selected")),
                                concat(literal("icon-"), get("type"), literal("-normal"))
                            )
                        ),
                        PropertyFactory.iconSize(0.75f),
                        PropertyFactory.iconAllowOverlap(true),
                        PropertyFactory.iconIgnorePlacement(true),
                        PropertyFactory.textField(get("nama")),
                        PropertyFactory.textAnchor(Property.TEXT_ANCHOR_TOP),
                        PropertyFactory.textOffset(arrayOf(0f, 1.4f)),
                        PropertyFactory.textColor(android.graphics.Color.parseColor("#1E293B")),
                        PropertyFactory.textSize(12f),
                        PropertyFactory.textHaloColor(android.graphics.Color.WHITE),
                        PropertyFactory.textHaloWidth(2.0f),
                        PropertyFactory.textHaloBlur(0.5f),
                        PropertyFactory.textAllowOverlap(false),
                        PropertyFactory.textIgnorePlacement(false)
                    )
                style.addLayer(symbolLayer)
            } else {
                style.getLayer(SITES_LAYER_ID)?.setProperties(
                    PropertyFactory.iconImage(
                        match(
                            get("id"),
                            literal(selectedSiteId ?: ""),
                            concat(literal("icon-"), get("type"), literal("-selected")),
                            concat(literal("icon-"), get("type"), literal("-normal"))
                        )
                    )
                )
            }
        }
    }

    fun updateUserLocationLayer(
        map: MapLibreMap,
        location: GeoPoint,
        isMoving: Boolean
    ) {
        map.getStyle { style ->
            val userPoint = Point.fromLngLat(location.longitude, location.latitude)
            val sourceId = "user-location-source"
            val source = style.getSourceAs<GeoJsonSource>(sourceId)

            if (source == null) {
                style.addSource(GeoJsonSource(sourceId, userPoint))
            } else {
                source.setGeoJson(userPoint)
            }

            val iconId = if (isMoving) "user-icon-walking" else "user-icon-static"
            if (style.getImage(iconId) == null) {
                style.addImage(
                    iconId,
                    MapMarkerBitmapGenerator.createStaticCharacterBitmap(isWalking = isMoving)
                )
            }

            val layerId = "user-location-layer"
            val existingLayer = style.getLayer(layerId)
            if (existingLayer == null) {
                val symbolLayer = SymbolLayer(layerId, sourceId)
                    .withProperties(
                        PropertyFactory.iconImage(iconId),
                        PropertyFactory.iconSize(1.0f),
                        PropertyFactory.iconAllowOverlap(true),
                        PropertyFactory.iconIgnorePlacement(true),
                        PropertyFactory.iconPadding(0f),
                        PropertyFactory.iconPitchAlignment(Property.ICON_PITCH_ALIGNMENT_VIEWPORT),
                        PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_VIEWPORT),
                        PropertyFactory.symbolZOrder(Property.SYMBOL_Z_ORDER_SOURCE)
                    )
                if (style.getLayer(SITES_LAYER_ID) != null) {
                    style.addLayerAbove(symbolLayer, SITES_LAYER_ID)
                } else {
                    style.addLayer(symbolLayer)
                }
            } else {
                existingLayer.setProperties(PropertyFactory.iconImage(iconId))
            }
        }
    }

    fun updateUserRadarLayer(
        map: MapLibreMap,
        location: GeoPoint,
        radiusMeters: Float,
        alpha: Float
    ) {
        map.getStyle { style ->
            val sourceId = "user-radar-source"
            val layerId = "user-radar-layer"
            val strokeLayerId = "user-radar-stroke-layer"

            val ring = GeoPoint.generateCirclePolygon(location, radiusMeters)
            val polygon = Polygon.fromLngLats(
                listOf(ring.map { Point.fromLngLat(it.longitude, it.latitude) })
            )
            val collection = FeatureCollection.fromFeature(Feature.fromGeometry(polygon))

            val source = style.getSourceAs<GeoJsonSource>(sourceId)
            if (source == null) {
                style.addSource(GeoJsonSource(sourceId, collection))
            } else {
                source.setGeoJson(collection)
            }

            val existingFill = style.getLayer(layerId) as? FillLayer
            if (existingFill == null) {
                val fillLayer = FillLayer(layerId, sourceId).withProperties(
                    PropertyFactory.fillColor(android.graphics.Color.parseColor("#4CAF50")),
                    PropertyFactory.fillOpacity((alpha * 0.25f).coerceIn(0f, 0.2f))
                )
                if (style.getLayer(SPEARS_PULSE_LAYER_ID) != null) {
                    style.addLayerBelow(fillLayer, SPEARS_PULSE_LAYER_ID)
                } else {
                    style.addLayer(fillLayer)
                }
            } else {
                existingFill.setProperties(
                    PropertyFactory.fillOpacity((alpha * 0.25f).coerceIn(0f, 0.2f))
                )
            }

            val existingStroke = style.getLayer(strokeLayerId) as? LineLayer
            if (existingStroke == null) {
                val stroke = LineLayer(strokeLayerId, sourceId).withProperties(
                    PropertyFactory.lineColor(android.graphics.Color.parseColor("#4CAF50")),
                    PropertyFactory.lineWidth(2.5f),
                    PropertyFactory.lineOpacity(alpha.coerceIn(0f, 0.6f))
                )
                if (style.getLayer(layerId) != null) {
                    style.addLayerAbove(stroke, layerId)
                } else {
                    style.addLayer(stroke)
                }
            } else {
                existingStroke.setProperties(
                    PropertyFactory.lineOpacity(alpha.coerceIn(0f, 0.6f))
                )
            }
        }
    }

    fun updateSpearLayer(
        map: MapLibreMap,
        spears: List<Spear>,
        pulseRadius: Float = 16f,
        pulseAlpha: Float = 0.15f,
        bobOffset: Float = 0f
    ) {
        map.getStyle { style ->
            val sourceId = "spears-source"

            if (style.getImage("spear-icon") == null) {
                style.addImage("spear-icon", MapMarkerBitmapGenerator.createSpearBitmap())
            }

            val features = spears.map { spear ->
                Feature.fromGeometry(Point.fromLngLat(spear.longitude, spear.latitude)).apply {
                    addStringProperty("id", spear.id)
                    addStringProperty("name", spear.name)
                    addStringProperty("difficulty", spear.difficulty)
                }
            }
            val collection = FeatureCollection.fromFeatures(features)

            val source = style.getSourceAs<GeoJsonSource>(sourceId)
            if (source == null) {
                style.addSource(GeoJsonSource(sourceId, collection))
            } else {
                source.setGeoJson(collection)
            }

            val existingPulse = style.getLayer(SPEARS_PULSE_LAYER_ID) as? CircleLayer
            if (existingPulse == null) {
                val pulseLayer = CircleLayer(SPEARS_PULSE_LAYER_ID, sourceId).withProperties(
                    PropertyFactory.circleRadius(pulseRadius),
                    PropertyFactory.circleColor(
                        match(
                            get("difficulty"),
                            literal("EASY"), literal("#4CAF50"),
                            literal("MEDIUM"), literal("#FFA500"),
                            literal("HARD"), literal("#FF6B6B"),
                            literal("#4CAF50")
                        )
                    ),
                    PropertyFactory.circleOpacity(pulseAlpha.coerceIn(0f, 0.4f)),
                    PropertyFactory.circleStrokeColor(android.graphics.Color.TRANSPARENT),
                    PropertyFactory.circleStrokeWidth(0f)
                )
                style.addLayer(pulseLayer)
            } else {
                existingPulse.setProperties(
                    PropertyFactory.circleRadius(pulseRadius),
                    PropertyFactory.circleOpacity(pulseAlpha.coerceIn(0f, 0.4f))
                )
            }

            val existingIcon = style.getLayer(SPEARS_ICON_LAYER_ID) as? SymbolLayer
            if (existingIcon == null) {
                val iconLayer = SymbolLayer(SPEARS_ICON_LAYER_ID, sourceId).withProperties(
                    PropertyFactory.iconImage("spear-icon"),
                    PropertyFactory.iconSize(0.9f),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true),
                    PropertyFactory.iconAnchor(Property.ICON_ANCHOR_CENTER),
                    PropertyFactory.iconTranslate(arrayOf(0f, bobOffset)),
                    PropertyFactory.iconPitchAlignment(Property.ICON_PITCH_ALIGNMENT_VIEWPORT),
                    PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_VIEWPORT)
                )
                style.addLayer(iconLayer)
            } else {
                existingIcon.setProperties(
                    PropertyFactory.iconTranslate(arrayOf(0f, bobOffset)),
                    PropertyFactory.iconPitchAlignment(Property.ICON_PITCH_ALIGNMENT_VIEWPORT),
                    PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_VIEWPORT)
                )
            }

            val existingLabel = style.getLayer(SPEARS_LABEL_LAYER_ID) as? SymbolLayer
            if (existingLabel == null) {
                val labelLayer = SymbolLayer(SPEARS_LABEL_LAYER_ID, sourceId).withProperties(
                    PropertyFactory.textField(get("name")),
                    PropertyFactory.textSize(10f),
                    PropertyFactory.textColor(android.graphics.Color.parseColor("#334155")),
                    PropertyFactory.textHaloColor(android.graphics.Color.WHITE),
                    PropertyFactory.textHaloWidth(1.8f),
                    PropertyFactory.textOffset(arrayOf(0f, 1.2f)),
                    PropertyFactory.textPadding(4f),
                    PropertyFactory.textOptional(true),
                    PropertyFactory.textAllowOverlap(false),
                    PropertyFactory.textIgnorePlacement(false),
                    PropertyFactory.textPitchAlignment(Property.TEXT_PITCH_ALIGNMENT_VIEWPORT),
                    PropertyFactory.textRotationAlignment(Property.TEXT_ROTATION_ALIGNMENT_VIEWPORT)
                )
                style.addLayerAbove(labelLayer, SPEARS_ICON_LAYER_ID)
            } else {
                existingLabel.setProperties(
                    PropertyFactory.textPitchAlignment(Property.TEXT_PITCH_ALIGNMENT_VIEWPORT),
                    PropertyFactory.textRotationAlignment(Property.TEXT_ROTATION_ALIGNMENT_VIEWPORT)
                )
            }
        }
    }

    fun drawRouteLayer(map: MapLibreMap, routePoints: List<GeoPoint>) {
        if (routePoints.isEmpty()) return

        map.getStyle { style ->
            val sourceId = "route-source"
            val layerId = "route-layer"

            val points = routePoints.map { Point.fromLngLat(it.longitude, it.latitude) }
            val lineString = LineString.fromLngLats(points)
            val feature = Feature.fromGeometry(lineString)
            val featureCollection = FeatureCollection.fromFeatures(arrayOf(feature))

            val source = style.getSourceAs<GeoJsonSource>(sourceId)
            if (source == null) {
                style.addSource(GeoJsonSource(sourceId, featureCollection))
            } else {
                source.setGeoJson(featureCollection)
            }

            if (style.getLayer(layerId) == null) {
                val lineLayer = LineLayer(layerId, sourceId).withProperties(
                    PropertyFactory.lineColor(OrangePrimary.toArgb()),
                    PropertyFactory.lineWidth(5f),
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
                )
                if (style.getLayer(SITES_LAYER_ID) != null) {
                    style.addLayerBelow(lineLayer, SITES_LAYER_ID)
                } else {
                    style.addLayer(lineLayer)
                }
            }
        }
    }
}
