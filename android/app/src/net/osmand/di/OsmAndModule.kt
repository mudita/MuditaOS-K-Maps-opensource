package net.osmand.di

import android.app.Application
import com.mudita.download.domain.DeleteMapUseCase
import com.mudita.map.common.geocode.GeocodingLookupService
import com.mudita.map.common.maps.GetMissingMapsUseCase
import com.mudita.map.common.maps.OnMapsReIndexedUseCase
import com.mudita.map.common.model.LocalizationHelper
import com.mudita.map.common.model.StorageType
import com.mudita.map.common.navigation.IntermediatePointReachedUseCase
import com.mudita.map.common.navigation.StopVoiceRouterUseCase
import com.mudita.map.common.region.GetRegionsIndexedEvents
import com.mudita.map.common.sharedPrefs.SetMapLastKnownLocationUseCase
import com.mudita.map.common.utils.ChangeMapRotationModeUseCase
import com.mudita.map.common.utils.KEY_CURRENT_STORAGE_DIRECTORY
import com.mudita.map.common.utils.KEY_CURRENT_STORAGE_TYPE
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import net.osmand.map.OsmandRegions
import net.osmand.navigation.OsmAndIntermediatePointReachedUseCase
import net.osmand.navigation.OsmAndStopVoiceRouterUseCase
import net.osmand.plus.LocalGeocodingLookupService
import net.osmand.plus.OsmAndLocationProvider
import net.osmand.plus.OsmandApplication
import net.osmand.plus.base.MapViewTrackingUtilities
import net.osmand.plus.base.OsmAndChangeMapRotationModeUseCase
import net.osmand.plus.download.OsmAndDeleteMapUseCase
import net.osmand.plus.helpers.AppLocalizationHelper
import net.osmand.plus.region.OsmAndGetRegionsIndexedEvents
import net.osmand.plus.resources.OsmAndOnMapsReIndexedUseCase
import net.osmand.plus.routing.OsmAndGetMissingMapsUseCase
import net.osmand.plus.routing.RoutingHelper
import net.osmand.plus.settings.backend.OsmAndSetMapLastKnownLocationUseCase
import net.osmand.plus.settings.backend.OsmandSettings
import net.osmand.plus.settings.datastorage.DataStorageHelper
import net.osmand.plus.settings.datastorage.item.getStorageType

@Module
@InstallIn(SingletonComponent::class)
interface OsmAndModule {

    @Binds
    fun bindLocalGeocodingLookupService(service: LocalGeocodingLookupService): GeocodingLookupService

    @Binds
    fun bindIntermediatePointReachedUseCase(useCase: OsmAndIntermediatePointReachedUseCase): IntermediatePointReachedUseCase

    @Binds
    fun bindStopVoiceRouterUseCase(useCase: OsmAndStopVoiceRouterUseCase): StopVoiceRouterUseCase

    @Binds
    fun bindOsmAndGetMissingMapUseCase(useCase: OsmAndGetMissingMapsUseCase): GetMissingMapsUseCase

    @Binds
    fun bindOsmAndOnMapsReIndexedUseCase(useCase: OsmAndOnMapsReIndexedUseCase): OnMapsReIndexedUseCase

    @Binds
    fun bindOsmAndDeleteMapUseCase(useCase: OsmAndDeleteMapUseCase): DeleteMapUseCase

    @Binds
    fun bindOsmAndSetMapLastKnownLocationUseCase(useCase: OsmAndSetMapLastKnownLocationUseCase): SetMapLastKnownLocationUseCase

    @Binds
    fun bindOsmAndChangeMapRotationModeUseCase(useCase: OsmAndChangeMapRotationModeUseCase): ChangeMapRotationModeUseCase

    @Binds
    fun bindGetRegionsIndexedEvents(useCase: OsmAndGetRegionsIndexedEvents): GetRegionsIndexedEvents

    @Binds
    fun bindLocalizationHelper(helper: AppLocalizationHelper): LocalizationHelper

    companion object {
        @Provides
        fun provideOsmAndApplication(application: Application): OsmandApplication = application as OsmandApplication

        @Provides
        fun provideLocationProvider(application: OsmandApplication): OsmAndLocationProvider = checkNotNull(application.locationProvider)

        @Provides
        fun provideSettings(application: OsmandApplication): OsmandSettings = checkNotNull(application.settings)

        @Provides
        fun provideRoutingHelper(application: OsmandApplication): RoutingHelper = checkNotNull(application.routingHelper)

        @Singleton
        @Provides
        fun provideDataStorageHelper(application: OsmandApplication): DataStorageHelper = DataStorageHelper(application)

        @Provides
        @Named(KEY_CURRENT_STORAGE_DIRECTORY)
        fun provideCurrentStorageDirectory(dataStorageHelper: DataStorageHelper): String = dataStorageHelper.currentStorage.directory

        @Provides
        @Named(KEY_CURRENT_STORAGE_TYPE)
        fun provideCurrentStorageType(dataStorageHelper: DataStorageHelper): StorageType = dataStorageHelper.currentStorage.getStorageType()

        @Provides
        fun provideMapViewTrackingUtilities(application: OsmandApplication): MapViewTrackingUtilities =
            checkNotNull(application.mapViewTrackingUtilities)

        @Provides
        fun provideOsmandRegions(application: OsmandApplication): OsmandRegions = checkNotNull(application.regions)
    }
}
