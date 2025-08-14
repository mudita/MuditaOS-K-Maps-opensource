package net.osmand.router.errors;

public class RouteCalculationError extends RuntimeException {

    public RouteCalculationError(String message) {
        super(message);
    }

    public static final class RouteIsTooComplex extends RouteCalculationError {

        public RouteIsTooComplex(String message) {
            super(message);
        }
    }

    public static final class CalculationTimeLimitExceeded extends RouteCalculationError {

        public CalculationTimeLimitExceeded(String message) {
            super(message);
        }
    }

    public static final class EmptyRoute extends RouteCalculationError {

        public EmptyRoute(String message) {
            super(message);
        }
    }

    public static final class StartPointTooFarFromRoad extends RouteCalculationError {

        public StartPointTooFarFromRoad(String message) {
            super(message);
        }
    }

    public static final class EndPointTooFarFromRoad extends RouteCalculationError {

        public EndPointTooFarFromRoad(String message) {
            super(message);
        }
    }

    public static final class IntermediatePointTooFarFromRoad extends RouteCalculationError {

        private final int intermediatePointIndex;

        public IntermediatePointTooFarFromRoad(String message, int intermediatePointIndex) {
            super(message);
            this.intermediatePointIndex = intermediatePointIndex;
        }

        public int getIntermediatePointIndex() {
            return intermediatePointIndex;
        }
    }

    public static final class RouteNotFound extends RouteCalculationError {

        public RouteNotFound(String message) {
            super(message);
        }
    }

    public static final class ApplicationModeNotSupported extends RouteCalculationError {

        public ApplicationModeNotSupported(String message) {
            super(message);
        }
    }

    public static final class SelectedServiceNotAvailable extends RouteCalculationError {

        public SelectedServiceNotAvailable(String message) {
            super(message);
        }
    }

    public static final class CalculationCancelled extends RouteCalculationError {

        public CalculationCancelled(String message) {
            super(message);
        }
    }

    public static final class MissingMaps extends RouteCalculationError {

        public MissingMaps(String message) {
            super(message);
        }
    }
}
