package com.it.fooddonation.core.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val logo: ImageVector
    get() {
        if (_logo != null) {
            return _logo!!
        }
        _logo = ImageVector.Builder(
            name = "logo",
            defaultWidth = 3001.dp,
            defaultHeight = 3001.dp,
            viewportWidth = 3001f,
            viewportHeight = 3001f
        ).apply {
            path(fill = SolidColor(Color(0xFFFF7E47))) {
                moveTo(1600f, 430.7f)
                curveTo(1600f, 430.7f, 1918.7f, 1054f, 1945f, 1199f)
                curveTo(2198.3f, 1248.2f, 2377.9f, 1872.3f, 2051.9f, 2004.5f)
                curveTo(1851.7f, 2032.3f, 1557.5f, 1554.7f, 1799.9f, 1246.1f)
                curveTo(1599.9f, 790f, 1485f, 390.9f, 1149.5f, 213.5f)
                curveTo(837f, 48.3f, 307.2f, 147.3f, 102.5f, 579.6f)
                curveTo(-127.6f, 999f, 216.1f, 1606.7f, 216.1f, 1606.7f)
                curveTo(216.1f, 1606.7f, 1253.9f, 3459f, 1956.8f, 2665.2f)
                curveTo(1270.7f, 2814.2f, 780.7f, 1677.3f, 780.7f, 1677.3f)
                curveTo(780.7f, 1677.3f, 587.3f, 1707.4f, 514.1f, 1434.3f)
                curveTo(483f, 1318.1f, 411.9f, 1082.8f, 412.2f, 1081.4f)
                curveTo(412.4f, 1080f, 402.8f, 1045.3f, 443.5f, 1026.5f)
                curveTo(491.4f, 1024.1f, 496.6f, 1033.3f, 506.2f, 1054f)
                curveTo(522.3f, 1105f, 596.4f, 1355.8f, 596.4f, 1355.8f)
                curveTo(596.4f, 1355.8f, 615.2f, 1399.2f, 655.2f, 1391.1f)
                curveTo(684.4f, 1381.3f, 697.8f, 1350.6f, 694.4f, 1340.2f)
                curveTo(691f, 1329.7f, 604.3f, 1034.4f, 604.3f, 1034.4f)
                curveTo(604.3f, 1034.4f, 595.4f, 971.8f, 655.2f, 967.7f)
                curveTo(694.8f, 979.1f, 698.6f, 1013f, 702.3f, 1018.7f)
                curveTo(705.9f, 1024.4f, 784.6f, 1297f, 784.6f, 1297f)
                curveTo(784.6f, 1297f, 804.3f, 1341.3f, 851.2f, 1332.3f)
                curveTo(899.6f, 1307.5f, 878.7f, 1253.9f, 878.7f, 1253.9f)
                lineTo(796.3f, 979.5f)
                curveTo(796.3f, 979.5f, 776.7f, 927.1f, 827.7f, 908.9f)
                curveTo(878.7f, 890.8f, 898.3f, 979.5f, 898.3f, 979.5f)
                lineTo(1015.9f, 1355.8f)
                curveTo(1015.9f, 1355.8f, 1063.4f, 1524.5f, 925.7f, 1630.3f)
                curveTo(1044.3f, 1836.8f, 1447.2f, 2492f, 1866.6f, 2465.3f)
                curveTo(2037.7f, 2426.6f, 2176.8f, 2402.3f, 2380.1f, 2183f)
                curveTo(2583.5f, 1963.8f, 3009f, 1376.2f, 2976f, 854f)
                curveTo(2910.7f, 313f, 2416.9f, 74.8f, 2046.9f, 152.3f)
                curveTo(1733.9f, 203.3f, 1600f, 430.7f, 1600f, 430.7f)
                close()
            }
        }.build()

        return _logo!!
    }

@Suppress("ObjectPropertyName")
private var _logo: ImageVector? = null
