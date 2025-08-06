// IMPLEMENTED (X) or MISSING ( ) FEATURES, (N/A) if not needed in this language:
//
// (X) Basic navigation prompts: route (re)calculated (with distance and time support), turns, roundabouts, u-turns, straight/follow, arrival
// (X) Announce nearby point names (destination / intermediate / GPX waypoint / favorites / POI)
// (X) Attention prompts: SPEED_CAMERA; SPEED_LIMIT; BORDER_CONTROL; RAILWAY; TRAFFIC_CALMING; TOLL_BOOTH; STOP; PEDESTRIAN; MAXIMUM; TUNNEL
// (X) Other prompts: gps lost, off route, back to route
// (X) Street name and prepositions (onto / on / to) and street destination (toward) support
// (X) Distance unit support (meters / feet / yard)
// (N/A) Special grammar: (please specify which)
// (X) Support announcing highway exits

var dictionary = {};
var metricConst;
var tts;

//// STRINGS
////////////////////////////////////////////////////////////////
function populateDictionary(tts) {
	// ROUTE CALCULATED
	dictionary["route_is"] = tts ? "A rota tem" : "route_is.mp3";
	dictionary["route_calculate"] = tts ? "Rota recalculada" : "route_calculate.mp3";
	dictionary["distance"] = tts ? "Distância de" : "distance.mp3";

	// LEFT/RIGHT
	dictionary["after"] = tts ? "depois de" : "after.mp3";
	dictionary["in"] = tts ? "após" : "in.mp3";

	dictionary["left"] = tts ? "vire à esquerda" : "left.mp3";
	dictionary["left_sh"] = tts ? "vire acentuadamente à esquerda" : "left_sh.mp3";
	dictionary["left_sl"] = tts ? "vire levemente à esquerda" : "left_sl.mp3";
	dictionary["right"] = tts ? "vire à direita" : "right.mp3";
	dictionary["right_sh"] = tts ? "vire acentuadamente à direita" : "right_sh.mp3";
	dictionary["right_sl"] = tts ? "vire levemente à direita" : "right_sl.mp3";
	// Note: "left_keep"/"right_keep" is a turn type aiding lane selection, while "left_bear"/"right_bear" is as brief "then..." preparation for the turn-after-next. In some languages l/r_keep may not differ from l/r_bear.
	dictionary["left_keep"] = tts ? "mantenha-se à esquerda" : "left_keep.mp3";
	dictionary["right_keep"] = tts ? "mantenha-se à direita" : "right_keep.mp3";
	dictionary["left_bear"] = tts ? "mantenha-se à esquerda" : "left_bear.mp3";   // in English the same as left_keep, may be different in other languages
	dictionary["right_bear"] = tts ? "mantenha-se à direita" : "right_bear.mp3";   // in English the same as right_keep, may be different in other languages

	dictionary["left_imp"] = tts ? "virar à esquerda" : "left_imp.mp3";
	dictionary["left_sh_imp"] = tts ? "virar acentuadamente à esquerda" : "left_sh_imp.mp3";
	dictionary["left_sl_imp"] = tts ? "virar levemente à esquerda" : "left_sl_imp.mp3";
	dictionary["right_imp"] = tts ? "virar à direita" : "right_imp.mp3";
	dictionary["right_sh_imp"] = tts ? "virar acentuadamente à direita" : "right_sh_imp.mp3";
	dictionary["right_sl_imp"] = tts ? "virar levemente à direita" : "right_sl_imp.mp3";
	dictionary["left_keep_imp"] = tts ? "manter-se à esquerda" : "left_keep_imp.mp3";
	dictionary["right_keep_imp"] = tts ? "manter-se à direita" : "right_keep_imp.mp3";

	// U-TURNS
	dictionary["make_uturn"] = tts ? "faça um retorno" : "make_uturn.mp3";
	dictionary["make_uturn_imp"] = tts ? "retornar" : "make_uturn_imp.mp3";
	dictionary["make_uturn_wp"] = tts ? "Retorne quando possível" : "make_uturn_wp.mp3";

	// ROUNDABOUTS
	dictionary["prepare_roundabout"] = tts ? "Prepare-se para a rotunda" : "prepare_roundabout.mp3";
	dictionary["roundabout"] = tts ? "entre na rotunda" : "roundabout.mp3";
	dictionary["then"] = tts ? "então" : "then.mp3";
	dictionary["and"] = tts ? "e" : "and.mp3";
	dictionary["take"] = tts ? "saia na" : "take.mp3";
	dictionary["exit"] = tts ? "saída" : "exit.mp3";

	dictionary["1st"] = tts ? "primeira" : "1st.mp3";
	dictionary["2nd"] = tts ? "segunda" : "2nd.mp3";
	dictionary["3rd"] = tts ? "terceira" : "3rd.mp3";
	dictionary["4th"] = tts ? "quarta" : "4th.mp3";
	dictionary["5th"] = tts ? "quinta" : "5th.mp3";
	dictionary["6th"] = tts ? "sexta" : "6th.mp3";
	dictionary["7th"] = tts ? "sétima" : "7th.mp3";
	dictionary["8th"] = tts ? "oitava" : "8th.mp3";
	dictionary["9th"] = tts ? "nona" : "9th.mp3";
	dictionary["10th"] = tts ? "décima" : "10th.mp3";
	dictionary["11th"] = tts ? "décima primeira" : "11th.mp3";
	dictionary["12th"] = tts ? "décima segunda" : "12th.mp3";
	dictionary["13th"] = tts ? "décima terceira" : "13th.mp3";
	dictionary["14th"] = tts ? "décima quarta" : "14th.mp3";
	dictionary["15th"] = tts ? "décima quinta" : "15th.mp3";
	dictionary["16th"] = tts ? "décima sexta" : "16th.mp3";
	dictionary["17th"] = tts ? "décima sétima" : "17th.mp3";

	// STRAIGHT/FOLLOW
	dictionary["go_ahead"] = tts ? "Siga em frente" : "go_ahead.mp3";
	dictionary["follow"] = tts ? "Siga o caminho por" : "follow.mp3";

	// ARRIVE
	dictionary["and_arrive_destination"] = tts ? "e chega ao destino" : "and_arrive_destination.mp3";
	dictionary["reached_destination"] = tts ? "chegou ao destino" : "reached_destination.mp3";
	dictionary["and_arrive_intermediate"] = tts ? "e chega ao ponto intermédio" : "and_arrive_intermediate.mp3";
	dictionary["reached_intermediate"] = tts ? "chegou ao ponto intermédio" : "reached_intermediate.mp3";

	// NEARBY POINTS
	dictionary["and_arrive_waypoint"] = tts ? "e chega ao ponto G P X" : "and_arrive_waypoint.mp3";
	dictionary["reached_waypoint"] = tts ? "chegou ao ponto G P X" : "reached_waypoint.mp3";
	dictionary["and_arrive_favorite"] = tts ? "e chega ao favorito" : "and_arrive_favorite.mp3";
	dictionary["reached_favorite"] = tts ? "chegou ao favorito" : "reached_favorite.mp3";
	dictionary["and_arrive_poi"] = tts ? "e chega ao POI" : "and_arrive_poi.mp3";
	dictionary["reached_poi"] = tts ? "chegou ao POI" : "reached_poi.mp3";

	// ATTENTION
	//dictionary["exceed_limit"] = tts ? "a exceder o limite de velocidade " : "exceed_limit.mp3";
	dictionary["exceed_limit"] = tts ? "limite de velocidade" : "exceed_limit.mp3";
	dictionary["attention"] = tts ? "Atenção" : "attention.mp3";
	dictionary["speed_camera"] = tts ? "radar" : "speed_camera.mp3";
	dictionary["border_control"] = tts ? "alfândega" : "border_control.mp3";
	dictionary["railroad_crossing"] = tts ? "Cruzamento de linha férrea" : "railroad_crossing.mp3";
	dictionary["traffic_calming"] = tts ? "obstáculo" : "traffic_calming.mp3";
	dictionary["toll_booth"] = tts ? "portagem" : "toll_booth.mp3";
	dictionary["stop"] = tts ? "pare" : "stop.mp3";
	dictionary["pedestrian_crosswalk"] = tts ? "passeio de pedestres" : "pedestrian_crosswalk.mp3";
	dictionary["tunnel"] = tts ? "túnel" : "tunnel.mp3";

	// OTHER PROMPTS
	dictionary["location_lost"] = tts ? "sem sinal g p s" : "location_lost.mp3";
	dictionary["location_recovered"] = tts ? "sinal g p s recuperado" : "location_recovered.mp3";
	dictionary["off_route"] = tts ? "desviou-se da rota por" : "off_route.mp3";
	dictionary["back_on_route"] = tts ? "retornou ao percurso" : "back_on_route.mp3";

	// STREET NAME PREPOSITIONS
	dictionary["onto"] = tts ? "para" : "onto.mp3";
	dictionary["on"] = tts ? "na" : "on.mp3";
	dictionary["to"] = tts ? "para" : "to.mp3";
	dictionary["toward"] = tts ? "em direção a" : "toward.mp3";

	// DISTANCE UNIT SUPPORT
	dictionary["meters"] = tts ? "metros" : "meters.mp3";
	dictionary["around_1_kilometer"] = tts ? "cerca de um quilómetro" : "around_1_kilometer.mp3";
	dictionary["around"] = tts ? "cerca de" : "around.mp3";
	dictionary["kilometers"] = tts ? "quilómetros" : "kilometers.mp3";

	dictionary["feet"] = tts ? "pés" : "feet.mp3";
	dictionary["1_tenth_of_a_mile"] = tts ? "um décimo de milha" : "1_tenth_of_a_mile.mp3";
	dictionary["tenths_of_a_mile"] = tts ? "décimos de milha" : "tenths_of_a_mile.mp3";
	dictionary["around_1_mile"] = tts ? "cerca de uma milha" : "around_1_mile.mp3";
	dictionary["miles"] = tts ? "milhas" : "miles.mp3";

	dictionary["yards"] = tts ? "jardas" : "yards.mp3";

	// TIME SUPPORT
	dictionary["time"] = tts ? "tempo estimado de" : "time.mp3";
	dictionary["1_hour"] = tts ? "uma hora" : "1_hour.mp3";
	dictionary["hours"] = tts ? "horas" : "hours.mp3";
	dictionary["less_a_minute"] = tts ? "menos de um minuto" : "less_a_minute.mp3";
	dictionary["1_minute"] = tts ? "um minuto" : "1_minute.mp3";
	dictionary["minutes"] = tts ? "minutos" : "minutes.mp3";
}


//// COMMAND BUILDING / WORD ORDER
////////////////////////////////////////////////////////////////
function setMetricConst(metrics) {
	metricConst = metrics;
}

function setMode(mode) {
	tts = mode;
	populateDictionary(mode);
}

function route_new_calc(dist, timeVal) {
	return dictionary["route_is"] + " " + distance(dist) + " " + dictionary["time"] + " " + time(timeVal) + (tts ? ". " : " ");
}

function distance(dist) {
	switch (metricConst) {
		case "km-m":
			if (dist < 17 ) {
				return (tts ? Math.round(dist).toString() : ogg_dist(Math.round(dist))) + " " + dictionary["meters"];
			} else if (dist < 100) {
				return (tts ? (Math.round(dist/10.0)*10).toString() : ogg_dist(Math.round(dist/10.0)*10)) + " " + dictionary["meters"];
			} else if (dist < 1000) {
				return (tts ? (Math.round(2*dist/100.0)*50).toString() : ogg_dist(Math.round(2*dist/100.0)*50)) + " " + dictionary["meters"];
			} else if (dist < 1500) {
				return dictionary["around_1_kilometer"];
			} else if (dist < 10000) {
				return dictionary["around"] + " " + (tts ? Math.round(dist/1000.0).toString() : ogg_dist(Math.round(dist/1000.0))) + " " + dictionary["kilometers"];
			} else {
				return (tts ? Math.round(dist/1000.0).toString() : ogg_dist(Math.round(dist/1000.0))) + " " + dictionary["kilometers"];
			}
			break;
		case "mi-f":
			if (dist < 160) {
				return (tts ? (Math.round(2*dist/100.0/0.3048)*50).toString() : ogg_dist(Math.round(2*dist/100.0/0.3048)*50)) + " " + dictionary["feet"];
			} else if (dist < 241) {
				return dictionary["1_tenth_of_a_mile"];
			} else if (dist < 1529) {
				return (tts ? Math.round(dist/161.0).toString() : ogg_dist(Math.round(dist/161.0))) + " " + dictionary["tenths_of_a_mile"];
			} else if (dist < 2414) {
				return dictionary["around_1_mile"];
			} else if (dist < 16093) {
				return dictionary["around"] + " " + (tts ? Math.round(dist/1609.3).toString() : ogg_dist(Math.round(dist/1609.3))) + " " + dictionary["miles"];
			} else {
				return (tts ? Math.round(dist/1609.3).toString() : ogg_dist(Math.round(dist/1609.3))) + " " + dictionary["miles"];
			}
			break;
		case "mi-m":
			if (dist < 17) {
				return (tts ? Math.round(dist).toString() : ogg_dist(Math.round(dist))) + " " + dictionary["meters"];
			} else if (dist < 100) {
				return (tts ? (Math.round(dist/10.0)*10).toString() : ogg_dist(Math.round(dist/10.0)*10)) + " " + dictionary["meters"];
			} else if (dist < 1300) {
				return (tts ? (Math.round(2*dist/100.0)*50).toString() : ogg_dist(Math.round(2*dist/100.0)*50)) + " " + dictionary["meters"];
			} else if (dist < 2414) {
				return dictionary["around_1_mile"];
			} else if (dist < 16093) {
				return dictionary["around"] + " " + (tts ? Math.round(dist/1609.3).toString() : ogg_dist(Math.round(dist/1609.3))) + " " + dictionary["miles"];
			} else {
				return (tts ? Math.round(dist/1609.3).toString() : ogg_dist(Math.round(dist/1609.3))) + " " + dictionary["miles"];
			}
			break;
		case "mi-y":
			if (dist < 17) {
				return (tts ? Math.round(dist/0.9144).toString() : ogg_dist(Math.round(dist/0.9144))) + " " + dictionary["yards"];
			} else if (dist < 100) {
				return (tts ? (Math.round(dist/10.0/0.9144)*10).toString() : ogg_dist(Math.round(dist/10.0/0.9144)*10)) + " " + dictionary["yards"];
			} else if (dist < 1300) {
				return (tts ? (Math.round(2*dist/100.0/0.9144)*50).toString() : ogg_dist(Math.round(2*dist/100.0/0.9144)*50)) + " " + dictionary["yards"];
			} else if (dist < 2414) {
				return dictionary["around_1_mile"];
			} else if (dist < 16093) {
				return dictionary["around"] + " " + (tts ? Math.round(dist/1609.3).toString() : ogg_dist(Math.round(dist/1609.3))) + " " + dictionary["miles"];
			} else {
				return (tts ? Math.round(dist/1609.3).toString() : ogg_dist(Math.round(dist/1609.3))) + " " + dictionary["miles"];
			}
			break;
	}
}

function time(seconds) {
	var minutes = Math.round(seconds/60.0);
	var oggMinutes = Math.round(((seconds/300.0) * 5));
	if (seconds < 30) {
		return dictionary["less_a_minute"];
	} else if (minutes % 60 == 0 && tts) {
		return hours(minutes);
	} else if (minutes % 60 == 1 && tts) {
		return hours(minutes) + " " + dictionary["1_minute"];
	} else if (tts) {
		return hours(minutes) + " " + (minutes % 60) + " " + dictionary["minutes"];
	} else if (!tts && seconds < 300) {
		return ogg_dist(minutes) + dictionary["minutes"];
	} else if (!tts && oggMinutes % 60 > 0) {
		return hours(oggMinutes) + " " + ogg_dist(oggMinutes % 60) + dictionary["minutes"];
	} else if (!tts) {
		return hours(oggMinutes);
	}
}

function hours(minutes) {
	if (minutes < 60) {
		return "";
	} else if (minutes < 120) {
		return dictionary["1_hour"];
	} else {
		var hours = Math.floor(minutes / 60);
        return  (tts ? hours.toString() : ogg_dist(hours)) + " " + dictionary["hours"];
	}
}

function route_recalc(dist, seconds) {
	return dictionary["route_calculate"] + " " + distance(dist) + " " + dictionary["time"] + " " + time(seconds) + (tts ? ". " : " ");
}

function go_ahead(dist, streetName) {
	if (dist == -1) {
		return dictionary["go_ahead"];
	} else {
		return dictionary["follow"] + " " + distance(dist) + " " + follow_street(streetName);
	}
}

function follow_street(streetName) {
	if ((streetName["toDest"] === "" && streetName["toStreetName"] === "" && streetName["toRef"] === "") || Object.keys(streetName).length == 0 || !tts) {
		return "";
	} else if (streetName["toStreetName"] === "" && streetName["toRef"] === "") {
		return dictionary["to"] + " " + streetName["toDest"];
	} else if (streetName["toRef"] === streetName["fromRef"] && streetName["toStreetName"] === streetName["fromStreetName"] ||
			(streetName["toRef"] == streetName["fromRef"] && streetName["toStreetName"] == "")) {
		return dictionary["on"] + " " + assemble_street_name(streetName);
	} else if (!(streetName["toRef"] === streetName["fromRef"] && streetName["toStreetName"] === streetName["fromStreetName"])) {
		return dictionary["to"] + " " + assemble_street_name(streetName);
	}
}

function turn(turnType, dist, streetName) {
	if (dist == -1) {
		return getTurnType(turnType) + " " + turn_street(streetName);
	} else {
		return dictionary["in"] + " " + distance(dist) + " " + getTurnType(turnType) + " " + turn_street(streetName);
	}
}

function take_exit(turnType, dist, exitString, exitInt, streetName) {
	if (dist == -1) {
		return getTurnType(turnType) + " " + dictionary["onto"] + " " + getExitNumber(exitString, exitInt) + " " + take_exit_name(streetName)
	} else {
		return dictionary["in"] + " " + distance(dist) + " "
			+ getTurnType(turnType) + " " + dictionary["onto"] + " " + getExitNumber(exitString, exitInt) + " " + take_exit_name(streetName)
	}
}

function take_exit_name(streetName) {
	if (Object.keys(streetName).length == 0 || (streetName["toDest"] === "" && streetName["toStreetName"] === "") || !tts) {
		return "";
	} else if (streetName["toDest"] != "") {
		return (tts ? ", " : " ") + streetName["toStreetName"] + " " + dictionary["toward"] + " " + streetName["toDest"];
	} else if (streetName["toStreetName"] != "") {
		return (tts ? ", " : " ") + streetName["toStreetName"]
	} else {
		return "";
	}
}

function getExitNumber(exitString, exitInt) {
	if (!tts && exitInt > 0 && exitInt < 18) {
		return nth(exitInt) + " " + dictionary["exit"];
	} else if (tts) {
		return  dictionary["exit"] + " " + exitString;
	} else {
		return dictionary["exit"];
	}
}

function  getTurnType(turnType) {
	switch (turnType) {
		case "left":
			return dictionary["left"];
			break;
		case "left_sh":
			return dictionary["left_sh"];
			break;
		case "left_sl":
			return dictionary["left_sl"];
			break;
		case "right":
			return dictionary["right"];
			break;
		case "right_sh":
			return dictionary["right_sh"];
			break;
		case "right_sl":
			return dictionary["right_sl"];
			break;
		case "left_keep":
			return dictionary["left_keep"];
			break;
		case "right_keep":
			return dictionary["right_keep"];
			break;
	}
}

function  getTurnTypeImp(turnType) {
	switch (turnType) {
		case "left":
			return dictionary["left_imp"];
			break;
		case "left_sh":
			return dictionary["left_sh_imp"];
			break;
		case "left_sl":
			return dictionary["left_sl_imp"];
			break;
		case "right":
			return dictionary["right_imp"];
			break;
		case "right_sh":
			return dictionary["right_sh_imp"];
			break;
		case "right_sl":
			return dictionary["right_sl_imp"];
			break;
		case "left_keep":
			return dictionary["left_keep_imp"];
			break;
		case "right_keep":
			return dictionary["right_keep_imp"];
			break;
	}
}

function then() {
	return dictionary["then"] + " ";
}

function roundabout(dist, angle, exit, streetName) {
	if (dist == -1) {
		return dictionary["take"] + " " + nth(exit) + " " + dictionary["exit"] + " " + turn_street(streetName);
	} else {
		return dictionary["in"] + " " + distance(dist) + " " + dictionary["roundabout"] + " " + dictionary["and"] + " " + dictionary["take"] + " " + nth(exit) + " " + dictionary["exit"] + " " + turn_street(streetName);
	}

}

function turn_street(streetName) {
	if ((streetName["toDest"] === "" && streetName["toStreetName"] === "" && streetName["toRef"] === "") || Object.keys(streetName).length == 0 || !tts) {
		return "";
	} else if (streetName["toStreetName"] === "" && streetName["toRef"] === "") {
		return dictionary["toward"] + " " + streetName["toDest"];
	} else if (streetName["toRef"] === streetName["fromRef"] && streetName["toStreetName"] === streetName["fromStreetName"]) {
		return dictionary["on"] + " " + assemble_street_name(streetName);
	} else if ((streetName["toRef"] === streetName["fromRef"] && streetName["toStreetName"] === streetName["fromStreetName"])
		|| (streetName["toStreetName"] === "" && streetName["toRef"] === streetName["fromRef"])) {
		return dictionary["on"] + " " + assemble_street_name(streetName);
	} else if (!(streetName["toRef"] === streetName["fromRef"] && streetName["toStreetName"] === streetName["fromStreetName"])) {
		return dictionary["onto"] + " " + assemble_street_name(streetName);
	}
	return "";
}

function assemble_street_name(streetName) {
	if (streetName["toDest"] === "") {
		return streetName["toRef"] + " " + streetName["toStreetName"];
	} else if (streetName["toRef"] === "") {
		return streetName["toStreetName"] + " " + dictionary["toward"] + " " + streetName["toDest"];
	} else if (streetName["toRef"] != "") {
		return streetName["toRef"] + " " + dictionary["toward"] + " " + streetName["toDest"];
	}
}

function nth(exit) {
	switch (exit) {
		case (1):
			return dictionary["1st"];
		case (2):
			return dictionary["2nd"];
		case (3):
			return dictionary["3rd"];
		case (4):
			return dictionary["4th"];
		case (5):
			return dictionary["5th"];
		case (6):
			return dictionary["6th"];
		case (7):
			return dictionary["7th"];
		case (8):
			return dictionary["8th"];
		case (9):
			return dictionary["9th"];
		case (10):
			return dictionary["10th"];
		case (11):
			return dictionary["11th"];
		case (12):
			return dictionary["12th"];
		case (13):
			return dictionary["13th"];
		case (14):
			return dictionary["14th"];
		case (15):
			return dictionary["15th"];
		case (16):
			return dictionary["16th"];
		case (17):
			return dictionary["17th"];
	}
}

function make_ut(dist, streetName) {
	if (dist == -1) {
		return dictionary["make_uturn"] + " " + turn_street(streetName);
	} else {
		return dictionary["in"] + " " + distance(dist) + " " + dictionary["make_uturn"] + " " + turn_street(streetName);
	}
}

function bear_left(streetName) {
	return dictionary["left_bear"];
}

function bear_right(streetName) {
	return dictionary["right_bear"];
}

function prepare_make_ut(dist, streetName) {
	return dictionary["after"] + " " + distance(dist) + " " + dictionary["make_uturn_imp"] + " " + turn_street(streetName);
}

function prepare_turn(turnType, dist, streetName) {
	return dictionary["after"] + " " + distance(dist) + " " + getTurnTypeImp(turnType) + " " + turn_street(streetName);
}

function prepare_roundabout(dist, exit, streetName) {
	return dictionary["after"] + " " + distance(dist) + " " + dictionary["prepare_roundabout"];
}

function and_arrive_destination(dest) {
	return dictionary["and_arrive_destination"] + " " + dest;
}

function and_arrive_intermediate(dest) {
	return dictionary["and_arrive_intermediate"] + " " + dest;
}

function and_arrive_waypoint(dest) {
	return dictionary["and_arrive_waypoint"] + " " + dest;
}

function and_arrive_favorite(dest) {
	return dictionary["and_arrive_favorite"] + " " + dest;
}

function and_arrive_poi(dest) {
	return dictionary["and_arrive_poi"] + " " + dest;
}

function reached_destination(dest) {
	return dictionary["reached_destination"] + " " + dest;
}

function reached_waypoint(dest) {
	return dictionary["reached_waypoint"] + " " + dest;
}

function reached_intermediate(dest) {
	return dictionary["reached_intermediate"] + " " + dest;
}

function reached_favorite(dest) {
	return dictionary["reached_favorite"] + " " + dest;
}

function reached_poi(dest) {
	return dictionary["reached_poi"] + " " + dest;
}

function location_lost() {
	return dictionary["location_lost"];
}

function location_recovered() {
	return dictionary["location_recovered"];
}

function off_route(dist) {
	return dictionary["off_route"] + " " + distance(dist);
}

function back_on_route() {
	return dictionary["back_on_route"];
}

function make_ut_wp() {
	return dictionary["make_uturn_wp"];
}

// TRAFFIC WARNINGS
function speed_alarm(maxSpeed, speed) {
	return dictionary["exceed_limit"] + " " + maxSpeed.toString();
}

function attention(type) {
	return dictionary["attention"] + (tts ? "! " : " ") + getAttentionString(type);
}

function getAttentionString(type) {
	switch (type) {
		case "SPEED_CAMERA":
			return dictionary["speed_camera"];
			break;
		case "SPEED_LIMIT":
			return "";
			break
		case "BORDER_CONTROL":
			return dictionary["border_control"];
			break;
		case "RAILWAY":
			return dictionary["railroad_crossing"];
			break;
		case "TRAFFIC_CALMING":
			return dictionary["traffic_calming"];
			break;
		case "TOLL_BOOTH":
			return dictionary["toll_booth"];
			break;
		case "STOP":
			return dictionary["stop"];
			break;
		case "PEDESTRIAN":
			return dictionary["pedestrian_crosswalk"];
			break;
		case "MAXIMUM":
			return "";
			break;
		case "TUNNEL":
			return dictionary["tunnel"];
			break;
		default:
			return "";
			break;
	}
}

function ogg_dist(distance) {
	if (distance == 0) {
		return "";
	} else if (distance < 20) {
		return Math.floor(distance).toString() + ".mp3 ";
	} else if (distance < 1000 && (distance % 50) == 0) {
		return distance.toString() + ".mp3 ";
	} else if (distance < 30) {
		return "20.mp3 " + ogg_dist(distance - 20);
	} else if (distance < 40) {
		return "30.mp3 " + ogg_dist(distance - 30);
	} else if (distance < 50) {
		return "40.mp3 " + ogg_dist(distance - 40);
	} else if (distance < 60) {
		return "50.mp3 " + ogg_dist(distance - 50);
	} else if (distance < 70) {
		return "60.mp3 " + ogg_dist(distance - 60);
	} else if (distance < 80) {
		return "70.mp3 "+ ogg_dist(distance - 70);
	} else if (distance < 90) {
		return "80.mp3 " + ogg_dist(distance - 80);
	} else if (distance < 100) {
		return "90.mp3 " + ogg_dist(distance - 90);
	} else if (distance < 200) {
		return "100.mp3 " + ogg_dist(distance - 100);
	} else if (distance < 300) {
		return "200.mp3 " + ogg_dist(distance - 200);
	} else if (distance < 400) {
		return "300.mp3 "+ ogg_dist(distance - 300);
	} else if (distance < 500) {
		return "400.mp3 " + ogg_dist(distance - 400);
	} else if (distance < 600) {
		return "500.mp3 " + ogg_dist(distance - 500);
	} else if (distance < 700) {
		return "600.mp3 " + ogg_dist(distance - 600);
	} else if (distance < 800) {
		return "700.mp3 " + ogg_dist(distance - 700);
	} else if (distance < 900) {
		return "800.mp3 " + ogg_dist(distance - 800);
	} else if (distance < 1000) {
		return "900.mp3 " + ogg_dist(distance - 900);
	} else {
		return ogg_dist(distance/1000) + "1000.mp3 " + ogg_dist(distance % 1000);
	}
}
