package com.rygent.monitor.ui.screens.adddevice;

import com.rygent.monitor.data.local.TokenManager;
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
public final class AddDeviceViewModel_Factory implements Factory<AddDeviceViewModel> {
  private final Provider<DeviceRepository> repositoryProvider;

  private final Provider<TokenManager> tokenManagerProvider;

  private final Provider<NetworkDiscoveryManager> discoveryManagerProvider;

  public AddDeviceViewModel_Factory(Provider<DeviceRepository> repositoryProvider,
      Provider<TokenManager> tokenManagerProvider,
      Provider<NetworkDiscoveryManager> discoveryManagerProvider) {
    this.repositoryProvider = repositoryProvider;
    this.tokenManagerProvider = tokenManagerProvider;
    this.discoveryManagerProvider = discoveryManagerProvider;
  }

  @Override
  public AddDeviceViewModel get() {
    return newInstance(repositoryProvider.get(), tokenManagerProvider.get(), discoveryManagerProvider.get());
  }

  public static AddDeviceViewModel_Factory create(Provider<DeviceRepository> repositoryProvider,
      Provider<TokenManager> tokenManagerProvider,
      Provider<NetworkDiscoveryManager> discoveryManagerProvider) {
    return new AddDeviceViewModel_Factory(repositoryProvider, tokenManagerProvider, discoveryManagerProvider);
  }

  public static AddDeviceViewModel newInstance(DeviceRepository repository,
      TokenManager tokenManager, NetworkDiscoveryManager discoveryManager) {
    return new AddDeviceViewModel(repository, tokenManager, discoveryManager);
  }
}
