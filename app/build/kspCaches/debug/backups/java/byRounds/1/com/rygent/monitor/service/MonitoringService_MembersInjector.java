package com.rygent.monitor.service;

import com.rygent.monitor.data.local.SettingsManager;
import com.rygent.monitor.domain.repository.DeviceRepository;
import com.rygent.monitor.ui.components.NotificationHelper;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class MonitoringService_MembersInjector implements MembersInjector<MonitoringService> {
  private final Provider<DeviceRepository> repositoryProvider;

  private final Provider<SettingsManager> settingsManagerProvider;

  private final Provider<NotificationHelper> notificationHelperProvider;

  public MonitoringService_MembersInjector(Provider<DeviceRepository> repositoryProvider,
      Provider<SettingsManager> settingsManagerProvider,
      Provider<NotificationHelper> notificationHelperProvider) {
    this.repositoryProvider = repositoryProvider;
    this.settingsManagerProvider = settingsManagerProvider;
    this.notificationHelperProvider = notificationHelperProvider;
  }

  public static MembersInjector<MonitoringService> create(
      Provider<DeviceRepository> repositoryProvider,
      Provider<SettingsManager> settingsManagerProvider,
      Provider<NotificationHelper> notificationHelperProvider) {
    return new MonitoringService_MembersInjector(repositoryProvider, settingsManagerProvider, notificationHelperProvider);
  }

  @Override
  public void injectMembers(MonitoringService instance) {
    injectRepository(instance, repositoryProvider.get());
    injectSettingsManager(instance, settingsManagerProvider.get());
    injectNotificationHelper(instance, notificationHelperProvider.get());
  }

  @InjectedFieldSignature("com.rygent.monitor.service.MonitoringService.repository")
  public static void injectRepository(MonitoringService instance, DeviceRepository repository) {
    instance.repository = repository;
  }

  @InjectedFieldSignature("com.rygent.monitor.service.MonitoringService.settingsManager")
  public static void injectSettingsManager(MonitoringService instance,
      SettingsManager settingsManager) {
    instance.settingsManager = settingsManager;
  }

  @InjectedFieldSignature("com.rygent.monitor.service.MonitoringService.notificationHelper")
  public static void injectNotificationHelper(MonitoringService instance,
      NotificationHelper notificationHelper) {
    instance.notificationHelper = notificationHelper;
  }
}
