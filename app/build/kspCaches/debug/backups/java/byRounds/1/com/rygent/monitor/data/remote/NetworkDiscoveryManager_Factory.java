package com.rygent.monitor.data.remote;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class NetworkDiscoveryManager_Factory implements Factory<NetworkDiscoveryManager> {
  private final Provider<Context> contextProvider;

  private final Provider<UdpDiscoveryManager> udpDiscoveryManagerProvider;

  public NetworkDiscoveryManager_Factory(Provider<Context> contextProvider,
      Provider<UdpDiscoveryManager> udpDiscoveryManagerProvider) {
    this.contextProvider = contextProvider;
    this.udpDiscoveryManagerProvider = udpDiscoveryManagerProvider;
  }

  @Override
  public NetworkDiscoveryManager get() {
    return newInstance(contextProvider.get(), udpDiscoveryManagerProvider.get());
  }

  public static NetworkDiscoveryManager_Factory create(Provider<Context> contextProvider,
      Provider<UdpDiscoveryManager> udpDiscoveryManagerProvider) {
    return new NetworkDiscoveryManager_Factory(contextProvider, udpDiscoveryManagerProvider);
  }

  public static NetworkDiscoveryManager newInstance(Context context,
      UdpDiscoveryManager udpDiscoveryManager) {
    return new NetworkDiscoveryManager(context, udpDiscoveryManager);
  }
}
