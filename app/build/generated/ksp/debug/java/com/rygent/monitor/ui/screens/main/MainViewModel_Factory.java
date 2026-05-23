package com.rygent.monitor.ui.screens.main;

import com.rygent.monitor.data.remote.NetworkDiscoveryManager;
import com.rygent.monitor.domain.repository.DeviceRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class MainViewModel_Factory implements Factory<MainViewModel> {
  private final Provider<DeviceRepository> repositoryProvider;

  private final Provider<NetworkDiscoveryManager> discoveryManagerProvider;

  public MainViewModel_Factory(Provider<DeviceRepository> repositoryProvider,
      Provider<NetworkDiscoveryManager> discoveryManagerProvider) {
    this.repositoryProvider = repositoryProvider;
    this.discoveryManagerProvider = discoveryManagerProvider;
  }

  @Override
  public MainViewModel get() {
    return newInstance(repositoryProvider.get(), discoveryManagerProvider.get());
  }

  public static MainViewModel_Factory create(Provider<DeviceRepository> repositoryProvider,
      Provider<NetworkDiscoveryManager> discoveryManagerProvider) {
    return new MainViewModel_Factory(repositoryProvider, discoveryManagerProvider);
  }

  public static MainViewModel newInstance(DeviceRepository repository,
      NetworkDiscoveryManager discoveryManager) {
    return new MainViewModel(repository, discoveryManager);
  }
}
