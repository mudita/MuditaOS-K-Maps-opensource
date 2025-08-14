package com.mudita.map.common.navigation

import android.net.Uri
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.navOptions
import com.google.gson.Gson

sealed class Screen(val route: String) {
	object Map: Screen("map") {
		val ROUTE_WITH_ARGS = "$route$ARG_SEARCH_ITEM$ARG_NAV_ITEM"
	}
	object Search: Screen("search")
	object SearchCity: Screen("search_city")
	object Menu: Screen("menu")
	object Settings: Screen("settings")
	object MyPlaces: Screen("my_places")
	object AddMyPlace: Screen("add_my_place")
	object Download: Screen("download")
	object MapTypes: Screen("map_types")
	object SearchHistory: Screen("search_history")
	object RequiredMaps: Screen("required_maps")

	companion object {
		const val ARG_ITEM = "/{item}"
		const val ITEM = "item"
		const val ARG_BOOL_FLAG = "/{isFlag}"
		const val ITEM_FLAG = "isFlag"
		const val ARG_SEARCH_ITEM = "/{searchItem}"
		const val SEARCH_ITEM = "searchItem"
		const val ARG_NAV_ITEM = "/{navigationItem}"
		const val NAV_ITEM = "navigationItem"
		const val MAP_NAMES = "map_names"
		const val ARG_MAP_NAMES = "/{$MAP_NAMES}"
	}
}

fun NavController.navigateToMap(
	searchItem: String? = null,
	navItem: String? = null,
	navOptions: NavOptions = navOptions { popUpTo(Screen.Map.ROUTE_WITH_ARGS) { inclusive = true } },
) {
	navigate(
		route = "${Screen.Map.route}/${searchItem.toString()}/${navItem.toString()}",
		navOptions = navOptions,
	)
}

fun NavController.navigateToRequiredMaps(missingMapNames: List<String>, gson: Gson) {
	val mapNamesJson = Uri.encode(gson.toJson(missingMapNames))
	navigate(route = "${Screen.RequiredMaps.route}/$mapNamesJson")
}
