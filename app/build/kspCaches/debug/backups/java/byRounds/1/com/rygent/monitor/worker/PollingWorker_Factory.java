package com.rygent.monitor.worker;

import android.content.Context;
import androidx.work.WorkerParameters;
import com.rygent.monitor.data.local.SettingsManager;
import com.rygent.monitor.domain.repository.DeviceRepository;
import com.rygent.monitor.ui.components.NotificationHelper;
import dagger.internal.DaggerGenerated;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class PollingWorker_Factory {
  private final Provider<DeviceRepository> repositoryProvider;

  private final Provider<SettingsManager> settingsManagerProvider;

  private final Provider<NotificationHelper> notificationHelperProvider;

  public PollingWorker_Factory(Provider<DeviceRepository> repositoryProvider,
      Provider<SettingsManager> settingsManagerProvider,
      Provider<NotificationHelper> notificationHelperProvider) {
    this.repositoryProvider = repositoryProvider;
    this.settingsManagerProvider = settingsManagerProvider;
    this.notificationHelperProvider = notificationHelperProvider;
  }

  public PollingWorker get(Context appContext, WorkerParameters workerParams) {
    return newInstance(appContext, workerParams, repositoryProvider.get(), settingsManagerProvider.get(), notificationHelperProvider.get());
  }

  public static PollingWorker_Factory create(Provider<DeviceRepository> repositoryProvider,
      Provider<SettingsManager> settingsManagerProvider,
      Provider<NotificationHelper> notificationHelperProvider) {
    return new PollingWorker_Factory(repositoryProvider, settingsManagerProvider, notificationHelperProvider);
  }

  public static PollingWorker newInstance(Context appContext, WorkerParameters workerParams,
      DeviceRepository repository, SettingsManager settingsManager,
      NotificationHelper notificationHelper) {
    return new PollingWorker(appContext, workerParams, repository, settingsManager, notificationHelper);
  }
}
