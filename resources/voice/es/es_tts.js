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

var metricConst;
var dictionary = {};
var tts;

//// STRINGS
////////////////////////////////////////////////////////////////
function populateDictionary(tts) {
	// ROUTE CALCULATED
	dictionary["route_is"] = tts ? "Ruta de" : "route_is.mp3";
	dictionary["route_calculate"] = tts ? "Ruta recalculada" : "route_calculate.mp3";
	dictionary["distance"] = tts ? "distancia" : "distance.mp3";

	// LEFT/RIGHT
	//dictionary["prepare"] = tts ? "Prepárate para" : "prepare.mp3";
	dictionary["after"] = tts ? "después de" : "after.mp3";
	dictionary["in"] = tts ? "en" : "in.mp3"; // duplicated with 'on.mp3' in Spanish

	dictionary["left"] = tts ? "gira a la izquierda" : "left.mp3";
	dictionary["left_sh"] = tts ? "gira cerrado a la izquierda" : "left_sh.mp3";
	dictionary["left_sl"] = tts ? "gira levemente a la izquierda" : "left_sl.mp3";
	dictionary["right"] = tts ? "gira a la derecha" : "right.mp3";
	dictionary["right_sh"] = tts ? "gira cerrado a la derecha" : "right_sh.mp3";
	dictionary["right_sl"] = tts ? "gira levemente a la derecha" : "right_sl.mp3";
	// Note: "left_keep"/"right_keep" is a turn type aiding lane selection, while "left_bear"/"right_bear" is as brief "then..." preparation for the turn-after-next. In some languages l/r_keep may not differ from l/r_bear.
	dictionary["left_keep"] = tts ? "mantente a la izquierda" : "left_keep.mp3";
	dictionary["right_keep"] = tts ? "mantente a la derecha" : "right_keep.mp3";
	dictionary["left_bear"] = tts ? "gira a la izquierda" : "left_bear.mp3";   // in English the same as left_keep, may be different in other languages
	dictionary["right_bear"] = tts ? "gira a la derecha" : "right_bear.mp3";    // in English the same as right_keep, may be different in other languages

	// U-TURNS
	dictionary["make_uturn"] = tts ? "Da la vuelta" : "make_uturn.mp3";
	dictionary["make_uturn_wp"] = tts ? "Cuando puedas, da la vuelta" : "make_uturn_wp.mp3";

	// ROUNDABOUTS
	dictionary["prepare_roundabout"] = tts ? "entra en la rotonda" : "prepare_roundabout.mp3";
	dictionary["roundabout"] = tts ? "entra en la rotonda" : "roundabout.mp3";
	dictionary["then"] = tts ? "luego" : "then.mp3";
	dictionary["and"] = tts ? "y" : "and.mp3";
	dictionary["take"] = tts ? "toma la" : "take.mp3";
	dictionary["exit"] = tts ? "salida" : "exit.mp3";

	dictionary["1st"] = tts ? "primera" : "1st.mp3";
	dictionary["2nd"] = tts ? "segunda" : "2nd.mp3";
	dictionary["3rd"] = tts ? "tercera" : "3rd.mp3";
	dictionary["4th"] = tts ? "cuarta" : "4th.mp3";
	dictionary["5th"] = tts ? "quinta" : "5th.mp3";
	dictionary["6th"] = tts ? "sexta" : "6th.mp3";
	dictionary["7th"] = tts ? "séptima" : "7th.mp3";
	dictionary["8th"] = tts ? "octava" : "8th.mp3";
	dictionary["9th"] = tts ? "novena" : "9th.mp3";
	dictionary["10th"] = tts ? "décima" : "10th.mp3";
	dictionary["11th"] = tts ? "undécima" : "11th.mp3";
	dictionary["12th"] = tts ? "duodécima" : "12th.mp3";
	dictionary["13th"] = tts ? "decimotercera" : "13th.mp3";
	dictionary["14th"] = tts ? "decimocuarta" : "14th.mp3";
	dictionary["15th"] = tts ? "decimoquinta" : "15th.mp3";
	dictionary["16th"] = tts ? "decimosexta" : "16th.mp3";
	dictionary["17th"] = tts ? "decimoséptima" : "17th.mp3";

	// STRAIGHT/FOLLOW
	dictionary["go_ahead"] = tts ? "Continúa recto" : "go_ahead.mp3";
	//dictionary["follow"] = tts ? "Sigue la vía durante" : "follow.mp3";
	dictionary["follow"] = tts ? "Continúa por" : "follow.mp3";  // 'Follow the course of the road for' perceived as too chatty by many users

	// ARRIVE
	dictionary["and_arrive_destination"] = tts ? "y llegarás al destino" : "and_arrive_destination.mp3";
	dictionary["reached_destination"] = tts ? "has llegado al destino" : "reached_destination.mp3";
	dictionary["and_arrive_intermediate"] = tts ? "y llegarás al punto intermedio" : "and_arrive_intermediate.mp3";
	dictionary["reached_intermediate"] = tts ? "has llegado al punto intermedio" : "reached_intermediate.mp3";

	// NEARBY POINTS
	dictionary["and_arrive_waypoint"] = tts ? "y pasarás tu punto GPX intermedio" : "and_arrive_waypoint.mp3";
	dictionary["reached_waypoint"] = tts ? "estás pasando tu punto GPX intermedio" : "reached_waypoint.mp3";
	dictionary["and_arrive_favorite"] = tts ? "y pasarás tu favorito" : "and_arrive_favorite.mp3";
	dictionary["reached_favorite"] = tts ? "estás pasando tu punto favorito" : "reached_favorite.mp3";
	dictionary["and_arrive_poi"] = tts ? "y pasarás el PDI" : "and_arrive_poi.mp3";
	dictionary["reached_poi"] = tts ? "estás pasando el PDI" : "reached_poi.mp3";

	// ATTENTION
	//dictionary["exceed_limit"] = tts ? "límite de velocidad excedido" : "exceed_limit.mp3";
	dictionary["exceed_limit"] = tts ? "límite de velocidad" : "exceed_limit.mp3";
	dictionary["attention"] = tts ? "atención" : "attention.mp3";
	dictionary["speed_camera"] = tts ? "radar de velocidad" : "speed_camera.mp3";
	dictionary["border_control"] = tts ? "control fronterizo" : "border_control.mp3";
	dictionary["railroad_crossing"] = tts ? "paso a nivel" : "railroad_crossing.mp3";
	dictionary["traffic_calming"] = tts ? "reductor de velocidad" : "traffic_calming.mp3";
	dictionary["toll_booth"] = tts ? "cabina de peaje" : "toll_booth.mp3";
	dictionary["stop"] = tts ? "señal de ESTOP" : "stop.mp3";
	dictionary["pedestrian_crosswalk"] = tts ? "paso de peatones" : "pedestrian_crosswalk.mp3";
	dictionary["tunnel"] = tts ? "túnel" : "tunnel.mp3";

	// OTHER PROMPTS
	dictionary["location_lost"] = tts ? "señal GPS perdida" : "location_lost.mp3";
	dictionary["location_recovered"] = tts ? "señal GPS encontrada" : "location_recovered.mp3";
	dictionary["off_route"] = tts ? "te has desviado de la ruta" : "off_route.mp3";
	dictionary["back_on_route"] = tts ? "has retomado la ruta" : "back_on_route.mp3";

	// STREET NAME PREPOSITIONS
	//dictionary["onto"] = tts ? "en dirección a" : "onto.mp3";   // unused in Spanish
	dictionary["on"] = tts ? "en" : "on.mp3";        // duplicated with 'in.mp3' in Spanish
	dictionary["to"] = tts ? "hacia" : "to.mp3";     // duplicated with 'toward.mp3' in Spanish
	dictionary["toward"] = tts ? "hacia" : "toward.mp3"; // duplicated with 'to.mp3' in Spanish

	// DISTANCE UNIT SUPPORT
	dictionary["meters"] = tts ? "metros" : "meters.mp3";
	dictionary["around_1_kilometer"] = tts ? "aproximadamente un kilómetro" : "around_1_kilometer.mp3";
	dictionary["around"] = tts ? "aproximadamente" : "around.mp3";
	dictionary["kilometers"] = tts ? "kilómetros" : "kilometers.mp3";

	dictionary["feet"] = tts ? "pies" : "feet.mp3";
	dictionary["1_tenth_of_a_mile"] = tts ? "una décima de milla" : "1_tenth_of_a_mile.mp3";
	dictionary["tenths_of_a_mile"] = tts ? "décimas de milla" : "tenths_of_a_mile.mp3";
	dictionary["around_1_mile"] = tts ? "aproximadamente una milla" : "around_1_mile.mp3";
	dictionary["miles"] = tts ? "millas" : "miles.mp3";
	dictionary["yards"] = tts ? "yardas" : "yards.mp3";

	// TIME SUPPORT
	dictionary["time"] = tts ? "tiempo" : "time.mp3";
	dictionary["1_hour"] = tts ? "una hora" : "1_hour.mp3";
	dictionary["hours"] = tts ? "horas" : "hours.mp3";
	dictionary["less_a_minute"] = tts ? "menos de un minuto" : "less_a_minute.mp3";
	dictionary["1_minute"] = tts ? "un minuto" : "1_minute.mp3";
	dictionary["minutes"] = tts ? "minutos" : "minutes.mp3";

	// SPECIAL NUMBERS
	dictionary["20_and"] = tts ? "veinti" : "20_and.mp3"; //unused
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
	return dictionary["route_is"] + " " + distance(dist) + (tts ? ", " : " ") + dictionary["time"] + " " + time(timeVal) + (tts ? ". " : " ");
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
	//	return dictionary["route_calculate"] + (tts ? ", " : " ") + dictionary["distance"] + " " + distance(dist) + " " + dictionary["time"] + " " + time(seconds) + (tts ? ". " : " ");
	return dictionary["route_calculate"] + (tts ? ": " : " ") + distance(dist) + ", " + time(seconds) + (tts ? ". " : " "); // short version
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
		return dictionary["in"] + " " + distance(dist) + (tts ? ", " : " ") + getTurnType(turnType) + " " + turn_street(streetName);
	}
}

function take_exit(turnType, dist, exitString, exitInt, streetName) {
	if (dist == -1) {
		return getTurnType(turnType) + " " + dictionary["on"] + " " + getExitNumber(exitString, exitInt) + take_exit_name(streetName);
	} else {
		return dictionary["in"] + " " + distance(dist) + (tts ? ", " : " ")
			+ getTurnType(turnType) + " " + dictionary["on"] + " " + getExitNumber(exitString, exitInt) + take_exit_name(streetName);
	}
}

function take_exit_name(streetName) {
	if (Object.keys(streetName).length == 0 || (streetName["toDest"] === "" && streetName["toStreetName"] === "") || !tts) {
		return "";
	} else if (streetName["toDest"] != "") {
		return (tts ? ", " : " ") + streetName["toStreetName"] + " " + dictionary["toward"] + " " + streetName["toDest"];
	} else if (streetName["toStreetName"] != "") {
		return (tts ? ", " : " ") + dictionary["toward"] + " " + streetName["toStreetName"];
	} else {
		return "";
	}
}

function getExitNumber(exitString, exitInt) {
	if (!tts && exitInt > 0 && exitInt < 18) {
		return nth(exitInt) + " " + dictionary["exit"];
	} else if (tts) {
		return dictionary["exit"] + " " + exitString;
	} else {
		return dictionary["exit"];
	}
}

function getTurnType(turnType) {
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

function then() {
	return (tts ? ", " : " ") + dictionary["then"];
}

function roundabout(dist, angle, exit, streetName) {
	if (dist == -1) {
		return dictionary["take"] + " " + nth(exit) + " " + dictionary["exit"] + " " + turn_street(streetName);
	} else {
		return dictionary["on"] + " " + distance(dist) + " " + dictionary["roundabout"] + (tts ? ", " : " ") + dictionary["and"] + " " + dictionary["take"] + " " + nth(exit) + " " + dictionary["exit"] + " " + turn_street(streetName);
	}
}

function turn_street(streetName) {
	if (Object.keys(streetName).length == 0 || (streetName["toDest"] === "" && streetName["toStreetName"] === "" && streetName["toRef"] === "") || !tts) {
		return "";
	} else if (streetName["toStreetName"] === "" && streetName["toRef"] === "") {
		return dictionary["toward"] + " " + streetName["toDest"];
	} else if (streetName["toRef"] === streetName["fromRef"] && streetName["toStreetName"] === streetName["fromStreetName"]) {
		return dictionary["on"] + " " + assemble_street_name(streetName);
	} else if ((streetName["toRef"] === streetName["fromRef"] && streetName["toStreetName"] === streetName["fromStreetName"])
		|| (streetName["toStreetName"] === "" && streetName["toRef"] === streetName["fromRef"])) {
		return dictionary["on"] + " " + assemble_street_name(streetName);
	} else if (!(streetName["toRef"] === streetName["fromRef"] && streetName["toStreetName"] === streetName["fromStreetName"])) {
		return dictionary["on"] + " " + assemble_street_name(streetName);
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
		return dictionary["on"] + " " + distance(dist) + " " + dictionary["make_uturn"] + " " + turn_street(streetName);
	}
}

function bear_left(streetName) {
	return dictionary["left_bear"];
}

function bear_right(streetName) {
	return dictionary["right_bear"];
}

function prepare_make_ut(dist, streetName) {
	return dictionary["on"] + " " + distance(dist) + " " + dictionary["make_uturn"] + " " + turn_street(streetName);
}

function prepare_turn(turnType, dist, streetName) {
	return dictionary["on"] + " " + distance(dist) + (tts ? ", " : " ") + getTurnType(turnType) + " " + turn_street(streetName);
}

function prepare_roundabout(dist, exit, streetName) {
	return dictionary["on"] + " " + distance(dist) + " " + dictionary["prepare_roundabout"];
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
	return dictionary["attention"] + (tts ? ", " : " ") + getAttentionString(type);
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

// DISTANCE MEASURE
function ogg_dist(distance) {
	if (distance == 0) {
		return "";
	} else if (distance < 20) {
		return Math.floor(distance).toString() + ".mp3 ";
	} else if (distance < 1000 && (distance % 50) == 0) {
		return distance.toString() + ".mp3 ";
	} else if (distance == 20) {
		return "20.mp3 ";
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
