package com.mudita.map.common.utils.route

import com.mudita.map.common.R
import net.osmand.router.TurnType
import net.osmand.router.TurnType.C
import net.osmand.router.TurnType.KL
import net.osmand.router.TurnType.KR
import net.osmand.router.TurnType.OFFR
import net.osmand.router.TurnType.RNDB
import net.osmand.router.TurnType.RNLB
import net.osmand.router.TurnType.TL
import net.osmand.router.TurnType.TR
import net.osmand.router.TurnType.TRU
import net.osmand.router.TurnType.TSHL
import net.osmand.router.TurnType.TSHR
import net.osmand.router.TurnType.TSLL
import net.osmand.router.TurnType.TSLR
import net.osmand.router.TurnType.TU

fun TurnType?.getIcon() = when (this?.value) {
    C -> R.drawable.icon_forward // Continue straight
    TL -> R.drawable.icon_left // Turn left
    TSLL -> R.drawable.icon_left_slightly // Turn slightly left
    TSHL -> R.drawable.icon_left_sharp // Turn sharp left
    TR -> R.drawable.icon_right // Turn right
    TSLR -> R.drawable.icon_right_slightly // Turn slightly right
    TSHR -> R.drawable.icon_right_sharp // Turn sharp right
    KL -> R.drawable.icon_keep_left // Keep left
    KR -> R.drawable.icon_keep_right // Keep right
    TU -> R.drawable.icon_turn_around // U-turn
    TRU -> R.drawable.icon_turn_around_right // U-turn right
    OFFR -> R.drawable.icon_unknown_direction // Off round
    RNDB -> { // Roundabout (right-hand traffic)
        when  {
            this.exitOut == 1 -> R.drawable.icon_rondabout_right
            this.exitOut == 2 -> R.drawable.icon_rondabout_forward
            this.exitOut >= 3 -> R.drawable.icon_rondabout_left
            else -> R.drawable.icon_rondabout_forward
        }
    }
    RNLB -> { // Roundabout (left-hand traffic)
        when  {
            this.exitOut == 1 -> R.drawable.icon_lefthand_roundabout_left
            this.exitOut == 2 -> R.drawable.icon_lefthand_roundabout_forward
            this.exitOut >= 3 -> R.drawable.icon_lefthand_roundabout_right
            else -> R.drawable.icon_lefthand_roundabout_forward
        }
    }
    else -> R.drawable.icon_unknown_direction // Unknown
}