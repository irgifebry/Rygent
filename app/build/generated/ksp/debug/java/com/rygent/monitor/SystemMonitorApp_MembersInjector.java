package com.rygent.monitor;

import androidx.hilt.work.HiltWorkerFactory;
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
public final class SystemMonitorApp_MembersInjector implements MembersInjector<SystemMonitorApp> {
  private final Provider<HiltWorkerFactory> workerFactoryProvider;

  public SystemMonitorApp_MembersInjector(Provider<HiltWorkerFactory> workerFactoryProvider) {
    this.workerFactoryProvider = workerFactoryProvider;
  }

  public static MembersInjector<SystemMonitorApp> create(
      Provider<HiltWorkerFactory> workerFactoryProvider) {
    return new SystemMonitorApp_MembersInjector(workerFactoryProvider);
  }

  @Override
  public void injectMembers(SystemMonitorApp instance) {
    injectWorkerFactory(instance, workerFactoryProvider.get());
  }

  @InjectedFieldSignature("com.rygent.monitor.SystemMonitorApp.workerFactory")
  public static void injectWorkerFactory(SystemMonitorApp instance,
      HiltWorkerFactory workerFactory) {
    instance.workerFactory = workerFactory;
  }
}
