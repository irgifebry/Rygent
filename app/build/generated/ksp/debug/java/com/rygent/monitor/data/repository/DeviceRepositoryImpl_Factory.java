package com.rygent.monitor.data.repository;

import com.rygent.monitor.data.local.TokenManager;
import com.rygent.monitor.data.local.dao.DeviceDao;
import com.rygent.monitor.data.remote.SystemMonitorApiService;
import com.squareup.moshi.Moshi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("javax.inject.Named")
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
public final class DeviceRepositoryImpl_Factory implements Factory<DeviceRepositoryImpl> {
  private final Provider<DeviceDao> daoProvider;

  private final Provider<SystemMonitorApiService> apiServiceProvider;

  private final Provider<SystemMonitorApiService> statusApiServiceProvider;

  private final Provider<TokenManager> tokenManagerProvider;

  private final Provider<Moshi> moshiProvider;

  public DeviceRepositoryImpl_Factory(Provider<DeviceDao> daoProvider,
      Provider<SystemMonitorApiService> apiServiceProvider,
      Provider<SystemMonitorApiService> statusApiServiceProvider,
      Provider<TokenManager> tokenManagerProvider, Provider<Moshi> moshiProvider) {
    this.daoProvider = daoProvider;
    this.apiServiceProvider = apiServiceProvider;
    this.statusApiServiceProvider = statusApiServiceProvider;
    this.tokenManagerProvider = tokenManagerProvider;
    this.moshiProvider = moshiProvider;
  }

  @Override
  public DeviceRepositoryImpl get() {
    return newInstance(daoProvider.get(), apiServiceProvider.get(), statusApiServiceProvider.get(), tokenManagerProvider.get(), moshiProvider.get());
  }

  public static DeviceRepositoryImpl_Factory create(Provider<DeviceDao> daoProvider,
      Provider<SystemMonitorApiService> apiServiceProvider,
      Provider<SystemMonitorApiService> statusApiServiceProvider,
      Provider<TokenManager> tokenManagerProvider, Provider<Moshi> moshiProvider) {
    return new DeviceRepositoryImpl_Factory(daoProvider, apiServiceProvider, statusApiServiceProvider, tokenManagerProvider, moshiProvider);
  }

  public static DeviceRepositoryImpl newInstance(DeviceDao dao, SystemMonitorApiService apiService,
      SystemMonitorApiService statusApiService, TokenManager tokenManager, Moshi moshi) {
    return new DeviceRepositoryImpl(dao, apiService, statusApiService, tokenManager, moshi);
  }
}
